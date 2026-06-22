**Architecture Overview**

- Pattern: MVVM (ViewModel + Repository + local data source)
- UI: Jetpack Compose driven by ViewModels that expose immutable UI state objects.
- Modules:
  - UI layer: `app/src/main/java/com/smartglasses/demo/ui` (Compose screens)
  - ViewModels: `app/src/main/java/com/smartglasses/demo/viewmodel` (state + logic)
  - Data layer:
    - `data/repository` — `AuthRepository`, `PatientRepository`
    - `data/local` — Room entities/DAOs
    - `data/scanning` — CameraX/ML Kit barcode analyzer
    - `MockDataSource` simulates remote patient data
- Data flow: UI -> ViewModel -> Repository -> (Room | MockDataSource | session DataStore)
- State management: `StateFlow` exposed from ViewModels; Compose collects state.
- Networking: No real remote API; `MockDataSource` simulates latency.
- Offline: All demo data is local (Room + MockDataSource). No sync implemented.

Extension points
- Replace `MockDataSource` with a real API client in `PatientRepository`.
- Add remote authentication by replacing `AuthRepository` logic.

Security notes
- Demo currently seeds users for local development; seeding is debug-only in this release.
