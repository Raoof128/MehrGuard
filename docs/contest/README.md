# Contest submission (historical)

These documents were written for the KotlinConf 2025–26 student contest in December 2025 and January 2026. They are kept as a record of what was submitted. **Do not rely on the figures in them.** A review in November 2026 found that several could not be reproduced, and some describe things that were never done.

The current, measured description of the project is the [README](../../README.md), with [EVALUATION.md](../EVALUATION.md) and [LIMITATIONS.md](../LIMITATIONS.md).

## Corrections

| What these documents say | What is true |
|---|---|
| "87% F1", "F1 87", a 330-URL corpus, a 2,500-URL confusion matrix | Not reproducible. Those sets are not in the repository, and the script that printed 87% printed a fixed number. The measured results on the committed 100-URL set are in [EVALUATION.md](../EVALUATION.md). |
| "<5 ms" analysis, "P50 2.1 ms", "500+ URLs per second" | Not measured. CI asserts that a full analysis of one URL takes under 50 ms at the 99th percentile on a GitHub-hosted runner's JVM. Nothing was measured on a phone. |
| "1,248 tests", "89% coverage" | The Kotlin sources hold 1,469 `@Test` functions (counted 17 Nov 2026). The coverage figure has not been reproduced. |
| "500+ brands" | 60 brands with 484 listed lookalike patterns. |
| "ML model trained on" phishing data, "gradient boosting", "ensemble of three models" | The scoring model's weights are set by hand. Nothing is trained. See [ML_TRAINING.md](../ML_TRAINING.md). |
| "100% offline", "zero network calls" | URL analysis makes no network request. The Android app does check GitHub Pages at startup for updated rules. |
| "about 80% shared code", "26,000 lines" | The shared module holds all of the detection logic and about a sixth of the hand-written lines. See [SHARED_CODE_REPORT.md](../SHARED_CODE_REPORT.md). |
| "identical verdicts on all five platforms", parity hashes | The parity tests run on the JVM in CI. The other targets are built from the same source; their verdicts are not compared in CI. |
| "19 red-team scenarios" | 18. |
| "production-ready", "enterprise", "100%" compliance and checklist scores | Self-assessments. This is a student project and has had no external review, security audit or user testing. |
| "587% increase in QR phishing" and other market statistics | Quoted without a source. Not verified. |

## Documents

| File | What it is |
|---|---|
| [ANDROID_AUDIT.md](ANDROID_AUDIT.md) | Self-audit of the Android app |
| [ANDROID_CHECKLIST.md](ANDROID_CHECKLIST.md) | Self-scored Android checklist |
| [ARCHITECTURE_TOUR.md](ARCHITECTURE_TOUR.md) | Architecture tour written for judges |
| [AUDIT_FIXES_2025-12-31.md](AUDIT_FIXES_2025-12-31.md) | Log of fixes made by an AI assistant on 31 December 2025 |
| [BEAT_THE_BOT_PARITY.md](BEAT_THE_BOT_PARITY.md) | Parity notes for the training game |
| [BEAT_THE_BOT_PARITY_AUDIT.md](BEAT_THE_BOT_PARITY_AUDIT.md) | Self-audit of the training game |
| [CONTEST_START.md](CONTEST_START.md) | Contest timeline and originality statement |
| [DEMO_SCRIPT.md](DEMO_SCRIPT.md) | Demo script |
| [ESSAY.md](ESSAY.md) | The 300-word contest essay |
| [IOS_ARCHITECTURE.md](IOS_ARCHITECTURE.md) | Why the iOS app uses SwiftUI over the shared engine |
| [IOS_CHECKLIST.md](IOS_CHECKLIST.md) | Self-scored iOS checklist |
| [JUDGE_QUICKSTART.md](JUDGE_QUICKSTART.md) | Guide for contest judges |
| [JUDGE_SUMMARY.md](JUDGE_SUMMARY.md) | One-page summary for judges |
| [JUDGE_VERIFICATION_SUMMARY.md](JUDGE_VERIFICATION_SUMMARY.md) | List of the check scripts as described to judges |
| [KOTLIN_CONTEST_OFFICIAL_RULES.txt](KOTLIN_CONTEST_OFFICIAL_RULES.txt) | The contest's rules, as published by the organiser |
| [MASTER_PACK.md](MASTER_PACK.md) | Project blueprint, part 1 |
| [MASTER_PACK_2.md](MASTER_PACK_2.md) | Project blueprint, part 2 |
| [MASTER_PACK_3.md](MASTER_PACK_3.md) | Project blueprint, part 3 |
| [PARITY.md](PARITY.md) | Platform parity notes |
| [PITCH.md](PITCH.md) | Pitch text |
| [PLATFORM_PARITY.md](PLATFORM_PARITY.md) | Platform parity notes |
| [PRODUCTION_READINESS.md](PRODUCTION_READINESS.md) | Self-assessed readiness report (December 2025) |
| [SUBMISSION_CHECKLIST.md](SUBMISSION_CHECKLIST.md) | Self-scored submission checklist |
| [VIDEO_DEMO.md](VIDEO_DEMO.md) | Demo video outline |
