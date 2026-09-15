# Live FYT feedback and remaining implementation evidence

## Scope

The project owner excluded new sound and camera work from the current goal. The remaining focus is instrument-panel customization, vehicle language, maintenance and diagnostics. New amplifier and camera code was removed. Existing 3.3.0 controls remain pending the owner's clarification.

## Confirmed subscription defect and implementation

The examined Joying service is `com.syu.ms` version 2.23.0711.1001. Its source identity and method excerpts are recorded in `sources.json`.

1. `f0/tp.<clinit>` allocates the integer settings cache as a Java integer array. Its initial entries are zero, not proof of vehicle feedback.
2. `f0/xp.register(callback, field, notify)` first subscribes to the field through `i1/v.b`. If `notify == 0`, it returns before replaying cached values.
3. With `notify != 0`, fields below 1000 are delegated to the decoder's `register`. The 298-family driver `module/canbus/v.register` returns the stored integer without an observation timestamp or an availability flag.
4. For profile field 1000, the wrapper returns the configured exact decoder ID. This is configuration identity, not vehicle state.
5. `f0/wp.g0 → i1/v.s` updates and broadcasts only when the stored value differs. Consequently, a received live update can be followed by a long period with no new events even when the vehicle value is unchanged. Silence does not establish freshness.

The app now requests an initial snapshot **only for profile 1000**. Vehicle settings use `notify=0` and stay unavailable until subsequent matching field events. Reconnect never upgrades an unknown-age cached setting to a fresh value. Expired values are removed from the screen and report, and an earlier expiry timer cannot remove newer feedback.

This intentionally means some fields may remain unavailable on firmware that only emits changes. No active read/refresh command with fresh vehicle-response provenance has been established here. No connection fallback or cached/default substitute is used. Static tests cannot establish physical compatibility with the owner's head unit.

## Remaining feature blockers

| Feature | Source evidence | Missing requirement |
|---|---|---|
| Vehicle language | RZC picker writes command 105 `[85, selection]`; Portuguese = 15. WC picker has a separate three-language command 112. The inspected pickers keep their own selected index. | A decoder-specific feedback field and encoding confirming the vehicle language. Neither the RZC UI callback nor the inspected callback constants identifies one. |
| Maintenance reset | BNR `AcrivitySiYuSettings` writes command 105 `[14,0]`; RZC confirmation dialog sends the same key through its own source path. | A verified successful/failed completion response. Dispatch alone is insufficient. |
| Restore initial settings | BNR/RZC action key 15 exists. | Completion response and verified resulting settings; default values must not be invented. |
| TPMS calibration | BNR/RZC action key 17 exists. | A verified calibration-completion response. |
| Oil/service-life readout | Fields 135 (unit), 136 (sign), 137 (magnitude) appear in `AcrivitySiYuSettings` and the 298 driver. | Applicable source route/visibility for the current exact profiles. `onResume` hides the value TextView `0x7f0b016f` on every currently supported BNR profile; some profiles also hide its parent. Shared callback numbers alone do not authorize the display. |
| Full cluster content, colors, wallpaper and startup animation | Existing verified persistent panel controls are already mapped. ZX6606 dashboard classes render app views; OEM meter APIs use a different service. | Exact compatible FYT write/readback contracts for physical cluster editing. |
| Vehicle fault diagnostics | Other Honda-family screens and OEM diagnostics exist in the research archive. | Matching supported decoder contracts for reading/clearing vehicle faults. App connection diagnostics are not vehicle DTC diagnostics. |

The source review is scoped evidence, not a claim that no other firmware can provide these features. The public web search found no usable primary protocol source for the missing acknowledgements. Forum anecdotes are not command contracts.

## Next required evidence

A real head-unit Report, the exact installed `com.syu.ms`/CANBUS firmware identity, and vendor protocol documentation or captured matching real FYT responses are needed. The Report identifies the target; it does not by itself establish the missing reset/language acknowledgements. Do not execute blind writes to try to create that evidence.
