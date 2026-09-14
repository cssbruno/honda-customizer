# What com.syu.ms does

Inspected 2026-09-14. Static inspection; nothing installed, executed on a head unit, or sent to a vehicle.

**It is the central SYU/FYT hardware integration service.** Apps ask it to operate the radio, audio hardware, vehicle interface and other equipment. It also coordinates important power and source transitions.

**Deeper follow-up:** [29-class investigation with smali evidence](deeper/FINDINGS.md) confirms app cleanup during sleep, conditional reboot, phone-number handling, Bluetooth delegation, local sensors, radio commands and steering-learning operations. Both DEX files were freshly disassembled and all 10,027 classes matched the earlier disassembly.

## Which APK this describes

The main findings concern the local Joying UIS7862 reference, version **2.23.0711.1001**, versionCode 2123071110, compiled 2023-07-11. SHA-256: `4b428302e29c9e2503ccf7844a127f5bed59450736317629aa42b5eb35a9b577`.

The local TEYES TPRO reference has the same package name but versionCode 2011301507, compiled 2020-11-30. Its manifest was inspected for comparison; its implementation was not exhaustively compared. Neither file is confirmed to be the APK installed in your unit.

Fresh manifests, hashes and selected bytecode are in [evidence](evidence/). Earlier detailed protocol/native analysis is in [the Joying replacement audit](../cabin/documents/research/JOYING-REPLACEMENT-AUDIT.md). Existing disassembly at `/tmp/cabin-syu-ms-smali` was used for additional targeted searches; eight important classes were freshly extracted from the hashed Joying APK for reproducible evidence.

## Function map

| Area | What the evidence establishes | Qualification |
|---|---|---|
| Central app interface | `ToolkitService` returns a Binder toolkit; clients obtain subsystem modules and exchange commands/state. | This is the service that compatible launchers and vendor apps use. |
| MCU communication | Initializes serial transport and exchanges framed commands with the head unit's microcontroller. | Device and baud depend on platform properties. |
| Radio | Radio module, FM service, native tuner integration and frequency/band feedback. | Actual tuner and available bands depend on hardware. |
| Sound / DSP | Sound-driver selection, EQ coefficient processing, native I²C writes and audio routing. | Fourteen driver cases exist; this does not identify your fitted DSP. |
| Amplifier | Separate amp module; native mute and DAC controls. | Vehicle amplifier integration is configuration dependent. |
| Bluetooth | Separate Bluetooth module and coordination of Bluetooth audio with phone-projection apps. | Full pairing/call implementation was not reconstructed in this inspection. |
| Vehicle / CAN box | Dispatches MCU vehicle payloads into the selected decoder; exposes the CANBUS module. | Available doors, climate, lights and other data depend on the chosen vehicle decoder. This does not prove raw CAN-ID access or any particular Honda command. |
| Steering controls | Dedicated steering-control module. | Button mapping depends on wiring and configuration. |
| Power | Ignition/ACC-related sleep preparation and acknowledgements, sleep properties, audio/power handling and wake-lock setup. | The complete state machine requires runtime validation. |
| Screen / reversing / lighting | Native interfaces for LCD power, reverse status, video positioning/overlay and LED color. | Interface presence is not proof every board uses every function. |
| GPS / time | Requests GPS updates and NMEA; processes latitude, longitude, speed and time; includes `gps_auto_time` behavior. | Location processing is confirmed; remote upload is not established. |
| Android integration | Boot and package-change receivers, dynamically registered screen/media/projection events, source switching, settings access. | Actual effects depend on branch and platform privileges. |
| Diagnostics | Conditional log recording when `/sdcard/record_debug` exists; test interfaces and CAN-update module. | This does not establish a general Internet firmware updater. |
| Configuration sharing | Exported `com.syu.ms.provider` serves integer configuration/status values. | Inspected insert/delete operations are no-ops; this is not evidence of arbitrary file access. |

## Complete toolkit inventory found

The factory maps IDs 0–19:

| ID | Subsystem |
|---|---|
| 0 | Main / system coordination |
| 1 | Radio |
| 2 | Bluetooth |
| 3 | DVD |
| 4 | Sound / DSP |
| 5 | iPod |
| 6 | TV |
| 7 | CANBUS |
| 8 | TPMS |
| 9 | DVR |
| 10 | Steering controls |
| 11 | Customer / vendor customization |
| 12 | OBD |
| 13 | Test |
| 14 | CAN update |
| 15 | Amplifier |
| 16 | Emitter |
| 17 | G-sensor |
| 18 | Gesture (`gestrue` in vendor spelling) |
| 19 | Local sensors (`Sensors_local`, factory `s0/b`); no mapped service action |

These are implemented factory/interface entries, not a list of accessories installed in your car. DVD, TV, OBD, TPMS, DVR and others may be unused or require separate hardware/apps.

**Correction to the previous audit wording:** `ModuleService` code recognizes 19 actions, while the freshly decoded manifest declares only 16 module actions (main through amp). The toolkit has 20 factories. These are three different counts.

## Permissions and background behavior

Both manifests request the shared system UID `android.uid.system`. The Joying manifest requests power/reboot, input injection, force-stopping apps, task inspection/removal, overlays, storage, logs, settings, Wi-Fi changes, Bluetooth, network access, precise/coarse location and wake locks. Requested permissions describe intended capability; installed signing and platform policy determine effective access.

The manifest declares services and receivers rather than a launcher activity. It includes a boot receiver, package-added/removed receiver, and an explicitly exported content provider. The service declarations have intent filters and no explicit component permission. This warrants a separate caller-authorization audit; it is not by itself proof of an exploitable vulnerability.

The GPS helper requests updates with a 1,000 ms minimum interval and zero minimum displacement, and registers an NMEA listener. Android may deliver at a different cadence. It reads location, speed and time and includes clock-setting logic.

Targeted bytecode searches found no explicit remote HTTP endpoint or direct Java URL/socket construction in the non-framework/vendor portions searched. Apparent YouTube/Flickr URLs occur in bundled Google message-parser code; XML URLs are feature identifiers. These strings alone do not demonstrate network traffic. The INTERNET permission, external native dependencies and other cooperating services mean this is **not a complete privacy or malware clearance**. No packet capture or full native-code audit was performed.

Shell execution exists: the log recorder invokes logcat, and selected decoder helpers include `su`/`ru` and `/dev/alarm` permission/time-setting code. These paths were read only; their presence does not establish that they run on your selected profile.

## What removing it would mean

Given these dependencies, disabling/replacing it could interrupt radio, sound, steering controls, vehicle data and ignition sleep behavior. This is an inference from the service and hardware call paths, not a removal test. Even `ToolkitService.onDestroy()` sends a hardware message, so stopping it is not necessarily passive.

For Cabin, the practical distinction is that replacing the visible launcher still leaves this service doing the hardware work. Replacing this APK would require reproducing the hardware protocols, client interfaces, drivers and power behavior.

## Limits of “fully”

This report maps the service's major responsibilities and complete discovered toolkit inventory. It does not claim every branch was reverse engineered. Unknowns include the APK actually installed on your unit, selected CAN profile, enabled accessories, full Bluetooth behavior, every native-library operation, all authorization checks and real network activity. Exact behavior requires the matching installed APK/native libraries and observation on the matching hardware.
