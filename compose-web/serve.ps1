Set-Location "C:\chtor\compose-web"
$root = "build\dist\wasmJs\developmentExecutable"
$port = 8088
$prefix = "http://localhost:$port/"

# Try to stop any existing listener on the port
Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue | ForEach-Object {
    try { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue } catch {}
}

$listener = New-Object System.Net.HttpListener
$listener.Prefixes.Add($prefix)
$listener.Start()
Write-Host "Chator dev server: http://localhost:$port/  (root=$root)"

$mime = @{
    '.html' = 'text/html; charset=utf-8'
    '.js'   = 'application/javascript; charset=utf-8'
    '.mjs'  = 'application/javascript; charset=utf-8'
    '.wasm' = 'application/wasm'
    '.json' = 'application/json'
    '.css'  = 'text/css; charset=utf-8'
    '.png'  = 'image/png'
    '.svg'  = 'image/svg+xml'
    '.ico'  = 'image/x-icon'
    '.map'  = 'application/json'
    '.txt'  = 'text/plain; charset=utf-8'
}

while ($listener.IsListening) {
    $context  = $listener.GetContext()
    $req      = $context.Request
    $url      = $req.Url.AbsolutePath.TrimStart('/')
    if ($url -eq '') { $url = 'index.html' }
    $filePath = Join-Path $root $url

    if (-not (Test-Path $LiteralPath $filePath)) {
        # SPA fallback
        $filePath = Join-Path $root 'index.html'
    }

    try {
        $bytes  = [System.IO.File]::ReadAllBytes($filePath)
        $ext    = [System.IO.Path]::GetExtension($filePath)
        $type   = if ($mime.ContainsKey($ext)) { $mime[$ext] } else { 'application/octet-stream' }
        $resp   = $context.Response
        $resp.ContentType    = $type
        $resp.ContentLength64 = $bytes.Length
        $resp.Headers.Add('Cache-Control', 'no-store')
        $resp.OutputStream.Write($bytes, 0, $bytes.Length)
        $resp.Close()
    } catch {
        $context.Response.StatusCode = 500
        $context.Response.Close()
    }
}
