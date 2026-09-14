# Adjacent Accord findings

Read-only code inspection of the local Joying reference APK. These are additional Honda-family capabilities, not proof of Civic instrument-cluster compatibility.

- `Accord9HSettingsAct.setBackgroundColor(int)`: callback field 3, command 1 with one integer; selected values wrap 1–4. `setScreenBright(int)`: callback field 1, command 0 with one integer; values wrap 0–2. These belong to the Accord screen settings path, not the Honda meter XML IDs.
- `XPAccord9ScreenActi` implements brightness, contrast, saturation, screen color and screen display callbacks. `onResume` changes visibility for profiles 0x29/0x10029 versus 0x4D/0x1004D/0x2004D/0x3004D/0x4004D, then sends its own screen-data query command 100 with [0xD3, 0]. This reinforces that screen settings cannot be copied wholesale into the Civic panel path.
- `XPAccord9SettingsActi` has additional update methods for keyless beep volume, lock feedback, security relock timer, lighting sensitivity and timers, trip A/B reset timing, outside-temperature adjustment and fuel-efficiency backlighting. Many overlap the Civic pages; do not count them again as distinct Civic features.
- `ActivityAccord7AirDiagnosis` subscribes to fields 50–63. Its `$1` callback formats coolant and cabin temperatures, sunlight, air-mix door opening, blower, sensor and motor states. No DTC read/clear command was found in this screen. The class name alone is not evidence of an OBD scanner implementation.

Decompiled classes are preserved alongside this report. APK provenance is in ../sources.json. Device compatibility, upstream routing into these Accord screens and raw CAN transport were not established in this pass.

## Additional language path

`Accord9LowSettingsAct.initView` builds English/Chinese choices using resources 0x7f0905b7/0x7f0905b8 and `send_lang = [1, 2]`. `initLauStyle` installs `$2.onItemClick`, which sends command **17** with a singleton `[languageValue]` integer array and null float/string arrays. English=1, Chinese=2. This differs from both WC Civic command112 and RZC command105/key85. Its selected index is stored locally; a vehicle-language acknowledgement was not established. Its applicability to Civic was not established.
