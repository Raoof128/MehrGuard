<p align="center">
  <img src="MehrGuard.iconset/icon_256x256@2x.png">
</p>

# Mehr Guard

> **Offline QR & Link Security for the Privacy-Conscious**
>
> **"Mehr"** (Persian: مهر) means *trust, covenant, light* — the foundation of secure scanning.

<p align="center">
  <a href="https://youtu.be/n8bheouj4jM">
    <img src="https://img.shields.io/badge/🎬_Demo_Video-Watch_Now-red?style=for-the-badge&logo=youtube" alt="Demo Video">
  </a>
</p>

<p align="center">
  <b>📺 Watch the Demo:</b> <a href="https://youtu.be/n8bheouj4jM">https://youtu.be/n8bheouj4jM</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-2.3.0-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/KMP_Targets-5-orange" alt="5 Platforms">
  <img src="https://img.shields.io/badge/Analysis-on--device-brightgreen" alt="On-device analysis">
  <img src="https://img.shields.io/badge/Tests-1,248+-brightgreen" alt="Tests">
  <img src="https://img.shields.io/badge/F1-90.7%25_on_100_author--written_URLs-blue" alt="F1 90.7% on 100 author-written URLs">
  <img src="https://img.shields.io/badge/License-Apache_2.0-purple" alt="License">
</p>

---

## Summary

Mehr Guard is a QR code and URL phishing scanner written in Kotlin Multiplatform for Android, iOS, desktop (JVM) and the web (JS and Wasm). One shared engine scores a URL with 25 heuristics and a small on-device model, and returns SAFE, SUSPICIOUS or MALICIOUS with the reasons.

**What is measured, and how to check it.** CI runs the engine over the committed dataset [`data/test_urls.csv`](data/test_urls.csv) and prints the result ([`DatasetEvaluationTest`](common/src/desktopTest/kotlin/com/raouf/mehrguard/core/DatasetEvaluationTest.kt)):

| Measured on 17 Nov 2026 | Result |
|---|---|
| Dataset | 100 URLs: 50 legitimate sites, 50 constructed phishing patterns |
| Phishing detected | 49 of 50 (recall 98.0%) |
| Legitimate sites flagged | 9 of 50, all as SUSPICIOUS (precision 84.5%) |
| F1 / accuracy | 90.7% / 90.0% |

**What this does not show.** The author wrote both the dataset and the rules, so this checks that the rules fire on the patterns they were written for. It is not a held-out benchmark, and no live phishing feed has been evaluated. The nine false alarms include all four Australian bank sites in the set, which matters for a scanner aimed at Australian users. The one miss is a punycode domain. The model's weights were fitted to synthetic URLs.

**Network use.** URL analysis runs on the device and scanned URLs are not sent anywhere. The Android app does make one kind of request: at startup it checks GitHub Pages for updated detection rules, which is why it holds the `INTERNET` permission. A downloaded update is applied only if it matches a SHA-256 published in the manifest, which detects corruption but not a compromised origin; signed manifests are not implemented. As of 17 Nov 2026 the update endpoint returns 404, so the check has no effect. See [Limitations](docs/LIMITATIONS.md).

**About the rest of this repository.** Mehr Guard was built for the KotlinConf 2025–26 student contest, and much of the documentation below and under `docs/` was written for that submission in December 2025. Where an older document quotes a figure that differs from this section, this section and the CI output are the ones to trust.

```bash
./gradlew :common:desktopTest --tests "*DatasetEvaluationTest*" --info   # prints the table above
```

---

## 🏆 KotlinConf 2025-2026 Contest Submission

| Item | Link |
|------|------|
| **📝 Essay (300 words)** | [ESSAY.md](ESSAY.md) |
| **🎬 Demo Video** | [YouTube](https://youtu.be/n8bheouj4jM) |
| **📋 Submission Checklist** | [SUBMISSION_CHECKLIST.md](SUBMISSION_CHECKLIST.md) |
| **🔍 Judge Quickstart** | [JUDGE_QUICKSTART.md](JUDGE_QUICKSTART.md) |
| **📅 Contest Timeline** | [CONTEST_START.md](CONTEST_START.md) |

---

## ⚡ 60-Second Judge Path

| Step | Action | Time |
|------|--------|------|
| **1** | 📱 [Download APK](https://github.com/Raoof128/Raoof128.github.io/releases/tag/v2.0.36-submission) | 10s |
| **2** | 🌐 [Open Web Demo](https://raoof128.github.io) — paste `paypa1-secure.tk` | 20s |
| **3** | ✅ Run `./judge/verify_accuracy.sh` (prints the measured accuracy table) | 60s |

> 📋 **Full Guide:** [JUDGE_QUICKSTART.md](JUDGE_QUICKSTART.md) • 📝 **Essay:** [ESSAY.md](ESSAY.md)

---

## What Is Mehr Guard?

Mehr Guard is a **privacy-first QR code and URL security scanner** built with Kotlin Multiplatform. It detects phishing attacks, brand impersonation, and malicious redirects **on the device**: no cloud API sees a scanned URL and nothing is collected. The Android app's only network request is a startup check for updated detection rules.

**Core Value Proposition:**
- **On-device analysis** — Your scanned URLs never leave your device
- **5 Platform Targets** — Android, iOS, Desktop (JVM), Web (JS), Web (Wasm)
- **90.7% F1 on the committed 100-URL set** — 25 heuristics plus a small model; author-written URLs, not a held-out benchmark
- **Fast enough for live scanning** — CI asserts a 99th-percentile analysis time under 50 ms per URL

---

## Quick Start

### 🌐 Try Instantly (No Build Required)

**Live Web Demo:** [raoof128.github.io](https://raoof128.github.io)

Paste any URL to test detection:
| URL | Expected Verdict |
|-----|------------------|
| `https://google.com` | ✅ SAFE |
| `https://paypa1-secure.tk/login` | 🔴 MALICIOUS — Brand impersonation |
| `https://gооgle.com` (Cyrillic о) | 🔴 MALICIOUS — Homograph attack |

### 📱 Pre-Built Android APK

**Download:** [MehrGuard-2.0.36-debug.apk](https://github.com/Raoof128/Raoof128.github.io/releases/tag/v2.0.36-submission)

Install on any Android 8+ device. No build required.

> **Note:** Debug-signed APK (suitable for judge evaluation). A release-signed APK is not required for evaluation.

---

## Build From Source

### Prerequisites

| Requirement | Version | Notes |
|-------------|---------|-------|
| **JDK** | 17+ | Temurin or Corretto recommended |
| **Gradle** | 8.13 (bundled) | Uses wrapper |
| **Android Studio** | Latest | For Android builds |
| **Xcode** | 15+ | For iOS builds (macOS only) |

### Clone Repository

```bash
git clone https://github.com/Raoof128/MehrGuard.git
cd MehrGuard
```

### Platform Build Commands

#### Android

```bash
# Debug build (installs to connected device/emulator)
./gradlew :androidApp:installDebug

# Release APK
./gradlew :androidApp:assembleRelease
# Output: androidApp/build/outputs/apk/release/androidApp-release.apk
```

**Note:** Release builds require signing. Copy `keystore.properties.template` to `keystore.properties` and configure your keystore, or the build will use debug signing.

#### Desktop (JVM)

```bash
# Run directly
./gradlew :desktopApp:run

# Package for distribution
./gradlew :desktopApp:packageDmg        # macOS
./gradlew :desktopApp:packageMsi        # Windows
./gradlew :desktopApp:packageDeb        # Linux
```

#### iOS (macOS only)

```bash
# Step 1: Build KMP framework
./gradlew :common:linkDebugFrameworkIosSimulatorArm64

# Step 2: Open Xcode project
open iosApp/MehrGuard.xcodeproj

# Step 3: In Xcode
#   - Select iPhone 16 Pro simulator (or any iOS 17+ simulator)
#   - Press ⌘+R to build and run
```

**First-time setup:** Link the framework in Xcode:
1. Select MehrGuard target → General tab
2. Scroll to "Frameworks, Libraries, and Embedded Content"
3. Add `common/build/bin/iosSimulatorArm64/debugFramework/common.framework`
4. Set Embed to "Embed & Sign"

#### Web (Kotlin/JS)

```bash
# Development server with hot reload
./gradlew :webApp:jsBrowserDevelopmentRun
# Opens http://localhost:8080

# Production build
./gradlew :webApp:jsBrowserProductionWebpack
# Output: webApp/build/dist/js/productionExecutable/
```

#### Web (Kotlin/Wasm)

```bash
# Development server
./gradlew :webApp:wasmJsBrowserDevelopmentRun

# Production build
./gradlew :webApp:wasmJsBrowserProductionWebpack
```

### Run Tests

```bash
# All shared tests
./gradlew :common:allTests

# Platform-specific
./gradlew :common:desktopTest          # JVM tests
./gradlew :androidApp:testDebugUnitTest # Android unit tests

# Coverage report
./gradlew :common:koverXmlReport
# Output: common/build/reports/kover/xml/report.xml
```

---

## Architecture

### 5-Target Kotlin Multiplatform Structure

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Application Layer                             │
├──────────────┬──────────────┬──────────────┬──────────────┬─────────┤
│  androidApp  │   iosApp     │  desktopApp  │   webApp     │  webApp │
│  Compose UI  │  SwiftUI +   │ Compose      │  Kotlin/JS   │ Kotlin/ │
│  CameraX     │  KMP Bridge  │ Desktop      │  Browser     │ Wasm    │
│  ML Kit      │  Vision      │ ZXing        │  jsQR        │ jsQR    │
└──────┬───────┴──────┬───────┴──────┬───────┴──────┬───────┴────┬────┘
       │              │              │              │            │
       └──────────────┴──────────────┴──────────────┴────────────┘
                                     │
                                     ▼
              ┌───────────────────────────────────────────────┐
              │           common module (100% shared)         │
              │  • PhishingEngine      (orchestration)        │
              │  • EnsembleModel       (3-model ML)           │
              │  • HeuristicsEngine    (25+ rules)            │
              │  • BrandDetector       (60+ brands)           │
              │  • FeatureExtractor    (URL features)         │
              │  • SQLDelight          (scan history)         │
              └───────────────────────────────────────────────┘
```

All security-critical logic lives in `common/src/commonMain/kotlin/`. Platform modules only implement camera/QR scanning and native UI.

---

## Detection Engine

### How It Works

1. **Heuristics Engine** (25+ checks)
   - Homograph detection (Cyrillic, Greek lookalikes)
   - Brand impersonation (typosquatting, fuzzy matching)
   - Suspicious TLDs (.tk, .ml, .ga, .cf)
   - IP obfuscation (decimal, hex, octal)
   - @ symbol injection, URL shorteners, credential paths

2. **Ensemble ML Model**
   - A logistic score over fifteen URL features, with hand-set weights
   - Fifteen hand-written threshold rules
   - Blended as a fixed weighted average (0.40 / 0.35 / 0.25); nothing is learned from data

3. **Verdict Determination**
   - Each component votes: SAFE / SUSPICIOUS / MALICIOUS
   - 3+ SAFE votes → SAFE
   - 2+ MALICIOUS votes → MALICIOUS
   - Critical escalations override (homograph, @ symbol)

### Performance

| Metric | Value |
|--------|-------|
| P99 latency, one URL | under 50 ms, asserted in CI (`PerformanceRegressionTest`) on GitHub-hosted runners |
| F1 on `data/test_urls.csv` | 90.7% (precision 84.5%, recall 98.0%) |
| False alarms on the set's 50 legitimate sites | 9 (18%), all SUSPICIOUS |

Latency on phones has not been benchmarked in this repository.

---

## Red Team Mode & Judge Testing

Red Team Mode is a **hidden developer feature** that exposes curated attack scenarios for testing the detection engine. This allows judges and developers to instantly verify detection accuracy without needing to print QR codes.

### How It Works (Technical Architecture)

```
┌─────────────────────────────────────────────────────────────────────┐
│                    RED TEAM MODE ARCHITECTURE                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │              SHARED MODULE (common/redteam/)                  │  │
│  │                                                               │  │
│  │  RedTeamScenarios.kt                                          │  │
│  │  ├── 18 curated attack scenarios (object SCENARIOS)          │  │
│  │  ├── Scenario data class (id, category, url, expectedScore)  │  │
│  │  ├── Categories: Homograph, IP Obfuscation, TLD, Brand, etc. │  │
│  │  └── Utility: groupedByCategory(), getById()                 │  │
│  │                                                               │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                              │                                       │
│              ┌───────────────┼───────────────┐                      │
│              ▼               ▼               ▼                      │
│  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐           │
│  │   Android     │  │     iOS       │  │   Desktop     │           │
│  │ RedTeamPanel  │  │ RedTeamPanel  │  │ RedTeamChips  │           │
│  │ (Compose)     │  │ (SwiftUI)     │  │ (Compose)     │           │
│  └───────────────┘  └───────────────┘  └───────────────┘           │
│                                                                      │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │   Web (JavaScript array mirrors Kotlin RedTeamScenarios)      │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Cross-Platform Parity

All 5 platforms share the **exact same scenarios** with identical IDs, URLs, and expected scores:

| Source File | Platform | Implementation |
|-------------|----------|----------------|
| `common/.../redteam/RedTeamScenarios.kt` | Android, Desktop | Direct Kotlin import |
| `iosApp/.../MockTypes.swift` | iOS | Swift enum mirroring Kotlin |
| `webApp/.../scanner.js` | Web (JS/Wasm) | JavaScript array mirror |

### How to Enable Red Team Mode

| Platform | Activation Method | UI Location |
|----------|-------------------|-------------|
| **Android** | Settings → Tap version number **7 times** → Developer Mode unlocks | Red Team panel appears at top of Scanner screen |
| **iOS** | Settings → Tap version number **7 times** → Developer Mode unlocks | Red Team scenarios panel in Scanner view |
| **Desktop** | Click "🕵️ Judge Mode" toggle in header bar | Horizontal scrollable chip bar in Scanner |
| **Web** | Settings → Security Settings → Toggle "Enable Red Team Scenarios" | Chip grid appears above scanner |

### Attack Scenario Categories

| Category | Count | Example | Detection Target |
|----------|-------|---------|------------------|
| **Homograph Attack** | 3 | `https://аpple.com` (Cyrillic 'а') | Mixed Unicode scripts |
| **IP Obfuscation** | 3 | `http://3232235777/malware` | Decimal/Hex/Octal IP |
| **Suspicious TLD** | 3 | `https://paypa1-secure.tk` | Free/abused TLDs |
| **Nested Redirect** | 2 | `https://legit.com?url=https://phishing.tk` | URL-in-URL patterns |
| **Brand Impersonation** | 3 | `https://paypa1.com` | Typosquatting |
| **URL Shortener** | 2 | `https://bit.ly/xyz` | Destination hiding |
| **Safe Control** | 2 | `https://google.com` | Baseline verification |

### Code Flow (One-Click Testing)

```kotlin
// 1. User taps a Red Team scenario chip
val scenario = RedTeamScenarios.getById("HG-001")  // Cyrillic Apple

// 2. URL is fed directly to PhishingEngine (bypasses camera)
val result = phishingEngine.analyze(scenario.maliciousUrl)

// 3. Result displayed with full breakdown
// - Expected: score 70-100, verdict MALICIOUS
// - Signals: MIXED_SCRIPTS, BRAND_IMPERSONATION, HOMOGRAPH_DETECTED
```

### Verification Commands

```bash
# Run all judge verification tests
./judge/verify_all.sh

# Individual verifications:
./judge/verify_offline.sh      # Timing sanity check; the engine has no HTTP client
./judge/verify_performance.sh  # Latency assertions
./judge/verify_accuracy.sh     # Measured precision, recall and F1 on the committed dataset
./judge/verify_parity.sh       # The parity tests on the JVM, and that each other target builds
```

---

## Privacy Guarantee

The detection engine makes **no network calls**: analysing a URL never contacts a server. The evidence is the code: the analysis module has no HTTP client. The script below is a timing sanity check, not a network monitor:

```bash
./judge/verify_offline.sh
```

The detection engine has no HTTP client dependencies. Separately from analysis, the Android app checks GitHub Pages at startup for updated detection rules; no scanned URL or other user data is part of that request.

---

## Internationalization

18 languages supported across all platforms:

English, German, French, Spanish, Italian, Portuguese, Russian, Chinese, Japanese, Korean, Hindi, Arabic, Thai, Vietnamese, Turkish, Indonesian, Hebrew, Persian

---

## Project Structure

```
├── common/                 # Shared KMP module (detection engine, ML, data)
│   ├── src/commonMain/     # Cross-platform Kotlin
│   ├── src/androidMain/    # Android-specific (SQLite driver)
│   ├── src/iosMain/        # iOS-specific (SQLite driver)
│   ├── src/desktopMain/    # Desktop-specific (SQLite driver)
│   ├── src/jsMain/         # JS-specific (IndexedDB driver)
│   └── src/commonTest/     # Shared tests (1,248+)
├── androidApp/             # Android application (Compose)
├── iosApp/                 # iOS application (SwiftUI + KMP)
├── desktopApp/             # Desktop application (Compose Desktop)
├── webApp/                 # Web application (Kotlin/JS + Wasm)
├── judge/                  # Verification scripts for claims
└── docs/                   # Technical documentation
```

---

## Verification Scripts

All claims are backed by reproducible evidence:

```bash
./judge/verify_all.sh        # Run all verifications
./judge/verify_offline.sh    # Timing sanity check; the engine has no HTTP client
./judge/verify_performance.sh # Latency assertions
./judge/verify_accuracy.sh   # Measured precision, recall and F1
./judge/verify_parity.sh     # Cross-platform parity
```

---

## Documentation

| Document | Purpose |
|----------|---------|
| [JUDGE_QUICKSTART.md](JUDGE_QUICKSTART.md) | 5-minute verification guide |
| [ESSAY.md](ESSAY.md) | Competition essay (300 words) |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | System design |
| [docs/SHARED_CODE_REPORT.md](docs/SHARED_CODE_REPORT.md) | KMP code sharing breakdown |
| [docs/ML_MODEL.md](docs/ML_MODEL.md) | The scoring model, and why it is hand-set and not trained |

---

## License

```
Copyright 2025-2026 Mehr Guard Contributors
Licensed under the Apache License, Version 2.0
```

See [LICENSE](LICENSE) for full text.

---

## Contact

**Author:** Mohammad Raouf Abedini  
**Email:** raoof.r12@gmail.com | mohammadraouf.abedini@students.mq.edu.au  
**University:** Macquarie University, Sydney, Australia  
**GitHub:** [@Raoof128](https://github.com/Raoof128)

---

<p align="center">
  <b>🛡️ Mehr Guard</b><br>
  <i>Scan smart. Stay protected.</i>
</p>
