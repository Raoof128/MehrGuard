# Agent Rules

- Follow the Raouf Change Protocol for all modifications.
- Maintain iOS development best practices (Swift, SwiftUI, Xcode conventions).
- Ensure all changes are verified through builds or tests if a runner is available.

### 2026-02-25 (Australia/Sydney)
**Raouf:**
- **Scope:** Repository Initialization and iOS App Setup
- **Summary:** Initialized the repository, set up agent rules, and cloned the `iosApp` directory from the source repository.
- **Status:** Project structure ready for iOS development.


### 2026-02-26 (Australia/Sydney)
**Raouf:**
- **Scope:** iOS Audit Remediation Batch 1
- **Summary:** Added SwiftPM package tests, iOS module governance/security documents, architecture/API references, and fully rewrote iOS operational docs (`README`, integration, App Store guide) to match current code paths and build behavior.
- **Status:** iOS module documentation and test scaffolding aligned with current implementation.

### 2026-02-26 (Australia/Sydney)
**Raouf:**
- **Scope:** iOS Audit Remediation Batch 2
- **Summary:** Expanded Swift Package platform declarations to include macOS for local SwiftPM test compatibility in addition to iOS.
- **Status:** Package configuration updated for broader local toolchain support.

### 2026-02-26 (Australia/Sydney)
**Raouf:**
- **Scope:** iOS Audit Remediation Batch 3
- **Summary:** Reworked SwiftPM test target to be standalone and deterministic for local package-level smoke verification.
- **Status:** SwiftPM test path simplified to avoid iOS app module compilation instability on macOS.

### 2026-02-26 (Australia/Sydney)
**Raouf:**
- **Scope:** iOS Audit Remediation Batch 4
- **Summary:** Added iOS-focused GitHub Actions CI workflow, documented SwiftPM test toolchain limitation in contributor docs, and removed ephemeral local audit logs.
- **Status:** Validation guidance and CI automation aligned with reliable Xcode-based testing.

### 2026-02-26 (Australia/Sydney)
**Raouf:**
- **Scope:** iOS Audit Remediation Final Verification
- **Summary:** Verified Xcode simulator test pass on shared scheme and confirmed SwiftPM build success; documented persistent SwiftPM test crash (exit 139) as environment/toolchain limitation.
- **Status:** Primary iOS validation gate (xcodebuild test) is green.

### 2026-11-17 (Australia/Sydney)
**Raouf:**
- **Scope:** CI workflow validity and licence text
- **Summary:** Fixed the two workflow files GitHub rejected on every push (`ci.yml` used the `secrets` context in step-level `if:`; `performance.yml` used double-quoted strings inside an expression), and replaced the abridged `LICENSE` with the full canonical Apache License 2.0 text so the licence is detected correctly.
- **Status:** Workflow files validate with actionlint; no application code changed.

### 2026-11-17 (Australia/Sydney)
**Raouf:**
- **Scope:** CI now runs on `master`, the default branch
- **Summary:** Extended the CI trigger to `master` and fixed what the first real run reported: a missing `VIBRATE` permission in the common library manifest, `--tests` filters pointing at a package that does not exist, missing token permissions for the PR comment and check steps, and an Android test summary step that failed when there were no report files.
- **Status:** All CI jobs pass on the pull request.

### 2026-11-17 (Australia/Sydney)
**Raouf:**
- **Scope:** Accuracy, latency and network claims brought in line with what is measured
- **Summary:** `scripts/eval.sh` and `judge/verify_accuracy.sh` printed fixed accuracy figures rather than computing them, and two documents quoted evaluation sets that are not in the repository. `DatasetEvaluationTest` now measures the engine on `data/test_urls.csv` in CI, and the README, evaluation, limitations and privacy documents state the measured figures and their limits.
- **Status:** Measured on 17 Nov 2026: precision 84.5%, recall 98.0%, F1 90.7% on 100 author-written URLs; 9 of 50 legitimate sites flagged.

### 2026-11-17 (Australia/Sydney)
**Raouf:**
- **Scope:** Model provenance, offline-check wording, judge files, contest-era banners
- **Summary:** A second review found that two documents and the weights file described model training that never happened (a 50,000-sample training set, data from PhishTank, an AUC). The weights are set by hand. Those documents are rewritten to say so, the invented metadata is removed from the weights file, and the README describes the scoring model as a fixed weighted blend. The offline check is described as the timing check it is. 44 contest-era documents carry a banner pointing at the measured figures.
- **Status:** No application code changed.

### 2026-11-17 (Australia/Sydney)
**Raouf:**
- **Scope:** OTA rule updates are validated and checksum-verified before they are cached
- **Summary:** `OtaUpdateManager` cached any download under 500 KB without parsing it, and its header said SHA-256 checking was unnecessary. It now parses the manifest and both data files (strict JSON, expected shape, size in bytes), checks each file's size and SHA-256 against `version.json`, refuses an update whose checksum is missing or a placeholder, and writes to the cache only when both files pass. Cached files are re-checked on load and ignored in favour of the bundled data if they fail. `data/updates/version.json` now carries the real checksums and sizes. The checksum comes from the same origin as the data, so it detects corruption, not a compromised origin; signed manifests are still not implemented. The update URL returned 404 at both candidate paths on 17 Nov 2026, so `UPDATE_BASE_URL` is unchanged and the docs say the check has no effect.
- **Status:** Detection logic and weights unchanged.

### 2026-11-17 (Australia/Sydney)
**Raouf:**
- **Scope:** Third review pass: brand-detection bug fixes, README first screen, contest material moved to `docs/contest/`, remaining unsupported claims
- **Summary:** The false alarms on all four Australian bank sites came from splitting hosts on the last two labels (`www.commbank.com.au` gave the site name `com`, a fuzzy match for `zoom`); `BrandDetector`, `DynamicBrandDiscovery` and `UrlAnalyzer` now use `PublicSuffixList`. The fuzzy threshold scales with brand length, a brand's own site is exempt only from that brand's checks, shared hosting suffixes are never exempt, a brand's exact name on a domain it does not own is reported, and Punycode hosts are decoded before the brand checks. On the committed dataset CI went from 49/50 phishing and 9/50 legitimate rows flagged (F1 90.7%) to 50/50 and 0/50; that set is author-written and its last errors were fixed by looking at them, so it is now a regression guard only. The README was rewritten around the measured table and its limits, 24 contest files moved to `docs/contest/` with a corrections table, documents and a workflow that held invented results were deleted, counts were replaced by counted values (60 brands, 484 patterns, 1,469 test functions), `OfflineOnlyTest` became `LocalAnalysisSanityTest` beside a new source scan for networking APIs, the "<5ms" and "87%" claims were removed from interface strings, and the web demo lists all 18 red-team scenarios.
- **Status:** Detection logic changed in the brand checks and host splitting only; heuristic weights and the scoring model are unchanged. See CHANGELOG.md for the full list and the follow-ups.

