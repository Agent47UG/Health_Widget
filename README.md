# Health Widget

A modern, native Android Home Screen AppWidget suite built with **Jetpack Glance**, **Jetpack Compose**, and **Health Connect**. Features customizable activity rings, sleep stage architecture graphs, and vitals trend metrics with background syncing powered by **WorkManager**.

---

## Features

### 📱 Home Screen Widgets (Glance)
* **Health Rings Widget**: Concentric activity rings displaying **Steps** (Blue), **Active Calories** (Orange), and **Exercise Minutes** (Green), alongside a 7-day steps chart.
* **Sleep Architecture Widget**: Sleep stage breakdown graph (Awake, REM, Light, Deep) showing net sleep duration (excluding awake time), timeframes, and intelligent sleep insights.
* **Vitals Metrics 1x5 Widget**: Interactive multi-metric carousel featuring Breathing Rate (BR), Blood Oxygen (SpO2), Resting Heart Rate (RHR), Heart Rate Variability (HRV), Skin Temperature, and Nutrition metrics.
* **Sleep & Vitals Combined Widget**: A 4x4 combined widget layout combining the sleep stage architecture timeline with the vitals carousel.

### 🔗 Health Connect & Fitbit Integration
* Direct synchronization with **Google Health Connect**.
* Tapping the Health or Sleep widgets directly launches the **Fitbit (Google Health)** app (`com.fitbit.FitbitMobile`) or Google Health Connect.

### 🔄 WorkManager Background Refresh & Persistence
* Automatic background sync scheduled every **1 hour** via `WorkManager` (`HealthSyncWorker`).
* Persistent offline caching in `SharedPreferences` ensures widgets retain real health metrics across app restarts without defaulting to sample data.
* Includes background Health Connect read permissions (`READ_HEALTH_DATA_IN_BACKGROUND`).

### ⚙️ Interactive Companion App
* Built with **Jetpack Compose** (Material 3 Dark Theme).
* Customize daily targets for Steps, Active Calories, and Exercise Minutes.
* Toggle between **Health Connect Live Data** and **Demo Mode**.

---

## Tech Stack & Architecture

* **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
* **AppWidget Framework**: [Jetpack Glance](https://developer.android.com/jetpack/compose/glance)
* **Health Integration**: [Health Connect SDK](https://developer.android.com/health-and-fitness/guides/health-connect)
* **Background Execution**: [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
* **Language**: [Kotlin](https://kotlinlang.org/)
* **Build System**: Gradle Version Catalog (`libs.versions.toml`)

---

## Getting Started

### Prerequisites
* Android Studio Ladybug | 2024.2.1 or newer
* Android Device / Emulator running **Android 9 (API level 28)** or higher (Android 14+ recommended for built-in Health Connect framework)
* **Fitbit** or **Health Connect** installed on device for live health data sync

### Build & Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/Agent47UG/Health_Widget.git
   cd Health_Widget
   ```
2. Open the project in Android Studio.
3. Build and run the app:
   ```bash
   ./gradlew assembleDebug
   ```

---

## Adding Widgets to Home Screen

1. Touch and hold an empty space on your Android Home screen.
2. Tap **Widgets** and search for **Health Widget**.
3. Drag your desired widget (**Health Rings**, **Sleep Architecture**, **Vitals**, or **Sleep & Vitals**) onto your home screen.

---

## License

```text
Copyright (c) 2025 Health Widget

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```
