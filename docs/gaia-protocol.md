# GAIA v4 Protocol over SPP/RFCOMM

## Transport

SoundPEATS Air3 Pro uses **SPP/RFCOMM** (Serial Port Profile), NOT BLE GATT. All control commands (ANC, Game, Touch, EQ, Battery) go through RFCOMM channel 1.

BLE GATT failed because it requires authentication (NOT_AUTHENTICATED error). SPP works because the device is already bonded via Android Bluetooth settings.

## GAIA v4 Packet Format

### Request (Phone → Earbuds)

```
Byte:  [0]  [1]  [2]  [3]        [4]  [5]        [6]  [7]        [8+]
       FF   04   00   payload_len  vendor_hi  vendor_lo  cmd_hi   cmd_lo   payload...
       SOP  ver  flags
```

- Byte 0: `0xFF` — Start of Frame
- Byte 1: `0x04` — GAIA v4
- Byte 2: `0x00` — Flags
- Byte 3: Payload length (bytes after header)
- Byte 4-5: Vendor ID (big-endian)
- Byte 6-7: Command ID (big-endian)
- Byte 8+: Payload

**No trailing FCS byte** — FCS is part of RFCOMM framing, not GAIA.

### Response (Earbuds → Phone)

```
FF 04 00 payload_len vendor_hi vendor_lo cmd_hi cmd_lo [status] [data...]
```

- Response cmd = request_cmd | 0x8000 (e.g. `0x0306` → `0x8306`)
- Byte 0 of payload = status (`0x00` = success)
- Byte 1+ of payload = actual data

### Response Parsing (Kotlin)

```kotlin
val isResponse = (cmd and 0x8000) != 0
val realCmd = cmd and 0x7FFF
val payload = if (isResponse && rawPayload.size > 1)
    rawPayload.copyOfRange(1, rawPayload.size)
else rawPayload
```

## Vendor Mapping

| Vendor | Hex | Bytes | For |
|--------|-----|-------|-----|
| Qualcomm | `0x000A` | `00 0A` | All commands except EQ |
| Custom | `0x001D` | `00 1D` | EQ preset (cmd `0x0E01`/`0x0E02`) |

## Command Table

### GET Commands (Query State)

| Feature | Cmd | Hex Packet | Response Data |
|---------|-----|------------|---------------|
| L Battery | `0x0306` | `FF 04 00 00 00 0A 03 06` | `[status] [0-100]` |
| R Battery | `0x0307` | `FF 04 00 00 00 0A 03 07` | `[status] [0-100]` |
| Firmware | `0x0309` | `FF 04 00 00 00 0A 03 09` | `[status] "Air3Pro_20220815_v1.70"` |
| Game Status | `0x030E` | `FF 04 00 00 00 0A 03 0E` | `[status] [0=off, 1=on]` |
| ANC Status | `0x0310` | `FF 04 00 00 00 0A 03 10` | `[status] [0=normal, 1=ANC, 2=transp]` |
| Touch Status | `0x0312` | `FF 04 00 00 00 0A 03 12` | `[status] [1=enabled, 0=disabled]` |

### SET Commands

#### ANC Mode (cmd=`0x0311`, vendor=`0x000A`)

| Mode | Value | Hex Packet |
|------|-------|------------|
| Normal (ANC Off) | `0` | `FF 04 00 01 00 0A 03 11 00` |
| Strong ANC | `1` | `FF 04 00 01 00 0A 03 11 01` |
| Transparency | `2` | `FF 04 00 01 00 0A 03 11 02` |

#### Game Mode (cmd=`0x030F`, vendor=`0x000A`)

| Mode | Value | Hex Packet |
|------|-------|------------|
| OFF | `0` | `FF 04 00 01 00 0A 03 0F 00` |
| ON | `1` | `FF 04 00 01 00 0A 03 0F 01` |

#### Touch Control (cmd=`0x0313`, vendor=`0x000A`)

Inverted logic:
| Mode | Value | Hex Packet |
|------|-------|------------|
| Enabled | `1` | `FF 04 00 01 00 0A 03 13 01` |
| Disabled | `0` | `FF 04 00 01 00 0A 03 13 00` |

GET response (`0x0312`): `1` = enabled, `0` = disabled

#### Factory Reset (cmd=`0x0305`, vendor=`0x000A`)

| Action | Hex Packet |
|--------|------------|
| Reset | `FF 04 00 00 00 0A 03 05` |

## GAIA Packet Builder (Kotlin)

```kotlin
private fun gaiaPacket(cmdId: Int, payload: ByteArray?, vendor: Int = 0x0A): ByteArray {
    val plen = payload?.size ?: 0
    val hdr = byteArrayOf(
        0xFF.toByte(), 0x04, 0x00, (plen and 0xFF).toByte(),
        ((vendor shr 8) and 0xFF).toByte(), (vendor and 0xFF).toByte(),
        (cmdId shr 8).toByte(), (cmdId and 0xFF).toByte()
    )
    return if (payload != null) hdr + payload else hdr
}
```
