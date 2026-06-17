Set-Location "C:\chtor\compose-web"
.\gradlew.bat clean wasmJsBrowserDevelopmentWebpack --no-daemon 2>&1 | Tee-Object -FilePath "C:\chtor\compose-web\build-rebuild3.log"
"EXIT=$LASTEXITCODE" | Add-Content "C:\chtor\compose-web\build-rebuild3.log"
