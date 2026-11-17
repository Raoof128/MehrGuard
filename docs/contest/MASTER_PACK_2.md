# Mehr Guard Master Pack - Part 2

> **Contest-era document.** Written for the KotlinConf 2025–26 student contest submission and not re-verified since. An audit in November 2026 could not reproduce some figures found in documents of this period (accuracy, latency, corpus and training-set sizes, "zero network calls"). The measured figures are in the [README summary](../README.md#summary) and [EVALUATION.md](../docs/EVALUATION.md).

---

# 5. FULL GIT REPO STRUCTURE

```
mehrguard/
├── README.md
├── LICENSE
├── CONTRIBUTING.md
├── SECURITY.md
├── .gitignore
├── .editorconfig
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/
│   └── libs.versions.toml
│
├── common/
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/
│       │   └── kotlin/com/mehrguard/
│       │       ├── core/
│       │       │   ├── PhishingEngine.kt
│       │       │   ├── UrlAnalyzer.kt
│       │       │   ├── RiskScorer.kt
│       │       │   └── VerdictEngine.kt
│       │       ├── engine/
│       │       │   ├── HeuristicsEngine.kt
│       │       │   ├── BrandDetector.kt
│       │       │   ├── TldScorer.kt
│       │       │   └── HomographDetector.kt
│       │       ├── model/
│       │       │   ├── ScanResult.kt
│       │       │   ├── RiskAssessment.kt
│       │       │   ├── UrlFeatures.kt
│       │       │   └── Verdict.kt
│       │       ├── scanner/
│       │       │   └── QrScanner.kt
│       │       ├── ml/
│       │       │   ├── LogisticRegressionModel.kt
│       │       │   └── FeatureExtractor.kt
│       │       ├── repository/
│       │       │   ├── HistoryRepository.kt
│       │       │   └── ScanHistoryItem.kt
│       │       ├── ui/
│       │       │   ├── SharedViewModel.kt
│       │       │   ├── UiState.kt
│       │       │   └── theme/
│       │       │       ├── Colors.kt
│       │       │       ├── Typography.kt
│       │       │       └── Theme.kt
│       │       └── utils/
│       │           ├── UrlParser.kt
│       │           ├── EntropyCalculator.kt
│       │           └── Constants.kt
│       ├── androidMain/
│       │   └── kotlin/com/mehrguard/
│       │       ├── scanner/
│       │       │   └── AndroidQrScanner.kt
│       │       └── repository/
│       │           └── AndroidDatabaseDriver.kt
│       ├── iosMain/
│       │   └── kotlin/com/mehrguard/
│       │       ├── scanner/
│       │       │   └── IosQrScanner.kt
│       │       └── repository/
│       │           └── IosDatabaseDriver.kt
│       ├── desktopMain/
│       │   └── kotlin/com/mehrguard/
│       │       ├── scanner/
│       │       │   └── DesktopQrScanner.kt
│       │       └── repository/
│       │           └── DesktopDatabaseDriver.kt
│       ├── webMain/
│       │   └── kotlin/com/mehrguard/
│       │       ├── scanner/
│       │       │   └── WebQrScanner.kt
│       │       └── repository/
│       │           └── WebDatabaseDriver.kt
│       └── commonTest/
│           └── kotlin/com/mehrguard/
│               ├── PhishingEngineTest.kt
│               ├── UrlAnalyzerTest.kt
│               ├── BrandDetectorTest.kt
│               └── RiskScorerTest.kt
│
├── androidApp/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── kotlin/com/mehrguard/android/
│       │   ├── MainActivity.kt
│       │   ├── MehrGuardApp.kt
│       │   └── ui/
│       │       ├── HomeScreen.kt
│       │       ├── ScanScreen.kt
│       │       ├── ResultScreen.kt
│       │       └── HistoryScreen.kt
│       └── res/
│           ├── values/
│           │   ├── strings.xml
│           │   ├── colors.xml
│           │   └── themes.xml
│           └── drawable/
│               └── ic_launcher.xml
│
├── iosApp/
│   ├── MehrGuard.xcodeproj/
│   ├── MehrGuard/
│   │   ├── AppDelegate.swift
│   │   ├── ContentView.swift
│   │   └── Info.plist
│   └── Podfile
│
├── desktopApp/
│   ├── build.gradle.kts
│   └── src/main/kotlin/com/mehrguard/desktop/
│       ├── Main.kt
│       └── DesktopApp.kt
│
├── webApp/
│   ├── build.gradle.kts
│   ├── webpack.config.d/
│   └── src/main/kotlin/com/mehrguard/web/
│       ├── Main.kt
│       └── WebApp.kt
│
├── models/
│   ├── phishing_model_weights.json
│   └── brand_database.json
│
├── docs/
│   ├── MASTER_PACK.md
│   ├── ARCHITECTURE.md
│   ├── UI_DESIGN_SYSTEM.md
│   ├── THREAT_MODEL.md
│   ├── COMPLIANCE.md
│   ├── DEMO_SCRIPT.md
│   ├── PITCH.md
│   └── screenshots/
│       ├── home.png
│       ├── scanning.png
│       ├── safe.png
│       ├── warning.png
│       ├── malicious.png
│       └── history.png
│
├── scripts/
│   ├── setup.sh
│   ├── build_all.sh
│   ├── run_tests.sh
│   └── generate_model.py
│
└── tests/
    ├── unit/
    │   ├── test_payloads.json
    │   └── expected_results.json
    └── integration/
        └── e2e_test_plan.md
```

---

# 6. UI/UX DESIGN SYSTEM

## Typography

```kotlin
object MehrGuardTypography {
    val displayLarge = TextStyle(
        fontFamily = FontFamily("Inter"),
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    )
    val headlineMedium = TextStyle(
        fontFamily = FontFamily("Inter"),
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp
    )
    val bodyLarge = TextStyle(
        fontFamily = FontFamily("Inter"),
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    )
    val labelMedium = TextStyle(
        fontFamily = FontFamily("Inter"),
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    )
}
```

## Color Palette

```kotlin
object MehrGuardColors {
    // Primary
    val primary = Color(0xFF6C5CE7)      // Deep Purple
    val primaryVariant = Color(0xFF5B4DCF)
    
    // Background
    val backgroundDark = Color(0xFF0D1117)
    val backgroundLight = Color(0xFFF6F8FA)
    val surfaceDark = Color(0xFF161B22)
    val surfaceLight = Color(0xFFFFFFFF)
    
    // Semantic - Verdicts
    val safe = Color(0xFF00D68F)          // Emerald Green
    val safeBackground = Color(0x1A00D68F)
    val warning = Color(0xFFFFAA00)       // Amber
    val warningBackground = Color(0x1AFFAA00)
    val danger = Color(0xFFFF3D71)        // Coral Red
    val dangerBackground = Color(0x1AFF3D71)
    
    // Text
    val textPrimaryDark = Color(0xFFF0F6FC)
    val textSecondaryDark = Color(0xFF8B949E)
    val textPrimaryLight = Color(0xFF24292F)
    val textSecondaryLight = Color(0xFF57606A)
}
```

## Animation Ideas

1. **Scan Animation**: Pulsing ring around camera viewfinder
2. **Result Reveal**: Risk score counts up from 0 with color transition
3. **Verdict Badge**: Bouncy entrance animation
4. **History Items**: Staggered fade-in on list load
5. **Navigation**: Shared element transitions between screens

## Screen Layouts

### Home Screen
```
┌────────────────────────────────┐
│  ┌──────────────────────────┐  │
│  │       🛡️ Mehr Guard      │  │
│  │    Protect Your Scans    │  │
│  └──────────────────────────┘  │
│                                │
│  ┌──────────────────────────┐  │
│  │                          │  │
│  │    [Camera Viewfinder]   │  │
│  │                          │  │
│  │    Point at QR code      │  │
│  │                          │  │
│  └──────────────────────────┘  │
│                                │
│  ┌────────┐  ┌────────────────┐│
│  │Gallery │  │ Paste from     ││
│  │Import  │  │ Clipboard      ││
│  └────────┘  └────────────────┘│
│                                │
│  ┌──────────────────────────┐  │
│  │ Recent Scans             │  │
│  │ ├─ google.com ✅         │  │
│  │ ├─ bit.ly/xxx ⚠️        │  │
│  │ └─ paypa1.com ❌         │  │
│  └──────────────────────────┘  │
│                                │
│  [🏠]     [📷]     [📋]        │
└────────────────────────────────┘
```

### Result Screen
```
┌────────────────────────────────┐
│  ← Back                        │
│                                │
│  ┌──────────────────────────┐  │
│  │                          │  │
│  │         [72]             │  │
│  │      SUSPICIOUS          │  │
│  │         ⚠️               │  │
│  │                          │  │
│  └──────────────────────────┘  │
│                                │
│  URL Analyzed:                 │
│  secure-paypa1.com/login       │
│                                │
│  Risk Factors:                 │
│  ┌──────────────────────────┐  │
│  │ ⚠️ Brand Impersonation   │  │
│  │    Looks like: PayPal    │  │
│  │                          │  │
│  │ ⚠️ Suspicious TLD        │  │
│  │    .com with IP prefix   │  │
│  │                          │  │
│  │ ⚠️ Login Page Detected   │  │
│  │    Contains /login path  │  │
│  └──────────────────────────┘  │
│                                │
│  [ ⚔️ Proceed Anyway ]        │
│  [ ✅ Stay Safe (Close) ]     │
│                                │
└────────────────────────────────┘
```

---

# 7. COMPETITION PITCH MATERIAL

## 30–45 Second Pitch

> **"Every day, millions of people scan QR codes without thinking twice. Attackers know this. QRishing attacks—phishing via QR codes—have exploded 587 since 2023.**
>
> **Mehr Guard is the first Kotlin Multiplatform solution that protects users everywhere—Android, iOS, Desktop, and Web—with a single codebase.**
>
> **Our intelligent engine combines 25+ cybersecurity heuristics, ML-powered scoring, and brand impersonation detection to deliver instant verdicts: Safe, Suspicious, or Malicious.**
>
> **Zero cloud dependencies. Zero data collection. 100% privacy-first.**
>
> **Mehr Guard: Scan smart. Stay protected."**

## Differentiators vs Other QR Scanners

| Feature | Mehr Guard | Generic Scanners | Enterprise Solutions |
|---------|-----------|------------------|---------------------|
| Cross-platform | ✅ All 4 | ❌ 1-2 | ⚠️ Some |
| Offline analysis | ✅ Yes | ❌ No | ⚠️ Partial |
| ML phishing detection | ✅ Yes | ❌ No | ⚠️ Cloud-only |
| Brand impersonation | ✅ Yes | ❌ No | ✅ Yes |
| Privacy-first | ✅ Zero telemetry | ❌ Ads/tracking | ⚠️ Enterprise data |
| Open source | ✅ Yes | ❌ No | ❌ No |
| KMP shared code | ✅ 80%+ | N/A | ❌ Native each |
