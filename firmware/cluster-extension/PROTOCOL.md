# HCX1 custom wire protocol

This contract is **newly implemented custom firmware**, not a stock XP command.
It is only for the experimental SHA-pinned HCX/111 port. No physical vehicle
compatibility or stock SYU response support is implied.

## Request body

Thirteen bytes, before serial framing:

| Offset | Meaning |
|---|---|
| 0 | `D7`, custom HCX type |
| 1 | `0B`, payload length 11 |
| 2–4 | `48 43 58`, ASCII HCX |
| 5 | `01`, protocol revision |
| 6 | Operation: `00` HELLO, `01` SET, `02` STATUS |
| 7–10 | Transaction ID, unsigned 32-bit big-endian |
| 11 | Feature selector |
| 12 | OEM API value |

SET requires a nonzero transaction ID, a valid feature/value and no outstanding
operation. The previous transaction ID is rejected on SET; callers must choose
a new ID for each operation. This is duplicate suppression, not authentication
or a persistent replay log. Reboot discards transaction history.

HELLO requires zero feature and value and writes no CAN. STATUS requires zero
feature/value and the exact recorded nonzero transaction ID; it returns that
transaction's result, **not a fresh vehicle-state reading**.

Serial framing uses `2E + body + ((sum(body) XOR FF) AND FF)`. The original
receiver validates this checksum before the custom dispatcher. The module-7
1008 caller body would be these 13 bytes; outer routing/framing is handled
downstream. Installed transport mode and response routing are not verified.

## Features and OEM mapping

| Feature | OEM setting | Allowed API values | CAN menu | Encoded value |
|---|---|---|---|---|
| `01` Meter configuration | `03/5B` | 1–3 | `15` | API − 1 |
| `02` Turn-by-turn display | `03/38` | 1–2 | `20` | API − 1 |
| `03` Color selection variant 1 | `03/29` | 1–7 | `32` | API |
| `04` Color selection variant 2 | `03/2A` | 1–8 | `33` | API 1 → 7; otherwise API − 1 |

Each produces extended CAN ID **`16305054`**, data frame, DLC 3,
payload **`40 menu encoded-value`**. Source `50` and sender `54` are fixed
for this experimental port, based on the audited Honda templates. They are
not discovered vehicle capabilities. There are no raw ID, DLC or arbitrary
payload parameters, default vehicle values or invented visual/color names.

The color-variant-2 encoding is not injective: API 1 and 8 both encode 7.
Consequently a matching reply does not distinguish those API selections.

## Responses

Responses have the same 13-byte body and serial framing. Byte 6 is a status;
bytes 7–10 identify the transaction; 11–12 echo its requested feature/API
value. Invalid/malformed requests return INVALID with zero transaction,
feature and value as unavailable metadata, not vehicle state.

| Status | Meaning |
|---|---|
| `10` READY | HELLO only: byte 11 is protocol revision 1, byte 12 is feature mask `0F` |
| `11` QUEUED | Accepted into the vendor CAN queue; no transmission confirmation |
| `12` TX_STARTED | Vendor hardware writer returned mailbox 0–2; no vehicle confirmation |
| `13` MATCHING_REPLY | Matching CAN response observed after mailbox submission and before timeout |
| `14` TIMEOUT | No timely transmit or matching response; no retry |
| `15` BUSY | Another operation or an expired owned queue entry is outstanding |
| `16` INVALID | Wrong protocol, length, operation, transaction lookup or feature/value |
| `17` QUEUE_FULL | Queue pointer unavailable or queue slot occupied |
| `18` TX_FAILED | Vendor hardware writer did not return mailbox 0–2 |
| `19` DUPLICATE | SET reused the last recorded transaction ID |

Acceptance is replied immediately. Main-loop polling reports status changes;
rapid transitions may coalesce to the newest result. STATUS can retrieve the
recorded result. Results are not cached vehicle-setting readings.

The expected observed reply is extended ID **`16305450`**, data frame, DLC 3,
**`C0 menu encoded-value`**. A matching response before transmission or after
800 vendor ticks is ignored for completion. CAN carries no HCX transaction
ID: an old identical CAN response arriving inside the window cannot be
distinguished by this protocol. The result therefore means matching response
observed, not cryptographic or causal proof of this particular write.

No response is translated into callback 1005 or another stock FYT setting.
The stock SYU service's handling of `D7` replies is unresolved. An Android UI
must not present these results without a verified receive path and must not
enable these commands on an unmodified XP box.
