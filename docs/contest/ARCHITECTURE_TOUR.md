# 🏗️ Mehr Guard Architecture Tour

> **Contest-era document, kept as a record.** Written for the KotlinConf 2025–26 student contest. Figures in it could not all be reproduced; the [corrections table](README.md#corrections) lists what is wrong and what is true.

> **A visual 5-minute guide to the codebase for judges and contributors.**

---

## 📂 Project Structure at a Glance

```
mehrguard/
├── 📦 common/                    ← SHARED KOTLIN CODE (~80%)
│   └── src/
│       ├── commonMain/           ← Core detection engine
│       ├── commonTest/           ← Shared tests (900+)
│       ├── androidMain/          ← Android-specific expect/actual
│       ├── iosMain/              ← iOS-specific expect/actual
│       ├── desktopMain/          ← Desktop-specific expect/actual
│       └── jsMain/               ← Web-specific expect/actual
│
├── 📱 androidApp/                ← ANDROID UI (Jetpack Compose)
│   └── src/main/kotlin/          ← Compose screens, viewmodels
│
├── 🍎 iosApp/                    ← iOS UI (Native SwiftUI)
│   └── MehrGuard/                 ← Swift views, viewmodels
│
├── 🖥️ desktopApp/               ← DESKTOP UI (Compose Desktop)
│   └── src/main/kotlin/          ← Desktop-specific UI
│
├── 🌐 webApp/                    ← WEB UI (Kotlin/JS + HTML)
│   └── src/jsMain/               ← Kotlin/JS entry point
│
└── 📚 docs/                      ← Documentation
```

---

## 🧠 Detection Engine Flow

```
┌──────────────────────────────────────────────────────────────────────┐
│                         URL ANALYSIS PIPELINE                         │
├──────────────────────────────────────────────────────────────────────┤
│                                                                       │
│   INPUT                                                               │
│   ─────                                                               │
│   "https://paypa1-secure.tk/login"                                   │
│                                                                       │
│         ↓                                                             │
│   ┌─────────────────┐                                                │
│   │ InputValidator  │  → Sanitize, validate URL format               │
│   └────────┬────────┘                                                │
│            ↓                                                          │
│   ┌─────────────────────────────────────────────────────────┐        │
│   │                   PARALLEL ANALYSIS                      │        │
│   ├─────────────────────────────────────────────────────────┤        │
│   │                                                          │        │
│   │  ┌───────────────┐  ┌───────────────┐  ┌─────────────┐ │        │
│   │  │ Heuristics    │  │ ML Model      │  │ Brand       │ │        │
│   │  │ Engine        │  │ (Logistic     │  │ Detector    │ │        │
│   │  │ ─────────     │  │ Regression)   │  │ ──────────  │ │        │
│   │  │ • 25+ rules   │  │ ───────────   │  │ • 500 brands│ │        │
│   │  │ • IP check    │  │ • 15 features │  │ • Levenshtein│ │        │
│   │  │ • TLD check   │  │ • <10ms       │  │ • Exact match│ │        │
│   │  │ • Homograph   │  │               │  │              │ │        │
│   │  └───────┬───────┘  └───────┬───────┘  └──────┬──────┘ │        │
│   │          ↓                  ↓                  ↓        │        │
│   └──────────────────────────────────────────────────────────┘        │
│                                                                       │
│            ↓                                                          │
│   ┌─────────────────┐                                                │
│   │  Risk Scorer    │  → Weighted combination of all signals         │
│   │  ───────────    │     Heuristics: 40%                            │
│   │                 │     ML: 30%                                     │
│   │                 │     Brand: 20%                                  │
│   │                 │     TLD: 10%                                    │
│   └────────┬────────┘                                                │
│            ↓                                                          │
│   ┌─────────────────┐                                                │
│   │ Verdict Engine  │  → Score → Verdict mapping                     │
│   │ ──────────────  │     0-29: SAFE                                 │
│   │                 │     30-69: SUSPICIOUS                          │
│   │                 │     70-100: MALICIOUS                          │
│   └────────┬────────┘                                                │
│            ↓                                                          │
│   OUTPUT                                                              │
│   ──────                                                              │
│   UrlAssessment(                                                      │
│     score = 87,                                                       │
│     verdict = MALICIOUS,                                              │
│     flags = [BRAND_IMPERSONATION, SUSPICIOUS_TLD, ...],              │
│     confidence = HIGH                                                 │
│   )                                                                   │
│                                                                       │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 🔑 Key Files to Review

### Core Detection (Shared Kotlin)

| File | Lines | Purpose |
|------|-------|---------|
| `PhishingEngine.kt` | 349 | Main orchestrator, entry point `analyze(url)` |
| `HeuristicsEngine.kt` | 451 | 25+ security heuristics |
| `BrandDetector.kt` | 291 | Fuzzy brand matching (500+ brands) |
| `LogisticRegressionModel.kt` | 377 | ML-lite scoring |
| `TldScorer.kt` | 189 | TLD risk scoring |
| `HomographDetector.kt` | 156 | Unicode attack detection |

### Platform-Specific UI

| Platform | File | Purpose |
|----------|------|---------|
| Android | `ScannerScreen.kt` | Camera + results UI |
| iOS | `ScannerView.swift` | SwiftUI camera view |
| iOS | `ResultCard.swift` | Native result display |
| Desktop | `Main.kt` | Compose Desktop entry |
| Web | `Main.kt` | Kotlin/JS entry + HTML bridge |

### Tests

| File | Tests | Coverage |
|------|-------|----------|
| `PhishingEngineTest.kt` | 45 | Core engine |
| `HeuristicsEngineTest.kt` | 38 | Individual heuristics |
| `RealWorldPhishingTest.kt` | 15 | Defanged real URLs |
| `PerformanceBenchmarkTest.kt` | 6 | Speed assertions |

---

## 🔄 expect/actual Pattern

The KMP magic that enables code sharing:

```kotlin
// 📦 commonMain - Declaration (expect)
expect class QrScannerFactory {
    fun create(): QrScanner
}

// 🤖 androidMain - Implementation (actual)
actual class QrScannerFactory(private val context: Context) {
    actual fun create(): QrScanner = AndroidQrScanner(context)
}

// 🍎 iosMain - Implementation (actual)
actual class QrScannerFactory {
    actual fun create(): QrScanner = IosQrScanner()
}

// 🖥️ desktopMain - Implementation (actual)
actual class QrScannerFactory {
    actual fun create(): QrScanner = DesktopQrScanner()
}

// 🌐 jsMain - Implementation (actual)
actual class QrScannerFactory {
    actual fun create(): QrScanner = WebQrScanner()
}
```

**Used for:**
- `QrScannerFactory` — Platform camera APIs
- `DatabaseDriverFactory` — SQLDelight drivers
- `PlatformUtils` — Clipboard, sharing, haptics

---

## 📊 Code Distribution

```
                    SHARED vs PLATFORM-SPECIFIC
                    
    ████████████████████████████████░░░░░░░░  80% Shared
    
    Breakdown:
    ┌────────────────────────────────────────────────────────┐
    │ Detection Engine (commonMain)    │ ████████████ 7,400+ │
    │ Tests (commonTest)               │ ████████   5,200+   │
    │ Android UI (androidApp)          │ █████     3,100+    │
    │ iOS UI (iosApp)                  │ ██████    3,800+    │
    │ Desktop UI (desktopApp)          │ ██        1,200+    │
    │ Web UI (webApp)                  │ ██        1,400+    │
    └────────────────────────────────────────────────────────┘
```

---

## 🏃 Quick Navigation Commands

```bash
# Find all expect declarations
grep -r "expect class\|expect fun" common/src/commonMain/

# Find all actual implementations
grep -r "actual class\|actual fun" common/src/*/

# See all heuristic checks
grep -rn "// Check:" common/src/commonMain/

# Run specific test
./gradlew :common:allTests --tests "*PhishingEngine*"
```

---

## 🎯 Entry Points by Platform

| Platform | Entry File | Start Function |
|----------|------------|----------------|
| Android | `MainActivity.kt` | `onCreate()` |
| iOS | `MehrGuardApp.swift` | `@main App` |
| Desktop | `Main.kt` | `main()` |
| Web | `Main.kt` | `fun main()` |
| Tests | Any `*Test.kt` | `./gradlew :common:allTests` |

---

## 📈 Performance Hotspots

If optimizing, focus here:

1. **`BrandDetector.detect()`** — Levenshtein on 500+ brands
2. **`HeuristicsEngine.analyze()`** — 25+ regex checks
3. **`LogisticRegressionModel.predict()`** — Matrix multiplication

All are designed for <50ms total analysis time.

---

*Last updated: December 2025*
