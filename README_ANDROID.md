# Nandan Android app — first native milestone

This branch introduces a native Android application for the OPPO A15 (CPH2185, 3 GB RAM) and Samsung Galaxy Tab A9 LTE (SM-X115).

## Included
- Native Kotlin + Jetpack Compose application.
- On-device Room database for tasks, explicit memories and reminders.
- Android notification receiver and AlarmManager scheduling.
- Fallback to inexact idle-allowed alarms when exact-alarm access is unavailable.
- No hosted AI API and no Windows runtime dependency in the Android app.

## Limitations
- The Assistant screen is a runtime placeholder. Local model inference is not connected yet.
- Reboot rescheduling, force-stop recovery and exact-alarm permission onboarding are follow-up work.
- Data is local to each device. Device pairing and synchronization are not implemented.
- Notification permission must be granted on Android 13+.
- Android may defer inexact alarms to protect battery life.

## Build
The Android CI workflow builds a debug APK on GitHub-hosted Linux runners. A laptop is not required to obtain the APK once the workflow succeeds.

## Device targets
- Minimum Android: API 26.
- Compile/target SDK: 35.
- Application ID: com.aventaro.nandan.
