# DailyWater

A simple water intake tracker for **iOS** and **Android**. Tracks today's intake only, with automatic midnight reset based on the user's local timezone.

---

## iOS — Run Instructions (Xcode)

### Step 1: Create Project
1. Open **Xcode 14+** → File → New → Project
2. Select **App** (under iOS)
3. Product Name: `DailyWater`
4. Interface: **SwiftUI**, Language: **Swift**
5. Uncheck "Include Tests" (optional)
6. Choose a location and click **Create**

### Step 2: Delete Default Files
- Delete the auto-generated `ContentView.swift` (you'll replace it)

### Step 3: Add Folder Groups
In the Project Navigator, right-click the `DailyWater` group:
- New Group → `Models`
- New Group → `ViewModels`
- New Group → `Storage`
- New Group → `Views`

### Step 4: Add Source Files
Copy the following files from `iOS/DailyWater/` into the corresponding Xcode groups:

| File | Xcode Group |
|------|-------------|
| `DailyWaterApp.swift` | Root (replace existing) |
| `Models/IntakeEntry.swift` | Models |
| `Storage/WaterStorage.swift` | Storage |
| `ViewModels/WaterViewModel.swift` | ViewModels |
| `Views/ContentView.swift` | Views |
| `Views/SettingsView.swift` | Views |
| `Views/CustomAddSheet.swift` | Views |

### Step 5: Build & Run
1. Select an **iOS 16+ Simulator** (e.g., iPhone 15)
2. Press **⌘R** to build and run
3. Tap the gear icon to set a daily goal, then start tracking!

### Key iOS Architecture
- **MVVM** with `ObservableObject` / `@Published`
- `scenePhase` detects foreground → calls `checkForNewDayAndResetIfNeeded()`
- `UserDefaults` with JSON encoding for persistence
- `Calendar.current` + `TimeZone.current` for local dateKey

---

## Android — Run Instructions (Android Studio)

### Step 1: Create Project
1. Open **Android Studio** (Hedgehog or newer recommended)
2. File → New → New Project
3. Select **Empty Activity** (Compose)
4. Name: `DailyWater`
5. Package name: `com.dailywater`
6. Language: **Kotlin**
7. Minimum SDK: **API 26** (Android 8.0)
8. Build configuration language: **Kotlin DSL**
9. Click **Finish**

### Step 2: Replace/Add Source Files
Copy files from `Android/app/src/main/java/com/dailywater/` into your project's corresponding package:

| Source File | Package |
|-------------|---------|
| `MainActivity.kt` | `com.dailywater` |
| `model/IntakeEntry.kt` | `com.dailywater.model` |
| `viewmodel/WaterViewModel.kt` | `com.dailywater.viewmodel` |
| `storage/WaterStorage.kt` | `com.dailywater.storage` |
| `util/DateKey.kt` | `com.dailywater.util` |
| `ui/HomeScreen.kt` | `com.dailywater.ui` |
| `ui/SettingsScreen.kt` | `com.dailywater.ui` |
| `ui/CustomAddDialog.kt` | `com.dailywater.ui` |
| `ui/theme/Theme.kt` | `com.dailywater.ui.theme` |

### Step 3: Update `app/build.gradle.kts`
Replace your `app/build.gradle.kts` with the provided one, or ensure these dependencies are present:

```kotlin
implementation(platform("androidx.compose:compose-bom:2024.01.00"))
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
implementation("androidx.datastore:datastore-preferences:1.0.0")
implementation("androidx.navigation:navigation-compose:2.7.6")
implementation("androidx.compose.material:material-icons-extended")
```

### Step 4: Update `AndroidManifest.xml`
Ensure the activity declaration references `com.dailywater.MainActivity`.

### Step 5: Update Theme Resource
Replace or add `res/values/themes.xml` with the provided one.

### Step 6: Build & Run
1. Sync Gradle (File → Sync Project with Gradle Files)
2. Select an emulator or connected device (API 26+)
3. Click **Run ▶**
4. Tap the gear icon to set a daily goal, then start tracking!

### Key Android Architecture
- **MVVM** with `AndroidViewModel` + `StateFlow`
- `LifecycleEventObserver` on `ON_RESUME` → calls `checkForNewDayAndResetIfNeeded()`
- **DataStore Preferences** for persistence with JSON string for entries
- `LocalDate.now()` uses device default timezone for local dateKey
- Navigation Compose for Home ↔ Settings

---

## Daily Reset Logic (Both Platforms)

```
dateKey = formatted as "yyyy-MM-dd" in LOCAL timezone

On app launch AND on every foreground resume:
  todayKey = format(now, "yyyy-MM-dd", localTimezone)
  if storedDateKey != todayKey:
      todayIntake = 0
      intakeEntries = []
      storedDateKey = todayKey
```

This ensures the reset happens based on the **local calendar midnight**, not "24 hours since first use".
