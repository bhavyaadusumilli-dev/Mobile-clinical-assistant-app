SmartGlassesDemo
=================

A compact Android demo application showcasing a mobile clinical assistant workflow for on-device barcode scanning and patient lookup using CameraX + ML Kit and Jetpack Compose.

Purpose
- Demonstrates a proof-of-concept mobile clinical assistant optimized for lightweight devices (smart glasses / handheld). The app scans patient barcodes and surfaces mocked patient data for quick triage.

Business problem solved
- Speeds identification and lookup of patient records at point-of-care using camera-based barcode scanning.

Intended users
- Developers and evaluators interested in mobile clinical assistant UX and device camera integration. The project is a developer demo; it is not a production medical system.

Core workflows & major features (implemented)
- Login (local demo users seeded in the app).
- Real-time barcode scanning via CameraX + ML Kit (multiple formats supported).
- Manual barcode entry fallback.
- Local Room database for demo user credentials.
- Mocked patient data source (simulates network latency) used by the PatientRepository.
- UI implemented with Jetpack Compose following MVVM (ViewModel + Repository) pattern.

Technical stack
- Language: Kotlin
- UI: Jetpack Compose
- Architecture: MVVM (ViewModel + Repository)
- Persistence: Room (local DB), DataStore (session)
- Camera & scanning: CameraX, ML Kit Barcode Scanning
- Build: Gradle Kotlin DSL (build.gradle.kts)

Notable files (evidence)
- App module: app/build.gradle.kts
- Manifest: app/src/main/AndroidManifest.xml (camera permission)
- Barcode logic: app/src/main/java/com/smartglasses/demo/data/scanning/BarcodeAnalyzer.kt
- Mock patient data: app/src/main/java/com/smartglasses/demo/data/MockDataSource.kt
- Authentication: app/src/main/java/com/smartglasses/demo/data/repository/AuthRepository.kt (demo users seeded)

Security & important findings
- Demo users with plaintext passwords are seeded in `AuthRepository.prepopulateDemoUsers()` (`drsmith` / `password123`, `drjones` / `password123`). See app/src/main/java/.../AuthRepository.kt.
- `UserEntity` stores the `password` field in plaintext (comment notes it should be hashed). See app/src/main/java/.../data/local/entity/UserEntity.kt.
- `local.properties` is present in the repository and contains an SDK path; this file should not be tracked for public releases.
- Build artifacts are present under `app/build/` and other generated folders; these should be removed from the repository and added to `.gitignore` (already added).

Build & run (Windows)
1. Ensure Android SDK and JDK are installed and `sdk.dir` is set locally (do not commit `local.properties`).
2. From the repository root, run:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
```

(Or use Android Studio to open the project and run on an emulator/device.)

Demo credentials
- Username: `drsmith` or `drjones`
- Password: `password123` (demo only)

Recommendations before publishing
- Remove `local.properties` from the repository and ensure it is git-ignored.
- Remove committed build artifacts (e.g., `app/build/`) from the repository history or at least from the working tree and add them to `.gitignore`.
- Replace plaintext demo credentials with secure, hashed storage if moving toward production.
- Consider adding unit/UI tests and CI build checks before wider release.

Repository owner
- Sole developer: bhavyaadusumilli-dev

License
- This repository includes an MIT license file.

*** End of README
