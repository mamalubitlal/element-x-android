// Tiny static file server for Chator PWA dev builds.
// Serves build\dist\wasmJs\developmentExecutable on http://localhost:8088/.
// No deps — uses Node's built-in `http` and `fs`.

const http = require('http');
const fs   = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, 'build', 'dist', 'wasmJs', 'developmentExecutable');
const PORT = 8090;

const MIME = {
    '.html': 'text/html; charset=utf-8',
    '.js':   'application/javascript; charset=utf-8',
    '.mjs':  'application/javascript; charset=utf-8',
    '.wasm': 'application/wasm',
    '.json': 'application/json',
    '.css':  'text/css; charset=utf-8',
    '.png':  'image/png',
    '.svg':  'image/svg+xml',
    '.ico':  'image/x-icon',
    '.map':  'application/json',
    '.txt':  'text/plain; charset=utf-8',
    '.webp': 'image/webp',
};

const server = http.createServer((req, res) => {
    let urlPath = decodeURIComponent(req.url.split('?')[0]).replace(/^\/+/, '');
    if (urlPath === '') urlPath = 'index.html';
    let filePath = path.join(ROOT, urlPath);
    if (!filePath.startsWith(ROOT)) { res.writeHead(403); return res.end('forbidden'); }
    if (!fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) {
        filePath = path.join(ROOT, 'index.html');  // SPA fallback
    }
    try {
        const buf = fs.readFileSync(filePath);
        const ext = path.extname(filePath).toLowerCase();
        res.writeHead(200, {
            'Content-Type':    MIME[ext] || 'application/octet-stream',
            'Content-Length':  buf.length,
            'Cache-Control':   'no-store',
            'Cross-Origin-Opener-Policy':   'same-origin',
            'Cross-Origin-Embedder-Policy': 'require-corp',
        });
        res.end(buf);
    } catch (e) {
        res.writeHead(500);
        res.end(String(e));
    }
});

server.listen(PORT, '127.0.0.1', () => {
    console.log(`Chator dev server: http://localhost:${PORT}/  (root=${ROOT})`);
});
