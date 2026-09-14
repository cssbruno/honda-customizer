# Honda dashboard configuration findings

Source: `/home/bruno/Downloads/1.F197.60.zip`, containing `1.F197.60/1110/SwUpdate.mdt`. The MDT is a ZIP archive with 5,575 entries. Original update files were not modified and no vehicle commands were sent.

## What is confirmed in the files

The original system/app extraction omits the vendor apps and frameworks. The full update contains DaSettings, VehicleInfoManager, VehicleCoordination, CpuComService, DealerDiag, DeveloperDiag, LetDiag, and honda-framework.

`system/vendor/etc/vehiclesetting/vehicle_customize_config.xml` includes the following category 0x03 entries. Translations below are descriptive translations of the source comments.

| Feature | Category / Id | XML line |
|---|---|---|
| Digital speedometer units | 0x03 / 0x25 | 2209 |
| Vehicle-speed display units | 0x03 / 0x36 | 2574 |
| Distance units | 0x03 / 0x56 | 3233 |
| Speed and distance units | 0x03 / 0x57 | 3255 |
| Ambient meter color change | 0x03 / 0x28 | 2274 |
| Ambient meter color selection, two variants | 0x03 / 0x29, 0x2A | 2296 |
| Display contents / favorites | 0x03 / 0x3D, 0x3E | 2724 |
| Wallpaper selection / image load / deletion | 0x03 / 0x41–0x43 | 2794 |
| Opening animation / background color | 0x03 / 0x45, 0x46 | 2874 |
| Meter display configuration | 0x03 / 0x5B | 3348 |
| Tachometer display | 0x03 / 0x5C | 3371 |
| Turn-by-turn guidance in meter | 0x03 / 0x38 | 2618 |
| South American Portuguese language setting | 0x03 / 0x0E | 1671 |

The DaSettings resource string pool includes “Meter Setup”, “Change Units for Digital Speedometer Display”, “Tachometer Setting”, “km/h”, and “mph”. This confirms relevant UI text exists, but the exact resource-to-setting value mappings have not been decoded. Generic wallpaper/background strings may also describe head-unit screens; their existence alone cannot prove dashboard applicability.

## Likely control path, not yet a decompiled call graph

DaSettings → VehicleInfoManager customization API → CpuComService / vehicle communication → instrument cluster.

Evidence: DaSettings contains VehicleInfoManager listener and CustomizeSettingData references. VehicleInfoManager contains CustomizeSettingControl, the XML path, requestCustomizeCategory, requestCustomizeMode, requestCustomizeSettingValue, requestCustomizeSettingChange, notifyBcanCustomFrame, and B-CAN MET_CUSTOM logs. CpuComService includes B-CAN send queue code names and F-CAN meter-speed callbacks. These symbols support the inferred architecture, but strings alone do not establish precise call order or payloads.

## Diagnostic / OBD-related findings

DealerDiag and DeveloperDiag resource pools include B-CAN, F-CAN, Meter Display, Meter Color Variation, Snap Shot Meter, DTCs, and TCU DTCs. CpuComService contains requestDtcRead, requestDtcClear, onDtcRead, onDtcClear and callbacks named for engine, AT, EPS, SRS, VSA, ACC and LKAS DTC data.

This is evidence of internal diagnostic functionality. It does not establish compatibility with an external OBD adapter, a generic OBD PID, or a validated command for coding the cluster. DTC clear methods were only observed as strings; none were executed. Diagnostic labels do not establish that each item is a writable setting.

## Limits and next evidence needed

This is a multi-variant configuration catalog, not a live dump of the user's enabled options. Function fields in the inspected entries are false and do not by themselves establish whether an option is disabled on a particular car. Feature negotiation and vehicle-specific selection require further code analysis or a live read-only capability dump.

The category/Id pairs are internal XML setting identifiers, NOT CAN frame IDs, UDS data identifiers, or OBD PIDs. DataList and InfoFrom do not yet supply a validated bus payload. The vehicle model, year, market and cluster/head-unit version are needed to narrow the supported subset. No support for arbitrary new cluster graphics or firmware flashing has been established.

The meter-settings-catalog.md file lists all category 0x03 entries, original comments, values and source lines. Companion *-strings.txt and *-resources.txt files contain filtered binary strings and decoded resource-pool text; they are evidence indexes, not decompiled source.
