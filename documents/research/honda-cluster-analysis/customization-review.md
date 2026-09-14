# Honda customization implementation: continued review

Static review of the supplied 1.F197.60 firmware. No vehicle connection or command execution was used. Cabin application code was not changed.

## Main findings

1. **The catalog overstates usable coverage if treated as implemented controls.** The XML has 313 settings, including 95 meter settings. Only 46 meter entries have an InfoFrom route; 49 have none. This is routing coverage in this firmware, not proof of support on a Civic Brasil.
2. **The tachometer toggle is implemented in the stock Settings app.** It uses category 0x03, id 0x5C. API value 1 means on; 2 means off. The XML maps those values to encoded values 0 and 1, with source 0x50 / menu 0x2E. These three identifier/value layers must stay separate.
3. **Settings are discovered dynamically.** Function=false is an initial state, not a permanent unsupported flag. The service resets discovery state, requests vehicle data and starts a customization capability check. Enabled categories and setting IDs come from the resulting function flags.
4. **An immediate API return is not confirmation that the dashboard changed.** The UI processes onNotifyCustomizeResult(type,result), then refreshes discovery/readback. In the direct write branch, a RemoteException is caught and execution can still reach return 0. A replacement must await the callback and verify the returned setting.

## Routing coverage for prominent meter entries

All IDs below are category 0x03. Source/menu are internal routing fields; this is not an OBD command list.

| Feature | Setting ID | XML source / menu |
|---|---|---|
| Digital speedometer units | 0x25 | No route |
| Ambient color change | 0x28 | 0x50 / 0x16 |
| Ambient color selection variants | 0x29 / 0x2A | 0x50 / 0x32 and 0x33 |
| Vehicle-speed units | 0x36 | 0x50 / 0x1D |
| Turn-by-turn display | 0x38 | 0x50 / 0x20 |
| Display contents / favorites | 0x3D / 0x3E | No route |
| Wallpaper selection / load / deletion | 0x41–0x43 | No route |
| Opening animation / background color | 0x45 / 0x46 | No route |
| Distance units | 0x56 | 0x50 / 0x0E |
| Speed and distance units | 0x57 | 0x50 / 0x2F |
| Meter configuration | 0x5B | 0x50 / 0x15 |
| Tachometer | 0x5C | 0x50 / 0x2E |

For missing or unresolved routes, getInfoFrom returns [-1,-1]; requestCustomizeSettingChange returns -1. This conclusion applies to the reviewed configuration and service path, not all Honda firmware versions. Labels for unit/color choices have not yet been tied to individual API values.

## Stock tachometer flow

SystemSettingActivity.requestTachometerSettingChange first calls the unit-information wrapper's setTachometer, constructs CustomizeSettingData(0x03,0x5C,value), sets customization mode according to isDriving(), then calls requestCustomizeSettingChange. The wrapper's persistence implementation was not traced in this pass.

The service resolves the route from the runtime source set and translates the value through DataList. The direct branch constructs BcanCommonData and calls CpuComService.notifyBcanCustomFrame. This is confirmation of the head-unit service path, not validation of an external adapter implementation.

SystemSettingActivity$12.onNotifyCustomizeResult exits customization mode, marks a tachometer refresh when result is 0, and requests categories again. onNotifyCustomizeSettingValue reads the returned tachometer entry and translates its value back to the UI representation.

The UI also checks whether a value is available, whether it is awaiting information, and its driving-operation status before enabling the tachometer control. A port should preserve these behavior checks; the exact driving-status policy needs its own trace.

## Runtime rules a replacement must preserve

- Use runtime-supported category/ID pairs and returned setting metadata. Do not expose every XML entry automatically.
- getInfoFrom chooses an available source from mFunctionInfoFromList; source 0 is a separate indirect customization path.
- getListValue can gather multiple settings sharing a source/menu. It substitutes the requested value for the target and uses cached current values for siblings, in table order. Read current values before changing such a group.
- Enumerated DataList keys are API values; DataList values are encoded values. Non-enumerated types take a separate path.
- Discovery is active: requestCustomizeCategory calls requestBcanReceiveData and notifyCustomCheckStart. Calling requestCustomizeMode also sends through notifyVehicleCustomize. Neither should be described as merely reading a local cache.
- Await callbacks, handle timeout/disconnection, and verify readback. A transport/API return alone is insufficient.
- The XML repeats outer Item IDs, but category/ID pairs are unique across all 313 entries. Use category/ID for identity.

## Evidence locations

Paths are relative to this directory; line numbers refer to the saved smali files.

- decompiled/DaSettings/com/mitsubishielectric/ada/app/dasettings/SystemSettingActivity.smali:1765 — on/off to API-value conversion; :2063 inverse conversion; :2241 complete tachometer request; :11137 control availability.
- decompiled/DaSettings/com/mitsubishielectric/ada/app/dasettings/SystemSettingActivity$12.smali:215 — result callback; :316 setting readback.
- decompiled/VehicleInfoManager/com/mitsubishielectric/ada/appservice/vehicleinfomanager/CustomizeSettingControl.smali:1205 — enabled categories; :1708 enabled IDs; :1872 route resolution; :2064 grouped value encoding; :3214 direct capability flags; :4739 readback decoding.
- decompiled/VehicleInfoManager/com/mitsubishielectric/ada/appservice/vehicleinfomanager/VehicleInfoManagerApService$7.smali:9147 — discovery; :9226 mode; :9555 write path.
- vehicle_customize_config.xml:3371 — tachometer entry.
- meter-route-audit.json — all 95 meter entries, routes, value maps, source lines, and explicit unverified live-support status.

## Verification and remaining scope

Re-deodexed DaSettings using API 17 and the update's system/vendor framework classpaths. Cross-checked the tachometer UI, service, and XML; counted all entries and checked identity uniqueness. No runtime or in-car test has been performed.

The next integration decision still requires the actual head-unit runtime and vehicle variant. An original Mitsubishi Electric Honda head unit can potentially use this Binder service after binding/permission requirements are verified. Cabin on TEYES/FYT needs a separately verified vendor transport mapping; Honda Binder transaction numbers cannot be copied into SYU commands.
