# Honda Customizer 3.4.3 — full-screen FYT audit

**FYT audit** replaces the small Report dialog with a full-screen view. Open
it from the main screen to see connection status, head-unit/service versions,
exact decoder profile, decoder identification, vehicle readings and connection
history. The screen updates while open and preserves its scroll position.
**Back** returns to the controls; **Copy** optionally copies the current audit
without closing it. No report file needs to be created or uploaded.

The audit distinguishes readings that have not arrived, invalid values and
feedback that expired after 30 seconds. It shows how many mapped settings have
recent feedback and the time since each value reached the app. It never uses
missing or expired data as a vehicle state. After disconnecting, vehicle
readings are cleared and retained decoder identification is marked historical.

XP decoder identification now reports an empty reply, invalid reply,
subscription failure or no reply within 8 seconds separately. Later valid
identification can recover from a timeout. Invalid identification clears any
earlier version text from the current connection.

For exact XP profile **0x4012A**, the audit explains the documented packet
route and the remaining cluster/raw CAN boundary alongside the reference
service version. These are source-code findings, not results of a hardware
transmission test. No new raw CAN/OBD command or extra cluster customization
is enabled. Opening the audit makes no additional connection or vehicle write.

Validation: **95 Java tests**, **11 Python tests**, release lint without
errors, and signed release build. The new tests cover live screen updates,
copying the latest data, navigation, feedback expiry/invalidity, decoder
timeout recovery and historical identification, with full-screen bounds in
portrait and landscape. Test fixtures remain outside the application APK.
Local emulator launch attempts exited before Android booted, so no native
emulator visual check is claimed. Vehicle compatibility has not been physically tested.

Version code **15**, retaining the existing signing certificate. Install
**Honda-Customizer-3.4.3-FYT.apk** or use the in-app update check.
