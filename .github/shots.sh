#!/usr/bin/env bash
# Installs the debug APK on the running emulator and screenshots the main screens (see Demo.kt for the routes).
set -u
APK=$1; OUT=$2
mkdir -p "$OUT"
adb wait-for-device
adb shell settings put global window_animation_scale 1
adb shell cmd uimode night yes || true
adb install -r -g "$APK"
PKG=app.forge.gym
shot() {
  local name=$1 route=$2 wait=${3:-9} demo=${4:-true}
  adb shell am force-stop $PKG
  adb shell am start -W -n $PKG/.MainActivity --ez forge_demo $demo --es forge_route "$route" > /dev/null
  sleep "$wait"
  adb exec-out screencap -p > "$OUT/$name.png"
  echo "shot $name ($(stat -c %s "$OUT/$name.png") bytes)"
}
# first launch seeds the demo data
shot warmup tab:0 12
shot home tab:0
shot explore tab:1
shot body tab:1:body
shot exercise exercise:Barbell_Full_Squat 24
shot exercise_home exercise:Goblet_Squat 24
adb logcat -c
shot viewer viewer:Goblet_Squat 45
adb logcat -d > "$OUT/viewer_logcat.txt" 2>&1 || true
adb shell dumpsys SurfaceFlinger | grep -i "GLES" > "$OUT/gpu.txt" 2>&1 || true
shot plan plan
shot workout session:0 22
shot walk tab:2
shot progress tab:3
shot settings settings
adb shell pm clear $PKG > /dev/null
shot onboarding onboarding 8 false
rm -f "$OUT/warmup.png"
