# Steering learning: clear, save, storage and applicability

Static inspection of the local Joying `com.syu.ms` reference and its matching firmware's `com.syu.steer` APK, 2026-09-14. No hardware commands were sent. APK identities are in [sources.json](sources.json).

## Confirmed command path

The client APK explicitly names `C_CLEAR = 3`, `C_SAVE = 4`, `C_DETECT = 2`, `C_MCU_KEY_CONTROL = 5` and `C_MCU_KEY = 6`. The service exposes steering as toolkit **module 10**. Its `u0/e.cmd` dispatches commands to `u0/b.b`.

`u0/c.i(1)` selects the concrete `u0/a` implementation; other selector values use a no-op implementation. The inspected MCU initialization in `y/k` selects 1. This establishes reference startup behavior, not a measurement on the user's head unit.

Sources: [client constants](evidence/com.syu.data.FinalSteer.smali), [service dispatcher](evidence/u0/e.smali), [selection](evidence/u0/c.smali), [MCU initialization](evidence/y/k.smali), [no-op implementation](evidence/u0/d$a.smali).

## What Clear and Save actually do

| Operation | App command on module 10 | Reference implementation |
|---|---|---|
| Clear | 3 | `u0/a.clear` sends payload `01 10 20` to `y/k.t`, then clears working/loaded mappings and removes preference keys `0`, `1`, `3`. |
| Save | 4 | `u0/a.save` sends payload `01 10 21`, clones working maps into active maps, and serializes/commits keys `0`, `1`, `3`. |

These payloads are recorded as static evidence, not transmission instructions; the normal MCU framing is added elsewhere. The code issues commands without proving that the MCU accepted or durably saved them.

**Clear does not wait for Save to remove those Android preferences.** `u0/a.i()` calls the removal helper directly. It also publishes empty updates on callback fields 10 and 11. It does not directly zero the entire ADC feedback array or remove preference group 2.

Source: [u0/a.smali](evidence/u0/a.smali), methods `clear`, `save`, `i`, `p`; [i1/f0.smali](evidence/i1/f0.smali), methods `r` and `d`.

## Where mappings are stored

The service creates `getSharedPreferences("Steer", 0)`. Its sparse integer maps are serialized as JSON strings under numeric keys:

| Key | Evidence from load/save and event code |
|---|---|
| `0` | Learned key-to-ADC values; updated from MCU learning feedback. |
| `1` | Key-to-action mappings, called `defFuncs` in logs. |
| `2` | Separate MCU/panel key-to-function map (`panelKeyFucs` / `learningKeyFuncs`). |
| `3` | Long-click action mappings. |

On a standard Android installation, this corresponds to the service's private `shared_prefs/Steer.xml` (typically `/data/user/0/com.syu.ms/shared_prefs/Steer.xml`, with `/data/data/com.syu.ms/...` as a common alias). This is an inferred standard location, not a file read from the actual head unit.

The UI app additionally uses its own **`steersave`** preferences for custom selection/display bookkeeping. That is a separate file in `com.syu.steer` app data, not the service's mapping store.

The helper uses `commit()` for nonempty changed maps and `apply()` for removal. Its sparse-map setter skips empty maps, so saving an empty map alone is not equivalent to the explicit clear path. Commit results are not propagated as verified hardware-save status.

## ADC learning flow

The service's labels explicitly refer to ADC values. `u0/c.b` receives the current ADC reading and publishes callback field 2. If a target function is selected, it invokes the assignment routine. Assignment can also use the latest ADC value when the target is selected, so the code supports either arrival order.

The concrete assignment routine rejects ADC value 50 as a sentinel and validates supported function slots. It sends slot-specific MCU messages. Incoming MCU feedback in `y/i.n` updates the learned values through `u0/c.f` and notifies clients on callback field 1.

This is evidence of electrical/ADC-based learning, consistent with directly wired resistive controls. The APK does not identify which physical KEY wire carries the user's buttons.

The client `SteerActivity.onResume` sends module 10 / command 2 / argument 1 to enable detection. `onPause` sends argument 0 to disable it. Its Save click sends command 4 and runs a callback that finishes the activity. The inspected pause method does not send command 4; leaving the screen is therefore not the same client action as pressing Save. This does not establish whether the MCU independently retains intermediate learning.

Sources: [u0/a.smali](evidence/u0/a.smali), methods `a`, `h`, `r`; [u0/c.smali](evidence/u0/c.smali), `b`, `f`, `g`; the existing [y/i evidence](../deeper/evidence/y/i.smali), `n`; [client activity](evidence/com.syu.steer.SteerActivity.smali); [client click handler](evidence/com.syu.steer.SteerModule$1.smali).

## Separate MCU key-learning flow

Module command **5** passes a subcommand into `u0/a.d`:

| Argument | Code/log meaning | Outgoing payload |
|---|---|---|
| 1 | Start learning | `C1 01` |
| 2 | End learning and save | `C1 02`; commits preference group 2 |
| 3 | Clear learned keys | Clears groups 0/1/3 locally, then `C1 03` |
| 4 | Query learned keys | Reloads group 2, then `C1 04` |

Command 6 selects a key/function pair through `u0/a.e`, sends a `C2` message and updates the working group-2 map. The incoming learned-key-list handler `u0/c.k` recognizes a leading `FF` as the path that clears group 2 and broadcasts empty/unlearned states. Thus the two clear operations and their group-2 cleanup should not be treated as interchangeable synchronous actions.

The vendor logs call this MCU/panel key learning. That does not establish universal support for remapping CAN-decoded steering controls.

## CAN buttons and long presses

There are CAN-side event paths: for example, `f0/hp.Y2` translates incoming key values into `u0/c.j`, which dispatches standard function IDs. The ADC Clear/Save path is not shown rewriting that decoder translation. **Whether a particular CAN-box button can be remapped remains profile dependent.**

Long-click support is also concrete: command 10 sets a custom click mapping and command 11 sets a long-click mapping/MCU parameter for supported key codes, with a prerequisite that the click mapping exists. Group 3 is loaded, saved and used to dispatch long-click actions. This does not prove every button can produce a long-press event.

Sources: [CAN-side handler](evidence/f0/hp.smali), `Y2`; [u0/c.smali](evidence/u0/c.smali), `j`; [u0/a.smali](evidence/u0/a.smali), `f`, `g`, `n`, `t`.

## Correction to the earlier table description

The legacy `get` and initial callback loops expose indices 0–49, but `u0/b.e` actually allocates **100** ADC entries. A separate array has six entries. These are internal storage/API limits, not physical button counts.

## What this establishes for Cabin

A compatible client can use the existing module-10 service instead of recreating MCU transport. A reliable learning screen would need detection state, live ADC/learning feedback, distinct ADC and MCU learning modes, and explicit handling of clear/save effects. None of these findings alone establishes that the user's Honda buttons arrive through the learnable ADC path; the installed profile and observed events must distinguish that from CAN-decoded actions.
