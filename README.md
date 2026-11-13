# Nandan

**A local-first personal AI operating system, built for the Samsung Galaxy Tab A9 LTE (SM-X115).**

Nandan is a **tablet-only** Android product for the Samsung SM-X115. The OPPO A15 and phone-sized layouts are not release targets.

## Current direction

The native Android application is the active product. See [README_ANDROID.md](README_ANDROID.md) for the tablet implementation, build process and current limitations.

The earlier React + FastAPI + Ollama implementation remains in the repository as a development prototype. Its Windows-specific reminder delivery is not the target runtime.

## Android tablet milestone

- Native Kotlin + Jetpack Compose tablet app.
- Large-screen navigation rail and split-pane task, memory and reminder workspaces.
- Room-based local data storage.
- Android AlarmManager notification scheduling with inexact fallback.
- GitHub Actions workflow to build and upload a debug APK.
- Local AI inference, reboot recovery and approved automation remain future milestones and require device testing.

## Product principles

- Offline-first data storage.
- No hosted AI API by default.
- No secret collection or unapproved external actions.
- Explicit user approval for consequential actions.
- Performance and model selection validated on the target tablet.

## Development

The Android build runs in GitHub Actions. No desktop computer is required to trigger the build or download its artifact.
