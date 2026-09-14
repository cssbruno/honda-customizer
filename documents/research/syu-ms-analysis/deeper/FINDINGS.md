# Deeper smali inspection — com.syu.ms

Inspected 2026-09-14. Main target: Joying version 2.23.0711.1001, SHA-256 `4b428302e29c9e2503ccf7844a127f5bed59450736317629aa42b5eb35a9b577`.

**New confirmed findings:** app cleanup during sleep, a conditional reboot path, phone-number handling, a separate Bluetooth service dependency, local sensor module 19, radio feature commands, steering-learning operations, and the common Binder command path.

## What was actually inspected

Both DEX files were freshly disassembled using Android SDK baksmali 3.0.9. They produced **10,027 classes and 78,206 declared methods**. All class files match the earlier disassembly byte for byte. These counts include bundled Android/framework classes: 5,234 classes are in the top-level `android` directory. They are inventory counts, **not claims that every method was manually reviewed**.

29 selected classes are preserved under [evidence](evidence/). [smali-inventory.json](smali-inventory.json) records every class hash, line count and method count. [targeted-searches.json](targeted-searches.json) preserves network, caller-check and shell search hits.

JADX was also used as a reading aid. It reported 12 decompilation errors, and some reconstructed methods contain warnings or incorrect control flow. Smali remains the authoritative source; the sleep and Binder findings were checked directly against it. No APK code was run on Android and no vehicle commands were transmitted.

## 1. It deliberately stops apps during the sleep sequence

In `y/i.j([BII)V`, the accepted `0x89 0x55` sleep path calls:

```
y/i.j → b/j.E → b/j.C → b/j.D(package)
                           ↳ forceStopPackage (reflection)
      → b/j.l → PowerManager.goToSleep (reflection)
```

The cleanup enumerates running processes and excludes configured/protected entries. It also constructs exclusions from service lists and explicitly skips process names starting with `com.antutu`. It does **not** indiscriminately stop every process.

Practical implication: an app disappearing after ignition-off can be part of this service's intended cleanup. This finding does not prove it caused a particular incident on your unit.

Evidence: [y/i.smali](evidence/y/i.smali), search `killAppWhenSleep`; [b/j.smali](evidence/b/j.smali), methods `C`, `D`, `E`, `l`; protection helper [b/c.smali](evidence/b/c.smali).

## 2. Reboot can be part of sleep handling

The `0x89 0x53` branch reads `sys.sleeptimes` and compares it with `p0/b.W1`. If the counter exceeds that configured value, it clears `sys.fyt.sleeping` and calls `b/j.V()`, which invokes `PowerManager.reboot(null)`.

The accepted sleep path increments `sys.sleeptimes`. This is a counter-based reboot condition, not evidence of a daily schedule. The configured threshold and the full counter-reset lifecycle have not been established for your unit.

Evidence: [y/i.smali](evidence/y/i.smali), `sys.sleeptimes`, `W1:I`, `Lb/j;->V`; [b/j.smali](evidence/b/j.smali), method `V`.

## 3. It handles phone numbers and adjusts audio around calls

`e0/e.p(String)` stores the number, publishes it to Bluetooth callback field 8, sends it to the vendor debug logger, and writes a nonempty value to `sys.btphone.number`. `e0/e.q(String[])` has a related path using the second array element. In these two methods an empty number does not clear the property, so its last nonempty value can remain until another path updates it or the system resets it.

`e0/e.r(int)` manages call-state transitions: saves the previous source, updates audio routing, tracks call timing for certain states, controls `syu.bt.show.pip`, and conditionally unmutes for a call using `syu.bt.calling.unmute`, restoring mute afterward. `e0/g.G2` also logs the command and formatted arguments through Android logging.

This establishes local handling/logging of phone information. It does not establish cloud upload, who can read the property under a particular firmware policy, or how long logs persist.

Evidence: [e0/e.smali](evidence/e0/e.smali), methods `p`, `q`, `r`; [e0/g.smali](evidence/e0/g.smali), method `G2`.

## 4. Some Bluetooth work lives in another APK/service

`module/bt/CmdBtSG9832` forwards multiple Bluetooth commands through `e0/c.e`. Its startup uses `c0/b`, whose connection loop binds action `com.syu.ps.toolkit` inside package **`com.syu.ps`**. The implementation also observes Android Bluetooth adapter state.

Many methods in this driver are intentionally empty, while the alternate `e0/a` implementation emits commands through `y/k.s`. An interface name or command slot alone therefore does not prove a feature works on every platform.

Practical implication: reproducing Bluetooth from this APK alone would be incomplete on the delegated-service path. The selected Bluetooth driver and matching `com.syu.ps` implementation matter.

Evidence: [CmdBtSG9832.smali](evidence/module/bt/CmdBtSG9832.smali), `cmdIn` and command methods; [c0/b$a.smali](evidence/c0/b$a.smali), `run`; [e0/a.smali](evidence/e0/a.smali).

## 5. Module 19 is the local sensor subsystem

The formerly unidentified factory `s0/b` names its state store **`Sensors_local`**. `Q2(1)` constructs an I²C sensor implementation using `/dev/i2c-2` and numeric argument 53; `Q2(2)` constructs the Android sensor implementation `s0/d`.

`s0/d.onSensorChanged` dispatches sensor types 1, 4 and 9: accelerometer, gyroscope and gravity. Its processing code includes calibration/motion calculations. In one platform branch, `g/m` selects the implementation based on the presence of `ro.ls.gyro`. This is distinct from toolkit module 17, the separately advertised G-sensor module.

This identifies the subsystem, not the sensor fitted to your unit or the accuracy of its calculated values.

Evidence: [s0/b.smali](evidence/s0/b.smali), `Q2`; [s0/d.smali](evidence/s0/d.smali), `onSensorChanged`; [g/m.smali](evidence/g/m.smali), constructor. The larger I²C implementation is preserved as [s0/a.smali](evidence/s0/a.smali).

## 6. The radio API covers most tuner settings

The radio dispatch `r0/i.G2` has directly named calls for:

- Next/previous preset, select/save preset, scan and save.
- Frequency step up/down, seek up/down and direct frequency setting.
- Band and region selection.
- Sensitivity, automatic sensitivity, stereo and local reception mode.
- RDS enable, traffic announcements, alternative frequencies and programme-type settings.
- Tuner power and a driver-specific transit operation.

These are concrete dispatches to the selected radio driver. Reception capability still depends on that driver and the tuner.

Evidence: [r0/i.smali](evidence/r0/i.smali), `G2`. Examples: command 5 calls `seekUp`, 8 calls `saveChannel`, 19 calls `rdsTaEnable`. These are internal app command IDs, not raw MCU opcodes.

## 7. Sound settings include hardware-specific DSP operations

Besides the main sound command dispatch, `t0/i.G2` handles operations only when the selected driver is an `AudioDevice`. Calls include subwoofer settings, bass enhancement, high-pass coefficients, surround settings, speaker gain, independent speaker adjustment, custom-preset saving and spectrum checking.

This is why a setting shown by an equalizer app can depend on the installed DSP: the service explicitly gates these operations by driver type. This pass confirms dispatch; it does not validate coefficient ranges or physical speaker output.

Evidence: [t0/i.smali](evidence/t0/i.smali), `G2` and `cmd`.

## 8. Steering-button learning has explicit clear/save commands

`u0/e.cmd` dispatches mapping operations to the selected steering implementation. Command 3 invokes `clear()` and command 4 invokes `save()`. Its query/callback code exposes a 50-entry table and a separate six-entry table. Those are software table sizes, not a claim of 56 physical buttons.

Evidence: [u0/e.smali](evidence/u0/e.smali), `cmd`, `get`, `register`.

## 9. CAN behavior is split between a common router and a selected decoder

`f0/xp` keeps a selected decoder object and forwards commands through `Q2`. For reads, IDs below 1000 delegate to that object; callback registration similarly delegates vehicle-specific fields. Common higher-numbered fields are handled by the router.

`f0/wp.z` persists the profile selection and separates its low 16-bit protocol identity from an upper-byte variant when composing configuration messages. Thus the full profile integer carries more than one piece of information. No specific Honda door/climate command has been verified by this pass.

Evidence: [f0/xp.smali](evidence/f0/xp.smali), `Q2`, `get`, `register`; [f0/wp.smali](evidence/f0/wp.smali), `z`.

## 10. Binder commands enter a queue; caller PID logging is not authorization

The common path is:

```
ToolkitService → b/i factory → subsystem Binder
  x/c$a.onTransact → cmd(command, int[], float[], String[])
  base/a.cmd → K2 queue → base/a$b handler → subsystem G2
```

The stub enforces the Binder interface descriptor. `base/a.cmd` optionally logs the caller PID and `/proc/<pid>/cmdline`, then queues the command. No caller UID/signature/permission check was found in this common path, and a broader targeted search outside Android/AndroidX classes found no matches for the five caller-check API names recorded in the search results.

This is a specific missing check in the inspected path, not a claim that arbitrary apps can control every function on every firmware. Binding restrictions, SELinux, other checks and runtime configuration were not exercised. Some modules also implement `cmd` directly instead of using the common queue.

Evidence: [x/c$a.smali](evidence/x/c$a.smali), `onTransact`; [base/a.smali](evidence/base/a.smali), `cmd`, `K2`; [base/a$b.smali](evidence/base/a$b.smali), `handleMessage`; [b/i.smali](evidence/b/i.smali).

## Startup, projection and networking follow-through

`App.onCreate` acquires a partial wake lock, initializes the hardware/configuration, sets up the volume overlay, initializes serial communication and registers listeners. `App$h` polls boot-animation, boot-completion and warning-screen properties; when ready it invokes the conditional log-recording startup. `ReceiverApp` logs package additions/removals and clears a matching stored package selection on removal; it is not an installer in the inspected code.

The ZLink integration recognizes wired/wireless CarPlay and wired Android Auto mode strings and adjusts source/audio state around projection events. This is integration with a separate projection app, not an implementation of the entire projection protocol.

The expanded network search found only two XML feature-identifier URLs after excluding Android, AndroidX and bundled Google parser classes; it found no matching direct URL/socket/HTTP-client references in the remaining classes. Eleven shell-execution matches occur in the log recorder and selected decoder helpers. Search patterns and exclusions are preserved in [targeted-searches.json](targeted-searches.json). Reflection, native libraries and cooperating packages remain outside what these negative search results can rule out.

## Remaining limits

This is a substantially deeper static inspection of the Joying reference, not a complete manual audit of 78,206 methods. Every vehicle decoder, native driver and accessory implementation has not been traced. The TEYES implementation was not the target of this pass. Runtime testing, exact installed-firmware matching, network capture and selected-profile reconstruction remain necessary to establish the full behavior of your actual head unit.
