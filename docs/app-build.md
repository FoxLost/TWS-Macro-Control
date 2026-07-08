# TWS Macro/Control — Build & Architecture

## Build Command

```bash
cmd.exe /c "set JAVA_HOME=C:\Program Files\Java\jdk-17&& set ANDROID_HOME=C:\Users\herla\AppData\Local\Android\Sdk&& cd /d C:\data data data data\TWS-Soundpeats\TWSControl&& gradlew.bat assembleDebug --no-daemon"
```

## App Architecture

```
foxlost.tws.macro/
├── SppController.kt
│     SPP RFCOMM connect/disconnect
│     GAIA packet builder
│     EQ preset + custom EQ send
│     Response reader/parser
│     Callbacks: battery, game, touch, ANC, connection
│     Singleton instance shared with TwsIntentReceiver
│
├── MainActivity.kt
│     UI layout + button handlers
│     ANC 3-mode buttons (green=active, dark=inactive)
│     Game/Touch toggle side by side
│     9 EQ preset buttons (2x5 grid)
│     Custom EQ slider panel (8 bands)
│     Dynamic device name/MAC detection
│
├── TwsIntentReceiver.kt
│     MacroDroid broadcast receiver
│     Uses SppController singleton if app is open
│     Falls back to own SPP connection when app closed
│     ANC, Game, Touch, EQ presets via EqData
│
├── EqData.kt
│     Shared EQ preset data + GAIA packet builder
│     Used by both SppController and TwsIntentReceiver
│     9 presets with per-band frequency/gain/Q data
│
└── AndroidManifest.xml
      activity: MainActivity (launcher)
      receiver: TwsIntentReceiver (action: foxlost.tws.macro.SET_MODE)
```

## Color Scheme

| State | Color | Hex |
|-------|-------|-----|
| Active/ON | Green | `#4CAF50` |
| Inactive/OFF | Dark blue | `#16213E` |
| Connect button | Red | `#E94560` |
| Reset/Custom | Purple | `#533483` |

## Dependencies

| Tool | Version | Path |
|------|---------|------|
| JDK | 17 | `C:\Program Files\Java\jdk-17` |
| Android SDK | — | `C:\Users\herla\AppData\Local\Android\Sdk` |
| ADB | — | `C:\Users\herla\platform-tools\adb.exe` |
| Python | 3.14 | `C:\Python314\python.exe` |
| Gradle | wrapper | `TWSControl\gradlew.bat` |
| JADX | — | `jadx/bin/jadx.bat` |

## APK

```
TWSControl/app/build/outputs/apk/debug/app-debug.apk
```
