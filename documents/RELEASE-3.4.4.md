# Honda Customizer 3.4.4 — request XP data like the stock app

For exact XP profile **0x4012A**, the app now sends the stock Honda entry
request **100 [0]** once after registering feedback callbacks. Earlier
versions subscribed without making this request. The reference service
requests packets 0x33 and 0x32; its 0x32 receive branch supplies the mapped
settings. See [source evidence](research/XP-DATA-REQUEST.md).

The full-screen **FYT audit** includes **Request FYT data** for an explicit
repeat. It shows dispatch progress and how many settings produced valid
events during an 8-second observation window. Missing feedback, a failed
call and a call timeout have separate outcomes. No automatic retry occurs.
Opening the audit makes no additional request.

Earlier readings are cleared when requesting again. Only received setting
events populate the screen; cache replay stays disabled. FYT can suppress
unchanged values even after a request, so this correction cannot guarantee
that the owner’s firmware will return every setting. The observed decoder
version **CRI V1.13.108BYPT** is research evidence, never an app default.
Additional OEM cluster customization and raw CAN/OBD remain unresolved.

Version code **16**, retaining the existing signing certificate. Includes
the full-screen audit introduced in 3.4.3. Install
**Honda-Customizer-3.4.4-FYT.apk**; the in-app updater can find it only after
this release has been published.

Validation: 101 Java tests, 11 Python tests, release lint and a signed
release build. Test fixtures are excluded from the APK. No physical FYT
hardware was connected; this is not a claim of a successful vehicle test.
