# FYT OBD versus XP raw transport — 2026-09-16

## Result

The reference SYU service has caller-controlled serial writes in OBD module 12.
They do **not establish a raw vehicle CAN interface for XP profile 0x4012A**.
The cluster bridge remains unresolved. This investigation established no new
vehicle command, and no device commands were executed. Version 3.4.3 displays
these findings in the app's full-screen audit; it adds no OBD transport.

Scope: reference service 2.23.0711.1001, APK SHA-256
`4b428302e29c9e2503ccf7844a127f5bed59450736317629aa42b5eb35a9b577`.
The owner's photo reports 2.23.0718.1700, which has not been extracted here.
Method locations and source hashes are in
[xp-obd-audit-sources.json](xp-obd-audit-sources.json).

The packaged developer notes were read from the separate older hardening
workspace extraction (`versionCode 2122120314`, `versionName 1.0` in its
`apktool.yml`). They are historical documentation, not notes proven to come
from either service build above.

## Module and transport selection

`b/i.G2` maps module 12 to `q0/h`; module 7 maps to `f0/xp`.
`q0/h.cmd` forwards commands below 1000 to its configured OBD driver.
`q0/d.a` selects that driver using `q0/b.a`, independently of the CAN profile
stored in `f0/tp.a`. The numeric value 0x4012A cannot select an OBD driver.

`y/k.k(path, baud, id)` opens serial writer `y/a.g`, configures its baud rate,
and calls `q0/d.b(id)`. The direct callers found in this reference are:

| Caller | OBD driver | Port / baud in the relevant branch |
|---|---:|---|
| `i0/j.F2` | 2 | `/dev/ttyS0`, 9600 |
| `i0/e.F2`, `i0/h1.F2` | 4 | `/dev/ttyS0`, 38400 |
| `i0/e.P2`, `i0/h1.P2` | 4 | `/dev/ttyS1`, 38400 |

These are conditional customer/platform branches, not observations of the
owner's configuration. No direct initialization caller selecting OBD driver 3
was found in this disassembly. This is not proof about other service builds or
reflective/native callers.

## Caller-controlled OBD writes

| OBD driver | Verified behavior | Missing evidence for this task |
|---|---|---|
| 1, `q0/i` | `cmd` is a no-op | No transmit route here |
| 2, `q0/j` | Named `OBD_0002_GPSHeat`; commands 0–2 build A0/A1/A2 packets | No XP raw CAN schema |
| 3, `q0/k` | Commands other than 3/4 register request metadata with `M2`, then escape caller bytes between C0 delimiters; `O2` writes to `y/a.g` | Installed driver, packet semantics, CAN address/format/length contract, and XP routing |
| 4, `q0/l` | Command 1 converts caller integers to bytes; `H2` writes them directly to `y/a.g` | Attached hardware and its packet protocol; `b(byte[])` is a no-op, so this class supplies no generic raw receive callback |

Driver 3's escape mapping is C0→DB DC and DB→DB DD. Its metadata parser reads
fixed offsets from the caller array and its send buffer is 256 bytes. An
arbitrary OEM packet therefore cannot be assumed valid input even before the
external hardware is considered. Commands 3/4 change stored driver options;
they are not raw transmit commands.

`q0/h.register` callback **1001** reports `q0/b.a` (selected OBD driver).
Callback **1000** reports `q0/b.g`, not that driver ID. Neither reports physical
hardware compatibility or confirms a vehicle write. The app does not subscribe
to these callbacks or automatically switch to this module.

## Other misleading leads checked

- The existing XP path remains module 7 / command 1008 → `y/k.t` → MCU writer
  `y/a.b` or the selected `g0` encoder. See [XP-RAW-SEND.md](XP-RAW-SEND.md).
  The OBD writer `y/a.g` is a separate configured endpoint. This does not imply
  separate physical wiring in every possible platform configuration.
- `module/canbus/c.F3`, `module/canbus/p.A3`, and `f0/zd.f3` log `WR OBD`, but
  call `y/k.r`, which writes to `y/a.i`. `y/k.f` labels that endpoint CANBUS.
  A log containing OBD is not evidence of a module-12 or XP passthrough route.
- `w0/n.Q2` logs `sendCmdobd`, but belongs to module 8 (the TPMS module slot)
  and writes `y/a.f`; `P2` uses a 55 AA wrapper. Neither establishes an XP
  Honda CAN-frame format.
- `chip/Chip.B` checks `sys.fyt.systemobd` only to toggle Android Bluetooth.
  The `com.android.SYSOBD` receiver branch sets `sys.obd=ddh`; it does not take
  a caller-provided CAN payload.
- The older packaged `dev/readme.txt` mentions `8288 send CC FRAME` and baud selection.
  The corresponding `y/k.h` opens the SPHE8288/DVD endpoint, not an identified
  XP CAN interface. The OBD TIP only discusses replacing the previous serial
  processing thread. The CANBUS TIP discusses optional UI/audio capabilities.
  None of these notes specifies a Honda XP raw CAN command.

## What would resolve the boundary

The missing contract is a verified XP packet accepting vehicle CAN address,
frame format, length and payload, together with its response path. The
[OEM cluster byte derivation](CLUSTER-BRIDGE-AUDIT.md) already supplies
conditional OEM messages; finding another writable serial port does not supply
that contract.

The installed SYU APK, decoder version text now exposed in the 3.4.2 report,
matching box firmware/protocol documentation, or a capture of a working
implementation can narrow that investigation. A decoder version string alone
is an identification lead, not proof of passthrough support. No working OBD
adapter or physical cluster-bus access was observed in this workspace.
