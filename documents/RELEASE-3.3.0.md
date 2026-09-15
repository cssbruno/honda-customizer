# Honda Customizer 3.3.0 — in-app updates

The app now checks for new stable GitHub releases when opened and offers download and installation. A manual check button allows retrying after network failures. Update messages are available in English and Portuguese.

Before installation, the updater verifies the GitHub asset SHA-256 digest, package name, release version, newer version code and matching installed signing certificate. Downloads use HTTPS and restricted GitHub hosts. A private, read-only provider shares the verified APK with the Android installer. No FYT commands or vehicle reports are involved.

## Install

Install **Honda-Customizer-3.3.0-FYT.apk** manually once. Future stable releases published to this repository will be discoverable in the app. Android asks for installation confirmation; on Android 8+ you may need to allow installation from this app, return, and tap **Install downloaded update**. Install while parked.

Version code 8; minimum API 17; existing local development signing certificate retained. Very old firmware may not support the HTTPS connection required to reach GitHub. Checks remain unavailable on network failure.

## Validation

Release unit tests, lint, APK signature/version checks and distribution checksums are required. Emulator checks remain excluded from release requirements. End-to-end downloading and installation on a real head unit are unverified. Physical FYT compatibility and previously documented unsupported functions remain unchanged.

Publication requires approval; generating these artifacts does not publish a GitHub release.
