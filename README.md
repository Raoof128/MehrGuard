# Mehr Guard

A QR code and URL phishing scanner written in Kotlin Multiplatform, with one shared detection engine for Android, iOS, desktop (JVM) and the web (JS and Wasm). "Mehr" (Persian: مهر) means trust.

It is a student project, built for the KotlinConf 2025–26 student contest. It is not a production security product.

## Summary

The shared engine scores a URL with 25 heuristics, a brand-lookalike check against 60 listed brands and a small scoring model whose weights are set by hand, and returns SAFE, SUSPICIOUS or MALICIOUS with the reasons.

**What is measured.** CI runs the engine over the committed dataset [`data/test_urls.csv`](data/test_urls.csv) on every push and prints the result ([`DatasetEvaluationTest`](common/src/desktopTest/kotlin/com/raouf/mehrguard/core/DatasetEvaluationTest.kt)):

| Measured in CI, 17 Nov 2026 | Result |
|---|---|
| Dataset | 100 URLs written by the author: 50 legitimate sites, 50 constructed phishing patterns |
| Phishing rows flagged | 50 of 50. Before a bug fix made the same day: 49 of 50 |
| Legitimate rows flagged | 0 of 50. Before the fix: 9 of 50, all as SUSPICIOUS |
| F1 | 100.0% after the fix, 90.7% before. See the limits below before quoting either |
| Latency, one URL | P99 under 50 ms, asserted by `PerformanceRegressionTest` on a GitHub-hosted runner (JVM). Not measured on a phone. |

**Read the table with these limits in mind.**

- The author wrote both the dataset and the rules. The table shows that the rules fire on the patterns they were written for. It is not a held-out benchmark, and no live phishing feed has been evaluated.
- Every row is now classified as labelled, which says less than it seems to. The last ten errors were fixed by looking at them: the engine took the last two labels of a host as the site, so every site under `.com.au`, `.co.uk` and similar suffixes was reported as a brand lookalike, including all four Australian banks in the set. Fixing that was right, but a score measured on the rows that prompted the fix is weak evidence about unseen sites, and the set now works only as a regression guard. [docs/EVALUATION.md](docs/EVALUATION.md) has the before and after.
- The scoring model is not trained. Its weights are constants set by hand ([docs/ML_TRAINING.md](docs/ML_TRAINING.md)).
- [docs/LIMITATIONS.md](docs/LIMITATIONS.md) lists what the engine misses and where it raises false alarms.

**Reproduce it.**

```bash
git clone https://github.com/Raoof128/MehrGuard.git
cd MehrGuard
./gradlew :common:desktopTest --tests "*DatasetEvaluationTest*" --info   # prints the table above
```

This needs JDK 17 and the Android SDK, because the shared module also builds an Android target. The same output is in the "Measure Dataset Metrics" step of every [CI run](https://github.com/Raoof128/MehrGuard/actions/workflows/ci.yml).

## Network use

Analysing a URL makes no network request, and a scanned URL is never sent anywhere. The analysis code has no HTTP client, and a test scans its source for networking APIs ([`AnalysisCodeHasNoNetworkApiTest`](common/src/desktopTest/kotlin/com/raouf/mehrguard/core/AnalysisCodeHasNoNetworkApiTest.kt)). That test is a text scan, not a network monitor.

The Android app does make one request of its own. At startup it checks GitHub Pages for updated detection rules, which is why it holds the `INTERNET` permission. A downloaded update is cached only if it matches a SHA-256 published in the manifest. That detects corruption but not a compromised origin, and signed manifests are not implemented. As of 17 Nov 2026 the update address returns 404, so the check does nothing.

## How it works

1. **Heuristics** ([`HeuristicsEngine.kt`](common/src/commonMain/kotlin/com/raouf/mehrguard/engine/HeuristicsEngine.kt)): 25 weighted checks, among them plain HTTP, an IP address as host, a URL shortener, an `@` in the URL, a risky TLD, mixed scripts, punycode, credential keywords and embedded redirects. [docs/HEURISTICS.md](docs/HEURISTICS.md) describes them.
2. **Brand lookalikes** ([`BrandDetector.kt`](common/src/commonMain/kotlin/com/raouf/mehrguard/engine/BrandDetector.kt)): 60 brands with 484 listed misspellings, homographs and brand-plus-keyword names, plus the brand name used as a subdomain or site name on a domain the brand does not own, plus an edit-distance check.
3. **Scoring model** ([`EnsembleModel.kt`](common/src/commonMain/kotlin/com/raouf/mehrguard/ml/EnsembleModel.kt)): a logistic score over fifteen URL features blended with fifteen threshold rules at fixed weights (0.40 / 0.35 / 0.25). Nothing in it is learned from data.
4. **Verdict** ([`ScoreCalculator.kt`](common/src/commonMain/kotlin/com/raouf/mehrguard/core/ScoreCalculator.kt)): a brand match or a risky TLD escalates on its own. Otherwise, in most cases, the four components vote.

All of this is in `common/src/commonMain/kotlin/`. The platform modules add camera scanning and the user interface.

| Module | What it is |
|---|---|
| `common/` | Shared engine, model, data and tests |
| `androidApp/` | Compose UI, CameraX, ML Kit barcode scanning |
| `iosApp/` | SwiftUI app that calls the shared Kotlin framework |
| `desktopApp/` | Compose Desktop app, ZXing for images |
| `webApp/` | Kotlin/JS and Kotlin/Wasm builds with an HTML front end, jsQR for scanning |
| `judge/` | Check scripts (see below) |
| `data/` | The evaluation dataset and the rule-update manifest |
| `docs/` | Design notes, limits and the contest submission |

## Build and test

Requirements: JDK 17 or later, the Android SDK for the Android and shared builds, and Xcode 15 or later for iOS.

```bash
./gradlew :common:desktopTest                 # shared tests on the JVM
./gradlew :androidApp:installDebug            # Android, to a connected device or emulator
./gradlew :desktopApp:run                     # desktop
./gradlew :webApp:jsBrowserDevelopmentRun     # web (Kotlin/JS) at http://localhost:8080
./gradlew :webApp:wasmJsBrowserDevelopmentRun # web (Kotlin/Wasm)

# iOS: build the shared framework, then open the Xcode project
./gradlew :common:linkDebugFrameworkIosSimulatorArm64
open iosApp/MehrGuard.xcodeproj
```

The Kotlin sources hold 1,469 `@Test` functions, 1,407 of them in the shared module (counted with `grep` on 17 Nov 2026). The iOS app has its own XCTest targets.

CI builds every target and runs the shared tests, detekt, and dependency and security scans on each push: [workflow](.github/workflows/ci.yml).

## Check scripts

Each script runs tests and fails if they fail. None of them prints a fixed figure.

| Script | What it runs | What it does not show |
|---|---|---|
| `./judge/verify_accuracy.sh` | `AccuracyVerificationTest` and `DatasetEvaluationTest`, then prints the measured table | Anything about URLs outside the two author-written sets |
| `./judge/verify_offline.sh` | The source scan for networking APIs and a timing sanity check | Network traffic: no monitor is involved |
| `./judge/verify_performance.sh` | The latency assertions on this machine's JVM | Latency on phones or in browsers |
| `./judge/verify_parity.sh` | The parity tests on the JVM (and on the iOS simulator, on macOS), and that the Android, JS and Wasm targets build | Verdicts computed on Android, JS or Wasm |
| `./judge/verify_all.sh` | All four | |

## Red-team mode

A developer mode feeds 18 fixed URLs to the engine without a camera: three homograph hosts, three obfuscated IP addresses, three risky TLDs, two nested redirects, three brand lookalikes, two shorteners and two safe controls. The list is [`RedTeamScenarios.kt`](common/src/commonMain/kotlin/com/raouf/mehrguard/redteam/RedTeamScenarios.kt) in the shared module, which Android and desktop use directly. iOS and the web front end each keep a copy with the same 18 ids (`MockTypes.swift`, `scanner.js`).

To turn it on: on Android and iOS, tap the version number in Settings seven times. On desktop, use the "Judge Mode" toggle in the header. On the web, enable "Red Team Scenarios" under Settings.

## Hosted demo and contest submission

The web demo at [raoof128.github.io](https://raoof128.github.io) and the Android APK under [Raoof128.github.io releases](https://github.com/Raoof128/Raoof128.github.io/releases/tag/v2.0.36-submission) are the contest submission as it was frozen in January 2026. They do not contain the fixes described here, and the demo's interface still shows a "<5ms" figure that was never measured.

The contest material is kept under [docs/contest/](docs/contest/README.md): the essay, the judge guides, the pitch, the [demo video](https://youtu.be/n8bheouj4jM) script and the submission checklist. Those documents quote figures that a review in November 2026 could not reproduce. The index page there lists each one with its correction.

## How this was built

The project was written in December 2025 and January 2026 with heavy use of AI coding assistants. [AGENT.md](AGENT.md) is their working log and [CHANGELOG.md](CHANGELOG.md) records the changes. In November 2026 the author audited the repository against its own documentation. The audit replaced asserted accuracy figures with measured ones, removed descriptions of model training that never happened, corrected the network and latency statements, and fixed the false alarms described above. The entries dated 2026-11-17 in the changelog give the details.

## Documentation

| Document | What it covers |
|---|---|
| [docs/EVALUATION.md](docs/EVALUATION.md) | The measurements, how to reproduce them, and what they do not show |
| [docs/LIMITATIONS.md](docs/LIMITATIONS.md) | Known gaps and false alarms |
| [docs/ML_MODEL.md](docs/ML_MODEL.md), [docs/ML_TRAINING.md](docs/ML_TRAINING.md) | The scoring model, and why it is hand-set and not trained |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Module layout and data flow |
| [docs/HEURISTICS.md](docs/HEURISTICS.md) | The heuristic checks |
| [docs/THREAT_MODEL.md](docs/THREAT_MODEL.md), [SECURITY_MODEL.md](SECURITY_MODEL.md) | What the scanner is meant to catch and what it is not |
| [PRIVACY.md](PRIVACY.md) | Data handling and the one network request |
| [docs/SHARED_CODE_REPORT.md](docs/SHARED_CODE_REPORT.md) | Lines of code per module |

## Licence

Apache License 2.0. See [LICENSE](LICENSE).

## Author

Mohammad Raouf Abedini, Macquarie University, Sydney. GitHub: [@Raoof128](https://github.com/Raoof128).
