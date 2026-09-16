# Honda Customizer 3.4.1 — ready XP actions

Adds three buttons on exact XP profile **0x4012A**, alongside the existing 22 persistent settings:

- Reset maintenance information: `C6 02 0E 00`.
- Restore vehicle customization settings: `C6 02 0F 00`.
- Request TPMS calibration: `C6 02 11 00`.

No byte entry is required. Parked acknowledgement and action-specific confirmation are required before sending. The report records the action name and packet. Each packet is sent once through FYT module 7 command 1008; FYT adds E3. This reproduces the verified stock XP command-105 payload, without substituting OEM controller frames.

Completion on the vehicle remains unconfirmed. Sending clears old readings and disconnects; reconnect for new feedback. No retries, decoder substitutions or service fallback. The existing manual packet editor remains available.

Validation: 84 Java tests, 7 Python tests, release lint with no errors. The actual buttons are tested for their exact outgoing packets and cancellation. Physical vehicle execution has not been tested.

Version code **13**, retaining the existing signing certificate. Install **Honda-Customizer-3.4.1-FYT.apk** or use the in-app update check.

[Protocol evidence](research/XP-READY-ACTIONS.md).
