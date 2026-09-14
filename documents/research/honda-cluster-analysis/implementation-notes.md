# Implementation investigation

Current status: Honda Customizer is retained as a separate FYT-only app. Original Honda implementation details below are historical research and are excluded from the FYT APK.

**Latest research scope (2026-09-13): original Honda Civic only.** See the [Civic OEM investigation](oem-civic/README.md) for the newly recovered language, instrument-content/preset and model-configuration evidence. The transport discussion below records the earlier implementation investigation; it is not the Civic-only feature inventory.

The Cabin repository currently implements TEYES/FYT/SYU vehicle interfaces. Its existing TeyesClusterMediaBridge is explicitly bound to vendor behavior and Civic profile 0298. Mitsubishi Electric Honda Binder transactions must not be interpreted as SYU command IDs.

Recovered the actual Dalvik implementation with baksmali API 17 and the update's own system and vendor boot classpaths. Initial disassembly at the default API failed; API 17 disassembly and subsequent deodexing succeeded. Decompiled evidence is preserved in decompiled/.

## Confirmed Honda API

Interface: com.mitsubishielectric.ada.appservice.vehicleinfomanager.IVehicleInfoManagerApService

- 0x4D: registerVehicleCustomizeCallback
- 0x4E: unregisterVehicleCustomizeCallback
- 0x4F: requestCustomizeMode(boolean)
- 0x50: requestCustomizeReset
- 0x51: requestCustomizeCategory()
- 0x52: requestCustomizeSettingValue(int)
- 0x53: requestCustomizeSettingChange(CustomizeSettingData)

These are Android Binder transaction numbers, not OBD service IDs or CAN arbitration IDs.

CustomizeSettingData.writeToParcel order: category int, id int, value int, dataType int, dataRange int array, FOBKey boolean array. The proxy includes the interface token and a presence marker for the Parcelable.

The service requestCustomizeSettingChange method resolves getInfoFrom(category,id), translates the selected value using getListValue, constructs BcanCommonData, and populates a B-CAN request. One branch starts the payload with 0x40 followed by the resolved setting identifier. B-CAN addressing depends on vehicle/navigation state. This is only a branch of the implementation and is not a standalone validated OBD command recipe.

## Confirmed implementation target

The user confirmed a Brazilian Civic 2020 EXL and subsequently replaced TEYES with Joying. They want support for all Civic versions rather than only this trim, covering units, appearance, displayed information and investigation of diagnostics. Cabin on the current Joying is the integration target; the original-Honda client below is a separate runtime. Exact Joying model, platform, firmware, CAN decoder and selected CANBUS profile remain unknown.

The workspace contains a Joying UIS7862 Android 10 firmware reference (2023-08-31) in ../cabin/artifacts/joying-uis7862/. Its SYU service evidence is useful for investigation, but it is not yet a confirmed match for the user's new unit. Existing Cabin Honda lighting and amplifier mappings are profile-specific and do not establish dashboard units, appearance, tachometer or diagnostic support. The earlier missing-framework observation was superseded by the decompiled Honda service findings below; the remaining gap is the compatible Joying vehicle transport, not locating Honda setting names.

Broad compatibility is a requested goal, not verified coverage. Implement support by verified decoder/profile capabilities and available vehicle settings, rather than assuming all years/trims use identical commands. Use the user's 2020 EXL as the first validation vehicle. Additional generations, trims and decoders need their own evidence before enabling customization writes.

The existing Cabin cluster media bridge sends legacy media broadcasts; it does not implement Honda dashboard customization. Existing Civic climate/telemetry profile support also does not prove customization support. The missing implementation dependency is a verified TEYES/SYU customization command and readback mapping for the actual CAN decoder/profile. Honda Binder transaction numbers must not be reused as SYU commands.

No dashboard-customization vehicle-writing implementation or firmware patch has been made. Existing Cabin uncommitted changes were preserved.

## Continued review

See [customization-review.md](customization-review.md) for the verified stock tachometer flow, dynamic capability rules, callback requirements, and corrected meter routing coverage (46 of 95 entries have XML routes). Machine-readable evidence: [meter-route-audit.json](meter-route-audit.json).

## APK client

An installable original-Honda-head-unit client has been built in ../honda-customizer/. See its README for implemented list/range controls, readback verification, tests, and the still-unverified special actions/hardware compatibility. This is not an OBD or TEYES implementation.

## FYT implementation progress

User requests proceeding without a decoder-model prerequisite. Added native Cabin Honda panel controls using the Joying reference app's `Wc_16Civic_Pannel` command listeners and callback fields. Profile selection is automatic from the existing SYU profile callback (1000); the implementation uses six explicit profile IDs present in the source visibility branches. No user-entered year/trim is used to guess commands.

See ../cabin/documents/research/HONDA-FYT-PANEL.md for the 13 controls, exact mappings and remaining scope. This supersedes the earlier statement that no FYT dashboard control mappings were located. Honda stock units, wallpaper, tachometer and diagnostics still have no implemented FYT equivalent. No vehicle testing has occurred.

## Parallel search for additional settings

Three agents reviewed the stock catalog and separate FYT Honda screens. See [additional-settings-review.md](additional-settings-review.md). New findings include FYT speed/distance-unit and two tachometer command/readback pairs, 27 additional WC door/light/remote/assistance mappings, and a WC language writer without verified readback. This supersedes the earlier lack of FYT unit/tachometer mappings as an investigation status; those controls remain unimplemented in Cabin. BNR units visibility restrictions and differing profile dialects must be preserved. This pass changed research artifacts only and sent no vehicle commands.

## Complete catalog and language audit

The previous research summary was not exhaustive. The subsequent [configuration audit](configuration-audit.md) exports all 313 stock XML entries with identifiers, routes, value maps and explicit missing-label status, and audits all 15 stock language entries. Stock South American cluster language is confirmed: 03/0E API1→encoded0→Português, API2→encoded1→English.

FYT RZC has a distinct 33-language picker including Portuguese: command 105, integer array [85,15], null float/string arrays. Its writer and menu path are traced, but vehicle-language readback is not established. The three-language WC Civic picker does not describe all FYT language support. These new controls remain research findings, not newly implemented Cabin features.
