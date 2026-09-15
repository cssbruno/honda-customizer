# Honda Customizer 3.3.0 — Joying / FYT

An Android app for FYT head units using the installed `com.syu.ms` service. It has no original Honda/Mitsubishi service dependency, permission, editor or executable client code in the APK.

[3.3.0 release notes](documents/RELEASE-3.3.0.md)

## Repository

This directory is the independent Honda Customizer Git repository. Run build and Git commands here. Its original project history was extracted from Cabin. This project lives outside the Cabin directory and has its own Git repository. The older original-head-unit workspace is preserved at `../R/honda-customizer-original-hu-backup`.

The source, Gradle wrapper, tests and CI workflow are self-contained. Research reports and derived catalogs are included under `documents/research`. Original firmware inputs for historical extraction scripts remain external and are not build dependencies.

## Documentation

Start with the [documentation index](documents/README.md) for FYT protocol evidence, OEM references, catalogs and validation limits.

## App updates

The app checks the latest stable release from `cssbruno/honda-customizer` on GitHub when opened. **Check for updates** retries manually. An available update offers **Download and install**; download starts only after your confirmation. Android confirms installation and may require permission to install updates from this app. After granting permission, return and tap **Install downloaded update**.

Downloads must match the release SHA-256 digest, application ID, newer version code and installed signing certificate. The installer receives read access to the verified APK in private cache. Failed checks stay unavailable; no vehicle state or head-unit report is sent. Internet access and a published GitHub release with its FYT APK and digest are required. Older firmware may lack working modern HTTPS support; network failure never triggers an insecure connection.

Install 3.3.0 manually once to get the updater. Future published stable releases can be discovered in the app. This is automatic checking with confirmed installation, not silent background installation. The updater never sends FYT commands.

## FYT connection

Connect uses `com.syu.ms.toolkit` (`app.ToolkitService`) and obtains CANBUS module 7. There is no automatic fallback to the direct CANBUS service or to OEM services. If the toolkit is unavailable, rejects access, or times out, the app disconnects and reports the failure. Firmware-specific exported toolkit names are resolved only inside `com.syu.ms`. See the [SYU service research](documents/research/syu-ms-analysis/REPORT.md).

The detected profile comes from FYT field 1000. An unmapped profile is reported as a connected FYT service with no mapped controls, rather than pretending the FYT service is absent. **Report → Copy** includes the service version, unit model, attempted toolkit connection, exact profile and received settings. It does not upload anything or issue vehicle commands.

## Decoder-specific controls

| Decoder profiles | Controls |
|---|---|
| WC: `0x40141`, `0x50141`, `0x60141`, `0xB0141`, `0xC0141`, `0xD0141` | Up to 13 panel controls, with the stock per-row restrictions: navigation directions, warning messages, Type 1–3, reverse tone, speed/message/idle-stop reminders, eco lighting, signs, alarm volume, trip A/B reset and outside-temperature adjustment. Command 106. Also up to 27 additional door, lighting, remote, camera, seat and driver-assistance controls with decoder restrictions and live feedback gating. |
| RZC Civic: `0x10012a`, `0x11012a`, `0x29012a` | 59 persistent settings: units, tachometers, doors, lights, mirrors/windows, cameras, seats and assistance. Exact command-105 keys and feedback fields are audited separately from WC/BNR. |
| BNR direct settings route: `0x6012a`, `0x7012a`, `0x8012a`, `0x9012a`, `0xA012a`, `0xB012a`, `0xF012a`, `0x28012a` | Up to 37 persistent mappings: 26, 31 or 32 rows are visible depending on the exact profile. Includes tachometer, doors, lighting, remote, panel and assistance settings. Units remain excluded because the stock entry route hides that row. |

These are explicit firmware profile IDs, not model-year guesses. WC commands cannot be sent through RZC/BNR settings. The second tachometer option retains the vendor wording because its distinction from display visibility is unresolved.

Connect, wait for live values, acknowledge parked, choose a setting and confirm. Only the active profile's fields are subscribed. Missing, invalid or older-than-30-second values disable editing; Refresh requests cached FYT values again. Successful Binder completion alone is not confirmation: the app also requires matching feedback received after dispatch. A timeout, changed profile or disconnect invalidates pending actions. An in-progress Binder transaction cannot be cancelled, and broadcast feedback has no request ID; do not operate concurrent vehicle editors.

Vehicle language, full panel-content editing, OEM camera controls, maintenance/reset actions, diagnostics and head-unit settings remain unavailable where complete FYT compatibility/feedback contracts are not established. The 59 persistent RZC settings and 37 BNR mappings have been audited and implemented with exact profile restrictions. The app interface supports English and Portuguese, selected by the Android system language. OEM catalog IDs are never substituted into FYT commands.

## Build and checks

```sh
ANDROID_HOME=/path/to/Android/Sdk ./gradlew :app:testReleaseUnitTest :app:lintRelease :app:assembleRelease --no-daemon
python3 -m unittest discover -s tools -p 'test_*.py'
python3 tools/package_release.py --sdk-root "$ANDROID_HOME"
```

APK: `app/build/outputs/apk/release/app-release.apk`, version code 8. Signed with the local development certificate; installing over an earlier copy requires a matching signing certificate. Minimum Android API 17 is a compatibility floor; the target hardware is the FYT head unit.

51 updater/service/protocol/localization regression tests and 7 smoke-result parser tests cover rejection without fallback (including a blocked toolkit), exact decoder commands, field subscriptions, fresh feedback, write failure, timeout and disconnect. Test-only Binder fixtures are excluded from the app; the application has no mock vehicle data. They do not establish operation on the user's physical FYT hardware. The exact installed firmware/decoder has not been supplied, and no vehicle is connected to this development environment.

Active sources: `app/src/fyt`, `fytTest`, `fytAndroidTest`. Original Honda sources/tests and `README-OEM-reference.md` are historical reference only; Gradle excludes their executable code. Old `original-HU` APKs in `dist` are not FYT builds. Emulator checks are not release requirements because emulators have no FYT service access. The distribution packaging script requires fresh release unit tests, lint, APK metadata and signature verification, and emits `Honda-Customizer-3.3.0-FYT.apk` only after those checks.

Command evidence: [FYT panel protocol](documents/HONDA-FYT-PANEL.md), [RZC/BNR findings](documents/research/honda-cluster-analysis/fyt-additional/other-honda/findings.md), and the saved `Acrivity_RZC_17CRVSettings`, `AcrivitySiYuSettings`, `HondaIndexActi`, and `FinalCanbus` bytecode. No firmware was flashed or vehicle commands sent during development.

## Additional WC controls

The [recorded WC command and feedback mappings](documents/research/honda-cluster-analysis/fyt-additional/doors-lights/findings.md) supply 27 additional controls. They retain the existing six exact WC profiles; light sensitivity and wiper linkage are hidden on `0x50141` and `0x60141`. Unknown values never become an Off/default selection. LaneWatch duration writes 4/5 and verifies returned 0/1. Vendor labels with unresolved meanings are explicitly marked. The UI groups controls by category.

These mappings establish the firmware contract, not installed vehicle equipment or physical compatibility. A real head-unit report and parked hardware validation are still required. No vehicle commands were sent during development.

See the [full completion audit](documents/FYT-IMPLEMENTATION-STATUS.md) for remaining requirements and RZC source anchors. The full implementation goal is not yet complete.
