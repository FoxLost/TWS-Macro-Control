# MacroDroid Macro Examples — TWS Macro/Control

## Prerequisites

1. Install **MacroDroid** from Play Store
2. Install **TWS Macro/Control** app
3. Pair your TWS earbuds via Bluetooth settings

---

## Example 1: Quick Toggle ANC via Notification Button

**Tujuan**: Tombol notifikasi untuk toggle ANC Strong / Normal.

### Setup

1. Buka MacroDroid → tap **+** buat macro baru
2. **Trigger** → pilih **Notifications** → **Notification Button**
   - Title: `TWS Control`
   - Icon: pilih icon bluetooth/headphone
   - Buttons: `ANC Toggle`, `Game`, `Touch`
3. **Action** → **Intents** → **Send Intent**
   - Action: `foxlost.tws.macro.SET_MODE`
   - Extra Key: `mode`
   - Extra Value: `anc_strong`
   - Package: `foxlost.tws.macro`
   - Class: `foxlost.tws.macro.TwsIntentReceiver`
   - Target: **Broadcast**
4. **Constraint** (opsional): hanya muncul saat Bluetooth connected

Buat macro kedua dengan notif button sama, extra value `anc_normal`.

---

## Example 2: EQ Preset via Widget

**Tujuan**: Widget di homescreen untuk ganti EQ preset.

### Setup

1. **Trigger** → **Widget** → **Widget Button**
   - Pilih widget position yang diinginkan
   - Label: `Bass Boost`
2. **Action** → **Intents** → **Send Intent**
   - Action: `foxlost.tws.macro.SET_MODE`
   - Extra Key: `mode`
   - Extra Value: `eq_bass_boost`
   - Package / Class seperti di atas
3. Beri nama macro: `EQ: Bass Boost`

**Buat macro serupa untuk preset lain:**
| Widget Label | Extra Value |
|-------------|-------------|
| Classic | `eq_classic` |
| Bass Reduction | `eq_bass_reduction` |
| Electronic | `eq_electronic` |
| Folk | `eq_folk` |
| Treble | `eq_treble` |

---

## Example 3: Auto Battery Check on Headphone Connect

**Tujuan**: Kirim notifikasi battery TWS saat terhubung.

### Setup

1. **Trigger** → **Bluetooth** → **Bluetooth Device Connected**
   - Pilih device TWS dari daftar
2. **Action** → **Intents** → **Send Intent**
   - Action: `foxlost.tws.macro.SET_MODE`
   - Extra Key: `mode`
   - Extra Value: `get_battery`
   - Package / Class seperti di atas
3. **Action** → **Delay** → `2 seconds`
4. **Action** → **Notifications** → **Notify Text**
   - Beri notifikasi yang menunjukkan hasil battery

> **Catatan**: `get_battery` mengirim query ke TWS, response diterima oleh app.
> Karena intent receiver tidak membaca response, gunakan app langsung untuk lihat battery.
> Alternatif: tambahkan **MacroDroid Intent Trigger** di macro lain.

---

## Example 4: NFC Tag Tap for Game Mode

**Tujuan**: Tap NFC untuk toggle Game Mode.

### Setup

1. Tempel NFC tag ke HP
2. **Trigger** → **NFC** → **NFC Tag Scanned**
3. **Action** → **Intents** → **Send Intent**
4. Extra Value: `game_on`

Buat NFC tag kedua untuk `game_off`.

---

## Example 5: Schedule Night Mode (ANC Normal + Touch Off)

**Tujuan**: Jam 10 malam otomatis set ANC Normal dan Touch Off.

### Setup

1. **Trigger** → **Date/Time** → **Specific Time**
   - Set: `22:00`
2. **Action** → **Intents** → **Send Intent**
   - Extra Value: `anc_normal`
3. **Action** → **Delay** → `1 second`
4. **Action** → **Intents** → **Send Intent**
   - Extra Value: `touch_off`

---

## Example 6: Volume Button Double-Tap for ANC

**Tujuan**: Double tap volume up untuk toggle ANC.

### Setup

1. **Trigger** → **Hardware Button** → **Volume Up** → **Double Press**
2. **Action** → **Intents** → **Send Intent**
   - Extra Value: `anc_strong`

---

## Complete Macro Template (JSON Export)

Berikut template macro dalam format MacroDroid JSON. Import via MacroDroid → Settings → Import Macro.

```json
{
  "macroName": "TWS: Bass Boost",
  "triggers": [
    {
      "type": "WIDGET_BUTTON",
      "config": {
        "label": "Bass Boost",
        "icon": "bluetooth"
      }
    }
  ],
  "actions": [
    {
      "category": "INTENTS",
      "type": "SEND_INTENT",
      "config": {
        "action": "foxlost.tws.macro.SET_MODE",
        "extraKey": "mode",
        "extraValue": "eq_bass_boost",
        "targetPackage": "foxlost.tws.macro",
        "targetClass": "foxlost.tws.macro.TwsIntentReceiver",
        "targetType": "Broadcast"
      }
    }
  ]
}
```

---

## Troubleshooting

| Problem | Cause | Fix |
|---------|-------|-----|
| "SPP failed" di log | TWS tidak terhubung | Pair TWS dulu via Bluetooth settings |
| Intent tidak terkirim | Package/Class salah | Pastikan `foxlost.tws.macro` dan `.TwsIntentReceiver` |
| Tidak ada response | Intent receiver fire-and-forget | Gunakan app untuk lihat status aktual |
| Macro tidak jalan | MacroDroid trigger tidak aktif | Cek MacroDroid accessibility service |

## Reference

| Extra Value | Fungsi |
|-------------|--------|
| `anc_normal` | ANC Off |
| `anc_strong` | ANC Strong |
| `anc_transparency` | Transparency |
| `game_on` / `game_off` | Game Mode |
| `touch_on` / `touch_off` | Touch Control |
| `eq_classic` — `eq_treble` | EQ Presets (9 pilihan) |
| `get_battery` | Query Battery |
| `reset` | Factory Reset |
| `raw:HEXSTRING` | Raw GAIA packet |
