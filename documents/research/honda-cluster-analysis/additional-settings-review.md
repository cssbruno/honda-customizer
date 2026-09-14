# Additional Honda settings review

Parallel review of the original Honda firmware, FYT Civic pages and other FYT Honda variants. The existing 13 Cabin panel controls are a subset. This review adds evidence only; it does not change the APK or send vehicle commands.

For the subsequent complete stock catalog and finished language trace, see [configuration-audit.md](configuration-audit.md).

## Main new findings

- **Speed/distance units:** FYT command 105, key 21, callback field 77; 0 selects km/h and km, 1 selects mph and miles. Found in both SiYu and RZC settings code. The BNR entry route hides the units row, so the handler alone is not enough to enable it there; the RZC screen route and restrictions are documented separately.
- **Tachometer display:** FYT command 105, key 22, feedback 78, values 0/1. The SiYu screen is routed from eight exact profiles, including BNR Civic profiles. This is new FYT transport evidence beyond the original Honda Binder trace.
- **A second tachometer setting:** command 105, key 35, feedback 87, values 0/1. Its precise difference from the display toggle is unresolved; keep both identifiers separate.
- **27 additional WC-family control mappings:** 5 door, 5 lighting, 4 keyless/remote and 13 assistance settings. Three lighting concepts overlap Cabin’s existing BNR controls but use different commands/fields. Examples include walk-away locking, auto lock/unlock, relock delay, lighting timers, LaneWatch and driver-attention preferences.
- **WC panel language writer:** command 112 with [1, language], English/Chinese/traditional Chinese. No verified language feedback callback or Portuguese option in this picker. The original Honda firmware separately contains a South American Portuguese setting.

These are findings for future implementation. None of the newly discovered controls was added to Cabin in this research pass. The two tachometer settings and units belong to a different FYT profile path than the existing WC command-106 panel controls.

## Original Honda catalog

| Category | Catalog entries | Entries with nonempty XML routes |
|---|---:|---:|
| TPMS | 1 | 1 |
| Driver assistance / cameras | 68 | 19 |
| Instrument panel | 95 | 46 |
| Seat / steering-position settings | 6 | 3 |
| Keyless / remote | 28 | 12 |
| Lighting | 22 | 13 |
| Locks / windows / mirrors | 55 | 38 |
| Wipers | 34 | 31 |
| Power tailgate | 3 | 3 |
| IDS drive mode | 1 | 1 |
| **Total** | **313** | **167** |

Source: [vehicle_customize_config.xml](vehicle_customize_config.xml). Counts include variant entries and defaults. Nonempty routes do not prove working controls on a particular car.

Additional routed panel entries include Portuguese for South America (03/0E), REV/ECO indicators (03/23–24), ambient color variants (03/29–2A), ECON mode retention (03/34), speed units (03/36), gear display (03/51–52), turn-signal sound (03/53), tire-angle monitor (03/55), distance units (03/56), combined speed/distance units (03/57), interior-light dimming (03/58), trip-computer units (03/5A) and tachometer visibility (03/5C).

The original Honda service has concrete setters for ACC detection sound and speed units, belt-fit assistance, FCW/CMBS timing, lead-vehicle movement notification, LKAS disengagement sound, road-departure mitigation, predictive ACC and rev matching. Separate methods implement TPMS calibration and maintenance reset. See `VehicleInfoManagerApService$7.requestCustomizeSettingChange`, `requestTPMSCalibration` and `requestMaintenanceReset` under [decompiled/VehicleInfoManager](decompiled/VehicleInfoManager/).

Important routing caveat: source 0 uses special handling. XML source0/menu5 (cruise-control speed units, category06/id01) has no corresponding setter in the reviewed generic write method; TPMS source0/menu3 uses its separate method. Do not equate the 167 nonempty routes with 167 implemented writes.

Wallpaper, opening animation, display favorites and the specific digital-speedometer entry still have no routes in the reviewed Honda XML. Their labels are not proof of working controls.

## FYT evidence

Detailed additional Civic page findings: [doors, lighting, remote and assistance](fyt-additional/doors-lights/findings.md).

Detailed unit, tachometer and other Honda-variant findings: [other Honda settings](fyt-additional/other-honda/findings.md).

Adjacent Accord settings and HVAC diagnostic-display evidence: [Accord findings](fyt-additional/accord/findings.md). Accord screen colors/brightness are not established as Civic cluster customization.

The stock Joying `HondaIndexActi.showWCCarInfo()` routes profiles whose low word is 0x141 into the Civic functional menu. Individual screens apply further visibility rules. Cabin currently supports six explicit panel profiles, so its existing implementation is intentionally narrower than this source menu route; broad menu routing alone does not prove every control exists on every vehicle.

A class-reference comparison found 57 Honda/Accord-related classes in the inspected Joying APK and 46 in the TEYES reference, with no TEYES-only classes in that filtered comparison. This is a class-name comparison, not proof that shared bytecode is identical. [Source hashes](fyt-additional/sources.json) and [class inventories](fyt-additional/class-comparison.json) are saved.

## Compatibility boundary

Original Honda category/setting IDs, FYT command IDs and physical CAN messages remain separate layers. Additional settings found in code are not automatically implemented or confirmed on a 2020 EXL. The implementation must use the correct profile, fresh feedback and verified command values for each control.
