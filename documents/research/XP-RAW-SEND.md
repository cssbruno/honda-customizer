# XP lower-level send route: confirmed service-side entry

## Correction

The inspected SYU service **does expose a caller-controlled packet path in CANBUS module 7**. This corrects the earlier assumption that the app could only submit the decoder's predefined setting keys. It does not yet establish an arbitrary physical CAN-frame interface.

Reference service: Joying `com.syu.ms` 2.23.0711.1001, SHA-256 `4b428302e29c9e2503ccf7844a127f5bed59450736317629aa42b5eb35a9b577`. The owner's photo reports a different service build, 2.23.0718.1700. Source hashes are recorded in [the source manifest](xp-raw-send-sources.json). No device commands were executed.

## Confirmed call path

1. `f0/xp.cmd`: module command **1008 / 0x3F0**, first entry of `:pswitch_data_26a`, enters `:pswitch_13d`.
2. For a nonempty integer array, it allocates a new array one element longer, prefixes **0xE3**, copies all caller elements, and calls `y/k.t`.
3. `y/k.t` checks service/hardware state. In the ordinary MCU path it calls `y/k.b`, then the transport writer `y/a.b.g`. It can silently return when its gates are not satisfied. Binder dispatch does not prove transmission.
4. `y/k.b` wraps the payload as `88 55 | length-high length-low | payload | XOR checksum`. This is an internal MCU packet, not a vehicle CAN frame.
5. In the alternate CAN transport mode (`f0/tp.h == 1`), `E3` packets go to `g0/v.a.f`. `g0/w.j0` maps protocol low word `0x12A` to `g0/r`. That encoder wraps the bytes after E3 with a `2E` prefix and complemented-sum checksum, using E9 as the outer routing selector. `g0/a.c` then forwards to its configured endpoint or MCU transport. The installed mode/endpoint is not established by the supplied photo.

## Relationship to existing XP settings

The Honda 298-family driver's `module/canbus/v.cmd` maps setting command 105 / 0x69 to `:pswitch_11a`. It constructs `E3 C6 02 key value`, converting the two arguments to bytes, and calls the same `y/k.t` function.

Therefore, in this reference implementation, sending module command 1008 with the equivalent decoder packet body reaches the same downstream transport input as the existing setting writer. This is source-level equivalence, not a new vehicle command or evidence of additional settings.

## What is still missing for original Honda features

Command 1008 removes a Java-side restriction on the decoder packet body. It does **not** define how the XP box accepts an arbitrary CAN arbitration ID, frame format, length and payload, or how it exposes the matching raw response. The XP box may support a passthrough packet type; no such packet type has been established by this trace.

Honda's original service uses its own system-controller protocol and dynamic B-CAN routing. Its internal packets cannot simply be placed after E3 and assumed to acquire the required XP framing/meaning. See [the OEM transport trace](honda-cluster-analysis/full-command-trace.md).

The next useful evidence is the XP box protocol/firmware, a verified caller exercising a raw-CAN packet type, or a capture of a working matching implementation. Those can establish the missing packet schema and response contract. The research itself sent no vehicle commands. Version 3.4.0 adds an explicitly confirmed manual XP packet editor for profile 0x4012A using this proven entry point; it does not invent packet types or translate OEM Honda frames. Manual dispatch is always reported as vehicle-unconfirmed, discards prior readings, and ends the connection. The parser has an application limit of 64 bytes, not a claimed hardware limit.
