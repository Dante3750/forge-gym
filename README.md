# Forge

A free Android gym app: 876 exercises, a 3D body map, plans for men and women from beginner to hard, training at the gym,
at home with dumbbells or with no equipment, a walking tracker and weight check-ins. Everything stays on your phone.

**[Download the latest APK](https://github.com/Dante3750/forge-gym/releases/latest)**

## Screens

Taken automatically on an Android emulator by CI after every build.

| Home | Explore | 3D body map |
|:---:|:---:|:---:|
| <img src="https://raw.githubusercontent.com/Dante3750/forge-gym/screenshots/home.png" width="240"> | <img src="https://raw.githubusercontent.com/Dante3750/forge-gym/screenshots/explore.png" width="240"> | <img src="https://raw.githubusercontent.com/Dante3750/forge-gym/screenshots/body.png" width="240"> |
| **Exercise with 3D trainer** | **Same move at home** | **Weekly plan** |
| <img src="https://raw.githubusercontent.com/Dante3750/forge-gym/screenshots/exercise.png" width="240"> | <img src="https://raw.githubusercontent.com/Dante3750/forge-gym/screenshots/exercise_home.png" width="240"> | <img src="https://raw.githubusercontent.com/Dante3750/forge-gym/screenshots/plan.png" width="240"> |
| **Workout** | **Walk** | **Progress** |
| <img src="https://raw.githubusercontent.com/Dante3750/forge-gym/screenshots/workout.png" width="240"> | <img src="https://raw.githubusercontent.com/Dante3750/forge-gym/screenshots/walk.png" width="240"> | <img src="https://raw.githubusercontent.com/Dante3750/forge-gym/screenshots/progress.png" width="240"> |
| **Onboarding** | **Settings** | |
| <img src="https://raw.githubusercontent.com/Dante3750/forge-gym/screenshots/onboarding.png" width="240"> | <img src="https://raw.githubusercontent.com/Dante3750/forge-gym/screenshots/settings.png" width="240"> | |

## What's inside

- **Exercise library:** 876 exercises with photos, steps, filters by muscle, level and equipment, and favourites.
- **3D trainer:** a realistic animated human for every movement, men and women, two camera angles and half speed.
  Rendered from code in [forge-motion](https://github.com/Dante3750/forge-motion).
- **Three ways to train it:** each move shown for the gym, home dumbbells and no equipment.
- **Plans:** a weekly program from your level, goal, days and where you train.
- **Workouts:** log sets, rest timer, personal records.
- **Walk mode:** step counter with daily goal and history.
- **Weight check:** manual weigh-ins, trend chart and BMI; ready for a smart scale later.

## Build

Pushing to `main` builds the APK with GitHub Actions and publishes it as a release, then takes the screenshots above.
Kotlin, Jetpack Compose, no server and no paid APIs.

Exercise data: [free-exercise-db](https://github.com/yuhonas/free-exercise-db) (Unlicense).
