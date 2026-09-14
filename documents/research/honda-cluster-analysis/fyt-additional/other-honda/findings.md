# Additional Honda settings found in FYT bytecode

Research only; no app edits or vehicle messages. Source: Joying UIS7862 CANBUS APK at `cabin/artifacts/joying-uis7862/extracted/applications/app/190000000_com.syu.canbus/190000000_com.syu.canbus.apk`. Bytecode was exported with Android SDK apkanalyzer; `resources.txt` and layout trees were exported with aapt2. Annotated layout trees resolve resource IDs to names/default labels.

## Three concrete additional instrument controls

Both `com.syu.carinfo.honda.AcrivitySiYuSettings` and `Acrivity_RZC_17CRVSettings` contain the following complete UI-write/read pairs:

| Setting | SYU command | First integer/key | Second integer | Feedback field | Meaning |
|---|---:|---:|---|---:|---|
| Speed/distance units | 105 (0x69) | 21 (0x15) | 0 or 1 | 77 (0x4d) | 0 = km/h and km; nonzero displayed as mph and miles |
| Tachometer display | 105 | 22 (0x16) | 0 or 1 | 78 (0x4e) | Checked for nonzero; label Chinese 转速计显示 establishes tachometer display |
| Tachometer setting | 105 | 35 (0x23) | 0 or 1 | 87 (0x57) | Checked for nonzero; exact distinction from the other tachometer toggle remains unknown |

`setCarInfo(int,int)` dispatches `DataCanbus.PROXY.cmd(0x69,key,value)`. The onclick handlers toggle current feedback between 0 and 1. These are FYT/SYU IDs, not OBD PIDs or Honda raw CAN frame IDs. Feedback names are independently defined in `Callback_0298_XP1_2015SIYU_CRV` as `U_CARINFO_SPEED_DISTANCE_UNIT`, `U_CARINFO_TACHOMETER`, `U_CARINFO_TACHOMETER_SET`.

Evidence anchors (line numbers of saved text):

- AcrivitySiYuSettings: unit updater 2452; tachometer updater 2499; second tachometer updater 2537; unit onclick 4091; tachometer onclick 4114; second toggle 4440; transport helper 5891.
- Acrivity_RZC_17CRVSettings: unit updater 3559; tachometer updater 3606; second tachometer updater 3644; onclick method 4780; transport helper 7806.
- `layout_298_siyu_settings.xmltree.annotated.txt` lines 729–772 and 1027–1044 connect controls and labels. The two tachometer labels really are separate rows, not two names for one callback.

## Profile and visibility restrictions

Do not apply these command 105 controls to the implemented command 106 WC profile family by similarity.

`HondaIndexActi.init` routes the exact profiles `0x6012a, 0x7012a, 0x8012a, 0x9012a, 0xa012a, 0xb012a, 0xf012a, 0x28012a` to `AcrivitySiYuSettings`. Its two tachometer rows are present by default in the layout and are not hidden by that activity's inspected onResume code. In `FinalCanbus`, 0x6012a/0x7012a are BNR 16Civic Vscreen low/high, and 0x28012a is BNR 16Civic NoAir.

Important limitation: `AcrivitySiYuSettings.onResume` explicitly shows the units parent row 0x7f0b013b only for `0xc012a, 0xd012a, 0x1c012a, 0x1d012a, 0x25012a, 0x26012a, 0x29012a, 0x2a012a`, and hides it otherwise. These do NOT overlap the HondaIndexActi direct-entry profiles above. Thus the units handler exists but is hidden in that BNR entry route; do not claim BNR units support from the handler alone. Additional routes may exist.

The RZC route is source-backed: `HondaIndexActi.showRZCSettings` accepts `(profile & 0xffff) == 0x12a`, except exact exclusions `0x1012a, 0x2012a, 0x3012a, 0x4012a, 0x5012a, 0x6012a, 0x7012a, 0x8012a, 0x9012a, 0xa012a, 0xb012a, 0xd012a, 0xf012a, 0x23012a, 0x28012a`. It opens `RZCCommpassActi`; that screen's button 0x7f0b00cd listener `$14` explicitly opens `Acrivity_RZC_17CRVSettings` (saved). This is a broad vendor UI predicate, not proof every theoretical profile or every model accepts every write. `FinalCanbus` maps 0x10012a/0x11012a to RZC Honda 16Civic Vscreen low/high and 0x29012a to RZC Honda 2022SIYU.

For any implementation, restrict to actual known profile constants plus the complete per-row layout/visibility behavior; do not use the broad masked predicate to authorize arbitrary future profiles. No Brazilian 2020 EXL hardware behavior was tested.

## Language omitted from current WC panel

Existing source `honda-cluster-analysis/fyt/com.syu.carinfo.honda.Wc_16Civic_Pannel$2.smali` sends SYU command 112 (0x70), integer array `[1, languageWire]`, null float/string arrays. The init list maps 1 = English, 2 = Chinese, 3 = traditional Chinese. Resources are 0x7f0904bc, 0x7f0904bd, 0x7f090d1e. No Portuguese option exists in this picker. No language-specific profile visibility branch was found in Wc_16Civic_Pannel.onResume; retain the separately verified WC screen entry profiles.

This picker stores the last tapped local list index in `language_set` (initial 255). Its `updateLauguageSet` only updates the local checkmark. The inspected notification listener does not update that member, so no confirmed vehicle language feedback mapping was established. Do not treat an optimistic selected checkmark as vehicle acknowledgement. The vendor onclick bounds check also permits `index == size` before array lookup; a new implementation should not copy that off-by-one behavior.

The RZC language investigation is now complete in `language-findings.md`: 33 languages, including Portuguese value 15, sent as command 105 `[85, value]`. No usable vehicle acknowledgement field was established. Its source entry route and language-row visibility are documented there; do not reuse the WC language mapping.

## ZX6606 dashboard is a data display

`ZX6606HondaNewCarinfoAct` and `$1` subscribe to fields 180, 181, 182, 183, 184, 185, 187, 189, 510, 513, 519 and render local views. No `RemoteModuleProxy.cmd` call or write listener exists in these inspected classes. A class/layout named dashboard therefore does not establish physical instrument-panel customization. The default-path layout dump was unavailable (likely qualified layout); source bytecode is saved and this is not a resolved layout inventory.

## Still unresolved

No actionable FYT mapping for meter colors, wallpaper, startup/opening animation, or meter fault read/clear was established by these inspected classes. This scoped negative result is not proof the entire APK lacks them. Other agents are investigating other Honda screens and stock Honda firmware. The three mapped controls above plus the WC language writer are additional source findings, not new Cabin functionality.
