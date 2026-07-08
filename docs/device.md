# SOUNDPEATS Air3 Pro — Device Specification

## TWS Earbuds

| Item | Detail |
|------|--------|
| **TWS Model** | SOUNDPEATS Air3 Pro |
| **Chipset** | Qualcomm QCC3046 |
| **Protocol** | GAIA v4 over SPP/RFCOMM (NOT BLE GATT) |
| **Firmware** | Air3Pro_20220815_v1.70 |
| **BLE MAC** | `98:80:BB:40:5B:AF` |
| **Classic Name** | "SOUNDPEATS Air3 Pro" |
| **BLE Name** | "SOUNDPEATS Air3Pro" |

## Original SoundPEATS App

| Item | Detail |
|------|--------|
| **Package** | `com.thirtydays.headset` |
| **Launch Activity** | `com.xingkeqi.peats.MainActivity` |
| **Database** | `/data/user/0/com.thirtydays.headset/databases/pa_database` |

## Our App (TWS Macro/Control)

| Item | Detail |
|------|--------|
| **Package** | `foxlost.tws.macro` |
| **Source** | `TWSControl/app/src/main/java/foxlost/tws/macro/` |
| **APK** | `TWSControl/app/build/outputs/apk/debug/app-debug.apk` |

## Phone Test Device

| Item | Detail |
|------|--------|
| **Model** | Redmi 6 (cereus) |
| **ADB ID** | `95bba8027d27` |
| **Root** | Yes |
| **OS** | Android 11 (API 30) |
| **Frida** | florida 17.6.2 at `/bin/florida` |

## SPP Connection (from SppBluetoothService.java)

1. `device.createRfcommSocketToServiceRecord(SPP_UUID)` — Standard
2. `device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)` — Insecure
3. `device.createRfcommSocket(1)` via reflection — Fallback

SPP UUID: `00001101-0000-1000-8000-00805F9B34FB`
