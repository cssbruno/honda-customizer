# Honda Customizer 3.4.2 — decoder identification for cluster research

The XP connection report now reads the CAN decoder version text from FYT field **1005**, in addition to the existing numeric profile and SYU Android service version. The previous callback parser ignored string-only replies.

On exact profile **0x4012A**, connect and open **Report → Copy**. If FYT supplies the decoder version, it appears on its own line. Missing text remains unavailable. The report explicitly labels the version as service-reported and potentially cached; it does not confirm the current physical hardware identity. Reconnection clears the previous session's text. This subscription sends no vehicle command.

**This release does not add extra physical cluster customization.** The source archive now includes an offline derivation of 48 conditional OEM packets covering meter configuration, ambient-color options, turn-by-turn display and tachometer display. Their XP passthrough mapping remains unresolved and they are not included in the app as executable presets.

Validation: 89 Java tests and 11 Python tests; release lint has no errors. Coverage includes string-only callbacks, malformed payloads, unavailable text, stale-session rejection, and no vehicle writes during identification. No physical head unit or vehicle is connected to the development environment.

Version code **14**, retaining the existing signing certificate. Install **Honda-Customizer-3.4.2-FYT.apk** or use the in-app update check.

[Investigation and exact source anchors](research/CLUSTER-BRIDGE-AUDIT.md).
