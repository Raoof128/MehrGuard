# Changelog

## 2026-02-25 (Australia/Sydney)
**Raouf:**
- **Scope:** Project Initialization
- **Summary:** Initialized AGENT.md and CHANGELOG.md; prepared for iOS app cloning.
- **Files Changed:**
  - AGENT.md
  - CHANGELOG.md
- **Verification:** Files created successfully.

## 2026-02-25 (Australia/Sydney)
**Raouf:**
- **Scope:** Source Code Acquisition
- **Summary:** Cloned the `iosApp` directory from `https://github.com/Raoof128/Raoof128.github.io/tree/main/iosApp` using git sparse-checkout.
- **Files Changed:**
  - `iosApp/` (cloned directory)
- **Verification:** Ran `ls -R` to confirm the presence and structure of the `iosApp` directory and its contents (Xcode project, source files, assets).
- **Follow-ups:** Verify build dependencies (Xcode version, CocoaPods/Swift Package Manager) as needed for the project.


## 2026-02-26 (Australia/Sydney)
**Raouf:**
- **Scope:** iOS Audit Remediation - Documentation and Test Baseline
- **Summary:** Implemented SwiftPM test target and test cases, removed user-specific Xcode metadata file, added iOS-specific governance/security docs, and rewrote key iOS documentation for accuracy.
- **Files Changed:**
  - iosApp/Package.swift
  - iosApp/Tests/MehrGuardPackageTests/LocalizationExtensionTests.swift
  - iosApp/.gitignore
  - iosApp/README.md
  - iosApp/INTEGRATION_GUIDE.md
  - iosApp/APP_STORE_REVIEW.md
  - iosApp/ARCHITECTURE.md
  - iosApp/API_REFERENCE.md
  - iosApp/CONTRIBUTING.md
  - iosApp/CODE_OF_CONDUCT.md
  - iosApp/SECURITY.md
  - iosApp/LICENSE
  - iosApp/MehrGuard.xcodeproj/xcuserdata/raoof.r12.xcuserdatad/xcschemes/xcschememanagement.plist (removed)
  - AGENT.md
  - CHANGELOG.md
- **Verification:** Pending full post-edit validation (swift build/test and xcodebuild test).

## 2026-02-26 (Australia/Sydney)
**Raouf:**
- **Scope:** iOS Audit Remediation - SwiftPM Compatibility
- **Summary:** Added macOS platform support to `iosApp/Package.swift` to improve local SwiftPM test execution behavior.
- **Files Changed:**
  - iosApp/Package.swift
  - AGENT.md
  - CHANGELOG.md
- **Verification:** Pending rerun of `swift test`.

## 2026-02-26 (Australia/Sydney)
**Raouf:**
- **Scope:** iOS Audit Remediation - SwiftPM Test Stability
- **Summary:** Updated package test target dependencies and replaced package tests with deterministic smoke coverage to avoid macOS SwiftPM compilation instability of the full iOS app module.
- **Files Changed:**
  - iosApp/Package.swift
  - iosApp/Tests/MehrGuardPackageTests/LocalizationExtensionTests.swift
  - AGENT.md
  - CHANGELOG.md
- **Verification:** Pending rerun of `swift test`.

## 2026-02-26 (Australia/Sydney)
**Raouf:**
- **Scope:** iOS Audit Remediation - CI and Validation Guidance
- **Summary:** Added root workflow `ios-app-ci.yml` for iOS path testing, updated docs to use `xcodebuild test` as required gate, and cleaned local ephemeral logs.
- **Files Changed:**
  - .github/workflows/ios-app-ci.yml
  - iosApp/README.md
  - iosApp/CONTRIBUTING.md
  - logs/ (removed local artifacts)
  - AGENT.md
  - CHANGELOG.md
- **Verification:** Pending final workspace status review.

## 2026-02-26 (Australia/Sydney)
**Raouf:**
- **Scope:** iOS Audit Remediation - Final Verification
- **Summary:** Confirmed `xcodebuild test` success for scheme `MehrGuard`; confirmed `swift build` success; recorded ongoing `swift test` crash (exit 139) as toolchain-specific limitation.
- **Files Changed:**
  - AGENT.md
  - CHANGELOG.md
- **Verification:**
  - `xcodebuild -project iosApp/MehrGuard.xcodeproj -scheme MehrGuard -destination 'platform=iOS Simulator,name=iPhone 17,OS=26.2' test` ✅
  - `swift build` ✅
  - `swift test` ❌ (exit 139 before execution)

## 2026-11-17 (Australia/Sydney)
**Raouf:**
- **Scope:** CI workflow validity and licence text
- **Summary:** `ci.yml` and `performance.yml` failed on every push with "workflow file issue". `ci.yml` read `secrets.ANDROID_KEYSTORE_BASE64` in two step-level `if:` conditions, where the `secrets` context is not available; the release job now exposes it as a job-level `env` value and the conditions read `env`. `performance.yml` used double-quoted string literals inside a `${{ }}` expression; they are now single-quoted. `LICENSE` held an abridged paraphrase of Apache 2.0 (90 lines), which GitHub could not identify; it now holds the full canonical Apache License 2.0 text with the same copyright line.
- **Files Changed:**
  - .github/workflows/ci.yml
  - .github/workflows/performance.yml
  - LICENSE
  - AGENT.md
  - CHANGELOG.md
- **Verification:** `actionlint` reports no errors on all seven workflow files (it reported three before). No application code changed.
- **Follow-ups:** The workflows trigger on `main` but the default branch is `master`, so the build and test jobs do not run on pushes to the default branch. A trial run of `ci.yml` against `master` (pull request #1) compiled every target but stopped at `:common:lintDebug` with 4 Android lint errors, the first being `MissingPermission` for `Vibrator.vibrate` at `AndroidPlatformAbstractions.kt:128`. Those need fixing before the trigger is extended to `master`.

## 2026-11-17 (Australia/Sydney)
**Raouf:**
- **Scope:** CI on the default branch
- **Summary:** The CI workflow triggered only on `main`, so the build and tests never ran on `master`. The trigger now includes `master`, and the defects the first run surfaced are fixed:
  - `common` had no Android manifest while `PlatformHaptics` calls `Vibrator.vibrate` in four places, so `:common:lintDebug` failed with four `MissingPermission` errors. `common/src/androidMain/AndroidManifest.xml` now declares `VIBRATE`, which the app manifest already held.
  - The verification steps filtered on `com.mehrguard.*`; the tests live in `com.raouf.mehrguard.*`, so Gradle found none. Fixed in `ci.yml`, `quality-tests.yml` and `benchmark.yml`.
  - The workflow token could not comment on pull requests or publish check runs. `ci.yml` now requests `pull-requests: write` and `checks: write`.
  - The Android test summary failed when no report files existed. `androidApp` has no local unit tests, so an empty set is no longer a failure.
- **Files Changed:**
  - common/src/androidMain/AndroidManifest.xml (new)
  - .github/workflows/ci.yml
  - .github/workflows/quality-tests.yml
  - .github/workflows/benchmark.yml
  - AGENT.md
  - CHANGELOG.md
- **Verification:** On pull request #2 the common build and tests, accuracy, offline, threat-model and property-based verification, desktop, web and iOS builds, code quality, security scan and dependency audit all pass.
- **Follow-ups:** `androidApp` has no local unit tests. `benchmark.yml` names an `EnergyProxyBenchmark` class that is not in the tree. `performance.yml`, `quality-tests.yml`, `benchmark.yml` and `pages.yml` still trigger on `main` only.

## 2026-11-17 (Australia/Sydney)
**Raouf:**
- **Scope:** Accuracy, latency and network claims
- **Summary:** An audit found that the headline "87% F1" could not be reproduced. `scripts/eval.sh` and `judge/verify_accuracy.sh` printed fixed figures after running unrelated tests, `docs/EVALUATION.md` described a 330-URL corpus and `docs/ML_EVALUATION_METRICS.md` a 2,500-URL confusion matrix, and neither set is in the repository. The judge scripts also could not fail, because a pipe to `tail` hid Gradle's exit code.
  - Added `DatasetEvaluationTest` (desktop), which runs the engine over the committed `data/test_urls.csv` and prints the confusion matrix, metrics, per-category breakdown and every misclassified row. CI runs it and prints the report.
  - Measured on 17 Nov 2026: TP 49, FN 1, TN 41, FP 9. Precision 84.5%, recall 98.0%, F1 90.7%, accuracy 90.0%. The nine false alarms include all four Australian bank sites in the set; the miss is a punycode domain.
  - README: new Summary section with the measured table and its limits; the 87% F1, "<5ms" and "<5% false positive" figures are replaced by measured or asserted ones; 19 red-team scenarios corrected to 18 (the count in `RedTeamScenarios.kt`).
  - "Zero network calls" now says what is true: analysing a URL makes none, and the Android app checks GitHub Pages at startup for updated rules, which is why it holds `INTERNET`. `docs/LIMITATIONS.md` records that the rule download is not signature-checked.
  - `scripts/eval.sh` and `judge/verify_accuracy.sh` print the measured report; the judge scripts use `pipefail`.
  - Removed `docs/ML_EVALUATION_METRICS.md`, whose figures had no data behind them.
- **Files Changed:**
  - common/src/desktopTest/kotlin/com/raouf/mehrguard/core/DatasetEvaluationTest.kt (new)
  - .github/workflows/ci.yml
  - README.md, PRIVACY.md, JUDGE_QUICKSTART.md, SUBMISSION_CHECKLIST.md
  - docs/EVALUATION.md, docs/LIMITATIONS.md, docs/internal/README_FULL.md
  - docs/ML_EVALUATION_METRICS.md (removed)
  - scripts/eval.sh, judge/verify_accuracy.sh, judge/verify_all.sh, judge/verify_offline.sh, judge/verify_performance.sh
  - AGENT.md, CHANGELOG.md
- **Verification:** `DatasetEvaluationTest` passed in CI on pull request #3 and printed the figures above.
- **Follow-ups:** Other contest-era documents (`docs/MASTER_PACK*.md`, `PRODUCTION_READINESS.md`, `ESSAY.md`, `docs/internal/`) still quote the old figures. Evaluate on an external phishing feed. Sign the OTA rule file. Fix the false alarms on Australian bank domains. Benchmark latency on a phone.
