# NextBus

> Production-grade realtime bus tracking app built with Jetpack Compose, Kotlin, and Firebase.

## 📱 Overview

NextBus is a modern transit companion app that lets users track buses in real time, search routes, compare travel options, and stay safe with SOS features. Built with a clean Compose-first architecture and Material 3 design system.

## ✨ Features

- **Live Tracking** — Smoothly animated bus markers with heading rotation and real-time ETA
- **Smart Route Search** — Origin/destination input with current location auto-fill and swap animation
- **Bus Comparison** — Compare buses by next departure, duration, fare, fuel type, and crowd level
- **Favorites** — Save routes for 1-tap tracking from the home screen
- **Travel History** — View past trips with route, distance, and duration
- **SOS Safety** — Share live trip links with family and emergency contacts
- **Bilingual** — Instant English/Hindi toggle without app restart
- **Material 3 Design** — Dynamic theming, glassmorphism cards, micro-interactions

## 🛠️ Tech Stack

| Layer | Technology |
|-------|------------|
| UI | Jetpack Compose, Material 3 |
| Language | Kotlin |
| Navigation | Compose NavHost |
| DI | Dagger Hilt |
| Backend | Firebase Realtime Database |
| Maps | Google Maps API + Maps Compose |
| Images | Coil |
| Architecture | MVVM + Clean Architecture |

## 🚀 Getting Started

### Prerequisites

- Android Studio Koala (2023.1.1) or newer
- Java JDK 11 or 17
- Android SDK API 24+

### Installation

```bash
# Clone the repository
git clone https://github.com/Heisenberg300604/NextBus.git

# Open in Android Studio
# File → Open → select the NextBus folder

# Sync Gradle and run on emulator/device
```

### Configuration

1. Add your `google-services.json` to `app/`
2. Set your Google Maps API key in `MapKey.kt`
3. Sync project with Gradle files
4. Build and run

## 📱 Screens

| Home | Map Tracking | Profile |
|------|--------------|---------|
| Live nearby buses & smart search | Real-time bus tracking with ETA | User profile, history, SOS |

## 🏗️ Architecture

```
app/src/main/java/com/ncs/nextbus/
├── ui/
│   ├── theme/          # Design tokens (Color, Type, Theme)
│   ├── components/     # Reusable UI components
│   ├── screens/        # Home, MapTracking, Profile
│   └── navigation/     # Compose NavHost
├── di/                 # Hilt modules
├── repository/         # Data repositories
└── MainActivity.kt     # Single-activity host
```

## 📄 Roadmap

- [x] Phase 1: Design System & Single-Activity Compose
- [x] Phase 2: Home Screen & Smart Route Search
- [x] Phase 3: Live Map & Tracking Overhaul
- [x] Phase 4: Profile, Favorites & Safety

## 📄 License

MIT License

---

Made with ❤️ for college transit.