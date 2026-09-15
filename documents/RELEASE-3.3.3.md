# Honda Customizer 3.3.3 — XP Civic profile support

The supplied head-unit report identifies profile **0x4012A**, which previously connected but showed no mapped controls. This release recognizes it as the vendor's **XP Honda 2016 Civic High** profile and adds its 22 verified persistent settings.

Includes tachometer options, trip-reset conditions, outside-temperature adjustment, lighting, remote access, locking and driver attention. Each setting requires valid live feedback before editing and matching feedback after a change. Settings without feedback remain unavailable.

The exact stock XP menu route, profile visibility, command keys and legal values were audited independently. Other decoder profiles and hidden XP settings are not enabled by this change. See the [XP evidence](research/XP-4012A.md).

Version code 11; same signing certificate as 3.3.2. Install **Honda-Customizer-3.3.3-FYT.apk** from this release, or use **Check for updates**. If the head unit reports updates unavailable, download the APK manually and install it over the existing app.

66 Java release tests and 7 Python tests passed; release lint reported no errors. Validation covers all 22 settings, exact profile subscriptions, rejection across decoder families, invalid feedback and matching write confirmation. Physical connection is evidenced by the owner's report; settings readback and writes remain unverified on that unit.
