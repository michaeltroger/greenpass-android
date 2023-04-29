#!/bin/sh

adb root
adb wait-for-device

adb shell rm -f -rR /sdcard/Pictures/screenshots
adb shell rm -f -rR /sdcard/Pictures/error_screenshots

adb push testdata/dummy.pdf /sdcard/dummy.pdf

adb shell settings put global sysui_demo_allowed 1
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1200
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false

./gradlew connectedCheck
exit_code=$?

adb pull /sdcard/Pictures/error_screenshots app/build | true
adb pull /sdcard/Pictures/screenshots | true

exit $exit_code
