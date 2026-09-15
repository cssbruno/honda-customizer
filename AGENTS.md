# Project instructions

- Communicate concisely, with clear priorities and short sections.
- Implement only features supported by verified FYT/SYU protocol evidence and exact compatible decoder profiles.
- No automatic connection fallback, OEM-service fallback, or substitution of another decoder's commands.
- Never use mock, demo, synthetic, or default vehicle values in the app. Missing, invalid, stale, or unavailable feedback must remain unavailable.
- Never present a dispatched command or local selection as confirmed vehicle state. Require real matching FYT feedback.
- Keep isolated protocol-test fixtures out of the application APK; they do not establish physical hardware compatibility.
