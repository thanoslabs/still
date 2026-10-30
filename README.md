# Still

An offline-first Android camera for polished, cinematic photography.

## Product principles

- Preserve the original photo.
- Suggest cinematic presets **after** capture, with manual override.
- Keep image processing on-device.
- Start with balanced controls and expand when needed.

## Current status

M1 in progress: CameraX preview, runtime camera permission, JPEG capture into Pictures/Still, and review of the latest capture. Automatic looks, non-destructive recipes, and personalized recommendations are upcoming milestones.

## Development

Requires JDK 17, Android SDK 35 and Gradle 8.11.1. Versions are in `.tool-versions`.

```sh
gradle :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```
