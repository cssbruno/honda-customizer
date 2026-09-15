# Honda Customizer 3.3.1 — live-feedback corrections

Fixes settings from the FYT service cache being treated as fresh vehicle values. Only decoder identity requests an initial snapshot; vehicle settings wait for subsequent live feedback. Expired values disappear from the screen and connection report. Reconnecting does not make cached settings fresh, and an older expiry timer cannot erase newer feedback. English and Portuguese instructions explain unavailable values.

The reference FYT service sends some fields only when their value changes. Those settings may stay unavailable after connection; the app does not invent a fresh-read command or substitute cached/default values.

New sound and camera additions were excluded by the project owner. Existing 3.3.0 controls are preserved. Full panel customization, vehicle language, maintenance resets and vehicle diagnostics still require missing exact-profile protocol/feedback evidence. Physical hardware and the complete updater installation flow remain unverified.

Version code 9; existing development signing certificate retained. Emulator checks are not required. Software verification consists of release unit tests, lint, APK signature/version checks, and artifact checksums.
