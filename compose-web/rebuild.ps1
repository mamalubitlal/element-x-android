Set-Location "C:\chtor\compose-web"
.\gradlew.bat wasmJsBrowserDevelopmentWebpack --no-daemon 2>&1 | Tee-Object -FilePath "C:\chtor\compose-web\build-rebuild.log"
"EXIT=$LASTEXITCODE" | Add-Content "C:\chtor\compose-web\build-rebuild.log"
