# Nandan

**A local-first personal AI operating system, being rebuilt Android-first.**

Nandan is being developed for two Android devices:
- Primary: Samsung Galaxy Tab A9 LTE (SM-X115).
- Companion: OPPO A15 (CPH2185, 3 GB RAM).

## Current direction

The native Android application is the active product target. See [README_ANDROID.md](README_ANDROID.md) for the Android implementation, build process and current limitations.

The earlier React + FastAPI + Ollama implementation remains in the repository as a development prototype. Its Windows-specific reminder delivery is not the target runtime for the Android product.

## Android milestone status

- Native Kotlin + Jetpack Compose app scaffold.
- Room-based local memories, tasks and reminders.
- Android AlarmManager notification scheduling with inexact fallback.
- GitHub Actions workflow to build and upload a debug APK.
- Local AI inference, device pairing/sync, reboot recovery and approved automation remain future milestones and must be validated before being called complete.

## Product principles

- Offline-first data storage.
- No hosted AI API by default.
- No secret collection or unapproved external actions.
- Explicit user approval for consequential actions.
- Device-specific performance testing before selecting a local model.

## Development

The Android build runs in GitHub Actions. No desktop computer is required to trigger the build or download its artifact.
