Set-Location "C:\chtor\compose-web"
.\gradlew.bat :compose-web:cleanWasmJsBrowserDevelopmentWebpack wasmJsBrowserDevelopmentWebpack --no-daemon 2>&1 | Tee-Object -FilePath "C:\chtor\compose-web\build-rebuild2.log"
"EXIT=$LASTEXITCODE" | Add-Content "C:\chtor\compose-web\build-rebuild2.log"
