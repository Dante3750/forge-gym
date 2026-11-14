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
# first launch seeds a demo name and a few past workouts
shot warmup tab:0 14
shot workout tab:0
shot exercises tab:1
shot chest home:chest
shot profile tab:2
shot live live 12
adb shell input tap 1000 1090; sleep 2; adb exec-out screencap -p > "$OUT/live_tick.png"
shot routine routine
shot body body 14
adb shell input tap 540 1060; sleep 3; adb exec-out screencap -p > "$OUT/body_tap.png"
adb logcat -c
shot exercise exercise:Barbell_Full_Squat 30
adb logcat -d | grep -E "ForgeViewer|Filament|AndroidRuntime" > "$OUT/exercise_logcat.txt" 2>&1 || true
adb logcat -c
shot viewer viewer:Goblet_Squat 45
adb logcat -d > "$OUT/viewer_logcat.txt" 2>&1 || true
adb shell pm clear $PKG > /dev/null
shot welcome welcome 8 false
rm -f "$OUT/warmup.png"
