# NextBus

A bus tracking companion app for Android, built with Kotlin, Jetpack Compose and Material 3.

> **Status: under active rebuild (branch `revamp`).** The app is being rebuilt milestone by milestone.
> The app shell and navigation are in place; screens are placeholders until their milestone lands.
> This README is completed at the end of the revamp.

## Revamp milestones

- [x] **M0 – Make it build:** modern toolchain, legacy XML/Fragment and Firebase code removed, Compose app shell with navigation
- [ ] **M1 – Data layer:** local-only Room + DataStore, seeded routes/stops/schedules, live bus simulator
- [ ] **M2 – Design system & app shell:** theme, components, splash, permissions, settings
- [ ] **M3 – Features:** home, search, live map & tracking, saved, SOS
- [ ] **M4 – Polish:** motion, accessibility, Hindi strings, release hardening, docs

## Tech stack

| Layer | Technology |
|-------|------------|
| UI | Jetpack Compose, Material 3 |
| Navigation | Navigation Compose (type-safe routes) |
| DI | Hilt (KSP) |
| Local data | Room, DataStore |
| Maps | Google Maps SDK + Maps Compose |
| Async | Kotlin Coroutines & Flow |

## Getting started

### Requirements

- Android Studio with the Android SDK Platform 36
- JDK 17 or newer to run Gradle (the Android Studio bundled JDK works)

### Google Maps key (optional)

The app builds and runs without a key; the live map then shows a setup hint instead of tiles.
To enable map tiles, add your own Maps SDK for Android key to the git-ignored `local.properties`:

```properties
MAPS_API_KEY=your-key-here
```

Never commit API keys. Restrict the key to the package name `com.ncs.nextbus` (and `com.ncs.nextbus.debug`) and your signing certificate SHA-1 in Google Cloud Console.

### Build and verify

```bash
./gradlew assembleDebug testDebugUnitTest lintDebug
```

## License

MIT License
