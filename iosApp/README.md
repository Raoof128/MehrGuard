<div align="center">

<img src="MehrGuard/Assets.xcassets/Logo.imageset/logo.png" alt="Mehr Guard Logo" width="96" height="96" />

# Mehr Guard

**Offline phishing and QR-code threat detection for iOS**

[![Build](https://github.com/Raoof128/MehrGuard/actions/workflows/ios-app-ci.yml/badge.svg)](https://github.com/Raoof128/MehrGuard/actions/workflows/ios-app-ci.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-iOS%2017%2B-lightgrey.svg)](https://developer.apple.com/ios/)
[![Swift](https://img.shields.io/badge/Swift-6.0-orange.svg)](https://swift.org)
[![Xcode](https://img.shields.io/badge/Xcode-17%2B-blue.svg)](https://developer.apple.com/xcode/)
[![Languages](https://img.shields.io/badge/languages-18-green.svg)](#localization)

Mehr Guard scans QR codes and evaluates URLs for phishing risk — entirely on your device, with no data ever leaving it. Built with SwiftUI and a Kotlin Multiplatform detection engine.

</div>

---

## Contents

- [Features](#features)
- [Detection Engine](#detection-engine)
- [Architecture](#architecture)
- [Requirements](#requirements)
- [Getting Started](#getting-started)
- [Development](#development)
- [Testing](#testing)
- [Localization](#localization)
- [Privacy](#privacy)
- [Contributing](#contributing)
- [Security](#security)
- [License](#license)

---

## Features

| | Feature |
|---|---|
| 📷 | **QR Code Scanner** — Real-time camera scanning with AVFoundation, plus gallery image import |
| 🔗 | **URL Analysis** — Heuristic scoring across 25+ threat signals with explainable results |
| 🛡️ | **Offline First** — All analysis runs on-device; no network calls, no accounts |
| 🎓 | **Beat the Bot** — Gamified phishing-detection training with live engine feedback |
| 📊 | **History & Export** — Full scan history with PDF/CSV export |
| 🔔 | **Widget** — Lock-screen and home-screen WidgetKit extension |
| 🌍 | **18 Languages** — Runtime language switching with full localization |
| ♿ | **Accessibility** — VoiceOver, Dynamic Type, and high-contrast support throughout |
| 🔒 | **Trust Centre** — User-controlled domain allow/block lists, sensitivity slider |

---

## Detection Engine

Mehr Guard uses a **unified analysis layer** that selects the best available engine at compile time:

```
URL Input
   │
   ▼
UnifiedAnalysisService
   ├── #if canImport(common)
   │       KMP HeuristicsEngine (Kotlin Multiplatform)
   │       25+ heuristics · ReasonCode enum · PublicSuffixList
   └── #else
           Swift Fallback Engine
           Homograph detection · TLD scoring · Brand impersonation
           IP obfuscation · Redirect detection · Typosquatting
```

**Scoring thresholds** (configurable via Trust Centre sensitivity slider):

| Sensitivity | Suspicious | Malicious |
|-------------|-----------|-----------|
| Low         | ≥ 50      | ≥ 85      |
| Balanced *(default)* | ≥ 31 | ≥ 71  |
| Paranoia    | ≥ 20      | ≥ 50      |

See [`ARCHITECTURE.md`](ARCHITECTURE.md) for the full layer breakdown and [`API_REFERENCE.md`](API_REFERENCE.md) for type documentation.

---

## Architecture

```
┌─────────────────────────────────────────────┐
│              SwiftUI Presentation           │
│  ScannerView · DashboardView · HistoryView  │
│  SettingsView · TrustCentreView · Widgets   │
├─────────────────────────────────────────────┤
│           ViewModels / State Objects        │
│  ScannerViewModel (@Observable, @MainActor) │
├─────────────────────────────────────────────┤
│           Domain / Service Layer            │
│  UnifiedAnalysisService · HistoryStore      │
│  SettingsManager · LanguageManager          │
├────────────────┬────────────────────────────┤
│  KMP Engine    │   Swift Fallback Engine    │
│  (common.fw)   │   (always available)       │
├────────────────┴────────────────────────────┤
│               Persistence                  │
│           UserDefaults · FileManager        │
└─────────────────────────────────────────────┘
```

---

## Requirements

| Requirement | Version |
|-------------|---------|
| macOS | Ventura 13+ |
| Xcode | 17+ |
| Swift | 6.0 |
| iOS deployment target | 17.0+ |

---

## Getting Started

```bash
# 1. Clone
git clone https://github.com/Raoof128/MehrGuard.git
cd MehrGuard/iosApp

# 2. Open in Xcode
open MehrGuard.xcodeproj

# 3. Select the MehrGuard scheme → choose an iPhone 17 simulator → Run
```

The app builds and runs without any additional dependencies. The Swift fallback analysis engine is always available.

### Optional: Link the Kotlin Multiplatform Framework

If you have the full monorepo with Gradle available:

```bash
cd iosApp
./scripts/build_framework.sh
# Copies common.framework → iosApp/Frameworks/common.framework
```

When the framework is linked, the app automatically uses the KMP `HeuristicsEngine`. Without it, the Swift fallback engine activates transparently. See [`INTEGRATION_GUIDE.md`](INTEGRATION_GUIDE.md) for details.

---

## Development

### Project Structure

```
iosApp/
├── MehrGuard/                   # App source
│   ├── App/                     # Entry point, scene lifecycle
│   ├── Models/                  # Services and data models
│   │   ├── UnifiedAnalysisService.swift
│   │   ├── KMPBridge.swift
│   │   ├── HistoryStore.swift
│   │   ├── SettingsManager.swift
│   │   └── MockTypes.swift
│   ├── Extensions/              # Color theme, localization helpers
│   ├── UI/                      # All SwiftUI screens
│   │   ├── Scanner/             # Camera + ViewModel
│   │   ├── Dashboard/           # Home screen
│   │   ├── Results/             # Threat result detail
│   │   ├── History/             # Scan history
│   │   ├── Training/            # Beat the Bot game
│   │   ├── Settings/            # App preferences
│   │   ├── Trust/               # Trust Centre + domain lists
│   │   ├── Export/              # PDF/CSV report export
│   │   ├── Onboarding/          # First-run flow
│   │   └── Components/          # Shared UI components
│   └── [18 locale folders]      # Localized strings
├── MehrGuardWidget/             # WidgetKit extension
├── MehrGuardTests/              # Unit tests (XCTest)
├── MehrGuardUITests/            # UI and accessibility tests
├── Tests/MehrGuardPackageTests/ # SwiftPM package-level tests
├── scripts/                     # build_framework.sh, import_assets.sh
├── ARCHITECTURE.md
├── API_REFERENCE.md
├── INTEGRATION_GUIDE.md
└── CONTRIBUTING.md
```

### Linting

```bash
# Install SwiftLint (first time)
brew install swiftlint

# Run
cd iosApp
swiftlint lint
```

Configuration: [`.swiftlint.yml`](.swiftlint.yml)

### Formatting

EditorConfig is enforced in all IDEs that support it. Configuration: [`.editorconfig`](.editorconfig)

---

## Testing

### Unit Tests (required gate)

```bash
xcodebuild \
  -project MehrGuard.xcodeproj \
  -scheme MehrGuard \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' \
  test
```

Runs `MehrGuardTests` (analysis engine, scoring logic, edge cases). This is the CI gate — all PRs must pass.

### UI Smoke Test

```bash
xcodebuild \
  -project MehrGuard.xcodeproj \
  -scheme MehrGuard \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' \
  test \
  -only-testing:MehrGuardUITests/MehrGuardUITests/testAppLaunches
```

### Full UI Test Suite

```bash
xcodebuild \
  -project MehrGuard.xcodeproj \
  -scheme MehrGuard \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' \
  test \
  -only-testing:MehrGuardUITests
```

Includes scanner flow, history flow, settings flow, accessibility compliance, and performance tests.

### SwiftPM Tests

```bash
cd iosApp
swift build   # package-level smoke
swift test    # may exit 139 on some Swift 6.2 toolchains — use xcodebuild as the authoritative gate
```

---

## Localization

Mehr Guard ships with full localization in **18 languages**:

Arabic · German · English · Spanish · Persian · French · Hebrew · Hindi · Indonesian · Italian · Japanese · Korean · Portuguese · Russian · Thai · Turkish · Vietnamese · Simplified Chinese

Language can be switched at runtime from the Settings screen without restarting the app.

---

## Privacy

- **No account required** — analysis is entirely local.
- **No network calls** — even in strict offline mode, no data leaves the device.
- **Local history only** — scan history is stored in `UserDefaults` on-device and can be cleared at any time.
- **Camera/photo access** — requested at first use; used only to read QR codes, never stored.
- **Privacy manifest** — `MehrGuard/PrivacyInfo.xcprivacy` declares all required reasons for API use.
- **Anonymous telemetry** — opt-in only; aggregated and anonymized.

---

## Contributing

Contributions are welcome. Please read [`CONTRIBUTING.md`](CONTRIBUTING.md) before opening a pull request.

**Quick checklist:**
- [ ] Build passes (`xcodebuild build`)
- [ ] Unit tests pass (`xcodebuild test`)
- [ ] SwiftLint passes (`swiftlint lint`)
- [ ] AGENT.md and CHANGELOG.md updated

---

## Security

To report a security vulnerability, please follow the process in [`SECURITY.md`](SECURITY.md). Do **not** open a public GitHub issue for security reports.

---

## License

Copyright 2025–2026 Mehr Guard Contributors.

Licensed under the [Apache License, Version 2.0](LICENSE). You may not use the files in this repository except in compliance with the License.
