#!/usr/bin/env bash
# Modified for WorkPause: artifact names, permission command, and build-tool discovery.
set -euo pipefail
cd "$(dirname "$0")"

export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}"
export ANDROID_HOME="$ANDROID_SDK_ROOT"
BUILD_TOOLS="$(find "$ANDROID_SDK_ROOT/build-tools" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -1)"
export PATH="$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/emulator:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$BUILD_TOOLS:$PATH"

echo "==> 1/4 Ensure Gradle wrapper (gradle-8.10.2)"
if [ ! -f gradlew ]; then
  # --no-validate-url: skip the distribution-URL connectivity probe, which can
  # fail behind CDNs/proxies even when the distribution is reachable/cached.
  gradle wrapper --gradle-version 8.10.2 --distribution-type bin --no-validate-url --no-daemon
fi

echo "==> 2/4 Build debug APK"
./gradlew :app:assembleDebug --no-daemon

echo "==> 3/4 Copy APK to project root"
cp app/build/outputs/apk/debug/app-debug.apk ./WorkPause-debug.apk
echo "    APK: $(ls -lh WorkPause-debug.apk | awk '{print $5}')"

echo "==> 4/4 Verify signature + badging"
apksigner verify --print-certs WorkPause-debug.apk
aapt2 dump badging WorkPause-debug.apk | grep -E "package:|targetSdkVersion|uses-permission"

cat <<EOF

============================================================
 Build complete: $(pwd)/WorkPause-debug.apk

 Install + enable silent mode (one time):
   adb install -r WorkPause-debug.apk
   adb shell pm grant io.github.marinfrankovic.workpause android.permission.MODIFY_QUIET_MODE
============================================================
EOF
