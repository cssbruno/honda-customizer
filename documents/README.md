# Honda Customizer documentation

## Current app: Joying / FYT

- [Install, use, build and supported decoder profiles](../README.md)
- [FYT panel commands and feedback](HONDA-FYT-PANEL.md) — shared protocol; Cabin integration details are historical context.
- [RZC/BNR decoder findings](research/honda-cluster-analysis/fyt-additional/other-honda/findings.md)
- [Door, lighting and other setting research](research/honda-cluster-analysis/fyt-additional/doors-lights/full-settings-inventory.md) — research does not imply app support.
- [SYU service contracts](research/syu-ms-analysis/REPORT.md)
- [Joying empty-reply transport fix](research/fyt-transport-audit/FINDINGS.md) — command/subscription wire contract and regression evidence.
- [XP Civic 0x4012A mapping](research/XP-4012A.md) — exact profile reported by the owner's head unit and independently traced settings.
- [XP stock data request](research/XP-DATA-REQUEST.md) — missing entry request restored in 3.4.4; new owner decoder identification and remaining feedback limits.
- [XP lower-level packet send path](research/XP-RAW-SEND.md) — verified module command 1008; arbitrary vehicle CAN framing remains unresolved.
- [FYT OBD transport audit](research/XP-OBD-AUDIT.md) — module 12 serial writes and why their routing does not establish XP raw CAN support.

## Custom XP firmware: experimental

- [HCX1 implementation and build](../firmware/cluster-extension/README.md) —
  four new cluster operations for the audited 111 image; not an installable port.
- [HCX1 custom protocol](../firmware/cluster-extension/PROTOCOL.md) — strict
  requests and transaction results; stock SYU receive integration remains pending.

## Original Honda head unit: reference only

These documents describe the earlier OEM implementation. They do not describe features shipped in the current FYT APK.

- [Original Honda app guide](../README-OEM-reference.md)
- [Civic evidence index](research/honda-cluster-analysis/oem-civic/README.md)
- [Complete settings catalog](research/honda-cluster-analysis/complete-settings-catalog.md)
- [Meter Binder protocol](research/honda-cluster-analysis/oem-civic/meter-binder-protocol.md)
- [Camera implementation](research/honda-cluster-analysis/oem-civic/camera-native-implementation.md)
- [Vehicle actions and maintenance](research/honda-cluster-analysis/oem-civic/action-implementation-evidence.md)
- [Compatibility report](OEM-COMPATIBILITY-REPORT.md)
- [Diagnostics](OEM-DIAGNOSTICS.md) and [display controls](OEM-DISPLAY.md)

## Evidence and provenance

Research reports, derived catalogs, source manifests and analysis scripts were copied into this repository from the existing workspace. Cabin's version takes precedence where both locations contained a file. Differing workspace versions are preserved under `research/workspace-versions`; [migration source hashes](research/migration-sources.json) record the inputs before link updates. Shared originals remain available to Cabin.

Historical reports retain original firmware paths, class names and tool commands as provenance. Proprietary firmware binaries, decompiled vendor classes and raw resource dumps remain external; references to those files are not repository links or build dependencies. Historical extraction scripts require their original inputs. The app builds from bundled source and assets.

Unit tests, lint and emulator checks do not establish operation on a physical vehicle. Use the current app README for implemented features and validation limits.
