# Build & Architecture

## Android App (TWS Macro/Control)

### Build Command

```bash
cmd.exe /c "set JAVA_HOME=C:\Program Files\Java\jdk-17&& set ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk&& cd /d TWSControl&& gradlew.bat assembleDebug --no-daemon"
```

### Architecture

```
foxlost.tws.macro/
├── SppController.kt
│     SPP RFCOMM connect/disconnect
│     GAIA packet builder
│     EQ preset + custom EQ send (delegated from TwsIntentReceiver)
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
│     Uses SppController.setEqPreset() if app is open (singleton)
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

### Color Scheme

| State | Color | Hex |
|-------|-------|-----|
| Active/ON | Green | `#4CAF50` |
| Inactive/OFF | Dark blue | `#16213E` |
| Connect button | Red | `#E94560` |
| Reset/Custom | Purple | `#533483` |
| Icon background | Warm cream | `#F5ECD5` |

### APK Output

```
TWSControl/app/build/outputs/apk/debug/app-debug.apk
```

---

## Windows App (TWSControlWin)

### Build Commands

```powershell
# Debug build
cd TWSControlWin
dotnet build

# Release .exe (self-contained, single file)
dotnet publish -c Release -r win-x64 --self-contained true -p:PublishSingleFile=true -o publish
```

### Architecture

```
TWSControlWin/
├── TWSControlWin.csproj    .NET 8.0 WPF + InTheHand.Net.Bluetooth
├── EqData.cs               EQ presets, GAIA packet builder (port dari EqData.kt)
├── SppController.cs        SPP RFCOMM connect, command send, response parse (port dari SppController.kt)
├── MainWindow.xaml         UI layout (port dari activity_main.xml)
├── MainWindow.xaml.cs      Button handlers, state management (port dari MainActivity.kt)
├── icon.ico                Ikon aplikasi
└── App.xaml                Entry point
```

### Dependencies

| Package | Purpose |
|---------|---------|
| InTheHand.Net.Bluetooth | RFCOMM SPP Bluetooth for Windows (32feet.NET) |
| .NET 8.0 | Runtime |

### EXE Output

```
TWSControlWin/publish/TWSControlWin.exe  (~155 MB, self-contained)
```
