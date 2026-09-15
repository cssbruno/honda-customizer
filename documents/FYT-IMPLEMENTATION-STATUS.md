# FYT implementation completion audit

The goal remains full FYT Honda implementation. A researched row is not automatically compatible. Only exact established profiles, valid command values, and matching real FYT feedback authorize edits. No automatic fallback or synthetic vehicle data.

## Implemented in active FYT sources

- Toolkit connection, module 7, exact decoder identification, connection report.
- 13 WC panel mappings and 27 WC door/light/remote/assistance/camera/seat mappings with per-row restrictions.
- All 59 persistent settings in the researched RZC screen, limited to the three known RZC profiles.
- 37 BNR persistent mappings, with exact per-profile visibility: 26, 31 or 32 available rows depending on the known BNR profile.
- Parked acknowledgement, explicit confirmation, fresh-value gating, matching post-dispatch feedback, disconnect/profile-change invalidation.

## Still required

- Remaining in-scope SiYu maintenance data/actions. New sound and camera additions are excluded by the project owner. Their newly traced contracts are not enabled. The hidden beep-volume row remains excluded.
- Resolve vehicle language feedback before enabling language writes; Portuguese command is known, acknowledgement is not.
- Resolve completion evidence for maintenance reset, initial-values restore and TPMS calibration.
- Oil/service-life fields 135–137 are traced, but the value row is hidden on all currently supported BNR profiles. Establish applicable exact-profile evidence before displaying it.
- Establish FYT contracts for full physical panel editing, diagnostics and head-unit settings. OEM code cannot supply these contracts.
- Real head-unit report, firmware/decoder identity and parked physical validation. No vehicle is attached here.

## RZC additional-row evidence audit

Source: external extracted `Acrivity_RZC_17CRVSettings.smali`, `$1.smali`, and annotated `layout_298_rzc_17crv_settings.xmltree` under `/home/bruno/Documentos/ChatGPT/R/honda-cluster-analysis/fyt-additional`.

The only activity `setViewState` call gates layout `0x7f0b0097` to profiles `0x32012a`, `0x20012a`, `0x27012a`, `0x26012a`. The layout proves that this is the **amplifier navigation row** (`0x7f0b040a`), not a seat row. `onResume` only registers notifications. The seven added rows are outside that parent; their writers have no profile-specific branches. The existing known RZC Civic profiles `0x10012a`, `0x11012a`, `0x29012a` remain the exact allowlist. BNR profiles are excluded.

| Field | Command/key | Values | Writer branch | Callback branch |
|---|---|---|---|---|
| 177 | 105/71 | 0 manual, 1 automatic folding | sswitch_32b | pswitch_215 |
| 190 | 105/81 | 0/1 off/on | sswitch_51b | pswitch_165 |
| 176 | 105/70 | 0/1 off/on | sswitch_52d | pswitch_17b |
| 175 | 105/74 | 0/1 off/on | sswitch_509 | pswitch_14f |
| 179 | 105/73 | 0 cruise control, 1 specified speed range | sswitch_2c3 / 2d3 | pswitch_274 |
| 193 | 105/79 | 0–3: +0/+5/+10/+15 km/h | sswitch_2e3 / 2f5 | pswitch_29e |
| 197 | 105/82 | 0 visual, 1 visual + audible | sswitch_307 / 319 | pswitch_24a |

All readback is full-integer validated. Mirror labels follow actual resources `klc_air_Manual` and `str_automatic_folding`. None of these contracts proves installed vehicle equipment; missing feedback remains unavailable.

## Full persistent-setting audit

RZC: all 59 feedback-backed persistent rows from the 65-row source inventory are represented. The excluded six rows are language, amplifier navigation, maintenance reset, restore defaults, TPMS calibration and panorama initialization. [New command/value transcript](research/rzc-persistent-contracts.json) and [callback dispatch audit](research/rzc-callback-audit.json) record the added 49 mappings. Existing ten mappings remain separately documented. RZC values use full integers and zero-based options; WC offsets must not be reused.

BNR: [independent BNR contracts](research/bnr-persistent-contracts.json) are transcribed from `AcrivitySiYuSettings`, not substituted from RZC. [Offline branch results](research/bnr-source-branch-audit.json) record original source execution for every supported profile. Reproduce with `python3 tools/audit_bnr_source.py <external AcrivitySiYuSettings.smali> --output <report.json>`; it never contacts Android or a vehicle.

- Profiles 6/7/28 (high word, low word 0x12a) expose 26 mapped rows.
- Profiles 8/9/A/B expose 31 mapped rows, adding reverse tone and tailgate/seat options.
- Profile F exposes 32 mapped rows; key/remote unlock, door lock/unlock modes, tailgate and fatigue/AWD rows differ. ACC/LKAS tones and reverse tone are hidden.
- Driver attention uses key **41 on 0xF012A**, key **36 elsewhere**. BNR reverse tone uses key 36 on its applicable profiles; RZC reverse tone uses key 50. These are separate control objects.
- Key/remote-unlock row 0x7f0b00b8 and beep-volume row 0x7f0b0121 start GONE. Only profile F shows the former. Beep volume remains excluded, as do units on all supported BNR direct-entry profiles.
- Non-F BNR seat-memory and entry/exit writes are limited to the GuanDao profiles 8/9/A/B. The source's constant-zero branches for other profiles are never offered as editable controls.

Vendor semantics that remain ambiguous retain explicit vendor wording. Firmware menu evidence and unit tests do not prove equipment or behavior on a physical vehicle.

## Portuguese UI and current validation

All 147 distinct control/category/option labels now have English and Portuguese Android resources. Connection status, parked acknowledgement, confirmation dialogs, missing/stale-value explanations and report text are also localized. Unknown vendor meanings stay explicitly marked; localization does not change wire values or authorize vehicle-language writes.

46 Java unit tests and 7 Python parser tests pass. Release lint reports no errors. The APK and instrumentation APK build successfully. The instrumentation contract now includes a packaged-Portuguese-resource check; it has not yet passed on an installed emulator in this work state.

The existing disposable API17 and API36.1 emulators both exited with SIGSEGV (139) before Android/ADB startup using software graphics; graphics-disabled retries also failed. These are emulator-host failures before app installation, not passing smoke runs. Emulator validation and physical FYT validation remain incomplete.

## 3.2.0 release policy

Emulator checks are no longer release requirements at the project owner’s request: emulators do not have access to the FYT service. Historical emulator failures above are not release blockers. Release packaging still requires fresh release unit tests, lint, matching APK metadata and a verified signature. Physical FYT validation remains unverified and is not replaced by software tests.

## 3.3.1 focused implementation audit

New sound/camera work has been removed at the owner’s direction. The current scope focuses on panel customization, language, maintenance and diagnostics. See [live-feedback and remaining-evidence audit](research/fyt-feedback-audit/FINDINGS.md).

The service subscription path was traced beyond the UI. Settings now use notify=0 so that untimestamped cache entries (including startup zeros) cannot be presented as fresh vehicle feedback. Only profile identity uses notify=1. Expired values are removed from the UI/report; expiry of an older observation cannot erase a newer one. Some values remain unavailable on firmware that suppresses unchanged notifications; no fresh-read command is assumed.

The full feature goal remains incomplete. Language/reset completion responses, additional physical cluster contracts and exact vehicle diagnostic contracts are still missing. No physical head-unit report or validation has been supplied.
