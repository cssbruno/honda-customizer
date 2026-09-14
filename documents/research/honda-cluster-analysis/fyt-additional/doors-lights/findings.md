# Additional FYT Honda settings: door, lights, remote, assistance

Read-only APK research, 2026-09-13. **27 distinct extra WC-family controls** identified: 5 door + 5 lighting + 4 remote + 13 safety/assistance. All 27 have click-command and callback paths. They are not evidence that the Brazilian 2020 EXL has all these functions. No app changes or vehicle writes were performed.

Source: `cabin/artifacts/joying-uis7862/extracted/applications/app/190000000_com.syu.canbus/190000000_com.syu.canbus.apk`. Original source SHA-256 record is `../../fyt/source.json`. This folder contains apkanalyzer smali for four activities and all their inner classes, decoded layouts, and resource-annotated copies. `resources.txt` is the full aapt2 resource table. Suffix `.annotated` files are generated reading aids; original `.smali` files preserve extracted evidence.

## Wire format and profile scope

Every command below is `DataCanbus.PROXY.cmd(command, [key, wireValue], null, null)`; decimal numbers are used. Feedback is `DataCanbus.DATA[field]`, dispatched by each activity's `$1.onNotify` and subscribed in `addNotify`. These are SYU client commands, **not raw Honda CAN frames or OBD PIDs**.

`../../fyt/com.syu.carinfo.honda.HondaIndexActi.smali`, `showWCCarInfo`, routes the settings menu when `(profile & 0xffff) == 0x141`. `Wc_16Civic_FunctionalActi$1/$2/$3/$4` then route respectively to Light, Remote, Door, and Safty pages. These four listeners are saved here and have no additional profile tests.

The Light page's `onResume` explicitly hides its first three rows (wiper/headlight linkage and both sensitivity controls) for profiles **0x50141 and 0x60141**; it shows them otherwise. No per-profile checks were found in Door, Remote, or Safty. No hidden rows were found in those layouts. This is a vendor menu-routing fact, not a per-car capability allowlist; do not silently widen Cabin's verified profile gates based on the menu alone.

## Door: `Wc_16Civic_DoorActi`

| Control | Cmd | Key | Feedback | Wire values / displayed meaning | Click evidence |
|---|---:|---:|---:|---|---|
| Walk-away lock (vendor: “Leaving the lock personalized settings”) | 104 | 3 | 58 | 0 off, 1 on | $2 |
| Remote-lock acknowledgement (vendor: “Remote lock up tips”) | 104 | 1 | 60 | 0 off, 1 on | $5 |
| Automatic relock time | 104 | 2 | 59 | 1=30s, 2=60s, 3=90s; 0 feedback invalid | $3/$4 |
| Automatic door-lock condition | 104 | 4 | 87 | 0 off, 1 speed-linked, 2 shift out of P | $6/$7 |
| Automatic door-unlock condition | 104 | 5 | 86 | 0 off, 1 shift into P, 2 ignition off | $8/$9 |

Labels are joined from `door.xmltree.annotated`; feedback meanings are in `m65D10/11/13/14/16`. Fields 58–60 are low-byte masked; 86–87 are used directly by this UI.

## Lighting: `Wc_16Civic_LightActi`

| Control | Cmd | Key | Feedback | Wire values / displayed meaning | Click evidence |
|---|---:|---:|---:|---|---|
| Wiper/headlight linkage | 102 | 5 | 49 | 0 off, 1 on | $2/$3 |
| Automatic interior-light sensitivity | 102 | 4 | 50 | 0 min, 1 low, 2 mid, 3 high, 4 max | $4/$5 |
| Automatic headlight sensitivity | 102 | 3 | 51 | 0 min, 1 low, 2 mid, 3 high, 4 max | $6/$7 |
| Automatic headlight-off delay | 102 | 2 | 52 | 0=0s, 1=15s, 2=30s, 3=60s | $8/$9 |
| Interior-light dimming time | 102 | 1 | 53 | 1=15s, 2=30s, 3=60s; 0 feedback invalid | $10/$11 |

All feedback is low-byte masked. The vendor dimming-time click handler cycles through 0 even though its display calls 0 “Invalid”; an implementation should omit 0 as a selectable value. First three rows have the exclusions above. Evidence: `light.xmltree.annotated`, `m67D00/03/10/12/14`, `onResume`.

## Remote: `Wc_16Civic_RemoteActi`

| Control | Cmd | Key | Feedback | Wire values / displayed meaning | Click evidence |
|---|---:|---:|---:|---|---|
| Keyless-access buzzer | 103 | 4 | 54 | 0 off, 1 on | $2 |
| Keyless-access exterior-light acknowledgement | 103 | 3 | 55 | 0 off, 1 on | $3 |
| Keyless alarm volume (ambiguous translation) | 103 | 2 | 56 | 0 “Close”, 1 “Distant” in vendor resources | $4 |
| Horn with remote start | 103 | 1 | 57 | 0 off, 1 on | $5 |

All feedback is low-byte masked. The third row's label says volume, but its display uses distance resources; **do not invent low/high wording without clarification from other source code or hardware**. Evidence: `remote.xmltree.annotated` and `m66D10/11/12/13`.

## Safety / driving assistance: `Wc_16Civic_SaftyActi`

| Control | Cmd | Key | Feedback | Wire values / displayed meaning | Click evidence |
|---|---:|---:|---:|---|---|
| LKAS suspension tone | 105 | 3 | 62 | 0 off, 1 on | $12 |
| ACC vehicle-ahead detection tone | 105 | 2 | 63 | 0 off, 1 on | $13 |
| Lane-departure system behavior (vendor: “Minor lane departure system”) | 105 | 4 | 61 | 1 middle, 2 wide, 3 warnings only; 0 invalid | $14/$15 |
| Forward-collision warning distance | 105 | 1 | 64 | 1 far, 2 middle, 3 near; 0 invalid | $16/$17 |
| LaneWatch with turn signal | 110 | 12 | 93 | 0 off, 1 on | $2 |
| LaneWatch display duration after turn signal | 110 | 12 | 94 | wire 4 => feedback 0 = 0s; wire 5 => feedback 1 = 2s | $10/$11 |
| Rear-view dynamic reminder (vendor wording) | 110 | 14 | 95 | 0 off, 1 on | $3 |
| “Rise warning” (meaning unresolved) | 105 | 8 | 96 | 0 off, 1 on | $4 |
| Driver attention monitor | 105 | 7 | 97 | 0 off, 1 visual, 2 tactile + visual warnings | $8/$9 |
| Seat-position memory linkage | 106 | 11 | 98 | 0 off, 1 on | $5 |
| Electronic preloaded seat-belt movement mode (vendor wording) | 106 | 12 | 99 | 0 off, 1 on | $6 |
| “SWITCH LOCK” (meaning unresolved) | 106 | 10 | 100 | 0 off, 1 on | $7 |
| Lane-departure prevention behavior | 105 | 9 | 101 | 1 standard, 2 delay, 3 warning only, 4 advance | $18/$19 |

Feedback meanings are in activity `m68D10/12/13/14`, `m_lane_watch_light`, `m_lane_watche_durtion`, `m_laneoffset_show`, and the other descriptively named `m_*` methods. `$1.onNotify` proves field-to-renderer mappings. For fields 61/64, vendor click handlers cycle through 0 despite “Invalid” display: omit this option. The LaneWatch-duration feedback-to-wire offset is essential: it shares command/key 110/12 with LaneWatch enable, with different value ranges. The 4/5 mapping is inferred directly from the click toggle and 0/1 renderer, not measured on hardware.

Fields 61–64 and 101 are low-byte masked in the getters/clicks. The extended `$1` feedback path passes full values for 93–101 into renderers; most boolean click handlers mask to the low byte, while duration/attention clicks use full data. A port must validate returned ranges rather than treating every unknown value as off.

## Difference from Cabin today

`cabin/app/src/main/kotlin/com/cabin/platform/SyuHondaPanelProtocol.kt` and `SyuFactoryControls.kt` implement the existing 13 panel controls, none of these 27 WC control paths. `SyuVehicleTelemetry.kt` already contains three **BNR** lighting controls (fields 122–124, a different profile list and command schema) and two amplifier controls (201–202). Headlight sensitivity, headlight delay and interior delay overlap in user meaning with those BNR controls, so the 27 count is additional WC-family control mappings, not 27 globally new UI concepts. Those three BNR mappings cannot be reused as WC bytes.

No validated OBD codes, raw B-CAN frames, speed-unit switching, tachometer, wallpaper, or diagnostic commands were found in these four pages. Unresolved issues before a production port: exact variant capability detection, ambiguous translated labels, safety equipment availability, and in-car feedback validation.
