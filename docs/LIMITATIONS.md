# Known Limitations & Technical Trade-offs

**Mehr Guard Detection Engine — Honest Assessment**

---

## Overview

This document transparently describes the limitations of Mehr Guard's detection approach. We believe acknowledging limitations demonstrates engineering maturity and helps users make informed decisions.

---

## 1. Hardcoded Brand Database

### Limitation
The brand impersonation detection relies on a **hardcoded database of 60 brands with 484 listed lookalike patterns** in `BrandDatabase.kt` (counted 17 Nov 2026). Each entry lists the brand's official domains, misspellings, homographs and brand-plus-keyword names. This has several implications:

### Implications

| Issue | Impact | Mitigation |
|-------|--------|------------|
| **New brands not covered** | A phishing site impersonating "CoolNewStartup.io" won't trigger brand detection | Other heuristics (TLD, entropy) still apply |
| **Regional brands missing** | Local banks, retailers may be absent | Database is extensible via updates |
| **Point-in-time snapshot** | Database was curated in Dec 2025 | Version updates can refresh |

### Why This Trade-off?

| Alternative | Downside |
|-------------|----------|
| Cloud API lookup | Requires internet, privacy concerns, latency |
| ML-based brand detection | Much larger model size, higher compute |
| No brand detection | Misses major attack vector |

**Decision:** Hardcoded database offers best balance of privacy, offline capability, and detection accuracy for major brands.

### Future Improvements
- Community-contributed brand lists
- Regional brand packs (downloadable)
- On-device brand learning (experimental)

---

## 2. Heuristic Fragility

### Limitation
URL heuristics are **pattern-based rules** that can be:

1. **Evaded** by sophisticated attackers
2. **False-positive prone** on unusual legitimate URLs
3. **Point-in-time** — attack patterns evolve

### Examples of Evasion

```
✅ Detected: https://paypa1.tk/login
❌ Evaded:   https://secure-payment-gateway.com/paypal (no brand in domain)

✅ Detected: http://192.168.1.1/steal
❌ Evaded:   https://legitimatecdn.com/redirect?target=evil.com (redirect chain)

✅ Detected: https://gооgle.com (Cyrillic o)
❌ Evaded:   https://google-support.com (looks similar, no homograph)
```

### Mitigation Strategy

The score combines four components with the default weights in `ScoringConfig.kt`:

```
Final Score =
    Heuristics (0.50) +
    Scoring model (0.20) +
    Brand detection (0.15) +
    TLD scoring (0.15)
```

The verdict is not a threshold on that score alone: a brand match or a risky TLD escalates on its own (`ScoreCalculator.kt`).

If one layer is evaded, others may still catch the attack.

### Documented Heuristic Limitations

| Heuristic | Limitation | False Positive Risk |
|-----------|------------|---------------------|
| Subdomain count | CDNs often have many subdomains | Medium |
| URL length | Some legitimate URLs are long | Low |
| Entropy | Random slugs (UUIDs) in legitimate URLs | Medium |
| Credential path | `/auth`, `/login` in legitimate flows | Low |
| URL shortener | Legitimate marketing campaigns use shorteners | Medium |

---

## 3. ML Model Constraints

### Limitation
The scoring model is small and **nothing in it is learned from data**:

- 15 URL features
- A logistic score with hand-set weights (0.40 of the blend)
- Fifteen hand-written threshold rules (0.35 and 0.25 of the blend)
- No training set, no held-out test set, no deep learning

See [ML_TRAINING.md](ML_TRAINING.md).

### Measured behaviour

On the committed 100-URL set every row is now classified as labelled (17 Nov 2026; see [EVALUATION.md](EVALUATION.md)). That set was written by the project's author and its last errors were fixed by looking at them, so it says little about unseen URLs. The model has not been evaluated on its own.

---

## 4. No Real-Time Threat Intelligence

### Limitation
Mehr Guard does **not** connect to:
- Google Safe Browsing API
- PhishTank API
- VirusTotal
- Any cloud threat feed

### Implication
A brand-new phishing domain registered 1 hour ago will **not** be in any blocklist.

### Why This Trade-off?

| Benefit | Cost |
|---------|------|
| Analysis needs no connection | No real-time threat data |
| No scanned URL leaves the device | Miss newest threats |
| No API keys required | No crowd-sourced intelligence |

### Mitigation
- Heuristics detect *patterns*, not specific domains
- Suspicious TLD lists cover common abuse vectors
- Brand database covers high-value targets

---

## 5. Platform-Specific Gaps

### Android
- Camera permission required for QR scanning
- No background scanning (intentional — battery preservation)

### iOS
- VisionKit requires iOS 13+
- No dynamic TLD updates without app update

### Web
- Camera API requires HTTPS and user permission
- No file system access for batch scanning
- jsQR library may miss damaged QR codes
- **Single-threaded JavaScript execution**: Kotlin/JS runs on the main browser thread. Analysis time in a browser has not been measured; a slow analysis would block the page while it runs. Web Workers could mitigate this but add significant complexity. Current implementation is acceptable for occasional scans but not optimized for batch processing.

### Desktop
- No native QR scanning (clipboard/file only)
- JVM startup time (~500ms cold start)

---

## 6. False Positive Scenarios

### Known False Positive Patterns

| URL Pattern | Why Flagged | Reality |
|-------------|-------------|---------|
| `https://accounts.google.com/o/oauth2/...` | Long URL, multiple subdomains | Legitimate Google OAuth |
| `https://bit.ly/official-campaign` | URL shortener | Legitimate marketing |
| `https://192.168.1.1/router-admin` | IP address | Local router admin |
| `https://xn--n3h.com` | Punycode | Legitimate IDN domain |

### User Recourse
- "Wrong Verdict?" feedback button (stores locally)
- Future: community-contributed allowlists

---

## 7. Attack Vectors We DON'T Detect

| Attack Type | Why Not Detected |
|-------------|------------------|
| **Malware download** | We check URLs, not file contents |
| **Legitimate but compromised sites** | No content analysis |
| **Credential stuffing** | Application-layer attack |
| **Session hijacking** | Requires network monitoring |
| **Zero-day browser exploits** | Beyond URL analysis |

**Mehr Guard's Scope:** Pre-click URL reputation scoring. Not an endpoint security solution.

---

## Summary

| Limitation | Severity | Mitigation |
|------------|----------|------------|
| Hardcoded brand database | Medium | Covers 60 listed brands |
| Heuristic evasion | Medium | Multi-layer defense |
| Simple ML model | Low | Explainability trade-off |
| No real-time feeds | High | Privacy is a feature |
| Platform gaps | Low | Graceful degradation |
| False positives | Medium | Feedback mechanism |

---

## Rule updates: checksum only, no signature, and the endpoint is not serving

On Android the app checks GitHub Pages at startup for updated detection rules (`OtaUpdateManager`). Before it caches an update it checks that the manifest and both data files are well-formed JSON of the expected shape, that each file is at most 500 KB, and that each file's size and SHA-256 equal the `sizeBytes` and `sha256:<hex>` listed in `version.json`. A manifest with a missing or placeholder checksum is treated as unverified and the update is not applied. A rejected download never replaces a cached file, and a cached file is re-checked on every load and ignored (the bundled data is used) if it fails.

**What the checksum does and does not do.** The checksum is fetched from the same origin as the data. It detects corruption and truncation, and a data file that does not match its manifest. It does not protect against a compromised origin: anyone who can change the data file there can change the manifest to match. Signed manifests are not implemented, so anyone able to change what that origin serves could still change the app's detection rules.

**The update endpoint is currently not serving.** `UPDATE_BASE_URL` points at a GitHub Pages path under a repository name that no longer exists. On 17 November 2026 both `https://raoof128.github.io/QDKMP-KotlinConf-2026-/data/updates/version.json` and `https://raoof128.github.io/data/updates/version.json` returned HTTP 404, so the check has no effect and the app runs on its bundled rules. The constant has not been changed because no URL that serves the file has been confirmed.

## False alarms on legitimate sites

Until 17 November 2026 the engine reported every site under a two-label public suffix (`.com.au`, `.co.uk`, `.co.jp`) as a brand lookalike, which flagged all four Australian bank sites in the committed dataset. That was a bug in how hosts were split and has been fixed ([EVALUATION.md](EVALUATION.md) has the details). No false-alarm rate on ordinary browsing has been measured, and the cases below are known to remain.

**Brand names that are ordinary words.** The brand check reports a brand's exact name used as a site name, or as one hyphen-separated part of it, on a domain the brand does not own. Several listed brands are ordinary words (apple, amazon, zoom, steam, slack, stripe, ledger, crypto, uber). An unrelated site such as `zoom-photography.example` or `crypto-news.example` is therefore reported as SUSPICIOUS. How often this happens on real sites has not been measured.

**Regional sites that are not listed.** A brand's official domains are listed by hand and only some regional domains are included. A real regional site that is not listed, for example a country domain of a listed bank or retailer, is reported as the brand's name on another domain.

**Shared platforms.** A host on a listed brand's own domain is not checked against that brand. Phishing hosted on such a domain, for example a form on a shared documents service, is not caught by the brand check. Hosts under the shared hosting suffixes listed in `BrandDetector.kt` are the exception and are always checked.

**Internationalised hosts.** Punycode hosts are decoded before the brand check, but only lookalikes listed for a brand are matched. An all-Cyrillic lookalike that is not in the list gets the generic punycode signal only, which by itself does not change the verdict.

---

## Our Position

**Mehr Guard is not meant to replace enterprise security solutions.** 

It's designed as a **privacy-first, educational tool** that:
1. Raises user awareness about URL risks
2. Catches obvious phishing attempts
3. Works without any cloud dependencies
4. Explains *why* something is suspicious

For users who need 99.9% detection with real-time threat intelligence, we recommend complementing Mehr Guard with cloud security services.

---

*First written December 2025. Revised 17 November 2026.*
