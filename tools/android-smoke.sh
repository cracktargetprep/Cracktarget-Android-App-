#!/usr/bin/env bash
set -euo pipefail

adb_bin="$ANDROID_HOME/platform-tools/adb"
emulator="$ANDROID_HOME/emulator/emulator"
app=com.cracktarget.app
apk=${CT_TEST_APK:-app/build/outputs/apk/debug/app-debug.apk}
serial=emulator-5554
out="$RUNNER_TEMP/cracktarget-smoke"
avd_name=cracktarget_qa
export ANDROID_USER_HOME="$RUNNER_TEMP/cracktarget-android"
export ANDROID_AVD_HOME="$RUNNER_TEMP/cracktarget-avd"
unset ANDROID_SDK_HOME
mkdir -p "$out" "$ANDROID_USER_HOME" "$ANDROID_AVD_HOME"

adb_device() { timeout 35 "$adb_bin" -s "$serial" "$@"; }

printf 'no\n' | avdmanager create avd --force --name "$avd_name" --path "$ANDROID_AVD_HOME/$avd_name.avd" \
    --package 'system-images;android-36;google_apis;x86_64' --device pixel_6
"$emulator" -list-avds | grep -Fx "$avd_name"
"$adb_bin" start-server
"$emulator" -avd "$avd_name" -no-window -no-audio -no-snapshot -no-boot-anim -no-metrics \
    -gpu swiftshader_indirect -port 5554 > "$out/emulator.log" 2>&1 &
emulator_pid=$!
trap 'adb_device logcat -d > "$out/adb.log" 2>/dev/null || true; kill "$emulator_pid" 2>/dev/null || true' EXIT

for _ in {1..90}; do
    if "$adb_bin" -s "$serial" get-state 2>/dev/null | grep -Fxq device; then break; fi
    if ! kill -0 "$emulator_pid" 2>/dev/null; then cat "$out/emulator.log"; exit 1; fi
    sleep 2
done
"$adb_bin" -s "$serial" get-state
for _ in {1..80}; do
    if [[ "$(adb_device shell getprop sys.boot_completed | tr -d '\r')" == 1 ]]; then break; fi
    sleep 2
done
[[ "$(adb_device shell getprop sys.boot_completed | tr -d '\r')" == 1 ]]
adb_device shell settings put global device_provisioned 1
adb_device shell settings put secure user_setup_complete 1
adb_device shell input keyevent 82
adb_device shell wm size > "$out/display.txt"
adb_device install "$apk"
adb_device shell cmd connectivity airplane-mode enable
adb_device shell svc wifi disable
adb_device shell svc data disable

for mode in threebutton gestural; do
    overlay="com.android.internal.systemui.navbar.$mode"
    adb_device shell cmd overlay enable-exclusive --category "$overlay"
    adb_device shell cmd overlay list android | grep -F "[x] $overlay"
    adb_device shell am force-stop "$app"
    adb_device shell am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n "$app/.MainActivity"
    for _ in {1..10}; do
        if adb_device shell uiautomator dump /sdcard/ct-qa.xml >/dev/null 2>&1; then
            adb_device pull /sdcard/ct-qa.xml "$out/$mode.xml" >/dev/null
            if grep -Fq 'text="Retry"' "$out/$mode.xml"; then break; fi
        fi
        sleep 2
    done
    adb_device exec-out screencap -p > "$out/$mode.png"
done

python3 - "$out" <<'PY'
from pathlib import Path
import re
import sys
from xml.etree import ElementTree

out = Path(sys.argv[1])
levels = {}
for mode in ('threebutton', 'gestural'):
    nodes = list(ElementTree.parse(out / f'{mode}.xml').iter('node'))
    display_bottom = int(re.findall(r'\d+x(\d+)', (out / 'display.txt').read_text())[-1])
    buttons = [n for n in nodes if n.get('class') == 'android.widget.Button' and n.get('package') == 'com.cracktarget.app']
    nav = {n.get('text'): tuple(map(int, re.findall(r'\d+', n.get('bounds')))) for n in buttons}
    assert 'Retry' in nav, f'{mode}: offline Retry missing'
    bounds = [nav[label] for label in ('Home', 'Study', 'Tests', 'Affairs', 'Account')]
    assert all(x0 < x1 and y0 < y1 < display_bottom for x0, y0, x1, y1 in bounds), (mode, bounds, display_bottom)
    assert all(left[2] <= right[0] for left, right in zip(bounds, bounds[1:])), (mode, bounds)
    assert len(set(rect[-1] for rect in bounds)) == 1, (mode, bounds)
    levels[mode] = display_bottom - bounds[0][-1]
assert levels['threebutton'] > levels['gestural'] > 0, levels
print('Installed and launched. Five navigation controls stayed above both system navigation modes. Offline Retry was present.', levels)
PY


if [[ "${CT_CAPTURE_PLAY_LISTING:-0}" == 1 ]]; then
    mkdir -p "$out/listing"
    adb_device shell cmd connectivity airplane-mode disable
    adb_device shell svc wifi enable
    adb_device shell svc data enable
    adb_device shell am force-stop "$app"
    adb_device shell am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n "$app/.MainActivity"

    capture() {
        local label=$1 filename=$2 marker=$3
        for _ in {1..10}; do
            if [[ -n "$label" ]] && adb_device shell uiautomator dump /sdcard/ct-listing.xml >/dev/null 2>&1; then
                adb_device pull /sdcard/ct-listing.xml "$out/listing-view.xml" >/dev/null
                local center
                center=$(python3 - "$out/listing-view.xml" "$label" <<'PYTHON'
from xml.etree import ElementTree
import re
import sys

xml, label = sys.argv[1:]
for node in ElementTree.parse(xml).iter("node"):
    if node.get("text") == label and node.get("class") == "android.widget.Button" and node.get("package") == "com.cracktarget.app":
        left, top, right, bottom = map(int, re.findall(r"\d+", node.get("bounds")))
        print((left + right) // 2, (top + bottom) // 2)
        break
PYTHON
)
                if [[ -n "$center" ]]; then
                    adb_device shell input tap $center
                    label=""
                fi
            fi
            if adb_device shell uiautomator dump /sdcard/ct-listing.xml >/dev/null 2>&1; then
                adb_device pull /sdcard/ct-listing.xml "$out/listing-view.xml" >/dev/null
                if grep -iqE "$marker" "$out/listing-view.xml" &&
                   grep -Fq 'class="android.webkit.WebView"' "$out/listing-view.xml" &&
                   ! grep -Fq 'text="Retry"' "$out/listing-view.xml" &&
                   ! grep -iqE 'text="(Checkout|Add to cart|Buy now)"' "$out/listing-view.xml"; then
                    adb_device exec-out screencap -p > "$out/listing/$filename.png"
                    return 0
                fi
            fi
            sleep 3
        done
        echo "Online listing screenshot could not be verified: $filename" >&2
        return 1
    }

    capture "" "01-home" 'Ultimate Exam Prep|Choose a mock test|CrackTarget.*Exam Prep'
    capture "Study" "02-study" 'Study Directory.*Exams|Study Directory.*Subjects|Explore by Subject'
    capture "Tests" "03-mock-tests" 'Free Mock Tests 2026|Exam-Wise Online Practice|UPPCS 2026 Mock Test'
    capture "Account" "04-account" 'Delete account.*|Aspirant Dashboard'
    grep -Fq 'text="Delete account"' "$out/listing-view.xml"
fi
