# 📦 Package Architecture

This document describes the package organization of the Mehr Guard common module.

---

## Package Overview

```
com.mehrguard/
├── orchestration/     # 🎯 Main entry points
├── analysis/          # 🔍 URL analysis utilities
├── core/              # ⚙️ Core engines and utilities
├── engine/            # 🔬 Detection engines
├── ml/                # 🤖 Machine learning
├── model/             # 📊 Data models
├── scanner/           # 📷 QR scanning (platform-specific)
├── security/          # 🔐 Input validation
├── data/              # 💾 Persistence
├── ui/                # 🎨 Shared UI state
├── share/             # 📤 Content sharing
└── utils/             # 🛠 Utilities
```

---

## Package Descriptions

### 🎯 `orchestration/`
**Purpose:** Main entry points that coordinate multiple engines.

| Class | Description |
|-------|-------------|
| `PhishingOrchestrator` | Main URL analysis coordinator (alias for `PhishingEngine`) |

**Usage:**
```kotlin
val orchestrator = PhishingOrchestrator()
val result = orchestrator.analyze("https://suspicious-url.com")
```

---

### 🔍 `analysis/`  
**Purpose:** URL analysis utilities and parsers.

| Class | Description |
|-------|-------------|
| `UrlAnalysisUtility` | URL parsing with security validation |
| `RiskScoringUtility` | Risk score calculation |
| `VerdictDeterminer` | Verdict determination logic |

---

### ⚙️ `core/`
**Purpose:** Core functionality and foundational components.

| File | Description |
|------|-------------|
| `Constants.kt` | Application constants, URLs, limits |
| `ErrorMessages.kt` | Localized error message definitions |
| `PhishingEngine.kt` | Main orchestrator implementation |
| `UrlAnalyzer.kt` | URL parsing and component extraction |
| `UrlParser.kt` | Low-level URL parsing utilities |
| `RiskScorer.kt` | Risk score calculation |
| `VerdictEngine.kt` | Final verdict determination |

---

### 🔬 `engine/`
**Purpose:** Detection engines with specific algorithms.

| File | Description |
|------|-------------|
| `HeuristicsEngine.kt` | 25+ security heuristic rules |
| `HeuristicRules.kt` | Rule definitions |
| `HeuristicWeightsConfig.kt` | Configurable rule weights |
| `BrandDetector.kt` | Static brand impersonation detection (60 brands) |
| `DynamicBrandDiscovery.kt` | Pattern-based brand detection for unknown brands |
| `BrandDatabase.kt` | 60 brand definitions with 484 listed lookalike patterns |
| `BrandDatabaseLoader.kt` | Brand data loading |
| `HomographDetector.kt` | Unicode lookalike detection |
| `TldScorer.kt` | TLD risk scoring |

---

### 🤖 `ml/`
**Purpose:** Machine learning models for phishing detection using ensemble architecture.

| File | Description |
|------|-------------|
| `FeatureExtractor.kt` | URL feature extraction (15 features) |
| `LogisticRegressionModel.kt` | Linear ML prediction model (40% weight) |
| `EnsembleModel.kt` | Ensemble combining LR + Gradient Boosting + Decision Rules |

---

### 📊 `model/`
**Purpose:** Data models and types.

| Class | Description |
|-------|-------------|
| `RiskAssessment` | Complete analysis result |
| `UrlAnalysisResult` | Detailed score breakdown |
| `Verdict` | SAFE/SUSPICIOUS/MALICIOUS enum |
| `ScanResult` | QR scan result sealed class |
| `ContentType` | QR content type enum |
| `ErrorCode` | Error code enum |
| `ScanHistoryItem` | History persistence model |
| `ScanSource` | Scan input source enum |

---

### 📷 `scanner/`
**Purpose:** QR code scanning (platform-specific implementations).

| File | Platform | Description |
|------|----------|-------------|
| `QrScanner.kt` | Common | Interface definition |
| `AndroidQrScanner.kt` | Android | ML Kit implementation |
| `IosQrScanner.kt` | iOS | Vision framework stub |
| `DesktopQrScanner.kt` | Desktop | ZXing implementation |
| `JsQrScanner.kt` | Web | JS library wrapper |

---

### 🔐 `security/`
**Purpose:** Input validation and sanitization.

| File | Description |
|------|-------------|
| `InputValidator.kt` | URL and input validation |
| `Sanitizer.kt` | Input sanitization |

---

### 💾 `data/`
**Purpose:** Persistence layer.

| File | Description |
|------|-------------|
| `ScanHistoryRepository.kt` | History CRUD operations |
| `SettingsRepository.kt` | User settings storage |
| `DatabaseDriverFactory.kt` | SQLDelight driver (expect/actual) |

---

### 🎨 `ui/`
**Purpose:** Shared UI state management.

| File | Description |
|------|-------------|
| `SharedViewModel.kt` | Cross-platform state |
| `UiState.kt` | UI state sealed class |
| `Theme.kt` | Color and styling constants |

---

## Dependency Graph

```
                    ┌─────────────────┐
                    │  orchestration/ │  (Entry Point)
                    └────────┬────────┘
                             │
         ┌───────────────────┼───────────────────┐
         │                   │                   │
         ▼                   ▼                   ▼
    ┌─────────┐        ┌─────────┐        ┌─────────┐
    │ engine/ │        │   ml/   │        │ security│
    └────┬────┘        └────┬────┘        └────┬────┘
         │                  │                   │
         └──────────────────┼───────────────────┘
                            │
                            ▼
                    ┌─────────────────┐
                    │     model/      │  (Data Types)
                    └─────────────────┘
```

---

## Performance Targets

These are the thresholds the performance tests assert on the JVM. The measured values are printed by the tests and are not recorded here.

| Component | Threshold asserted in tests |
|-----------|-----------------------------|
| Full URL Analysis | < 50ms |
| Heuristics Engine | < 10ms |
| ML Inference | < 5ms |
| Brand Detection | < 15ms |
| Throughput | > 100 URLs/sec |

Run them: `./gradlew :common:desktopTest --tests "*Performance*"`

---

## Design Principles

1. **Stateless Engines** - All detection engines are stateless and thread-safe
2. **Defensive Coding** - All inputs validated, scores bounded
3. **No Network** - Analysis runs on the device and names no networking API
4. **Configurable** - Heuristic weights can be tuned
5. **Testable** - Each engine can be tested in isolation
