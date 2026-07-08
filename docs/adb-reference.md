# ADB Commands Reference

## Device Info

```bash
adb devices
```

## Force Stop Apps

```bash
# Stop TWS Macro/Control
adb -s 95bba8027d27 shell "am force-stop foxlost.tws.macro"

# Stop SoundPEATS app (MUST do before connecting TWS Macro/Control)
adb -s 95bba8027d27 shell "am force-stop com.thirtydays.headset"
```

## Launch App

```bash
adb -s 95bba8027d27 shell "am start -n foxlost.tws.macro/.MainActivity"
```

## Install & Run

```bash
adb -s 95bba8027d27 install -r "C:\data data data data\TWS-Soundpeats\TWSControl\app\build\outputs\apk\debug\app-debug.apk"
adb -s 95bba8027d27 shell "am force-stop com.thirtydays.headset; am start -n foxlost.tws.macro/.MainActivity"
```

## btsnoop Capture

```bash
# Enable btsnoop
adb -s 95bba8027d27 shell "su -c 'setprop persist.bluetooth.btsnooplogfilter 0xFFFF'"

# Restart Bluetooth (starts fresh log)
adb -s 95bba8027d27 shell "su -c 'svc bluetooth disable'; sleep 2; su -c 'svc bluetooth enable'"

# Pull btsnoop log
adb -s 95bba8027d27 pull /data/misc/bluetooth/logs/btsnoop_hci.log

# Pull rotated log
adb -s 95bba8027d27 pull /data/misc/bluetooth/logs/btsnoop_hci.log.last
```

## UI Automation

```bash
# Dump UI hierarchy
adb -s 95bba8027d27 shell "uiautomator dump /data/local/tmp/ui.xml"

# Tap coordinate
adb -s 95bba8027d27 shell "input tap X Y"
```

## Query SoundPEATS Database

```bash
adb -s 95bba8027d27 shell "echo 'SELECT * FROM preset_eqs;' | su -c 'sqlite3 /data/user/0/com.thirtydays.headset/databases/pa_database'"
adb -s 95bba8027d27 shell "echo 'SELECT * FROM device_feature_states;' | su -c 'sqlite3 /data/user/0/com.thirtydays.headset/databases/pa_database'"
```

## Logcat

```bash
# Filter SppController logs
adb -s 95bba8027d27 logcat -s "SppController"

# Filter TwsIntentReceiver logs
adb -s 95bba8027d27 logcat -s "TwsIntentReceiver"

# Clear buffer
adb -s 95bba8027d27 logcat -c
```

## Send MacroDroid Intent via ADB

```bash
adb shell "am broadcast -a foxlost.tws.macro.SET_MODE --es mode anc_strong -n foxlost.tws.macro/.TwsIntentReceiver"
adb shell "am broadcast -a foxlost.tws.macro.SET_MODE --es mode eq_folk -n foxlost.tws.macro/.TwsIntentReceiver"
```
