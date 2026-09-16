# Honda Customizer 3.4.0 — manual XP packets

Adds **Manual XP packet / Pacote XP manual** alongside the existing FYT settings on exact decoder profile **0x4012A**.

Connect, acknowledge parked, open the editor, enter the XP packet body as hexadecimal bytes separated by spaces, then review and confirm. The editor starts empty. It accepts 1–64 bytes and sends them once through FYT module 7 command **1008**, whose documented implementation adds E3 and forwards the supplied body to the decoder transport.

This implements a manual XP packet sender. It does not provide automatic Honda-service-to-XP conversion, additional OEM feature presets, or a proven arbitrary physical CAN-frame format.

The app records the request and dispatch outcome in the connection report. It never treats dispatch or unrelated feedback as vehicle confirmation. Existing readings are discarded when sending starts; the app disconnects afterward. Reconnect to obtain new live feedback. There are no automatic retries or service fallbacks. Connection changes, timeout and stale confirmation dialogs invalidate requests that have not yet been dispatched.

English and Portuguese UI are included. Test coverage includes exact byte preservation, empty/malformed/oversized input, profile restrictions, parked acknowledgement, connection ownership, cancellation before dispatch, timeout, rejection, concurrent-setting exclusion, and the actual review/confirmation dialogs. All 80 Java release tests and 7 Python tests passed; release lint reported no errors. Hardware execution remains unverified.

Version code **12**, retaining the existing signing certificate. Install **Honda-Customizer-3.4.0-FYT.apk** over the current version or use **Check for updates**.

Evidence: [XP lower-level send route](research/XP-RAW-SEND.md).
