# AGENTS

Project: PromoDisplay

Purpose:
- Android TV app that downloads and loops promotional videos for a federal hair
  salon chain.

Notes for contributors/agents:
- Keep Gradle dependencies in `gradle/libs.versions.toml`.
- Main entry point is `app/src/main/java/ru/offerfactory/promodisplay/MainActivity.kt`.
- Target device is Android 13 TV set-top box; prioritize unattended stability.
- Prefer Kotlin Coroutines + Flow for async and Media3 for playback.
- Persist config and playback stats locally with Room.
- Background work should go through WorkManager.

Common commands:
- `./gradlew :app:assembleDebug`
- `./gradlew :app:testDebugUnitTest`
