# Honda Customizer 3.2.0 — FYT / Joying

## Changes

- Expanded exact-profile support: up to 40 WC controls, 59 RZC persistent settings and 37 BNR mappings (26–32 visible for each supported BNR profile).
- English and Portuguese interface, including control labels, confirmation dialogs and connection reports.
- Updated app icon and stronger connection/feedback handling. Only matching real FYT feedback confirms vehicle state; unavailable or stale values disable editing.
- Explicit toolkit connection through `com.syu.ms`, CANBUS module 7. No automatic service or decoder fallback.
- Removed emulator checks from release requirements and CI at the project owner’s request. Emulators have no FYT service access. Offline protocol fixtures remain outside the application APK.

## Install

Download **Honda-Customizer-3.2.0-FYT.apk** and install it on the FYT head unit. Version code 7; minimum Android API 17. The APK retains the local development signing certificate; an existing installation must have a matching certificate to update in place.

## Verification and limits

46 release Java tests and 7 Python parser tests passed. Release lint has no errors. Packaging verifies fresh build/test evidence, APK version, non-debuggable status, signature and exclusion of original Honda service code. SHA-256 checksums, source archive and verification reports accompany the APK.

**Physical FYT hardware is unverified.** Software tests do not establish decoder/vehicle compatibility. Connect and inspect the real head-unit report; only exact supported profiles with fresh feedback enable their controls. Vehicle language, maintenance/reset actions, full panel editing, diagnostics and other unsupported FYT functions remain unavailable. See the implementation status document for remaining protocol work.
