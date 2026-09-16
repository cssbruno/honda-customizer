# XP 0x4012A: the missing stock data request

## Finding

The owner’s new report photographs show profile **0x4012A**, SYU
**2.23.0718.1700 / 2123071817**, decoder identification
**CRI V1.13.108BYPT**, and all 22 mapped settings unavailable. This confirms
what the app displayed at that moment; it does not establish whether packets
reached the decoder or whether the service suppressed unchanged values.

Customizer through 3.4.3 subscribed to future setting callbacks but omitted
a stock Honda entry request. The reference CANBUS APK calls **module 7,
command 100, integer array [0]** when creating `HondaIndexActi`.
The corresponding service driver sends requests for packet types **0x33**
and **0x32**. Its **0x32** receive branch decodes the mapped settings.

This establishes a source-backed request to implement. It does not establish
that this request will make the owner’s newer service return all settings.

## Exact source route

Reference CANBUS APK SHA-256:
`98103bc4b64cfc87cd5957a3659304c078a69d8388ae4878fe4950119f15110d`.
Reference service 2.23.0711.1001 SHA-256:
`4b428302e29c9e2503ccf7844a127f5bed59450736317629aa42b5eb35a9b577`.
File hashes and source anchors are in [the manifest](xp-data-request-sources.json).

1. `ActivityLauncher.launchCanbus` explicitly maps **0x4012A** to branch
   `sswitch_9c`. With the vendor UI configuration equal to 1 it opens
   `HondaIndexActi`; the other UI configuration opens `ActivitySiYuIndex`.
   This is an observed compatible stock entry path, not an assertion that
   every vendor UI variant sends the request.
2. `HondaIndexActi.onCreate`, after `init`, invokes
   `DataCanbus.PROXY.cmd(100, 0)` without a further decoder condition.
3. `f0/xp.cmd` delegates commands below 1000 through `Q2` to the selected
   driver. The existing exact-profile audit selects `module/canbus/v` for
   this 298-family profile.
4. `module/canbus/v.cmd`, branch `cond_206`, requires a nonempty integer
   array and sends the following inputs to `y/k.t`, in order:

   | Service transport input | Origin |
   |---|---|
   | `E3 90 02 33 00` | First request with the stock argument 0 |
   | `E3 90 02 32 00` | Fixed second request in the same branch |

   The implementation invokes the stock command once and lets the service
   perform this sequence. It does not add a second manual packet dispatch.
5. `module/canbus/v.M2`, packed switch starting at **0x30**, maps **0x32**
   to `pswitch_729`. This branch publishes fields 58–87 and additional
   fields, with profile-specific branches. For **0x4012A**, field 78 is
   `(byte[p2 + 6] >> 2) & 1`, field 87 is
   `(byte[p2 + 8] >> 7) & 1`, and field 114 is
   `(byte[p2 + 8] >> 5) & 3`. These are decoded response fields, not write
   parameters or a raw CAN frame schema.

## Implementation in 3.4.4

- Only exact **0x4012A** can issue this data request.
- One request follows successful setting subscriptions on each explicit
  connection. Duplicate profile notifications do not issue another request.
- **Request FYT data** in the full-screen audit offers an explicit repeat.
  Opening the audit itself sends nothing.
- The app clears earlier readings and observes valid subsequent field events
  for 8 seconds. It distinguishes dispatch, no observed feedback, failed
  calls and calls that did not finish in time. Repeats and other commands
  are disabled during this window; there is no periodic retry.
- Counts mean distinct setting fields with valid events observed during the
  window. They are not counts of confirmed request responses: broadcasts
  carry no transaction ID. Events queued before dispatch do not count.
- Disconnect/profile changes and the pre-dispatch deadline guard cancel
  queued requests. An IPC call already in progress cannot be cancelled.

## Remaining limits

`f0/wp.g0 → i1/v.s` still suppresses unchanged values. Even a real new
decoder response can produce no field event if the service cache already
contains the same value. Cache replay has no freshness information and is
still excluded. No returned value is fabricated, and no setting write is
confirmed by completing the data-request call.

This corrects the earlier investigation’s missing entry-request path. It
does not supply a synchronous fresh snapshot, a request acknowledgement,
arbitrary raw CAN/OBD transport, or extra OEM cluster commands. The matching
XP raw-CAN packet format remains unresolved. No vehicle commands were sent
from this development environment.
