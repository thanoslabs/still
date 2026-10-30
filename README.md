# Still

An offline-first Android camera for polished, cinematic photography.

## Product direction

- Original captures are preserved.
- Cinematic presets are recommended **after** capture, never forced on the live viewfinder.
- Preset selection is always overridable.
- Image processing and preferences remain on-device.
- The camera starts simple, with optional advanced controls.

## Status

M0: native Android foundation. The camera, image pipeline, and recommendations are planned for later milestones; this starter does not capture photos.

## Development

Requires JDK 17, Android SDK 35, and Gradle 8.11.1. Versions are pinned in `.tool-versions`.

```sh
gradle :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

CI builds a debug APK for each PR. See `docs/` for future project documentation.
