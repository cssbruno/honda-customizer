# HCX1-dev.1 — experimental firmware development package

The custom firmware extension now constructs and sends four extra Honda
cluster operations through the audited 111 CAN queue: meter configuration,
turn-by-turn display and two color-selection variants. Twenty enumerated API
values are translated from the OEM XML. This is newly implemented HCX code,
not a newly discovered stock XP feature.

Five checked ARM call-site patches integrate initialization, serial dispatch,
main-loop polling, CAN transmission and CAN reception. The extension rejects
invalid/concurrent requests and tracks queued, mailbox-started, matching-reply,
timeout and failure results. Expired queued custom frames are suppressed
before the hardware writer. Existing stock dispatch is preserved.

## Validation

- 21 repository Python/C test methods passed, including 10 new HCX methods.
- All 20 supported values match the independent OEM XML reconstruction.
- 1,772 invalid feature/value combinations are rejected by the C tests.
- 31 isolated ARM cases pass, exercising the five actual patched call sites.
- Three unknown/modified firmware inputs are rejected before producing output.
- The combined image and extension are reproducible with the recorded compiler.

These use explicit test fixtures, not a physical CAN controller or vehicle.
They verify code and hook behavior, not board, bootloader or installed-box
compatibility. Firmware source and host fixtures remain outside the APK.

Experimental combined-image SHA-256:
`506b97070a41826a24d1888c6272c709c9ffcf6eaef6d2c9076db5ef99fa6057`.

## Distribution status

**Not installable.** The local development bundle contains the experimental
memory image, extension, ELF, sources, protocol, hashes and ARM report. It is
not a `.UPDE` container or an APK, and is not published through the app updater.
Manufacturer-derived memory images remain external to this repository.

The owner's 108 firmware/board, RAM/flash layout, recoverable backup and
updater integrity format still need validation. The current SYU/Android
receive path for the new HCX `D7` response also remains unresolved. There is
no physical installation or end-to-end vehicle success claim.

[Source and build instructions](../firmware/cluster-extension/README.md) ·
[Custom protocol](../firmware/cluster-extension/PROTOCOL.md)
