# 🎯 Threat Model

> **Contest-era document.** Written for the KotlinConf 2025–26 student contest submission and not re-verified since. An audit in November 2026 could not reproduce some figures found in documents of this period (accuracy, latency, corpus and training-set sizes, "zero network calls"). The measured figures are in the [README summary](../README.md#summary) and [EVALUATION.md](../docs/EVALUATION.md).

> **Who attacks, what we detect, what we don't, and why.**

---

## 📋 Summary

| Attack Class | Detected? | Confidence |
|--------------|-----------|------------|
| Lazy phishing (.tk, typosquats) | ✅ Yes | High |
| Homograph attacks (Cyrillic) | ✅ Yes | High |
| URL shortener obfuscation | ✅ Yes | Medium |
| IP-based phishing | ✅ Yes | High |
| Brand impersonation | ✅ Yes | High |
| Brand-new domains (no pattern) | ❌ No | N/A |
| Sophisticated APT campaigns | ❌ No | N/A |
| Zero-day browser exploits | ❌ No | N/A |

---

## 👤 Attacker Profiles

### 1. Lazy Phisher

**Profile:**
- Uses free TLDs (.tk, .ml, .ga, .cf, .gq)
- Registers obvious typosquats (paypa1.com, g00gle.com)
- Uses IP addresses as hosts
- Reuses common URL patterns

**Mehr Guard Detection:** ✅ **EXCELLENT**
- TLD scoring catches free domain abuse
- Brand detector finds typosquats
- IP host heuristic triggers immediately

**Example:**
```
http://192.168.1.1:8080/paypal-login
→ Verdict: MALICIOUS (score: 85)
→ Flags: IP host, port number, brand keyword
```

---

### 2. Script Kiddie

**Profile:**
- Uses URL shorteners to hide destination
- Tries homograph attacks (Cyrillic characters)
- Uses @ symbol injection
- Copies phishing kits from forums

**Mehr Guard Detection:** ✅ **GOOD**
- Homograph detector catches Unicode tricks
- URL shortener flagging alerts users
- @ symbol injection is high-weight heuristic

**Example:**
```
https://bit.ly/3abc123 → Suspected shortener
https://аpple.com → Homograph (Cyrillic а)
https://google.com@evil.com → @ injection
```

---

### 3. Credential Harvester

**Profile:**
- Creates convincing login pages
- Uses legitimate-looking but new domains
- May use HTTPS (Let's Encrypt is free)
- Targets specific organizations

**Mehr Guard Detection:** ⚠️ **PARTIAL**
- Credential keywords in URL trigger medium risk
- Dynamic brand discovery catches trust words
- BUT: New clean domains may pass

**Limitations:**
- We can't blocklist domains we've never seen
- HTTPS doesn't mean trustworthy
- Sophisticated pages look legitimate

---

### 4. Nation-State / APT (Rare in QR)

**Profile:**
- Uses legitimate compromised infrastructure
- Clean domains registered months in advance
- Zero-day exploits in landing page
- Highly targeted (spear-QRishing)

**Mehr Guard Detection:** ❌ **NOT DESIGNED FOR THIS**
- These attacks use legitimate-looking everything
- Require real-time threat intelligence
- Beyond scope of offline detection

**Why That's Okay:**
- APTs rarely target random QR scanners
- Nation-states have resources beyond heuristics
- Privacy trade-off is intentional

---

## 🔍 What We Detect (Detail)

### Heuristic Detectors

| Detector | What It Catches | Weight |
|----------|-----------------|--------|
| `IpHostDetector` | IP addresses as hosts | 25 |
| `HttpNotHttpsDetector` | Unencrypted connections | 15 |
| `PunycodeDetector` | Internationalized domains | 18 |
| `AtSymbolDetector` | @ character before host | 18 |
| `ShortenerDetector` | bit.ly, tinyurl, etc. | 10 |
| `PortDetector` | Non-standard ports | 10 |
| `HomographDetector` | Mixed Unicode scripts | Variable |
| `EntropyDetector` | Random-looking domains | 12 |
| `TldScorer` | Free/abused TLDs | Variable |

### ML Detectors

Nothing here is learned from data; the weights and thresholds are set by hand ([ML_TRAINING.md](ML_TRAINING.md)).

| Component | What it is | Weight in the blend |
|-----------|------------|---------------------|
| Logistic score | A weighted sum of fifteen URL features | 0.40 |
| Ten threshold rules | Each tests one feature | 0.35 |
| Five more threshold rules | Hard signals (IP host, @, port) | 0.25 |

### Brand Detection

| Method | Coverage |
|--------|----------|
| Static database | 60 brands with official domains |
| Levenshtein matching | Site names within one edit of a short brand, or two of a brand of six letters or more |
| Dynamic discovery | Trust words, action words, urgency patterns |

---

## ❌ What We Don't Detect

### 1. Brand-New Clean Domains

**Scenario:** Attacker registers `secure-bank-login.com` yesterday.

**Why We Miss It:**
- Domain has no abuse history
- TLD is standard (.com)
- No typosquat pattern

**Mitigation:** Dynamic brand discovery catches some via trust word abuse.

---

### 2. Compromised Legitimate Sites

**Scenario:** Attacker injects phishing page into `university.edu/temp/login.php`.

**Why We Miss It:**
- Domain is legitimate and trusted
- .edu TLD is low-risk
- No structural indicators

**Mitigation:** Unusual path depth + credential keywords may trigger SUSPICIOUS.

---

### 3. Redirect Chains

**Scenario:** `safe-looking.com` → 302 → `evil.com`

**Why We Miss It:**
- We analyze the initial URL only
- Following redirects requires network
- Would break offline promise

**Mitigation:** Shortener detection flags suspicious redirect patterns.

---

### 4. Payload Injection After Load

**Scenario:** Page loads safe content, then JavaScript fetches malicious payload.

**Why We Miss It:**
- We analyze URL, not page content
- Would require sandboxed browser
- Scope is QR URL, not web security

**Mitigation:** Out of scope—this is browser security, not QR security.

---

## 📊 Detection Rates

No detection rate per attack type has been measured. An earlier version of this section gave estimates (for example "about 95%" for free-TLD phishing); they were guesses and have been removed. What has been measured is in [EVALUATION.md](EVALUATION.md).

---

## ⚖️ Trade-offs We Accept

| Trade-off | Why We Accept It |
|-----------|------------------|
| No real-time blocklists | Privacy is non-negotiable |
| Lower recall on new threats | Heuristics look for patterns, so they do not depend on a blocklist |
| Some false positives | SUSPICIOUS ≠ blocked |
| No page content analysis | Scope is URL, not browser |

---

## 🛡️ Defense in Depth Recommendation

Mehr Guard is **one layer** in a security stack:

1. **Mehr Guard** → Catch obvious phishing before navigation
2. **Browser security** → Block known malware domains
3. **User awareness** → Verify sensitive actions
4. **2FA** → Protect credentials even if phished

---

*No scanned URL leaves the device. How much real phishing the engine catches has not been measured.*
