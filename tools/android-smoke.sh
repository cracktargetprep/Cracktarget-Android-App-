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
cleanup() {
    adb_device logcat -d > "$out/adb.log" 2>/dev/null || true
    kill "$emulator_pid" 2>/dev/null || true
    if [[ -n "${listing_proxy_pid:-}" ]]; then kill "$listing_proxy_pid" 2>/dev/null || true; fi
}
trap cleanup EXIT

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
dump_view() {
    adb_device shell uiautomator dump /sdcard/ct-qa.xml >/dev/null 2>&1 &&
        adb_device pull /sdcard/ct-qa.xml "$1" >/dev/null
}

tap_label() {
    local label=$1 class=${2:-}
    local center
    dump_view "$out/tap.xml"
    center=$(python3 - "$out/tap.xml" "$label" "$class" <<'PYTHON'
from xml.etree import ElementTree
import re
import sys

xml, label, widget = sys.argv[1:]
for node in ElementTree.parse(xml).iter("node"):
    if node.get("package") != "com.cracktarget.app" or (widget and node.get("class") != widget):
        continue
    if node.get("text") == label or (node.get("content-desc") or "").startswith(label + ","):
        left, top, right, bottom = map(int, re.findall(r"\d+", node.get("bounds")))
        print((left + right) // 2, (top + bottom) // 2)
        break
else:
    raise SystemExit(f"Missing tap target: {label}")
PYTHON
)
    read -r x y <<< "$center"
    adb_device shell input tap "$x" "$y"
}

expect_screen() {
    local marker=$1 destination=$2
    for _ in {1..12}; do
        if dump_view "$destination" && grep -iqE "$marker" "$destination"; then return 0; fi
        sleep 2
    done
    echo "Screen marker not found: $marker" >&2
    return 1
}

if [[ "${CT_CAPTURE_PLAY_LISTING:-0}" == 1 ]]; then
    mkdir -p "$out/listing"
    adb_device shell wm size 1080x1920
    python3 tools/android-listing-proxy.py > "$out/listing-network.txt" 2>&1 &
    listing_proxy_pid=$!
    curl --fail --silent --show-error --retry 3 --retry-connrefused --max-time 20 \
        --proxy http://127.0.0.1:18888 -o /dev/null https://cracktarget.com/
    adb_device reverse tcp:18888 tcp:18888
    adb_device shell settings put global http_proxy 127.0.0.1:18888
    adb_device shell am force-stop "$app"
    adb_device shell am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n "$app/.MainActivity"

    capture() {
        local filename=$1 marker=$2
        expect_screen "$marker" "$out/listing-$filename.xml"
        if grep -iqE 'text="(Checkout|Add to cart|Buy now)"' "$out/listing-$filename.xml"; then
            echo "Unexpected commerce element in Play edition: $filename" >&2
            return 1
        fi
        adb_device exec-out screencap -p > "$out/listing/$filename.png"
    }

    capture "01-home" 'text="Quick access"'
    tap_label "Study" android.widget.Button
    capture "02-study" 'text="All free study material"'
    tap_label "Tests" android.widget.Button
    capture "03-mock-tests" 'text="Free practice"'
    tap_label "Updates" android.widget.Button
    expect_screen 'text="Exam updates"' "$out/updates.xml"
    tap_label "Current affairs"
    capture "04-current-affairs" 'text="Latest daily edition"'
    tap_label "Updates" android.widget.Button
    expect_screen 'text="Exam updates"' "$out/updates.xml"
    tap_label "Vacancy notices"
    capture "05-vacancies" 'text="Railway Recruitment Boards|text="Uttar Pradesh Public Service Commission|text="Union Public Service Commission|text="Export-Import Bank|text="Staff Selection Commission'
    tap_label "Home" android.widget.Button
    expect_screen 'text="Quick access"' "$out/home.xml"
    tap_label "Exclusive books"
    capture "06-books" 'text="My book downloads"'
    tap_label "Account" android.widget.Button
    capture "07-account" 'text="Delete account"'
    adb_device shell settings put global http_proxy :0
    adb_device reverse --remove tcp:18888
    adb_device shell wm size reset
fi

adb_device shell cmd connectivity airplane-mode enable
adb_device shell svc wifi disable
adb_device shell svc data disable

for mode in threebutton gestural; do
    overlay="com.android.internal.systemui.navbar.$mode"
    adb_device shell cmd overlay enable-exclusive --category "$overlay"
    adb_device shell cmd overlay list android | grep -F "[x] $overlay"
    # The overlay is enabled before System UI finishes applying new insets.
    sleep 3
    adb_device shell am force-stop "$app"
    adb_device shell am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n "$app/.MainActivity"
    for _ in {1..10}; do
        if adb_device shell uiautomator dump /sdcard/ct-qa.xml >/dev/null 2>&1; then
            adb_device pull /sdcard/ct-qa.xml "$out/$mode.xml" >/dev/null
            if grep -Fq 'text="Quick access"' "$out/$mode.xml"; then break; fi
        fi
        sleep 2
    done
    # Pair the final accessibility bounds with the settled frame.
    sleep 2
    adb_device shell uiautomator dump /sdcard/ct-qa.xml >/dev/null
    adb_device pull /sdcard/ct-qa.xml "$out/$mode.xml" >/dev/null
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
    assert any(n.get('text') == 'Quick access' for n in nodes), f'{mode}: offline native home missing'
    bounds = [nav[label] for label in ('Home', 'Study', 'Tests', 'Updates', 'Account')]
    assert all(x0 < x1 and y0 < y1 < display_bottom for x0, y0, x1, y1 in bounds), (mode, bounds, display_bottom)
    assert all(left[2] <= right[0] for left, right in zip(bounds, bounds[1:])), (mode, bounds)
    assert len(set(rect[-1] for rect in bounds)) == 1, (mode, bounds)
    levels[mode] = display_bottom - bounds[0][-1]
assert levels['threebutton'] > levels['gestural'] > 0, levels
print('Native home launched offline. Five navigation controls stayed above both system navigation modes.', levels)
PY


tap_label "Study" android.widget.Button
expect_screen 'text="All free study material"' "$out/offline-study.xml"
tap_label "All free study material"
expect_screen 'text="Retry"' "$out/offline-reader.xml"
adb_device shell input keyevent 4
expect_screen 'text="Study by exam"' "$out/offline-back.xml"
echo "The offline reader showed Retry and Android Back returned to the native study screen."
