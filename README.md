# TWS Macro/Control

Android app for controlling SoundPEATS Air3 Pro TWS earbuds with MacroDroid (or anything) via GAIA v4 protocol over SPP/RFCOMM.

**Package**: `foxlost.tws.macro`

## Features

- SPP RFCOMM connection to paired TWS device
- ANC mode switching: Normal / Strong ANC / Transparency
- Game Mode toggle
- Touch Control toggle
- 9 EQ presets with per-band GAIA encoding (Classic, Bass Boost, Bass Reduction, Electronic, Popular, Classical, Rock, Folk, Treble)
- Custom EQ: 8-band gain slider
- Battery level display (L/R)
- Device name and MAC detection from bonded devices
- Factory Reset with confirmation
- MacroDroid intent receiver for automation

## Architecture

```
SppController.kt     - SPP connection, GAIA protocol, EQ send, response parsing
MainActivity.kt      - UI layout, button handlers, dynamic device info
TwsIntentReceiver.kt - MacroDroid broadcast receiver (singleton or own SPP)
EqData.kt            - Shared EQ preset data and packet builder
```

## Build

```
set JAVA_HOME=C:\Program Files\Java\jdk-17
set ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk
gradlew.bat assembleDebug --no-daemon
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Install & Run

```
adb install -r "path\to\app-debug.apk"
adb shell "am force-stop com.thirtydays.headset; am start -n foxlost.tws.macro/.MainActivity"
```

## MacroDroid Integration

Broadcast intent with action `foxlost.tws.macro.SET_MODE`, extra key `mode`.

| Value | Function |
|-------|----------|
| `anc_normal`, `anc_strong`, `anc_transparency` | ANC modes |
| `game_on`, `game_off` | Game mode |
| `touch_on`, `touch_off` | Touch control |
| `eq_classic` through `eq_treble` | EQ presets |
| `reset` | Factory reset |
| `get_battery` | Query battery |
| `raw:HEX` | Raw GAIA packet |

Works with or without the app open.

## Protocol

GAIA v4 over SPP/RFCOMM (not BLE GATT). Packet format:

```
FF 04 00 [len] [vendor_hi] [vendor_lo] [cmd_hi] [cmd_lo] [payload...]
```

Vendor `0x000A` for standard commands, `0x001D` for EQ. Response has bit 15 set on command byte.

See `docs/gaia-protocol.md` for full command table.

## Documentation

| File | Content |
|------|---------|
| `docs/device.md` | TWS specs, chipset, firmware, phone config |
| `docs/gaia-protocol.md` | GAIA v4 packet format, command table |
| `docs/eq-presets.md` | 9 EQ presets with band data and encoding |
| `docs/macrodroid.md` | MacroDroid intent reference |
| `docs/adb-reference.md` | ADB commands |
| `docs/app-build.md` | Build instructions, architecture |
| `docs/project-history.md` | Development timeline |

## Fork
Just fork and make change that works with your devices, do what ever you want :)

## License

Free Time and Free Will by FoxLost
