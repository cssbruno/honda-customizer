# FYTCanbusMonitor / FytRadio: lower-level CAN audit

Inspected 2026-09-16. No vehicle commands, firmware changes, or hardware tests.

## Result

The monitor exposes a useful **receive observation point**, but neither project supplies an arbitrary vehicle CAN transmit packet for the XP profile. The existing app's module-7 command 1008 already reaches the caller-controlled service packet route described in [XP-RAW-SEND.md](XP-RAW-SEND.md). This audit does not establish that firmware flashing is necessary.

The known OEM Honda cluster bytes and their CAN IDs remain distinct from the missing XP packet format that would carry those frames. Reusing Binder transaction numbers does not supply that format.

## Pinned public sources

- [FYTCanbusMonitor](https://github.com/AxesOfEvil/FYTCanbusMonitor/tree/849645a8490aba4b0b64bae3a1a200b71cc8334f): ordinary toolkit Binder connection, module command/get/register/unregister transactions 1/2/3/4. The example subscribes to module 7 updates 0–50 and 1000–1036 with initial notification enabled; this can replay cached values. It is not a hook into the physical CAN driver.
- [Monitor callback constants](https://github.com/AxesOfEvil/FYTCanbusMonitor/blob/849645a8490aba4b0b64bae3a1a200b71cc8334f/fytcanbusmonitor/src/main/java/com/aoe/fytcanbusmonitor/CanbusIdCodes.kt): 1019 is named `U_CANBUS_FRAME_TO_UI`; 1024 is named `U_CANBUS_FRAME_TO_MTU`. These are **update identifiers**, not transmit commands. Update 1008 (`U_SHOW_AIR_WINDOW`) is also unrelated to command 1008 despite its equal number.
- [FytRadio bridge](https://github.com/PimpinPumpkin/FytRadio/blob/de9b74b3028ddc036ded5e24c3f1e4ece2811833/app/src/main/kotlin/com/fytradio/radio/SyuRadioBridge.kt): the same toolkit/module Binder interface, with radio/main/sound operations. No XP `(CAN_ID, DLC, DATA[])` transmit implementation was identified. Its source-selection fallbacks and reply handling are not imported into this app.

## Reference service receive trace

Reference `com.syu.ms` is **2.23.0711.1001**, APK SHA-256 `4b428302e29c9e2503ccf7844a127f5bed59450736317629aa42b5eb35a9b577`. The owner's photo shows **2.23.0718.1700**; the installed service has not been inspected. The following table describes the reference, not a physical capture.

| MCU selector in `y/i` | Callback | Published data |
| --- | --- | --- |
| `E3`, sparse case `-0x1d`, `:sswitch_7b4` | 1019 | Payload after the selector, after calling the selected driver's `M2`; bytes sign-extended into integers. Requires subscriber and `p0/b.M3`. |
| `E9`, packed case `-0x17`, `:pswitch_51d` | 1019 | Payload after the selector, also passed to the configured receiver/encoder; bytes converted to unsigned integers. |
| `10`, `:sswitch_77a` | 1019 | Payload after the selector, unsigned integers; associated log says `0x10 MCU--> UI`. |
| `90`, sparse case `-0x70`, `:sswitch_830` | 1024 | Payload after the selector, also passed to `y/a.C.u1`; bytes sign-extended into integers. |

Consequences:

- Callback 1019 merges multiple MCU routes and does not preserve the outer selector. It cannot alone prove which route supplied a packet.
- Neither callback constructs a CAN arbitration ID/DLC/data record. Calling it a physical CAN capture would overstate the evidence.
- Hex display must normalize signed byte values using `value & 0xff`, while retaining the actual received array for auditing. It must not infer vehicle state from unparsed bytes.
- `p0/b.M3` is initialized **true** in the reference static initializer; the other identified writes (`g/d.b`, `i0/e.in`, `i0/h1.in`) also set true. No enabling command or factory-mode change is needed for this reference's E3 observation gate.
- `f0/xp.register` accepts these update identifiers. Subscription with initial-notify zero avoids requesting a cached replay. Actual arrival is still dependent on service/hardware traffic.

`g0/k.b` also publishes received bytes to 1019 and `g0/k.f` forwards input to `g0/a.c`. However, `g0/w.j0` selects `g0/k` for low-word profile **0**, whereas low-word **0x12A** selects **`g0/r`**. The pass-through behavior of `g0/k` must not be substituted for XP.

## Transmit boundary

Confirmed reference route:

```text
Toolkit transaction 1 → module 7
Module transaction 1 → command 1008, caller int[]
f0/xp.cmd → E3 + caller bytes → y/k.t
  ordinary MCU mode: 88 55 + length + payload + XOR → transport writer
  alternate CAN mode: XP g0/r → E9 / 2E framing + caller bytes + checksum
```

The remaining unknown is the **stock XP decoder opcode and payload contract for arbitrary physical CAN transmission**, including response semantics. Neither newly inspected repository resolves it. Existing packet dispatch must remain unconfirmed; no extra cluster control or raw-CAN compatibility is claimed from this audit.
