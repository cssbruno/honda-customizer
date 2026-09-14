# Full two-screen Honda settings inventory

Read-only breadth audit, 2026-09-13. **45 rows** in `AcrivitySiYuSettings` and **65 rows** in `Acrivity_RZC_17CRVSettings`. These are 110 screen-row occurrences, **71 distinct label-resource IDs** across the two screens, including language, amplifier navigation, maintenance/service information, and reset actions. Excluding language (traced separately) leaves 70 distinct label IDs. This count is not 70 new independent vehicle capabilities: many rows duplicate concepts across adapter families, and three traffic-sign labels may refer to related functions.

This is a complete row inventory of the two saved default layouts, not an exhaustive inventory of the entire CANBUS APK, stock Honda firmware, or every vehicle variant. No app edits or device writes. Existing 27 WC mappings cover only four pages; these two larger screens include many additional categories.

## Newly established breadth

Previously omitted categories include electric tailgate remote-opening conditions and external-handle behavior; tailgate sensor; automatic mirror folding; remote windows; rear-seat reminder; automatic high beam; blind-zone warning; reversing guidelines, camera retention, parking-space width, panorama initialization and reversing delay; fatigue/AWD information; oil-service-life display; maintenance reset, restore initial values and TPMS calibration.

Resource 0x7f090dd1 is misleading in English (“rear view auto”), but Chinese `后视镜自动折叠开关` explicitly means **automatic rear-view mirror folding switch**. Resources 0x7f090c47/0x7f090c48 explicitly mention electric tailgate in Chinese and Vietnamese; they are not general door-handle settings.

## Evidence and interpretation

Layout sources: `../other-honda/layout_298_siyu_settings.xmltree.annotated.txt` and `../other-honda/layout_298_rzc_17crv_settings.xmltree.annotated.txt`. Saved here: main activities, `$1.onNotify` handlers, and `Callback_0298_XP1_2015SIYU_CRV.smali`. Machine inventories `siyu-inventory.json` and `rzc_17crv-inventory.json` include every row's label resource, all parent/control/display view IDs, renderer names, and established feedback fields.

In the tables, “ID” gives the first interactive checkbox, otherwise the displayed text ID (full button IDs are in JSON). Feedback names omit the common `U_CARINFO_` prefix where present; other callback names remain intact. Numeric fields are SYU fields, not Honda raw CAN addresses or OBD PIDs.

Status: **Mapped already** means the previous research established that same-family command/value/feedback pair (units and two tachometer controls); it does not mean implemented. **New candidate** means this inventory establishes the row and a read/handler path but its full write values, profile gates and hardware behavior are not yet audited. **Unresolved** highlights an ambiguous label or incomplete semantics, rather than hiding the row from inventory. “Action mapped now” identifies the newly traced one-shot writers below, which have no completion acknowledgement established. Amplifier entries navigate to another screen and are not counted as a single amplifier setting.

All per-row profile branches still matter. See `../other-honda/findings.md` for known menu predicates and the units visibility mismatch. `AcrivitySiYuSettings` has many `isBNRSiYuOrGuanDao` write branches; do not copy RZC toggle behavior indiscriminately. Similar labels in Cabin's 13 WC panel controls / three BNR lighting controls are concept overlap, not proof that these screen-specific bytes are already implemented.


## AcrivitySiYuSettings — 45 rows

| # | Source label / clarified meaning | ID | Established feedback / renderer | Status |
|---:|---|---|---|---|
| 1 | Amplifier settings | `0x7f0b0096` (ctv_checkedtext1) | No returned field established | Navigation; separate page |
| 2 | Trip A Reset Timing | `0x7f0b0099` (tv_text1) | 58 `TRIP_A` | New candidate |
| 3 | Trip B Reset Timing | `0x7f0b009d` (tv_text2) | 59 `TRIP_B` | New candidate |
| 4 | adjust outside temp | `0x7f0b00a1` (tv_text3) | 60 `OUT_TEMP_SHOW` | New candidate |
| 5 | auto light sensitivity | `0x7f0b00a5` (tv_text4) | 61 `AUTO_LIGHT_SENSITIVITY` | New candidate |
| 6 | Headlight auto off time | `0x7f0b00a9` (tv_text5) | 62 `HEADLIGHT_AUTO_OFF_TIME` | New candidate |
| 7 | Interior light dimming time | `0x7f0b00ae` (tv_text6) | 63 `INTERIOR_LIGHT_DIMMING_TIME` | New candidate |
| 8 | Keyless lock answer back | `0x7f0b00cb` (ctv_checkedtext2) | 64 `KEY_LOCK_ANSWER` | New candidate |
| 9 | Key and remote unlock mode | `0x7f0b00cc` (ctv_checkedtext3) | 65 `KEY_AND_REMOTE_UNLOCK_MODE` | New candidate |
| 10 | Security relock time | `0x7f0b00b2` (tv_text7) | 66 `SECURITY_RELOCK_TIME` | New candidate |
| 11 | Auto Door Locks → auto door UNLOCK (resource/getter name) | `0x7f0b00b6` (tv_text8) | 67 `AUTO_DOOR_UNLOCK` | New candidate |
| 12 | Auto door lock with | `0x7f0b00ba` (tv_text9) | 68 `AUTO_DOOR_LOCK` | New candidate |
| 13 | Remote control beep sound beep size | `0x7f0b00cd` (ctv_checkedtext4) | 108 `KEYLESS_ACCESS_BEEPVOL` | New candidate |
| 14 | Keyless access beep | `0x7f0b00d1` (ctv_checkedtext5) | 69 `KEYLESS_ACCESS_BEEP` | New candidate |
| 15 | Remote start system | `0x7f0b00d2` (ctv_checkedtext6) | 70 `REMOTE_START_SYS` | New candidate |
| 16 | Door unlock mode | `0x7f0b00d3` (ctv_checkedtext7) | 71 `DOOR_UNLOCK_MODE` | New candidate |
| 17 | Keyless access light flash | `0x7f0b00d4` (ctv_checkedtext8) | 72 `KEYLESS_ACCESS_LIGHT_FLASH` | New candidate |
| 18 | Auto interio illumination | `0x7f0b00c6` (tv_text12) | 73 `AUTO_INTER_ILLUMINATION` | New candidate |
| 19 | adjust_alarm_volume | `0x7f0b008d` (tv_text13) | 74 `ADJUST_ALARM_VOLUME` | New candidate |
| 20 | Fuel efficiency backlight | `0x7f0b00d5` (ctv_checkedtext9) | 75 `FUEL_EFFIC_BACKLIGHT` | New candidate |
| 21 | New message Notifications | `0x7f0b00d6` (ctv_checkedtext10) | 76 `NEW_MSG_NOTIF` | New candidate |
| 22 | Speed distance units | `0x7f0b00d7` (ctv_checkedtext11) | 77 `SPEED_DISTANCE_UNIT` | Mapped already |
| 23 | Tachmeter | `0x7f0b00d8` (ctv_checkedtext12) | 78 `TACHOMETER` | Mapped already |
| 24 | Walk away auto luck | `0x7f0b00d9` (ctv_checkedtext13) | 79 `WALK_AWAY_AUTO_LOCK` | New candidate |
| 25 | Auto headlight on with wiper | `0x7f0b00da` (ctv_checkedtext14) | 80 `AUTO_HEADLIGHT_WIPER` | New candidate |
| 26 | Volume Voice Alarm System | `0x7f0b00db` (ctv_checkedtext15) | 81 `VOLUME_ALARM_SYS` | Unresolved semantics; path found |
| 27 | Energy-saving automatic start and stop | `0x7f0b00dc` (ctv_checkedtext16) | 82 `ENERGY_SAVE_AUTO_ENGHINE` | New candidate |
| 28 | Acc discovery vehicle in front tone | `0x7f0b013f` (ctv_checkedtext17) | 83 `ACC_DISCOVERY_VEHICLE_IN_FRONT_TONE` | New candidate |
| 29 | Pause LKAS tone | `0x7f0b0141` (ctv_checkedtext18) | 84 `PAUSE_LKAS_TONE` | New candidate |
| 30 | Set front hazard distance | `0x7f0b016b` (tv_text16) | 85 `SET_FRONT_HAZARD_DISTANCE` | New candidate |
| 31 | Minor lane departure system settings | `0x7f0b016c` (tv_text17) | 86 `MINOR_LANE_DEPARTURE_SYS_SETTINGS` | New candidate |
| 32 | Driver attention monitor | `0x7f0b016d` (tv_text18) | 114 `U_DRIVER_ATTENTION_MONITOR` | New candidate |
| 33 | Tachmeter Settings | `0x7f0b0143` (ctv_checkedtext19) | 87 `TACHOMETER_SET` | Mapped already |
| 34 | Reverse gear tone | `0x7f0b0145` (ctv_checkedtext20) | 109 `BACK_CAR_BEEP_TONE` | New candidate |
| 35 | Sets the remote control on condition → electric tailgate remote-opening condition | `0x7f0b0147` (ctv_checkedtext21) | 110 `ELE_DOOR_REMOTE_OPEN_CONDITION` | New candidate |
| 36 | Set to apply the external handle to open electrically → electric tailgate external-handle behavior | `0x7f0b0149` (ctv_checkedtext22) | 111 `ELE_DOOR_OPEN_AUTO_OR_MANULE` | New candidate |
| 37 | Driving position Personalization Set the memory position linkage | `0x7f0b014c` (ctv_checkedtext23) | 112 `DRIVING_POSITION_REMORY` | New candidate |
| 38 | Enter / exit seat position movement | `0x7f0b01f8` (ctv_checkedtext24) | 113 `INOUT_SEAT_SPORT` | New candidate |
| 39 | Fatigue driving information | `0x7f0b01fa` (ctv_checkedtext25) | 149 `PILAO_DRIVER` | New candidate |
| 40 | AWD information | `0x7f0b01fc` (ctv_checkedtext26) | 150 `AWD_INFO` | New candidate |
| 41 | Turn right enter camera | `0x7f0b01fe` (ctv_checkedtext27) | No returned field established | New candidate |
| 42 | Vehicle maintenance information reset | `0x7f0b0234` (ctv_checkedtext28) | No returned field established | Action mapped now; acknowledgement unresolved |
| 43 | Maintenance information Oil service life | `0x7f0b016f` (tv_text20) | 135 `MAINTANCE_OIL_SERVICE_LIFE_UNIT`; 136 `MAINTANCE_OIL_SERVICE_LIFE_PN_UNIT`; 137 `MAINTANCE_OIL_SERVICE_LIFE` | New service-data display; not a setting |
| 44 | Restore initial values | `0x7f0b0225` (ctv_checkedtext29) | No returned field established | Action mapped now; acknowledgement unresolved |
| 45 | Tire Pressure Calibration | `0x7f0b022e` (ctv_checkedtext30) | No returned field established | Action mapped now; acknowledgement unresolved |

## Acrivity_RZC_17CRVSettings — 65 rows

| # | Source label / clarified meaning | ID | Established feedback / renderer | Status |
|---:|---|---|---|---|
| 1 | Language Settings | `0x7f0b00c8` (all_func_btn_lauguage_set) | No returned field established | Separate language-agent audit |
| 2 | Amplifier settings | `0x7f0b040a` (ctv_checkedtext45) | No returned field established | Navigation; separate page |
| 3 | Vehicle maintenance information reset | `0x7f0b0403` (ctv_checkedtext42) | No returned field established | Action mapped now; acknowledgement unresolved |
| 4 | Restore initial values | `0x7f0b0404` (ctv_checkedtext43) | No returned field established | Action mapped now; acknowledgement unresolved |
| 5 | Tire Pressure Calibration | `0x7f0b0409` (ctv_checkedtext44) | No returned field established | Action mapped now; acknowledgement unresolved |
| 6 | adjust outside temp | `0x7f0b0099` (tv_text1) | 60 `OUT_TEMP_SHOW` | New candidate |
| 7 | Trip A Reset Timing | `0x7f0b009d` (tv_text2) | 58 `TRIP_A` | New candidate |
| 8 | Trip B Reset Timing | `0x7f0b00a1` (tv_text3) | 59 `TRIP_B` | New candidate |
| 9 | Interior light dimming time | `0x7f0b00a5` (tv_text4) | 63 `INTERIOR_LIGHT_DIMMING_TIME` | New candidate |
| 10 | Headlight auto off time | `0x7f0b00a9` (tv_text5) | 62 `HEADLIGHT_AUTO_OFF_TIME` | New candidate |
| 11 | auto light sensitivity | `0x7f0b00ae` (tv_text6) | 61 `AUTO_LIGHT_SENSITIVITY` | New candidate |
| 12 | Auto interio illumination | `0x7f0b00c6` (tv_text12) | 73 `AUTO_INTER_ILLUMINATION` | New candidate |
| 13 | Automatic high beam | `0x7f0b041f` (ctv_checkedtext34) | 192 `U_CARSET_D32_D10_B2` | New candidate |
| 14 | Keyless lock answer back | `0x7f0b00cc` (ctv_checkedtext3) | 64 `KEY_LOCK_ANSWER` | New candidate |
| 15 | Key and remote unlock mode | `0x7f0b00b2` (tv_text7) | 65 `KEY_AND_REMOTE_UNLOCK_MODE` | New candidate |
| 16 | Security relock time | `0x7f0b00b6` (tv_text8) | 66 `SECURITY_RELOCK_TIME` | New candidate |
| 17 | Auto Door Locks → auto door UNLOCK (resource/getter name) | `0x7f0b00ba` (tv_text9) | 67 `AUTO_DOOR_UNLOCK` | New candidate |
| 18 | Auto door lock with | `0x7f0b00be` (tv_text10) | 68 `AUTO_DOOR_LOCK` | New candidate |
| 19 | Keyless access beep | `0x7f0b00cd` (ctv_checkedtext4) | 69 `KEYLESS_ACCESS_BEEP` | New candidate |
| 20 | Remote start system | `0x7f0b00d1` (ctv_checkedtext5) | 70 `REMOTE_START_SYS` | New candidate |
| 21 | Door unlock mode | `0x7f0b00c2` (tv_text11) | 71 `DOOR_UNLOCK_MODE` | New candidate |
| 22 | Keyless access light flash | `0x7f0b00d2` (ctv_checkedtext6) | 72 `KEYLESS_ACCESS_LIGHT_FLASH` | New candidate |
| 23 | Lockout Preset | `0x7f0b0307` (ctv_checkedtext33) | 191 `U_CARSET_D32_D10_B3` | Unresolved semantics; path found |
| 24 | adjust_alarm_volume | `0x7f0b008d` (tv_text13) | 74 `ADJUST_ALARM_VOLUME` | New candidate |
| 25 | Fuel efficiency backlight | `0x7f0b00d3` (ctv_checkedtext7) | 75 `FUEL_EFFIC_BACKLIGHT` | New candidate |
| 26 | New message Notifications | `0x7f0b00d4` (ctv_checkedtext8) | 76 `NEW_MSG_NOTIF` | New candidate |
| 27 | Speed distance units | `0x7f0b0090` (tv_text14) | 77 `SPEED_DISTANCE_UNIT` | Mapped already |
| 28 | Tachmeter | `0x7f0b00d5` (ctv_checkedtext9) | 78 `TACHOMETER` | Mapped already |
| 29 | Walk away auto luck | `0x7f0b00d6` (ctv_checkedtext10) | 79 `WALK_AWAY_AUTO_LOCK` | New candidate |
| 30 | Auto headlight on with wiper | `0x7f0b00d7` (ctv_checkedtext11) | 80 `AUTO_HEADLIGHT_WIPER` | New candidate |
| 31 | Volume Voice Alarm System | `0x7f0b0093` (tv_text15) | 81 `VOLUME_ALARM_SYS` | Unresolved semantics; path found |
| 32 | Energy-saving automatic start and stop | `0x7f0b00d8` (ctv_checkedtext12) | 82 `ENERGY_SAVE_AUTO_ENGHINE` | New candidate |
| 33 | Acc discovery vehicle in front tone | `0x7f0b00d9` (ctv_checkedtext13) | 83 `ACC_DISCOVERY_VEHICLE_IN_FRONT_TONE` | New candidate |
| 34 | Pause LKAS tone | `0x7f0b00da` (ctv_checkedtext14) | 84 `PAUSE_LKAS_TONE` | New candidate |
| 35 | Set front hazard distance | `0x7f0b016b` (tv_text16) | 85 `SET_FRONT_HAZARD_DISTANCE` | New candidate |
| 36 | Minor lane departure system settings | `0x7f0b016c` (tv_text17) | 86 `MINOR_LANE_DEPARTURE_SYS_SETTINGS` | New candidate |
| 37 | Tachmeter Settings | `0x7f0b00db` (ctv_checkedtext15) | 87 `TACHOMETER_SET` | Mapped already |
| 38 | Driver attention monitor | `0x7f0b016d` (tv_text18) | 114 `U_DRIVER_ATTENTION_MONITOR` | New candidate |
| 39 | Sets the remote control on condition → electric tailgate remote-opening condition | `0x7f0b016e` (tv_text19) | 110 `ELE_DOOR_REMOTE_OPEN_CONDITION` | New candidate |
| 40 | Set to apply the external handle to open electrically → electric tailgate external-handle behavior | `0x7f0b00dc` (ctv_checkedtext16) | 111 `ELE_DOOR_OPEN_AUTO_OR_MANULE` | New candidate |
| 41 | Traffic sign recognition system small icon | `0x7f0b013f` (ctv_checkedtext17) | 151 `TRAFFIC_SIGN` | New candidate |
| 42 | Rise warning | `0x7f0b0141` (ctv_checkedtext18) | 152 `RISE_WARNING` | Unresolved semantics; path found |
| 43 | Memory position seat linkage | `0x7f0b0143` (ctv_checkedtext19) | 153 `MEMORY_SEAT` | New candidate |
| 44 | Electronic preloaded seat belt movement mode setting | `0x7f0b0145` (ctv_checkedtext20) | 154 `SEAT_BELT` | New candidate |
| 45 | Turning point guide sign | `0x7f0b0224` (ctv_checkedtext32) | 178 `U_CARSET_D32_D10_B5` | New candidate |
| 46 | Straight line driving assistance | `0x7f0b0170` (tv_text21) | 179 `U_CARSET_D32_D10_B4` | New candidate; writer traced below |
| 47 | Overspeed warning deviation | `0x7f0b0171` (tv_text22) | 193 `U_CARSET_D32_D11_B75` | New candidate; writer traced below |
| 48 | Traffic sign recognition system | `0x7f0b0421` (ctv_checkedtext35) | 194 `U_CARSET_D32_D11_B4` | Unresolved semantics; path found |
| 49 | Traffice Sign Recognition | `0x7f0b0423` (ctv_checkedtext36) | 195 `U_CARSET_D32_D11_B3` | Unresolved semantics; path found |
| 50 | Blind zone | `0x7f0b0172` (tv_text23) | 197 `U_CARSET_D32_D11_B1` | New candidate; writer traced below |
| 51 | Static guide line | `0x7f0b0147` (ctv_checkedtext21) | 155 `STATIC_LINE` | New candidate |
| 52 | Dynamic guide line | `0x7f0b0149` (ctv_checkedtext22) | 156 `DYNAMIC_LINE` | New candidate |
| 53 | Show the camera after reversing | `0x7f0b014c` (ctv_checkedtext23) | 157 `SHOW_CAMERA` | New candidate |
| 54 | Parking space width for reversing | `0x7f0b016f` (tv_text20) | 158 `PARK_SPACE` | New candidate |
| 55 | Rear view dynamic reminder system settings | `0x7f0b01f8` (ctv_checkedtext24) | 159 `REMINDER_SYSTEM` | New candidate |
| 56 | Rear multifunctional system | `0x7f0b01fa` (ctv_checkedtext25) | 161 `MULTI_FUNCTION` | New candidate |
| 57 | Reverse gear tone | `0x7f0b01fc` (ctv_checkedtext26) | 109 `BACK_CAR_BEEP_TONE` | New candidate |
| 58 | Panoramic image initial | `0x7f0b01fe` (ctv_checkedtext27) | No returned field established | Initialization action; writer traced below |
| 59 | Reversing Delay | `0x7f0b0426` (ctv_checkedtext37) | 196 `U_CARSET_D32_D11_B2` | New candidate |
| 60 | Auto open tunk | `0x7f0b0234` (ctv_checkedtext28) | 166 `U_CAMERA_AUTO_OPEN_TUNK` | New candidate |
| 61 | Enter / exit seat position movement | `0x7f0b0096` (ctv_checkedtext1) | 173 `U_CARSET_D32_D8_B6` | New candidate |
| 62 | Rear seat reminder | `0x7f0b0225` (ctv_checkedtext29) | 175 `U_CARSET_D32_D8_B5` | New candidate; writer traced below |
| 63 | Remote window control | `0x7f0b022e` (ctv_checkedtext30) | 190 `U_CARSET_D32_D8_B4` | New candidate; writer traced below |
| 64 | Trunk sensor opening and closing | `0x7f0b022f` (ctv_checkedtext31) | 176 `U_CARSET_D32_D10_B7` | New candidate; writer traced below |
| 65 | rear view auto → automatic mirror folding | `0x7f0b0173` (tv_text24) | 177 `U_CARSET_D32_D10_B6` | New candidate; writer traced below |

## Concrete new writers verified in this pass

All below use SYU command **105**, integer array `[key, value]` (equivalent `cmd(III)` helper for ordinary settings). These are source-derived client calls, not tested vehicle commands.

| Row | Key | Values / callback | Evidence |
|---|---:|---|---|
| Automatic mirror folding | 71 | 0/1 toggle; field 177 | RZC `onClick` shared branch `sswitch_32b` for minus/plus24; key register v8 initialized 0x47; `$1` renderer uses field177 |
| Remote window control | 81 | 0/1 toggle; field190 | RZC `sswitch_51b`, checkbox30 |
| Trunk sensor opening/closing | 70 | 0/1 toggle; field176 | RZC `sswitch_52d`, checkbox31 |
| Rear-seat reminder | 74 | 0/1 toggle; field175 | RZC `sswitch_509`, checkbox29 |
| Straight-line driving assistance | 73 | 0/1 toggle; field179. 1: active within specified speed range; 0: active during cruise control | RZC `sswitch_2c3`, `$1` `pswitch_274`; Chinese value literals |
| Overspeed warning deviation | 79 | 0–3; field193 displays +0, +5, +10, +15 km/h | RZC `sswitch_2e3` and paired plus handler; `$1` `pswitch_29e` / packed value table |
| Blind-zone warning | 82 | 0/1; field197. 1 visual + audible warning; 0 visual warning | RZC `sswitch_307` and paired plus handler; `$1` `pswitch_24a`; Chinese literals |
| Panorama initialization | 48 | value0; completion feedback unresolved | RZC `sswitch_4f0`, checkbox27 |
| Maintenance-information reset | 14 | value0, no completion feedback established | SiYu `sswitch_353`; RZC `sswitch_5ab` → dialog → `$4` → `$4$1.run` |
| Restore initial values | 15 | value0, no completion feedback established | SiYu `sswitch_35a`; RZC `sswitch_5b5` → same dialog path |
| TPMS calibration | 17 | value0, no completion feedback established | SiYu `sswitch_361`; RZC `sswitch_5bf` → same dialog path |

The RZC confirmation callback `$4$1.run` allocates a two-integer array, sets index0 to the passed key, leaves index1 at Java default0, and calls command105. SiYu's reset branches directly pass constant register v3=0 to `setCarInfo`. These are customization/reset operations, not generic OBD diagnostic fault clearing.

Oil/service-life display uses fields **135** (`MAINTANCE_OIL_SERVICE_LIFE_UNIT`), **136** (`MAINTANCE_OIL_SERVICE_LIFE_PN_UNIT`) and **137** (`MAINTANCE_OIL_SERVICE_LIFE`). `uOilSrvLifeUnit` selects km/mile, `uOilSrvLifePN` supplies the minus-sign indicator, and `uOilSrvLife` renders the distance. Do not label this a percentage: the inspected code renders a signed distance.

## Remaining audit work

Every row is inventoried, but most newly inventoried rows still need exact write-enum ranges and full profile/visibility mapping before a production port. The “Traffic sign recognition system small icon”, “Traffic sign recognition system”, and “Traffice Sign Recognition” rows have separate view IDs and feedback151/194/195; the precise semantic distinction is unresolved. Likewise preserve unresolved labels such as “Rise warning” and “Lockout Preset” until corroborated. Header text and default placeholder values were excluded from row counts. Read-only service displays and one-shot reset actions remain explicitly distinguished from persistent settings.
