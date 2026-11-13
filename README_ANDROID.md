# Nandan Android app — Samsung tablet edition

Nandan is now being developed **only for the Samsung Galaxy Tab A9 LTE (SM-X115)**. The OPPO A15 and phone-sized layouts are no longer product targets for this release.

## Included in this milestone
- Native Kotlin + Jetpack Compose tablet application.
- Large-screen navigation rail and split-pane workspaces.
- On-device Room database for tasks, explicit memories and reminders.
- Android notification receiver and AlarmManager scheduling.
- Fallback to inexact idle-allowed alarms when exact-alarm access is unavailable.
- GitHub Actions debug APK build.

## Not complete yet
- The Assistant overview is a truthful placeholder; local model inference is not connected.
- Reboot rescheduling, force-stop recovery and exact-alarm permission onboarding remain follow-up work.
- The tablet's real-device installation, display, notification and performance tests are pending.
- No phone pairing or cross-device synchronization is planned for this tablet-only release.

## Device target
- Primary device: Samsung Galaxy Tab A9 LTE, model SM-X115.
- Tablet/large-screen form factors only (small and normal screen support disabled in manifest).
- Minimum Android API 26; compile/target SDK 35.
- Application ID: com.aventaro.nandan.

## Build
GitHub Actions builds a debug APK on GitHub-hosted runners. No laptop is required to trigger the build or download its artifact.
