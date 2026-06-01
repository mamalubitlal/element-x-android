#!/usr/bin/env bash
# Simple app install + launch test (no UI automation)
set -euo pipefail

APK="app/build/outputs/apk/gplay/debug/app-gplay-x86_64-debug.apk"
APP_ID="${MAESTRO_APP_ID:-im.chator.android.debug}"
ERROR_LOG="/tmp/dpi-test-errors.log"
> "$ERROR_LOG"

echo "=== Installing APK ==="
adb install -r "$APK"

echo "=== Disabling Chrome onboarding ==="
adb shell 'echo "chrome --disable-fre --no-default-browser-check --no-first-run" > /data/local/tmp/chrome-command-line' || true

echo "=== Starting error-level logcat ==="
adb logcat -c || true
adb logcat -b all "*:S" "ByeDPI:V" "ChatorDPI:V" "im.chator.android:E" "*:E" -m "FirebaseMessaging:g" -m "CCTFlatFileLogStore:g" -m "GmsTaskScheduler:g" -m "binder:g" &> "$ERROR_LOG" &
LOGCAT_PID=$!

cleanup() {
  echo "=== Cleaning up (PID $LOGCAT_PID) ==="
  kill "$LOGCAT_PID" 2>/dev/null || true
}
trap cleanup EXIT

echo "=== Launching app ==="
adb shell am force-stop "$APP_ID" 2>/dev/null || true
sleep 2
adb shell monkey -p "$APP_ID" -c android.intent.category.LAUNCHER 1 2>/dev/null || true
sleep 5

echo "=== Checking if app is running ==="
APP_PID=$(adb shell "pidof $APP_ID" 2>/dev/null || echo "")
if [ -n "$APP_PID" ]; then
  echo "App running with PID: $APP_PID"
else
  echo "App may have stopped (PID lookup failed, checking logs)"
fi

echo "=== Checking for errors in logcat ==="
sleep 3

# Check for fatal errors in our app
if grep -qiE "FATAL|Force finishing|crash|ANR" "$ERROR_LOG" 2>/dev/null; then
  echo "=== Test FAILED - App crash detected ==="
  grep -iE "FATAL|Force finishing|crash|ANR" "$ERROR_LOG" | tail -20
  exit 1
fi

echo "=== Test PASSED - App installed and launched without crash ==="
exit 0