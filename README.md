# MeFit 💪

An Android app for gym and home training. It has a custom training calendar, auto-built workouts that list the machines you'll use, an 876-exercise library, and diet plans with personal calorie and macro targets.

Built with Kotlin and Jetpack Compose (Material 3, dark theme). Requires Android 8.0 (API 26) or newer.

## Features

| Tab | What it does |
|---|---|
| **Today** | Week strip, monthly stats and streak. Shows the planned session for the selected day: type, gym or home, estimated time, the **machines and stations you'll use**, and each exercise with sets × reps and rest for your goal. Tick off exercises, mark the day complete, or tap **New mix** to reshuffle. You can switch between gym and home with one tap. |
| **Plan** | A custom month calendar. Tap any day to choose one of 10 session types (Push, Pull, Legs, Upper, Lower, Full body, Core, Cardio & HIIT, Mobility, Rest), gym or home, add a note, or mark it complete. **Auto-plan** fills 1–8 weeks from a template: PPL, Upper/Lower, Full body 3x, Home only, or Hybrid. |
| **Exercises** | Search and filter by gym machines and weights, home (your equipment), muscle, equipment or type. The detail page shows an animated start/end demo, the machine needed, muscles worked, step-by-step instructions and suggested sets and reps. |
| **Diet** | Daily calories (Mifflin-St Jeor × activity, adjusted for your goal), protein/carb/fat targets and water. There are 6 diet styles (Balanced, High-protein cut, Lean bulk, Mediterranean, Vegetarian, Budget) with a sample day scaled to your calories, key principles, and what to eat more of or limit. |
| **Settings** (gear icon) | Name, level, training goal (strength, muscle or endurance), the equipment you own at home, and your body profile for the diet targets. |

## Where the data comes from

* **Exercises:** [free-exercise-db](https://github.com/yuhonas/free-exercise-db) is an open, **public-domain (Unlicense)** dataset of 876 exercises with instructions, muscles, equipment and images. The JSON is bundled in `app/src/main/assets/exercises.json`, so the library works offline. Images load from the project's GitHub over the internet.
* **Diet targets:** These use the Mifflin-St Jeor equation, protein of 1.6–2.0 g/kg (in line with sports-nutrition guidance such as the ISSN position stand) and about 25–33% of energy from fat. The meal plans are general templates written for the app. They are not medical advice.

## Build & run

### Option A: Android Studio (easiest)
1. Install [Android Studio](https://developer.android.com/studio) (Ladybug 2024.2 or newer).
2. Choose **File → Open** and select this `MeFit` folder. Let Gradle sync; it downloads the dependencies.
3. Plug in your phone (with USB debugging on) or start an emulator, then press **Run ▶**.

To get an installable APK, choose **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.

### Option B: Command line
```bash
./gradlew assembleDebug          # needs JDK 17 and the Android SDK (ANDROID_HOME or local.properties)
# APK → app/build/outputs/apk/debug/app-debug.apk
```

### Option C: Let GitHub build it (no setup)
Push this folder to a GitHub repo. The included workflow (`.github/workflows/build-apk.yml`) builds the APK on every push. Download it from the run's **Artifacts** section and install it on your phone.

## Project layout
```
app/src/main/java/com/mefit/app/
├── data/        Models.kt (Exercise, DayPlan, Settings…), Repository.kt (assets + local storage)
├── domain/      Training.kt (session types, workout generator, machine names, week templates)
│                Nutrition.kt (calorie/macro math, diet plans)
└── ui/          MeFitApp.kt (navigation), AppViewModel.kt, theme/, components/, screens/
```

Ideas for next steps: rest-timer notifications, logging weights per set, progress charts, and syncing with the wger API.
