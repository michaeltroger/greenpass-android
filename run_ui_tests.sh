#!/bin/sh

rm -rd screenshots | true

adb root
adb wait-for-device

adb shell rm -f -rR /sdcard/Pictures/screenshots
adb shell rm -f -rR /sdcard/Pictures/error_screenshots

adb shell mkdir /sdcard/testdata
while [ $? -ne 0 ]; do !!; sleep 1; done
adb push testdata /sdcard/

adb shell settings put global sysui_demo_allowed 1
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1200
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
adb shell am broadcast -a com.android.systemui.demo -e command network -e mobile show -e datatype none -e level 4
adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4
adb shell am broadcast -a com.android.systemui.demo -e command battery -e plugged false
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100
adb shell am broadcast -a com.android.systemui.demo -e command network -e mobile show -e datatype none -e level 4

./gradlew connectedDebugAndroidTest
exit_code=$?

adb pull /sdcard/Pictures/error_screenshots app/build
adb pull /sdcard/Pictures/screenshots

exit $exit_code
