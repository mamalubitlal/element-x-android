#!/usr/bin/env bash
# Runs the app, tests login with DPI bypass, retries on connection failure.
# Outputs only error-level logcat to keep logs unbloated.
set -euo pipefail

APK="app/build/outputs/apk/gplay/debug/app-gplay-x86_64-debug.apk"
APP_ID="${MAESTRO_APP_ID:-im.chator.android.debug}"
MAX_RETRIES=10          # max connection attempts before giving up
RETRY_INTERVAL=30       # seconds between retries
ERROR_LOG="/tmp/dpi-test-errors.log"
> "$ERROR_LOG"

echo "=== Installing APK ==="
adb install -r "$APK"

echo "=== Disabling Chrome onboarding ==="
adb shell 'echo "chrome --disable-fre --no-default-browser-check --no-first-run" > /data/local/tmp/chrome-command-line'

echo "=== Starting error-level logcat ==="
# Clear any existing logcat, then stream only errors and fatals
adb logcat -c || true
adb logcat -b all -e "^(E|F)/" --pid="$(adb shell ps | grep "$APP_ID" | awk 'NR==1{print $2}')" &> "$ERROR_LOG" &
LOGCAT_PID=$!

cleanup() {
  echo "=== Cleaning up logcat (PID $LOGCAT_PID) ==="
  kill "$LOGCAT_PID" 2>/dev/null || true
}
trap cleanup EXIT

start_logcat_filtered() {
  # Restart logcat targeting only our app's errors
  local PID
  PID=$(adb shell "pidof $APP_ID" 2>/dev/null || echo "")
  if [ -n "$PID" ]; then
    adb logcat -b all --pid="$PID" -e "^(E|F)/" &>> "$ERROR_LOG" &
    LOGCAT_PID=$!
  else
    # Fallback: grep for the tag
    adb logcat -b all "*:S" "ByeDPI:V" "ChatorDPI:V" "*:E" &>> "$ERROR_LOG" &
    LOGCAT_PID=$!
  fi
}

echo "=== Running DPI Bypass Test (attempt 1 of $MAX_RETRIES) ==="
ATTEMPT=0
SUCCESS=false

while [ $ATTEMPT -lt $MAX_RETRIES ]; do
  ATTEMPT=$((ATTEMPT + 1))
  echo "[Attempt $ATTEMPT/$MAX_RETRIES] Launching app and testing..."

  # Start the app fresh
  adb shell am force-stop "$APP_ID"
  sleep 3
  adb shell monkey -p "$APP_ID" -c android.intent.category.LAUNCHER 1 2>/dev/null || true
  sleep 10

  # Restart filtered logcat for this app instance
  kill "$LOGCAT_PID" 2>/dev/null || true
  start_logcat_filtered

  # Run the DPI bypass flow (login + DPI toggle)
  # Use continue-on-failure so we can detect and retry
  set +e
  ~/.maestro/bin/maestro test .maestro/dpiBypassTest.yaml \
    -e MAESTRO_USERNAME="$MAESTRO_USERNAME" \
    -e MAESTRO_PASSWORD="$MAESTRO_PASSWORD" \
    -e MAESTRO_RECOVERY_KEY="${MAESTRO_RECOVERY_KEY:-}" \
    -e MAESTRO_APP_ID="$APP_ID" 2>&1 | tee /tmp/maestro-output.log
  MAESTRO_EXIT=$?
  set -e

  if [ $MAESTRO_EXIT -eq 0 ]; then
    SUCCESS=true
    echo "=== DPI Bypass Test SUCCEEDED ==="
    break
  fi

  # Check if failure was due to connectivity issues
  if grep -qiE "can't connect|timeout|connection refused|network" /tmp/maestro-output.log 2>/dev/null; then
    echo "[Attempt $ATTEMPT] Connection failure detected. Retrying in ${RETRY_INTERVAL}s..."
    if [ "$ATTEMPT" -lt "$MAX_RETRIES" ]; then
      sleep "$RETRY_INTERVAL"
    else
      echo "[Attempt $ATTEMPT] Max retries reached. Giving up."
    fi
    continue
  fi

  # Non-connectivity failure — fail immediately
  echo "=== DPI Bypass Test FAILED (non-connectivity error) ==="
  grep -iE "^(E|F)/" /tmp/maestro-output.log >> "$ERROR_LOG" 2>/dev/null || true
  break
done

echo ""
echo "=== Summary ==="
if [ "$SUCCESS" = true ]; then
  echo "DPI bypass test passed on attempt $ATTEMPT."
  exit 0
else
  echo "DPI bypass test failed after $ATTEMPT attempt(s)."
  echo "Errors (filtered):"
  if [ -f "$ERROR_LOG" ] && [ -s "$ERROR_LOG" ]; then
    # De-duplicate and show only error lines
    grep -h "^(E|F)/" "$ERROR_LOG" 2>/dev/null | sort -u | tail -50 || \
      tail -50 "$ERROR_LOG"
  else
    echo "(no error log generated)"
  fi
  exit 1
fi
