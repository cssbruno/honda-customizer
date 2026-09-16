# HCX1 — experimental Honda XP cluster extension

**Status: implemented and tested in isolated execution; not an installable
firmware release.** This is a new custom protocol and five application hooks
for the SHA-pinned **CRI V1.13.111BYPT** binary. It is not a discovered feature
of stock XP firmware. The owner's reported **108** hardware is not validated.

The owner explicitly authorized custom firmware after the earlier APK-only
investigation. This directory implements that new scope. No device has been
flashed. No manufacturer update container is generated, and no Android APK
is changed to claim compatibility.

## Implemented

- Four extra cluster operations, with 20 enumerated OEM API values: meter
  configuration, turn-by-turn display and the two color-selection menus.
- New `D7` HCX1 serial command with a transaction identifier and strict
  revision/length/value checks. Other packet types retain the original dispatcher.
- CAN frame construction and insertion into the audited vendor queue.
- Separate queued, mailbox-started, matching-response, timeout and failure
  results. A queue acceptance or mailbox number never means vehicle success.
- Only one pending operation; no retries. Full queues, duplicate last
  transaction IDs and concurrent writes are rejected.
- An expired queued custom frame is cancelled at the transmit hook before
  the hardware writer. New requests stay blocked until that owned frame drains.
- Replies require the corresponding CAN direction, extended-ID flag, data
  frame, exact DLC, `C0`, menu and encoded value after mailbox submission.
  Wrong, early or late replies do not complete the operation.

See [PROTOCOL.md](PROTOCOL.md) for the complete new wire contract. This is a
bounded cluster extension; it does not expose arbitrary CAN/OBD transmission.

## Exact port

Original SHA-256:
`df254fb42d5fe02e985ab57f9d23fcce963fa70868e337eb9941a99c8196bf99`.
The input is the manufacturer BIN described in the
[read-only audit](../../documents/research/XP-BOX-FIRMWARE.md).

| Call site | Original target | Replacement |
|---|---|---|
| `080151D6` | `08015260`, verified no-op | Initialize HCX state |
| `0800E9C6` | `080133C0`, after checksum validation | Dispatch HCX or original packet |
| `08015256` | `0800E74A` | Preserve original call, then poll HCX |
| `0800CC68` | `080110E8`, CAN mailbox writer | Check expiry, preserve writer, record mailbox result |
| `0800CA9C` | `0801125E`, CAN receive reader | Preserve reader, then inspect matching response |

The extension starts at `08016400`. Build checks pin the entire original
binary and every original call instruction, reject overlap and unexpected
runtime data sections, and verify that only these five original call sites
changed. Original bootloader bytes are preserved. The CAN mailbox writer
mutates its input ID, so matching occurs before calling it.

State access is protected with PRIMASK and the previous mask is restored.
No serial reply is sent inside the receive interrupt or masked state section.
The transmit and queue operations use their existing nonblocking interfaces.
The timeout is **800 vendor counter ticks**, not a verified number of seconds.

## Build and reproduce

Requires a host C compiler, ARM-capable Clang/LLVM and Unicorn 2.1.4 for the
isolated ARM audit. The local Android NDK LLVM tools can cross-compile this
freestanding Cortex-M3 code; there is no Android dependency in the image.

```sh
python3 -m unittest discover -s tools -p 'test_*.py'
python3 tools/build_hcx_bench.py \
  --firmware /path/to/manufacturer-111.BIN \
  --toolchain-dir /path/to/llvm/bin \
  --output-dir /tmp/hcx-build
PYTHONPATH=/path/to/unicorn-python python3 tools/audit_hcx_bench.py \
  --firmware /path/to/manufacturer-111.BIN \
  --build-dir /tmp/hcx-build --output /tmp/hcx-build/arm-audit.json
```

Output includes the ELF, extension machine code, combined experimental
memory image and hash/patch manifest. `host_stub.c` is only linked into the
host test library. Test fixtures and firmware code are outside the APK.

Validation executes all 20 values against the independent OEM XML, plus
rejection and state-transition cases. The ARM audit executes the five actual
patched call sites, byte receiver and custom code. It intercepts peripheral,
queue and RTOS operations with explicit fixtures. It does **not** test boot,
physical CAN wiring, real scheduling or the vehicle's support for these menus.

## What still prevents an installable release

1. The installed XP108 binary, exact board/bootloader identity and a recoverable
   backup are unavailable. A product-family match is insufficient.
2. The bench RAM reservation `20004E80..20004EFF`, stack margin and appended
   flash region need validation on that hardware.
3. The manufacturer updater's integrity/container format is unresolved.
   Renaming the experimental BIN to `.UPDE` would not solve it.
4. Real CAN replies and end-to-end serial delivery need a bench check.
5. The reference SYU service has no established route for the new `D7`
   response. The current APK can submit caller bytes but cannot consume HCX
   status as confirmed FYT vehicle feedback. That integration remains pending.

Do not install the experimental memory image on the car's box. These are
concrete missing port/installation requirements, not proof that the implemented
state machine or the existing box has been validated on physical hardware.
