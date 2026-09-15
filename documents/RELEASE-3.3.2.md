# Honda Customizer 3.3.2 — FYT connection fix

Fixes the `Invalid FYT reply` failure when connecting to the documented Joying SYU service. Its command, register and unregister methods return no reply body. Previous versions required an exception header for every call, so successful subscription could immediately trigger a disconnect.

The transport now accepts empty replies only for module transactions 1, 3 and 4, following the existing documented Cabin correction. It retains synchronous dispatch for services that return exception headers. Toolkit lookup and reads still require replies; nonempty exceptions and unhandled transactions still fail. Commands are never retried automatically.

Decoder restrictions, fresh-feedback requirements and matching post-dispatch confirmation remain in force. Dispatch alone does not confirm a vehicle change. No connection fallback or new vehicle commands are added.

Version code 10; existing development certificate retained. Install **Honda-Customizer-3.3.2-FYT.apk**, or use **Check for updates** in version 3.3.0 or later.

Validation: the empty-reply regression failed before the fix. Release unit tests cover empty and exception-header replies, connection, cleanup, command encoding, rejection and feedback confirmation. All 62 Java release tests and 7 Python tests passed; release lint reported no errors. APK metadata and signature checks are required by the distribution packager. Physical head-unit operation remains unverified.

See the [wire-contract evidence](research/fyt-transport-audit/FINDINGS.md).

CI setup now requests `platform-tools` explicitly, avoiding the retired `tools` package that stopped Android setup before tests could run.
