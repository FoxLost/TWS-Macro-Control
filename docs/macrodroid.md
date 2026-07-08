# MacroDroid Intent Reference

## App Info

```
Package : foxlost.tws.macro
Name    : TWS Macro/Control
```

## Intent Receiver

| Field | Value |
|-------|-------|
| Action | `foxlost.tws.macro.SET_MODE` |
| Class | `foxlost.tws.macro.TwsIntentReceiver` |
| Target | Broadcast |
| Extra Key | `mode` |

## Available Commands

### ANC Modes
| Extra Value | Function |
|-------------|----------|
| `anc_normal` | ANC Off (Normal) |
| `anc_strong` | ANC Strong |
| `anc_transparency` | Transparency |

### Game & Touch
| Extra Value | Function |
|-------------|----------|
| `game_on` | Game Mode ON |
| `game_off` | Game Mode OFF |
| `touch_on` | Touch Control ON |
| `touch_off` | Touch Control OFF |

### EQ Presets
| Extra Value | Function |
|-------------|----------|
| `eq_classic` | EQ Classic |
| `eq_bass_boost` | EQ Bass Boost |
| `eq_bass_reduction` | EQ Bass Reduction |
| `eq_electronic` | EQ Electronic |
| `eq_popular` | EQ Popular |
| `eq_classical` | EQ Classical Music |
| `eq_rock` | EQ Rock & Roll |
| `eq_folk` | EQ Folk |
| `eq_treble` | EQ Treble Enhancement |

### Other
| Extra Value | Function |
|-------------|----------|
| `reset` | Factory Reset |
| `get_battery` | Query Battery L/R |
| `raw:FF040001000A031101` | Send raw GAIA hex packet |

## ADB Test

```bash
adb shell "am broadcast -a foxlost.tws.macro.SET_MODE --es mode anc_strong -n foxlost.tws.macro/.TwsIntentReceiver"
adb shell "am broadcast -a foxlost.tws.macro.SET_MODE --es mode eq_bass_boost -n foxlost.tws.macro/.TwsIntentReceiver"
adb shell "am broadcast -a foxlost.tws.macro.SET_MODE --es mode game_on -n foxlost.tws.macro/.TwsIntentReceiver"
```

## Notes

- Intent works whether the app is open or closed
- When app is open, intent uses the existing SPP connection (singleton path)
- When app is closed, intent opens its own SPP connection, sends packets, then closes
- All 17 commands verified working via ADB test
