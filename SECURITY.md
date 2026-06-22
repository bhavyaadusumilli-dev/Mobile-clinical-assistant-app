Security

This repository is a demonstration project and contains no production backend integrations.

Important notes:
- Demo credentials were previously seeded in code; seeding now runs only when `BuildConfig.DEBUG` is true.
- `UserEntity` currently contains a `password` field. If moving beyond a demo, migrate to hashed passwords (bcrypt/Argon2) and remove plaintext storage.
- Do not commit `local.properties` or keystore files. These are ignored by `.gitignore`.

Responsible disclosure
- To report security issues, open an Issue in the repository and mark it `security`.
- Provide reproducible steps and avoid posting secrets in the issue body; maintainers will respond with instructions for secure disclosure.
