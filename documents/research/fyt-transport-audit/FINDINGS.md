# Joying FYT reply-contract correction

## Source

The documentation index points to the SYU service report, which references Cabin's Joying research. Cabin's `documents/research/JOYING-FYT-VOID-REPLIES.md` already records this exact defect and its correction. This app had retained the earlier strict reply check.

Reference: Joying UIS7862 `com.syu.ms` 2.23.0711.1001, APK SHA-256 `4b428302e29c9e2503ccf7844a127f5bed59450736317629aa42b5eb35a9b577`. The local APK hash was verified again on 2026-09-15.

External source: `/home/bruno/Documentos/ChatGPT/R/cabin/artifacts/joying-uis7862/extracted/applications/app/190000000_com.syu.ms/190000000_com.syu.ms.apk`.

Reviewed disassembly in `/tmp/cabin-syu-ms-smali`:

| File | SHA-256 | Contract |
|---|---|---|
| `x/c$a.smali` | `ad89a47f49f538648719796d2a98c08fc5d362326f3b66528eb3e28b7c4ba7a4` | Module operations 1/3/4 return true without writing a body; operation 2 writes an exception header and result. No branch on transaction flags. |
| `x/c$a$a.smali` | `5259ee36874104f5b059d691827419c37296169839544db69f8a944be4b2b205` | Vendor proxy uses flag 1 for command/register/unregister; flag 0 for reads. |
| `x/d$a.smali` | `8e7e1732d6fc4e5b9fa044ef59eddd1dbb55cd00c059e3a72bc6cc72ea18d0e2` | Toolkit lookup writes an exception header and module binder. |

## Change

Follow Cabin's correction: keep synchronous calls, accept a zero-length response exclusively for `IRemoteModule` transactions 1, 3 and 4. This works with the inspected stub and retains error handling for exception-header services. It is a reply-format rule, not an automatic retry or service fallback. The accepted empty response is never vehicle-state confirmation.

The previous client test fixture always wrote `writeNoException`, masking the actual Joying behavior. It now defaults to empty replies. Separate coverage retains exception-header behavior. A new transport regression reproduced the pre-fix `RemoteException` during registration.

Toolkit/read/unknown-operation empty replies, wrong descriptors, unhandled commands and explicit exceptions remain rejected. Existing live-feedback, exact decoder, stale-value and write-confirmation tests now run against empty module replies. Fixtures remain test-only and are not in the application APK.

Physical compatibility remains unverified. This fixes a proven transport defect; it does not establish missing language/reset/diagnostic feedback contracts or supply unavailable settings.
