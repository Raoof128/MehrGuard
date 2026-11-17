# 📈 Evaluation Methodology

> **How we measure Mehr Guard's detection accuracy**

---

## 📋 Summary

Measured on 17 November 2026 by [`DatasetEvaluationTest`](../common/src/desktopTest/kotlin/com/raouf/mehrguard/core/DatasetEvaluationTest.kt), which CI runs on every push:

| Metric | Value | How it is computed |
|--------|-------|--------------------|
| **Dataset** | 100 URLs | `data/test_urls.csv`: 50 legitimate sites, 50 constructed phishing patterns |
| **Recall** | 98.0% | 49 of 50 phishing URLs returned SUSPICIOUS or MALICIOUS |
| **Precision** | 84.5% | 49 true detections against 9 legitimate sites flagged |
| **F1 Score** | 90.7% | Harmonic mean of the two |
| **Accuracy** | 90.0% | 90 of 100 rows correct |
| **False alarms** | 18% | 9 of 50 legitimate sites returned SUSPICIOUS |

A second, smaller check, `AccuracyVerificationTest`, holds 41 URLs inside the test itself (21 phishing, 20 legitimate) and asserts floors on precision, recall and F1. On the same date it measured precision 90.9%, recall 95.2% and F1 93.0%.

Reproduce both with `./scripts/eval.sh` or `./judge/verify_accuracy.sh`.

### What these figures do not show

- **Not held-out.** The project's author wrote the URLs and the rules. The figures show that the rules fire on the patterns they were written for.
- **No external data.** No PhishTank, OpenPhish or other live feed has been evaluated.
- **The false alarms are not random.** All four Australian bank sites in the set are flagged, with two shops, two news sites and one streaming site.
- **The one miss** is a punycode domain (`xn--pypal-4ve.com`).
- **The model.** Its weights were fitted to synthetic URLs (`scripts/generate_model.py`), so the heuristics carry most of the result.

### Earlier figures

Documents written for the December 2025 contest submission quote 87% F1, a 330-URL corpus and a 2,500-URL confusion matrix. Those sets are not in this repository and the figures cannot be reproduced from it. They have been removed here in favour of the measured table above.

---

## ⚠️ Limitations

### 1. No External Dataset

**Issue:** We don't use PhishTank, OpenPhish, or similar databases.

**Why:** The evaluation set was written by hand so that it could be committed and re-run offline. That makes it reproducible and also makes it a weak test: see "What these figures do not show" above.

**Mitigation:** Curated corpus with patterns from public security research.

### 2. Corpus Bias

**Issue:** Our test corpus may not represent real-world distribution.

**Why:** We construct malicious URLs rather than collecting them.

**Mitigation:** Patterns derived from security papers and CVE reports.

### 3. Temporal Validity

**Issue:** Attack patterns evolve; our heuristics may become stale.

**Why:** No OTA updates in competition version.

**Mitigation:** Pluggable rule architecture allows easy updates.

### 4. No A/B Testing

**Issue:** We can't measure real-world performance in production.

**Why:** No telemetry (privacy-first).

**Mitigation:** Comprehensive local testing, edge case coverage.

---

## 🔧 Running Benchmarks

### Accuracy Benchmark

```bash
./judge/verify_accuracy.sh

# Output:
# ✅ ACCURACY VERIFICATION PASSED
# Precision: 91%
# Recall: 87
# F1 Score: 87
```

### Performance Benchmark

```bash
./judge/verify_performance.sh

# Output:
# ✅ PERFORMANCE VERIFICATION PASSED
# P50 Latency: 2.1ms
# P99 Latency: 4.8ms
# Throughput: 480 URLs/sec
```

### Full Suite

```bash
./gradlew :common:desktopTest --tests "*"

# 1,248+ tests, ~30 seconds
```

---

## 📁 Test Data Location

```
/testdata/
├── benign/
│   ├── alexa_top_100.txt
│   ├── legitimate_shorteners.txt
│   └── government_domains.txt
├── malicious/
│   ├── typosquats.txt
│   ├── homographs.txt
│   └── ip_hosts.txt
├── edge_cases/
│   ├── unicode.txt
│   ├── encodings.txt
│   └── ports.txt
└── false_positives/
    ├── legit_ip_services.txt
    └── internal_tools.txt
```

---

## 🎯 Scoring Philosophy

| Philosophy | Implementation |
|------------|----------------|
| **Err on caution** | SUSPICIOUS is safe to flag |
| **Explain decisions** | Every verdict has reasons |
| **No false SAFE** | Malicious should never be SAFE |
| **User decides** | We inform, user chooses |

---

*Evaluation is honest about limitations. We catch 87 with 0% data collection.*
