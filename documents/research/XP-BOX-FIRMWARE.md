# Honda XP box-side firmware audit

This is read-only analysis of manufacturer code to understand the existing
protocol. The owner subsequently authorized a separate custom-firmware
implementation: [HCX1 experimental extension](../../firmware/cluster-extension/README.md).
That extension is not an installable update and does not establish installed
108 hardware compatibility. This audit itself modifies no firmware and
establishes no requirement to change the installed firmware.

## Result

The manufacturer firmware **CRI V1.13.111BYPT** contains the Honda CAN
translation, including the physical cluster ID `0x16305054`. Isolated execution
reproduces the existing tachometer and eco/ambient-change commands and agrees
with the independently extracted OEM XML.

**No new layout, color-selection or navigation control is implemented by this
audit.** Those four OEM menus are absent from the fixed customization table.
Inserting their menu numbers into the XP key slot either selects a different
setting or produces no CAN queue input in the tested settings path, despite
the decoder acknowledgement routine being called.

This is evidence from **111**, not the owner's **108** binary. It does not
establish that the installed hardware accepts any additional command, or that
all possible firmware paths have been exhausted. The generic CAN passthrough
contract remains unverified.

## Manufacturer source

The Simple Soft / XP manufacturer lists the same archive on:

- [RP5-HD-001 / HD-SS-06C](http://www.ssinterface.com/show.php?id=752)
- [RP5-HD-101 / HD-SS-06C](http://www.ssinterface.com/show.php?id=753)

The product lists include Civic 2016–2021. Both pages link
`HDSS06C_V1.13.111BYPT_230804.zip`, dated August 4, 2023; its release note
describes an Odyssey air-conditioning fix. This does not identify the owner's
unlabelled box or establish an upgrade path for it.

[Manufacturer archive](http://www.ssinterface.com/upload/file/20230825/1692940642406248.zip):

| File | Bytes | SHA-256 |
|---|---:|---|
| ZIP | 124360 | `32786b6a54ef1f33fa26e46df2c052b1faea9ddcd5dedf3782bcb75b38e65d9a` |
| BIN | 91120 | `df254fb42d5fe02e985ab57f9d23fcce963fa70868e337eb9941a99c8196bf99` |
| UPDE | 75760 | `8e4c85123a3d389412d25ba9b1106ce858328d82f3952a0858bb9e2f9b2d4a8b` |

The BIN has a Cortex-M vector table at file offset `0x4000`; the application
code and tables used below are plaintext. Flash addresses are file offsets
plus `0x08000000`. The string at offset `0x40FC` identifies **111**. The UPDE
was not installed or used to update anything. Firmware binaries are external
inputs and are not redistributed in this repository or APK.

## Exact receiver and CAN path

All addresses below refer only to the SHA-pinned BIN above:

1. `0x0800EA24` handles the `2E` serial framing. `0x0800E948` checks the
   complemented sum of the type, length and data, then calls `0x080133C0`.
2. `0x080133C0` dispatches types `81, 90, C0, C2, C3, C4, C5, C6, C8, C9,
   CA, CB, E0`. Its default returns zero. This list alone is not a description
   of raw CAN capabilities.
3. The `C6` branch at `0x08013640` calls decoder acknowledgement routine
   `0x0800E92C` **before** processing the setting. That acknowledgement is
   not a vehicle reply. Except for special keys `60/61`, it stores `key+1`
   at `0x2000438C+0x82` and the supplied value at `+0x83`.
4. The state machine at `0x08006B48` selects defined settings and translates
   values. The branch at `0x08007464` maps keys `16` and `23` to the same
   template index `17`; input 1 becomes OEM 0, other input becomes OEM 1.
   The app continues to allow only its verified enumerated inputs.
5. `0x08004896` selects a 20-byte template at
   `0x08014E78 + 20 * index`, places `40` at payload byte 0 and the translated
   value at payload byte 2. `0x08004814` queues that structure for CAN.
   Structure offsets: CAN ID +4, IDE +8, RTR +9, DLC +10, data +11.
6. The matching response branch at `0x08007774` checks the expected reply ID,
   `C0`, menu and value before setting its success flag. The harness does
   not supply that vehicle reply or claim a successful write.

The sender ID in the traced CAN templates is `0x54`. The firmware itself
supplies the ID and menu; the normal `C6` caller cannot simply replace either.

## Executed cases

The harness executes the actual checksum checker, dispatcher and one settings
state-machine step. It captures the input to the CAN queue before peripheral
access. The state-machine readiness bytes are explicit **test fixtures**, not
observed vehicle capability. Only flash and RAM are mapped: no device, serial
port, CAN controller, RTOS or complete car is emulated.

| XP key | Value | Captured CAN ID / payload | Meaning established by OEM comparison |
|---|---:|---|---|
| `16` or `23` | 0 | `16305054 / 40 2E 01` | Tachometer off |
| `16` or `23` | 1 | `16305054 / 40 2E 00` | Tachometer on |
| `01` or `13` | 0 | `16305054 / 40 16 01` | OEM ambient-change API value 2 |
| `01` or `13` | 1 | `16305054 / 40 16 00` | OEM ambient-change API value 1 |
| `15` | 0 or 1 | `16305054 / 40 2F value` | Units menu, **not** OEM layout menu `15` |
| `20`, `32`, `33` | 0 or 1 | None in the tested settings step | Decoder ACK occurs without a CAN queue input |

The existing XP eco-background setting uses key `13`. An OEM ambient-change
match does not add the separate color-selection menus `32/33` or name colors.
OEM layout menu `15` and navigation menu `20` also have no matching scalar
entry in the extracted fixed table. Existing FYT field 87/key `23` is a second
tachometer route in this binary, not a three-option layout selector.

All **16 valid-checksum cases** passed their independent output checks. A
seventeenth case corrupts the checksum: it yields `F0` and reaches neither
the acknowledgement routine nor the CAN queue. The generated
[machine-readable report](xp-box-firmware-audit.json) includes all 35 fixed
templates, test preconditions, captured outputs and the physical-test limit.

## Literal OEM bytes through the serial receiver

The additional audit calls the actual byte receiver at `0x0800EA24` once per
input byte and the pending-message processor at `0x0800E9E8` after each call.
It then executes one ready-state settings step. The RTOS notification routine
at `0x08012382` ("Uart Send Sem") is intercepted; scheduling and UART interrupts
are not simulated. The parser is explicitly selected by the harness, without
reading or changing the hardware register that selects it on the box.

The reference SYU `2.23.0711.1001` method `g0/r.f` frames the caller's body as
`2E + body + ((sum(body) XOR FF) AND FF)` after the outer `E9` routing selector.
It copies the caller's bytes unchanged, including any byte interpreted as a
length. This is source evidence for that encoder, not a capture from the
owner's `2.23.0718.1700` service or installed `108` decoder.

| Input to the isolated 111 serial receiver | Observed result |
|---|---|
| `2E C6 02 16 01 20` | One decoder ACK routine call and CAN queue input `16305054 / 40 2E 00` |
| Same XP packet with final checksum `21` | Serial output `F0`; no ACK routine call or CAN queue input |
| Literal OEM payload `40 menu value`, with the same SYU framing | No completed command or CAN queue input |
| Literal OEM 16-byte native record, with the same SYU framing | Serial checksum rejection(s); no CAN queue input |
| Literal OEM 26-byte syscon IPC record, with the same SYU framing | Serial checksum rejection(s); no CAN queue input |

The last three rows cover each of OEM settings `03/28`, `03/29`, `03/2A`,
`03/38`, `03/5B`, `03/5C`: **18 literal OEM cases**, with API value 1, source
`50` and sender `54` explicitly supplied as isolated test inputs. Even the
known tachometer's OEM representations fail this direct substitution while
the stock XP command reaches the expected CAN queue input. For example,
`2E 40 15 00 AA` leaves the parser waiting: it interprets `15` as a payload
length, not as the Honda layout menu. Sending the known Honda bytes does not
provide the missing XP transmit command.

These **20 serial cases**, together with the original 17 dispatcher cases,
pass the audit's assertions. They exclude these specific literal substitutions
on this tested path; they do not prove that every possible XP transmit route
is absent, nor that updating or modifying firmware is necessary.

## Alternate receiver examined

The UART handler at `0x08013BF8` calls `0x0800EF9E` to select its receiver.
That helper tests bit 1 at register address `0x4002201C`: a nonzero result
selects `0x0800EA24`; zero selects `0x0800E110`. The installed value is unknown.

The alternate path uses an `FD` prefix and a different length/checksum
structure. Its checksum dispatcher at `0x0800E1C4` routes command `FF` into
`0x0800E48C`, which handles factory/configuration state. The inspected branch
copies four byte-swapped words into RAM and posts a base-task operation. This
does not establish a CAN arbitration-ID/DLC/payload interface. No alternate
mode, hardware-register write or configuration operation was added to the
APK or executed on a box.

## Reproduce

Use the BIN extracted from the manufacturer archive, not the UPDE. An unknown
or modified firmware hash is rejected before emulation. Install the pinned
offline-analysis dependency in a separate directory if needed:

```sh
python3 -m pip install --target /tmp/honda-xp-python unicorn==2.1.4
PYTHONPATH=/tmp/honda-xp-python python3 tools/audit_xp_box_firmware.py \
  --firmware /path/to/extracted.BIN --output /tmp/xp-box-audit.json
```

The implementation is in [audit_xp_box_firmware.py](../../tools/audit_xp_box_firmware.py).
It adds no Android transport, no automatic fallback and no new vehicle-state
value. The remaining app-side feature work needs a verified compatible box
command and its actual response mapping for each extra OEM menu.
