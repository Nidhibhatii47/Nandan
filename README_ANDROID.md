# Nandan Android app — Samsung tablet edition

Nandan is now being developed **only for the Samsung Galaxy Tab A9 LTE (SM-X115)**. The OPPO A15 and phone-sized layouts are no longer product targets for this release.

## Included in this milestone
- Native Kotlin + Jetpack Compose tablet application.
- Large-screen navigation rail and split-pane workspaces.
- On-device Room database for tasks, explicit memories and reminders.
- Android notification receiver and AlarmManager scheduling.
- Fallback to inexact idle-allowed alarms when exact-alarm access is unavailable.
- Offline speech-command input (requires an on-device English speech pack on Android 12+).\n- OM-inspired launcher icon and animated OM zoom-out launch screen.\n- GitHub Actions debug APK build.

## Not complete yet
- Voice commands currently use a small deterministic command set: add/create task, remember/save memory, set a reminder in N minutes, and navigate to Tasks, Memory, Reminders or Overview. This is not yet open-ended conversational reasoning.\n- Voice input is restricted to Android's on-device recognizer; if no offline speech pack is installed, voice input reports that it is unavailable rather than silently sending audio to a cloud recognizer.\n- The Assistant overview is a truthful placeholder; local language-model inference is not connected.
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
