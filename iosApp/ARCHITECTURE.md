# Architecture

## Overview

Mehr Guard is a SwiftUI iOS application with an optional Kotlin Multiplatform (KMP) detection engine. The design follows a strict layered architecture: presentation → domain/service → platform → persistence.

```
┌──────────────────────────────────────────────────────┐
│                  Presentation Layer                  │
│  ScannerView  DashboardView  HistoryView  Settings   │
│  TrustCentreView  BeatTheBotView  ReportExportView   │
│  OnboardingView  MehrGuardWidget (WidgetKit)         │
├──────────────────────────────────────────────────────┤
│              ViewModel / State Layer                 │
│  ScannerViewModel (@Observable, @MainActor)          │
│  All other views use local @State + service calls    │
├──────────────────────────────────────────────────────┤
│               Domain / Service Layer                 │
│  UnifiedAnalysisService   HistoryStore               │
│  SettingsManager          LanguageManager            │
├────────────────────┬─────────────────────────────────┤
│   KMP Engine       │   Swift Fallback Engine         │
│   (common.fw)      │   (always available)            │
│   HeuristicsEngine │   UnifiedAnalysisService        │
│   PhishingEngine   │   analyzeWithSwift()            │
│   PublicSuffixList │                                 │
├────────────────────┴─────────────────────────────────┤
│                 Persistence Layer                    │
│         UserDefaults (settings + history)            │
└──────────────────────────────────────────────────────┘
```

---

## Layers

### Presentation

All SwiftUI screens live under `MehrGuard/UI/`. Screens are organised by feature:

| Directory | Screen(s) |
|-----------|-----------|
| `UI/Scanner/` | `ScannerView`, `CameraPreview` |
| `UI/Dashboard/` | `DashboardView` |
| `UI/Results/` | `ScanResultView` |
| `UI/History/` | `HistoryView`, `ThreatHistoryView` |
| `UI/Training/` | `BeatTheBotView` |
| `UI/Settings/` | `SettingsView` |
| `UI/Trust/` | `TrustCentreView` |
| `UI/Export/` | `ReportExportView` |
| `UI/Onboarding/` | `OnboardingView` |
| `UI/Navigation/` | `MainMenuView` |
| `UI/Components/` | `ResultCard`, `DetailSheet`, `BrainVisualizer`, `ImagePicker` |

Compose Multiplatform components (when the KMP framework is linked) are embedded via `UIViewControllerRepresentable` wrappers in `ComposeInterop.swift`.

### ViewModel / State

`ScannerViewModel` is the primary ViewModel, annotated `@Observable @MainActor`. It owns:
- Camera session lifecycle (AVFoundation)
- QR code debounce and sanitisation
- Delegation to `UnifiedAnalysisService`
- Result state + haptic feedback

All other screens consume services and `@State` directly — no additional ViewModel classes are needed.

### Domain / Service

| Service | Responsibility |
|---------|---------------|
| `UnifiedAnalysisService` | Engine selection, URL analysis, user settings integration |
| `HistoryStore` | Scan persistence, retrieval, deletion |
| `SettingsManager` | Haptics, sounds, app preferences |
| `LanguageManager` | Runtime language switching |

### Analysis Engine

Engine selection is compile-time, not runtime:

```swift
func analyze(url: String) -> RiskAssessmentMock {
    refreshUserSettings()          // pulls sensitivity + domain lists from UserDefaults
    #if canImport(common)
    return analyzeWithKMP(url: url)
    #else
    return analyzeWithSwift(url: url)
    #endif
}
```

**KMP path** (`analyzeWithKMP`): calls `HeuristicsEngine.analyze(url:)` from the Kotlin `common.framework`, extracts `HeuristicCheck` results, maps score to verdict using sensitivity-aware thresholds.

**Swift path** (`analyzeWithSwift`): runs 12+ check categories in order:

1. NFKC Unicode normalisation of host
2. User-blocked domain list (immediate +80)
3. Cyrillic/Latin mixed-script homograph detection
4. URL shortener detection (16 services)
5. Nested redirect detection (all occurrences)
6. Trusted domain list (built-in + user-managed)
7. Login/verify keyword scoring
8. Urgency language scoring
9. ASCII homograph patterns
10. High-risk and medium-risk TLD scoring
11. Brand impersonation detection
12. Structural checks: subdomain depth, IP detection, excessive hyphens
13. `@` symbol (credential theft)
14. Typosquatting patterns

**Verdict thresholds** (sensitivity-configurable):

| Sensitivity | Suspicious (≥) | Malicious (≥) |
|-------------|---------------|--------------|
| Low | 50 | 85 |
| Balanced (default) | 31 | 71 |
| Paranoia | 20 | 50 |

### Persistence

All persistence uses `UserDefaults`:
- Scan history (JSON-encoded `[HistoryItemMock]`)
- User-managed trusted/blocked domain lists
- Settings (haptics, sounds, sensitivity, language, etc.)

---

## Key Design Decisions

### Compile-time engine selection

`#if canImport(common)` guards ensure no runtime overhead for engine detection. The Swift fallback engine is a full implementation, not a stub — it provides complete analysis without the KMP framework.

### Normalised output model

Both engines produce a `RiskAssessmentMock` value type. Views never branch on which engine produced a result. This allows the KMP framework to be added or removed without touching any UI code.

### Sensitivity-aware thresholds

Verdict thresholds are read from `UserDefaults` before every analysis call via `refreshUserSettings()`. This means changes in the Trust Centre take effect on the next scan with no restart required.

### User domain lists

User-managed trusted and blocked domains are loaded from `UserDefaults` and merged with built-in lists at analysis time. Blocked domains short-circuit to a high score immediately.

### @MainActor isolation

`UnifiedAnalysisService` and `ScannerViewModel` are both `@MainActor` isolated. Background work (camera metadata processing) is dispatched via a dedicated serial `DispatchQueue` and then returned to the main actor via `Task { @MainActor in ... }`.

---

## Extension Surface

### WidgetKit

`MehrGuardWidget` provides five widget variants:
- Lock-screen circular, rectangular, and inline accessories
- Home-screen small and medium widgets

All variants deep-link to the scanner via the `mehrguard://scan` URL scheme.

### Deep Links

The app handles `mehrguard://scan` for direct scanner launch from widgets and system shortcuts.

---

## Testing Strategy

| Layer | Test type | Location |
|-------|-----------|----------|
| Analysis engine | Unit (XCTest) | `MehrGuardTests/SmokeTests.swift` |
| UI flows | UI (XCUITest) | `MehrGuardUITests/` |
| Accessibility | XCUITest | `MehrGuardUITests/AccessibilityUITests.swift` |
| Performance | XCUITest metrics | `MehrGuardUITests/PerformanceUITests.swift` |
| Package smoke | SwiftPM | `Tests/MehrGuardPackageTests/` |

Unit tests run in the default `xcodebuild test` gate. UI tests run via explicit `-only-testing:MehrGuardUITests` invocation (excluded from the default gate to keep CI fast).
