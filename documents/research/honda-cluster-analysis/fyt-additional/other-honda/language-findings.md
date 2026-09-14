# RZC Honda language — completed code trace

**Portuguese exists in this FYT implementation.** `Acrivity_RZC_17CRVSettings` offers **33 languages** and sends **SYU command 105 with integers `[85, languageValue]`**. Portuguese is **value 15**, i.e. **`cmd(105, [85, 15], null, null)`**. This is an Android FYT service command; it is not an OBD PID/raw CAN frame. No vehicle write was sent during this research.

This completes the language investigation left explicitly unresolved in `findings.md`. It also corrects any impression that the WC panel's three-option picker represents all Honda language support. The RZC and WC pickers have different commands and mappings.

## Options and exact wire values

`setListener` builds 33 labels in order (source Java lines 80–112), creates an int array of length 0x21, leaves index 0 at its initialized value 0, explicitly writes 1–32 to matching indexes, and stores that array in `send_lang`. Thus wire value equals the zero-based selection index. It checks that list and array lengths agree. Machine-readable values: `rzc-language-options.json`.

The intended-language column normalizes obvious country labels/vendor typos; the default label column is copied exactly. For example, default “Snowflake” is a bad translation: its Chinese string is 斯洛伐克 and Japanese is スロバキア, meaning Slovak. Portuguese resource 0x7f0905bf is confirmed by German “Portugiesisch”, Chinese “葡萄牙语” and Japanese “ポルトガル語”. There is one Portuguese choice; the code does not distinguish Brazilian versus European Portuguese.

| Value | Intended language | Vendor default label | Resource ID |
|---:|---|---|---|
| 0 | Chinese (simplified) | Chinese | 0x7f0904bd |
| 1 | English | English | 0x7f0904bc |
| 2 | Chinese (traditional) | Chinese(TW) | 0x7f090d1e |
| 3 | Thai | Thai | literal |
| 4 | Malay | Malaysia | literal |
| 5 | Indonesian | Indonesia | literal |
| 6 | Korean | Korean | 0x7f090d22 |
| 7 | German | German | 0x7f0904cb |
| 8 | Italian | Italian | 0x7f0905ba |
| 9 | French | French | 0x7f0905bb |
| 10 | Spanish | Spanish | 0x7f0905bd |
| 11 | Dutch | Dutch | 0x7f0904c5 |
| 12 | Swedish | Sweden | 0x7f0904e1 |
| 13 | Norwegian | Norwegian | 0x7f0904d6 |
| 14 | Danish | Danish | 0x7f0905c3 |
| 15 | Portuguese | Portug | 0x7f0905bf |
| 16 | Greek | Greek | 0x7f0905c4 |
| 17 | Polish | Poland | 0x7f0904d7 |
| 18 | Turkish | Turkey | 0x7f0904e3 |
| 19 | Russian | Russia | 0x7f0904db |
| 20 | Czech | Czech | 0x7f0904c3 |
| 21 | Hungarian | Hungarian | 0x7f0904ce |
| 22 | Romanian | Romagnio | 0x7f0904da |
| 23 | Slovenian | Slovenia | 0x7f0904de |
| 24 | Arabic | Arabic | 0x7f0905c5 |
| 25 | Bulgarian | Bulgarian | 0x7f0904be |
| 26 | Hebrew | Hebrew | 0x7f0904e5 |
| 27 | Latvian | Latvia | literal |
| 28 | Lithuanian | Lithuania | 0x7f0904d5 |
| 29 | Serbian | Serbia | 0x7f0904dc |
| 30 | Croatian | Croatia | 0x7f0904c2 |
| 31 | Slovak | Snowflake | 0x7f0904dd |
| 32 | Finnish | Finnish | 0x7f0904c8 |

## UI → writer → transport

1. `onCreate` selects layout 0x7f0302fb, `layout_298_rzc_17crv_settings`, and calls `setListener`.
2. `setListener` binds button 0x7f0b00c8 (`all_func_btn_lauguage_set`) to `$3`.
3. `$3.onClick` initializes the popup through `initLauStyle`, displays it, and calls `updateLauguageSet` to restore its local checkmark.
4. `initLauStyle` attaches inner class `$2` as `AdapterView.OnItemClickListener`.
5. `$2.onItemClick` stores the selected position in `language_set`, then directly calls `DataCanbus.PROXY.cmd(0x69, [0x55, send_lang[language_set]], null, null)` and dismisses the popup. It does **not** call the two-int `setCarInfo` helper; the raw array signature is explicit in this listener. Both ultimately target the CANBUS RemoteModuleProxy.

Saved bytecode anchors: main class lines 368–483 (popup binding), 484–1200 (options/array/button), `$2` lines 97–129 (write), `$3` complete onClick. The length check in the vendor `$2` incorrectly accepts `position == size` before indexing; future implementation should use `< size`.

## Readback and callback result

**No vehicle-language callback is established by this UI.** This is a confirmed local-state-only picker implementation, not an inferred successful acknowledgement:

- `language_set` begins at 255 in the constructor.
- The only subsequent write to that field in the activity and its inspected inner classes is `$2` assigning the tapped list position.
- `updateLauguageSet` (main class lines 8092–8129) only checks index bounds and calls `mLauStylelv.setItemChecked(index, true)`; it does not read `DataCanbus.DATA` or parse an incoming value.
- Notification listener `$1` has no `language_set` reference and does not call `updateLauguageSet`.
- The row's `lauguage_set_curr` TextView (0x7f0b00c9) is GONE in the layout, with an English placeholder. The activity never references its resource ID. A static placeholder is not readback.
- `Callback_0298_XP1_2015SIYU_CRV` has no named language field; an undocumented/generic feedback field elsewhere cannot be ruled out. This research establishes no usable language acknowledgement field, so an application must not report confirmed vehicle language from the last local selection.

## Exact entry route and visibility gates

The verified explicit class-reference route is:

`HondaIndexActi.init → RZCCommpassActi → RZCCommpassActi$14.onClick → Acrivity_RZC_17CRVSettings → language picker`.

`HondaIndexActi.showRZCSettings` returns true when `(profile & 0xffff) == 0x12a`, excluding these exact values:

`0x1012a, 0x2012a, 0x3012a, 0x4012a, 0x5012a, 0x6012a, 0x7012a, 0x8012a, 0x9012a, 0xa012a, 0xb012a, 0xd012a, 0xf012a, 0x23012a, 0x28012a`.

The preceding WC route uses low word 0x141, so does not overlap. The preceding direct `AcrivitySiYuSettings` route uses eight exact BNR-related profiles (6,7,8,9,A,B,F,28 high words with low word 0x12a), all excluded above. Do not confuse that different class with the RZC language screen.

`RZCCommpassActi` binds 0x7f0b00cd to `$14`; its layout places that button in a visible row with “Vehicle personalization”. The parent row has no profile-dependent visibility ID. The activity's profile gating for 0x2e012a, 0x2f012a, 0x3c012a, 0x40012a, 0x3d012a affects six other compass rows (0x7f0b0097, 009b, 009f, 00a3, 00a7, 00ac), not this entry button. No further entry gate was found.

In `Acrivity_RZC_17CRVSettings`, language parent 0x7f0b00dd (`all_func_btn_lauguage_set_view`) is visible by default and is never referenced by the activity's visibility code; button 0x7f0b00c8 is bound without any language-specific profile condition. The hidden current-language TextView is the only language-related visibility attribute. Both this screen's layout and the compass layout have only a default configuration in the resources table. The referenced `bz408_item_style` controls height, not visibility. No callback hides the language row. All 33 options are populated unconditionally.

A DEX code-item scan for `const-class Acrivity_RZC_17CRVSettings`, followed by inspection of its candidate, found only `RZCCommpassActi$14.onClick`; candidate results are saved in `rzc-settings-const-class-candidates.json`. This checks explicit class launches; reflection/string-based external launches are not a proved absence.

Source profile examples from `FinalCanbus`: 0x10012a/0x11012a are RZC Honda 16Civic Vscreen low/high, and 0x29012a is RZC Honda 2022SIYU; they satisfy this vendor route. BNR 16Civic 0x6012a/0x7012a/0x28012a do not. The source route is not hardware validation for any Brazilian trim. No decoded Honda bus frame or Portuguese acknowledgement has been established.

## Difference from WC picker

| Implementation | Command | Payload | Languages | Portuguese |
|---|---:|---|---|---|
| RZC `Acrivity_RZC_17CRVSettings` | 105 | `[85, value]` | 33, values 0–32 | value 15 |
| WC `Wc_16Civic_Pannel` | 112 | `[1, value]` | English 1, Chinese 2, traditional Chinese 3 | absent from this picker |

Both inspected pickers use local selection state without an established vehicle language callback. Their commands must remain separated by source profile family.
