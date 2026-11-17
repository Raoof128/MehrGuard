# 🔐 Mehr Guard Security Model

> **Contest-era document.** Written for the KotlinConf 2025–26 student contest submission and not re-verified since. An audit in November 2026 could not reproduce some figures found in documents of this period (accuracy, latency, corpus and training-set sizes, "zero network calls"). The measured figures are in the [README summary](README.md#summary) and [EVALUATION.md](docs/EVALUATION.md).

> **Threat Model & Detection Capabilities**

This document outlines Mehr Guard's security model, including attacker assumptions, detection capabilities, known limitations, and our privacy-first design rationale.

---

## 📋 Table of Contents

- [Threat Model Overview](#-threat-model-overview)
- [Attacker Assumptions](#-attacker-assumptions)
- [What We Detect](#-what-we-detect)
- [What We Don't Detect](#-what-we-dont-detect)
- [Offline-First Justification](#-offline-first-justification)
- [Evaluation Evidence](#-evaluation-evidence)
- [Security Principles](#-security-principles)

---

## 🎯 Threat Model Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Mehr Guard THREAT MODEL                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ATTACKER                    ATTACK VECTOR              TARGET               │
│  ────────                    ─────────────              ──────               │
│  • Opportunistic scammer     • Physical QR sticker      • Mobile users       │
│  • Organized phishing ring   • Email/SMS QR code        • Credential theft   │
│  • Social engineer           • Fake payment portal      • Financial fraud    │
│                              • Malware download link    • Device compromise  │
│                                                                              │
│  ─────────────────────────────────────────────────────────────────────────── │
│                                                                              │
│  Mehr Guard DEFENSE                                                           │
│  ─────────────────                                                           │
│  • URL heuristics (25+ signals)                                              │
│  • Brand impersonation detection (60 brands)                                 │
│  • Homograph/punycode attack detection                                       │
│  • TLD risk scoring                                                          │
│  • Ensemble ML (Logistic + Boosting + Rules)                                 │
│  • Component voting system (v1.19.0) - democratic verdict                    │
│  • On-device analysis (privacy-preserving)                                   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 🗳️ Component Voting System (v1.19.0)

Mehr Guard uses a **democratic voting approach** where each detection component votes independently:

```
┌─────────────────────────────────────────────────────────────────┐
│                    COMPONENT VOTING SYSTEM                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Each component casts ONE vote:                                  │
│                                                                  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌────────┐ │
│  │ Heuristic   │  │   ML Model  │  │   Brand     │  │  TLD   │ │
│  │  (0-40)     │  │  (0.0-1.0)  │  │  (0-20)     │  │ (0-10) │ │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └────┬───┘ │
│         │                │                │               │     │
│      SAFE ✅          SUS ⚠️           SAFE ✅         SAFE ✅   │
│                                                                  │
│  Final Tally: 3 SAFE, 1 SUSPICIOUS                              │
│  Verdict: SAFE (majority wins) ✅                                │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

**Why Voting Beats Pure Scoring:**
- Prevents one cautious component from dominating
- More resilient to model quirks and edge cases
- Better reflects "wisdom of the crowd"
- Critical signals still override (safety-first)

---

## 👤 Attacker Assumptions

### Who We Defend Against

| Attacker Profile | Sophistication | Attack Method | Mehr Guard Effectiveness |
|------------------|----------------|---------------|------------------------|
| **Opportunistic Scammer** | Low | Typosquatting, free TLDs, obvious fakes | ✅ **HIGH** - Easily detected |
| **Phishing Kit User** | Medium | Brand impersonation, URL shorteners | ✅ **HIGH** - Pattern matching |
| **Social Engineer** | Medium | Homograph attacks, convincing domains | ✅ **MEDIUM-HIGH** - Unicode detection |
| **Advanced Threat Actor** | High | Zero-day domains, sophisticated evasion | ⚠️ **MEDIUM** - Heuristics help, not foolproof |
| **Nation-State Actor** | Very High | Custom infrastructure, no known patterns | ❌ **LOW** - Beyond heuristic scope |

### Attacker Capabilities Assumed

1. **CAN** create lookalike domains (typosquatting, homograph)
2. **CAN** use URL shorteners to hide destinations
3. **CAN** obtain free/cheap hosting on high-risk TLDs
4. **CAN** mimic legitimate brand login pages
5. **CAN** encode malicious URLs in QR codes
6. **CAN** distribute QR codes physically or digitally

### Attacker Limitations Assumed

1. **CANNOT** easily obtain legitimate brand certificates (EV SSL)
2. **CANNOT** register exact-match brand domains on reputable TLDs
3. **CANNOT** evade all 25+ heuristic signals simultaneously
4. **CANNOT** predict our specific scoring thresholds

---

## ✅ What We Detect

### Detection Categories

| Category | Signals | Confidence | Examples |
|----------|---------|------------|----------|
| **Brand Impersonation** | Fuzzy matching, subdomain abuse | HIGH | `paypa1.com`, `secure-paypal.tk` |
| **Homograph Attacks** | Unicode analysis, punycode detection | HIGH | `pаypal.com` (Cyrillic 'а') |
| **Suspicious TLDs** | Risk-weighted scoring | HIGH | `.tk`, `.ml`, `.ga`, `.cf` |
| **URL Shorteners** | Pattern matching | MEDIUM | `bit.ly/*`, `t.co/*`, `goo.gl/*` |
| **IP-Based URLs** | Direct IP detection | HIGH | `http://192.168.1.100/phish` |
| **Credential Paths** | Path analysis | MEDIUM | `/login`, `/signin`, `/verify` |
| **Encoded Payloads** | Base64, double encoding | MEDIUM | `?data=aHR0cHM6Ly9ldmls` |
| **Protocol Issues** | HTTP vs HTTPS | MEDIUM | `http://bank.com` |
| **Long/Obfuscated URLs** | Entropy analysis | LOW-MEDIUM | Random subdomains, long paths |

### Detection Accuracy Targets

These were design goals. They are not measurements, and whether the engine meets them on real traffic has not been tested.

| Metric | Target | Rationale |
|--------|--------|-----------|
| **True Positive Rate** | >90% | Catch most obvious attacks |
| **False Positive Rate** | <5% | Don't annoy users with false alarms |
| **True Negative Rate** | >95% | Correctly identify safe URLs |
| **False Negative Rate** | <10% | Accept some sophisticated attacks slip through |

---

## ❌ What We Don't Detect

### Explicit Limitations

> **Transparency is key.** We document what we cannot detect so users understand our scope.

| Limitation | Reason | Mitigation |
|------------|--------|------------|
| **Legitimate domains serving malware** | Clean URL, compromised content | Beyond URL analysis |
| **Zero-day phishing domains** | No prior pattern data | Heuristics catch some |
| **Cleverly crafted new domains** | Novel evasion techniques | ML model helps |
| **Content-based attacks** | We don't fetch page content | Privacy trade-off |
| **Redirect chains (actual)** | Offline analysis only | Simulate common patterns |
| **Drive-by downloads** | Requires runtime analysis | Out of scope |
| **Legitimate short URLs** | Can't verify destination | Flag as suspicious, not malicious |

### Attack Types Outside Scope

1. **Content-based phishing** - Requires fetching and analyzing page HTML
2. **Credential stuffing** - Server-side attack
3. **Session hijacking** - Runtime attack
4. **DNS poisoning** - Network-level attack
5. **SSL stripping** - MITM attack

### Why These Limitations Exist

```
┌─────────────────────────────────────────────────────────────────┐
│                    DESIGN TRADE-OFFS                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Privacy                          Detection Power                │
│  ───────                          ───────────────                │
│  ████████████████░░░░░░           ░░████████████████            │
│  ↑ Our choice                                                    │
│                                                                  │
│  We prioritize:                                                  │
│  • No network requests in analysis                               │
│  • Fast local analysis (<50ms)                                   │
│  • No external dependencies                                      │
│                                                                  │
│  We sacrifice:                                                   │
│  • Real-time blocklist lookups                                   │
│  • Actual redirect chain following                               │
│  • Page content analysis                                         │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔒 Offline-First Justification

### Why We Never Send Data to Servers

1. **Privacy Preservation**
   - Scanned URLs reveal browsing intent, locations, habits
   - Medical, legal, financial QR scans deserve privacy
   - No data collection, no profiling, no selling

2. **Regulatory Compliance**
   - GDPR: No personal data processing
   - CCPA: No data collection to disclose
   - HIPAA-friendly: No PHI transmission

3. **Trust Model**
   - Users don't need to trust our servers
   - No server breaches can expose scan history
   - Works in airgapped/restricted environments

4. **Performance**
   - <50ms analysis time (no network latency)
   - Works offline, on airplane, in tunnels
   - No rate limiting or API quotas

### What This Costs Us

| Capability Lost | Impact | Acceptable? |
|-----------------|--------|-------------|
| Real-time blocklists | Miss newly reported URLs | ✅ Yes - heuristics compensate |
| Cloud ML models | Less sophisticated detection | ✅ Yes - local ML sufficient |
| Page content analysis | Miss content-based phishing | ✅ Yes - privacy > coverage |
| Crowd-sourced reports | No community intelligence | ✅ Yes - privacy priority |

---

## 📊 Evaluation

An earlier version of this section listed a 100-URL test set with a confusion matrix, 97.9% precision and 94.0% recall. Those results could not be reproduced and have been removed. The measured results, and their limits, are in [docs/EVALUATION.md](docs/EVALUATION.md).

---

## 🛡️ Security Principles

### Defense in Depth

Mehr Guard is one layer of protection:

```
┌─────────────────────────────────────────────────────────────────┐
│                    DEFENSE LAYERS                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Layer 1: Mehr Guard (Pre-click analysis)     ◀── WE ARE HERE    │
│  Layer 2: Browser safe browsing warnings                        │
│  Layer 3: Anti-phishing extensions                               │
│  Layer 4: Password manager autofill (domain check)              │
│  Layer 5: 2FA (limits credential theft impact)                  │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Responsible Disclosure

If you discover a security vulnerability or detection bypass:

1. **DO NOT** publicly disclose before we've addressed it
2. **DO** email security concerns to: [security@mehrguard.dev]
3. **DO** include proof-of-concept URLs (defanged)
4. **DO** allow 90 days for fixes before disclosure

### Continuous Improvement

We commit to:
- Regular evaluation dataset updates
- Detection algorithm refinements
- Transparent changelog of security updates
- Community feedback integration

---

## 📄 License

This security model document is part of Mehr Guard, licensed under Apache 2.0.

See [LICENSE](LICENSE) for full terms.
