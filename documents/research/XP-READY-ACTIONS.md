# Ready XP requests: exact profile 0x4012A

These are requests, not confirmed vehicle states. The outgoing sequences are traced independently for the stock XP entry route in [XP-4012A.md](XP-4012A.md). The reference service version differs from the installed version; hardware execution is unverified.

## Stock activity and visibility

`AcrivitySiYuSettings.smali` SHA-256: `9e4ec835ad344bd54401e02a53e544f1f29a63948b7fdf3129ae6d529b831b14`.

The offline interpreter in `tools/audit_bnr_source.py` was run against `onResume` with profile `0x4012a`, and against `onClick` with each view below. The layout defaults and runtime predicates leave the parents and action views visible. The results are saved in [xp-ready-actions.json](xp-ready-actions.json).

| Action | Parent / action view | onClick branch | Command 105 arguments | Command 1008 body |
|---|---|---|---|---|
| Maintenance reset | 0202 / 0234 | sswitch_353 | 14, 0 | C6 02 0E 00 |
| Restore defaults | 02fd / 0225 | sswitch_35a | 15, 0 | C6 02 0F 00 |
| TPMS calibration | 02fe / 022e | sswitch_361 | 17, 0 | C6 02 11 00 |

All view IDs have prefix `0x7f0b`. Stock labels are `str_298_reset_maintenance`, `str_298_default_all`, and `str_298_tpms_cal`. `setCarInfo` dispatches command 105 with the key and zero value.

## Equivalent transport

In the reference service, `module/canbus/v.smali` command 105 (`pswitch_11a`) builds `E3 C6 02 key value`, truncating key/value to bytes, then calls `y/k.t`. `f0/xp.smali` command 1008 (`pswitch_13d`) adds E3 to the caller body and calls the same transport. Consequently the four-byte bodies above reach the same transport as their stock requests. See [XP-RAW-SEND.md](XP-RAW-SEND.md) for service hashes and framing.

No OEM syscon frame or arbitrary CAN-ID encoding is inferred. No neighboring decoder is allowed.

## Application contract

The named action dialog describes its effect and requires explicit confirmation with the current parked acknowledgement and connection token. It uses the existing one-shot XP sender, including cancellation before dispatch, timeout, no retries and session invalidation. The report contains both action name and bytes. Dispatch cannot establish completion; neither unrelated callbacks nor cached readings are used to claim success. Old settings are discarded and the connection closes after dispatch.

The previous exclusion of these actions from persistent settings remains correct: they have no verified completion/readback contract. Version 3.4.1 makes their verified outgoing requests available separately, without a confirmed-state UI. Tests exercise actual Portuguese buttons and inspect Binder command 1008 and all four bytes. Fixtures are test-only and do not establish hardware compatibility.
