# Project History — TWS Macro/Control

## Key Phases

1. **Windows BLE investigation** → FAILED
   - Tried to pair/connect via Windows Bluetooth LE APIs
   - Device bonding prevented BLE GATT access

2. **Phone-based BLE capture** → PARTIAL
   - Used nRF Connect, LightBlue, btsnoop
   - Found some GAIA packets over BLE but auth blocked control

3. **APK decompilation (JADX)** → SUCCESS
   - Found command IDs, packet format, GAIA v4 protocol
   - Identified `SppBluetoothService` as the actual transport

4. **TWSControl v1 (BLE GATT)** → FAILED
   - BLE GATT required authentication
   - No notification callback received

5. **SPP/RFCOMM Discovery** → BREAKTHROUGH
   - Found app uses SPP/RFCOMM, NOT BLE GATT
   - SPP worked immediately without authentication
   - All commands functional via RFCOMM channel 1

6. **TWSControl v2 (SPP/RFCOMM)** → SUCCESS
   - ANC, Game, Touch, Battery all working
   - Real-time response parsing

7. **EQ Preset Analysis**
   - v1: Sent per-band data → caused TWS disconnect
   - v2: Sent preset index (1 byte) → worked but didn't match app
   - v3 (final): Reverse-engineered per-band encoding from btsnoop
   - Encoding: `freq*3`, `gain*60`, `Q*4096`, `masterGain*60` as 16-bit BE

8. **Package Migration**
   - `com.herlan.twscontrol` → `foxlost.tws.macro`
   - Intent action: `com.herlan.tws.SET_MODE` → `foxlost.tws.macro.SET_MODE`
   - Cleaned up unnecessary files (~820 MB removed)

9. **Custom EQ + MacroDroid Support**
   - 8-band slider UI for custom EQ
   - BroadcastReceiver for MacroDroid integration
   - Dynamic device name/MAC detection

10. **Shared EqData Source**
    - Extracted EQ data into shared EqData.kt
    - TwsIntentReceiver uses same preset data as MainActivity
    - Both paths produce identical GAIA packets

11. **Singleton SPP Controller**
    - SppController.instance shared with TwsIntentReceiver
    - Intent works both with app open (singleton) and closed (own SPP connection)
    - All 17 intents (ANC, Game, Touch, EQ, Battery) verified working in both modes

12. **Color Scheme Update**
    - All active/ON buttons: green (`#4CAF50`)
    - All inactive/OFF buttons: dark blue (`#16213E`)
    - Touch toggle color changed from red to dark blue when disabled

13. **EQ Intent Fix**
    - TwsIntentReceiver EQ intent was calling `existing.write()` directly, causing disconnect
    - Fixed to delegate to `existing.setEqPreset()` — same method as MainActivity
    - Both singleton path and own SPP path verified working

14. **Hardcoded MAC Removed**
    - `98:80:BB:40:5B:AF` removed from all source files
    - Both SppController and TwsIntentReceiver use `findTwsDevice()` dynamic discovery
    - Scans bonded devices by name (SOUNDPEATS, Air3, PEATS)

15. **Adaptive Icon**
    - Icon from `icon.ico` converted to adaptive icon (108x108 viewport)
    - Foreground: 256x256 PNG in density-specific drawable folders
    - Background: warm cream `#F5ECD5`

16. **TWSControlWin (Windows Port)**
    - Full C# WPF port with .NET 8.0 + 32feet.NET
    - All features ported: ANC, Game, Touch, EQ, Battery, Reset
    - Self-contained single-file .exe build
    - Auto-discover bonded TWS devices

17. **Firmware Extraction**
    - Wrote `extract_firmware.py` for QCC3046 firmware analysis
    - Firmware uses proprietary Qualcomm format (not gzip)
    - Found QCC sections, CSR partition tables, GAIA strings, audio data
    - Build info: 2020-08-14, auraplus_p0_d00_signed

## What Works

- SPP RFCOMM connect/disconnect
- GAIA v4 packet write & read
- ANC: Normal / Strong / Transparency
- Game Mode: ON / OFF toggle
- Touch Control: ON / OFF toggle
- Battery: L/R percentage display
- Device name + MAC (dynamic from bonded devices)
- EQ Presets: 9 presets with per-band data
- Custom EQ: 8-band slider
- Factory Reset with confirmation
- MacroDroid intent: 17 commands via broadcast receiver
- Intent works with app open (singleton) or closed (own SPP)
- Adaptive icon
- Windows app with full feature parity

## Notes

- SoundPEATS app features NOT implemented: Adaptive Equalizer, ANC Scenes, Transparency Scenes, Firmware Upgrade, User Guide
- The `0x0E01` command supports both 1-byte (preset index) and 9-byte (per-band) payloads. The earbuds built-in presets differ from the app database, so per-band data must be sent to match the app sound.
