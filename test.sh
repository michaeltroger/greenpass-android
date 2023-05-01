adb shell mkdir /sdcsdfsdfard/testdata
# accessing sdcard on emulator is sometimes flaky, try again until it works:
while [ $? -ne 0 ]; do !!; sleep 1; done
