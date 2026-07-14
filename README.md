# TWS Macro/Control

Cross-platform app for controlling SoundPEATS Air3 Pro TWS earbuds via GAIA v4 protocol over SPP/RFCOMM.

**Android Package**: `foxlost.tws.macro`

## Platforms

| Platform | Project | Build |
|----------|---------|-------|
| Android | `TWSControl/` | `gradlew.bat assembleDebug` |
| Windows | `TWSControlWin/` | `dotnet publish -c Release -r win-x64 --self-contained true -p:PublishSingleFile=true` |

## Features

- Auto-discover paired TWS devices (no hardcoded MAC)
- SPP RFCOMM connection
- ANC mode: Normal / Strong / Transparency
- Game Mode toggle
- Touch Control toggle
- 9 EQ presets with per-band GAIA encoding (Classic, Bass Boost, Bass Reduction, Electronic, Popular, Classical, Rock, Folk, Treble)
- Custom EQ: 8-band gain slider (-12dB to +12dB)
- Battery level display (L/R)
- Factory Reset with confirmation
- MacroDroid intent receiver for automation (Android)
- Adaptive icon (Android)

## Android Build

```
set JAVA_HOME=C:\Program Files\Java\jdk-17
set ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk
gradlew.bat assembleDebug --no-daemon
```

APK: `TWSControl/app/build/outputs/apk/debug/app-debug.apk`

## Windows Build

```powershell
cd TWSControlWin
dotnet build                           # Debug build
dotnet publish -c Release -r win-x64 --self-contained true -p:PublishSingleFile=true  # .exe
```

EXE: `TWSControlWin/publish/TWSControlWin.exe`

Requires .NET 8.0 SDK, Windows with Bluetooth Classic (BR/EDR) support. TWS must be paired via Windows Bluetooth Settings first.

## Install (Android)

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

Works with or without the app open. See `docs/macrodroid-examples.md` for macro setup examples.

## Protocol

GAIA v4 over SPP/RFCOMM (not BLE GATT). Packet format:

```
FF 04 00 [len] [vendor_hi] [vendor_lo] [cmd_hi] [cmd_lo] [payload...]
```

Vendor `0x000A` for standard commands, `0x001D` for EQ. Response has bit 15 set on command byte.

EQ per-band encoding:
- Header: `(bandCount << 4) | (bandIndex + 1)`
- masterGain: `(int)(masterGain * 60)` as 16-bit BE signed
- freq: `(int)(freq * 3)` as 16-bit BE
- gain: `(int)(gain * 60)` as 16-bit BE signed
- Q: `(int)(Q * 4096)` as 16-bit BE

See `docs/gaia-protocol.md` for full command table.

## Documentation

| File | Content |
|------|---------|
| `docs/device.md` | TWS specs, chipset, firmware, phone config |
| `docs/gaia-protocol.md` | GAIA v4 packet format, command table |
| `docs/eq-presets.md` | 9 EQ presets with band data and encoding |
| `docs/macrodroid.md` | MacroDroid intent reference |
| `docs/macrodroid-examples.md` | Macro setup examples |
| `docs/adb-reference.md` | ADB commands |
| `docs/app-build.md` | Build instructions, architecture |
| `docs/project-history.md` | Development timeline |

## Fork

Just fork and make changes that work with your devices.

## License

Free Time and Free Will by FoxLost
