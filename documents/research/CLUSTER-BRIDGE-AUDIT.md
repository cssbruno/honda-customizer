# Cluster bridge investigation — 2026-09-16

## Implementation scope

The owner has now explicitly authorized **custom XP firmware**, superseding
the earlier APK-only constraint. [HCX1](../../firmware/cluster-extension/README.md)
implements four new operations and 20 OEM values in a custom extension for
the SHA-pinned 111 binary. Source, build tools and isolated tests are retained.
No firmware has been installed; the owner's 108 board and updater format are
still unverified.

The existing APK already exposes caller-controlled XP packet dispatch through
module 7 / command 1008. The remaining gap is the installed decoder's accepted
packet format for the additional OEM cluster messages and its response path.
The fixed-table audit below does not prove that a firmware change is needed,
or that every existing transport route has been exhausted. The new custom
extension implements layout, color selection and navigation packet generation;
their installed-box port and APK response integration remain incomplete.

## New box-side evidence

The [manufacturer firmware audit](XP-BOX-FIRMWARE.md) now traces the actual
Honda XP **111** receiver and CAN customization table. An isolated ARM harness
verifies 17 cases, including tachometer/ambient-change translation and decoder
acknowledgements without CAN queue inputs. The owner's reported **108** binary
has not been obtained. Layout, color selection and navigation have no new
stock-XP mapping; their OEM menus are absent from the inspected fixed table.
The earlier unresolved transport discussion below is retained as the Android
and OEM side of the investigation, not a statement that no box firmware has
now been located.

## Work completed

The OEM byte construction is now executable for six cluster settings, not only
the tachometer. `tools/trace_cluster_transport.py` derives 48 conditional cases
from the saved OEM XML, including both sender identities. The result is saved in
[cluster-oem-transport-cases.json](cluster-oem-transport-cases.json). It performs
no device I/O and is excluded from the Android application sources.

| OEM setting | Source / menu | API value → encoded value |
|---|---|---|
| Ambient color change, 03/28 | 50 / 16 | 1→0, 2→1 |
| Ambient color selection variant 1, 03/29 | 50 / 32 | 1–7→1–7 |
| Ambient color selection variant 2, 03/2A | 50 / 33 | 1→7, 2–8→1–7 |
| Turn-by-turn display, 03/38 | 50 / 20 | 1→0, 2→1 |
| Meter configuration, 03/5B | 50 / 15 | 1→0, 2→1, 3→2 |
| Tachometer display, 03/5C | 50 / 2E | 1→0, 2→1 |

Setting IDs, sources and menus above are hexadecimal; API and encoded values
are decimal. Color names and the visual meanings of the three meter variants
are not inferred from their numbers. These are entries in this OEM firmware,
not claims that the installed vehicle supports all of them.

## Byte derivation

The direct branch of `VehicleInfoManagerApService$7.requestCustomizeSettingChange`
builds `40 menu value...`, obtains the list through
`CustomizeSettingControl.getListValue`, and sets length to list size plus two.
`getListValue` iterates all items sharing source/menu in table order. All six
settings above have singleton menus across the full saved table, so their
three-byte payload needs no sibling setting values.

The utility rejects shared menus, absent routes, non-enumerated settings,
unknown API values, and unspecified sender identity. It never takes XML
`DataValue` defaults as readings. The ID is `16300054/55 + (source << 8)`;
the existing [native transport trace](honda-cluster-analysis/full-command-trace.md)
supplies the 16-byte structure and internal 26-byte IPC wrapper. The sender
identity and supported source/menu set still require runtime discovery.

The four new unit tests cover the independent saved native tachometer vector,
panel/color value translation, rejected missing/shared inputs, and explicit
absence of an XP packet/hardware claim. All 11 Python tests passed.

```sh
python3 tools/trace_cluster_transport.py --output /tmp/cluster-cases.json
python3 -m unittest discover -s tools -p 'test_*.py'
```

## Why a numeric substitution fails

The known XP tachometer writer is `C6 02 16 value`, with 0=Off and 1=On.
The OEM tachometer menu is 2E, with 0=On and 1=Off. Both the key and value
meaning change between these interfaces.

The OEM meter-configuration menu is 15. The existing stock FYT settings
activity uses command-105 key 15 for units, not meter configuration (and hides
that units row on exact XP 4012A). Replacing the key with the OEM menu is thus
not a translation. This is a source-level counterexample; no packets were sent
to test it on a car.

## Additional entry points examined

The complete reference CANBUS APK and `f0/xp.cmd` were inspected, rather than
only the previous Honda activity extracts:

- 1008 (3F0): prepends E3 and forwards caller bytes to `y/k.t`.
- 1012 (3F4) and 1013 (3F5): analogous forwarding with 91 and 10 prefixes.
  These branches do not define a CAN arbitration-ID/DLC/payload structure.
- 1030 (406): calls the active `g0/a.f` encoder directly. For the traced
  Honda low-word 12A encoder, `g0/r.f` replaces the first routing element with
  the E9/2E wrapper and adds a complemented sum. It does not translate Honda
  OEM IDs into decoder keys or define a raw-CAN command type.
- The Honda driver's command 105 builds the fixed `E3 C6 02 key value` packet.
  Its other exposed commands likewise build named decoder packets; none of
  the inspected branches establishes the missing OEM-CAN passthrough schema.

This does not prove that the hardware has no passthrough mode. It establishes
what these specific service branches do. MCU routing prefixes alone cannot
define an unknown CAN-box command.

### Factory test entry points

The same reference SYU service's module factory `b/i.G2` maps module 13 to
`v0/a.E2`. Its `cmd` method checks command 0 and a string before calling
`b/h.o`; the inspected implementation exposes no CAN transmit structure.

`module/canbus/HJ_AutoTestService` reads the `SetInfo` integer array when its
`from` extra is `set`, and forwards that array directly to `y/k.t`. This is
another entry to the previously traced MCU writer. Its other media/test
branches do not supply the missing Honda CAN-ID/DLC/payload encoding. No
service was launched or added as a fallback.

The [box firmware audit](XP-BOX-FIRMWARE.md#literal-oem-bytes-through-the-serial-receiver)
now also exercises the real byte receiver using the reference SYU encoder's
framing. The known XP command succeeds at the isolated CAN queue boundary;
18 literal OEM payload/native/IPC cases do not. This distinguishes an Android
API that forwards arbitrary bytes from a box command that transmits a chosen
Honda CAN frame. The compatible latter contract is still missing.

## ProtocolUpdate APK ruled out as a firmware source

The reference firmware's `ProtocolUpdate.apk` was extracted and its three
SQLite databases opened read-only. They contain manufacturer/model/profile
metadata, visibility and ordering fields, not packet definitions or CAN-box
machine code. The main database explicitly records 4012A as Civic
2016–2021 HIGH. The alternate database includes XP/RZC display names for the
same numeric profile; that label is not permission to substitute protocols.

The APK's provider exposes those database tables. No box firmware payload or
firmware download route was established from this APK. The CANBUS APK's
`Can_Back.ogg` is a ZIP containing two WebP images and a UI XML resource,
not CAN-box firmware. Database schemas, profile rows and binary hashes are
saved in [cluster-bridge-audit-sources.json](cluster-bridge-audit-sources.json).

Public source checks also found a
[Toyota SimpleSoft decoder](https://github.com/zugetor/simplesoft-canbus-box-reverse-engineer)
and [replacement CAN-box firmware](https://github.com/smartgauges/canbox).
Their documented targets do not establish Honda XP passthrough compatibility;
no command from either project was added to the app.

## Remaining exact boundary

The new output deliberately leaves `xp_passthrough_packet` null. The missing
evidence is the installed XP box's packet decoder/firmware or an observed
working Honda-CAN transmit command for that box, including its response path.
The Android profile 4012A and SYU service build do not identify that firmware.
The box model was requested to narrow this investigation. No new cluster feature is claimed by this research change.


## Identification without a box label

The Honda driver does expose a decoder version string. `module/canbus/v.M2`
packet type 30 hex (`pswitch_a9c`) reads the text after the type/length bytes
and calls `f0/wp.A`. That function stores `f0/tp.X` and publishes callback
1005 (3ED hex). `f0/xp.register` can replay that string; `i1/v.j` serializes
null int and float arrays and one string. This is distinct from the numeric
profile callback 1000.

The app previously consumed only callbacks with a nonempty integer array, so
it could not capture this text. Version 3.4.2 subscribes to 1005 for the exact
XP profile and includes returned text in the report. The label explicitly
states that the string may be cached and does not establish current hardware
identity or vehicle state. Null/empty text remains unavailable; reconnection
clears the previous session's diagnostic text. The parser rejects unexpected
array shapes, oversized text, control characters and trailing content. The
read-only subscription sends no vehicle command and changes no setting state.
