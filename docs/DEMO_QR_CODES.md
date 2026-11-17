# 🎯 Demo QR Code Gallery

> **Contest-era document.** Written for the KotlinConf 2025–26 student contest submission and not re-verified since. An audit in November 2026 could not reproduce some figures found in documents of this period (accuracy, latency, corpus and training-set sizes, "zero network calls"). The measured figures are in the [README summary](../README.md#summary) and [EVALUATION.md](../docs/EVALUATION.md).

**Printable QR codes to test Mehr Guard detection capabilities**

---

## How to Use

1. Print this page or display on another device
2. Scan each QR code with Mehr Guard
3. Verify the detection matches the expected result

---

## 🟢 SAFE URLs (Expected: Score < 30)

### 1. Google Homepage
![Google](https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=https://google.com)

```
URL: https://google.com
Expected: SAFE (Score ~5-10)
```

---

### 2. GitHub Repository
![GitHub](https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=https://github.com/Raoof128/Raoof128.github.io)

```
URL: https://github.com/Raoof128/Raoof128.github.io
Expected: SAFE (Score ~8-12)
```

---

### 3. Wikipedia Article
![Wikipedia](https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=https://en.wikipedia.org/wiki/QR_code)

```
URL: https://en.wikipedia.org/wiki/QR_code
Expected: SAFE (Score ~5-10)
```

---

## 🟡 SUSPICIOUS URLs (Expected: Score 30-70)

### 4. URL Shortener
![Shortener](https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=https://bit.ly/3example)

```
URL: https://bit.ly/3example
Expected: SUSPICIOUS (Score ~35-45)
Reasons: URL shortener hides destination
```

---

### 5. Unusual TLD
![Unusual TLD](https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=https://example-store.xyz/shop)

```
URL: https://example-store.xyz/shop
Expected: SUSPICIOUS (Score ~40-50)
Reasons: .xyz TLD commonly abused
```

---

### 6. Multiple Subdomains
![Subdomains](https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=https://secure.login.verify.example.com/auth)

```
URL: https://secure.login.verify.example.com/auth
Expected: SUSPICIOUS (Score ~45-55)
Reasons: Excessive subdomains, credential path
```

---

## 🔴 MALICIOUS URLs (Expected: Score > 70)

> ⚠️ These are **defanged examples** that demonstrate detection patterns.
> They do NOT lead to real phishing sites.

### 7. Brand Impersonation (PayPal)
![PayPal Fake](https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=https://paypa1-secure.tk/login)

```
URL: https://paypa1-secure.tk/login
Expected: MALICIOUS (Score ~80-90)
Reasons: 
- Brand impersonation (paypa1 ≈ paypal)
- Suspicious TLD (.tk)
- Credential harvesting path (/login)
```

---

### 8. IP Address Host
![IP Host](https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=http://192.168.1.100/secure/verify.php)

```
URL: http://192.168.1.100/secure/verify.php
Expected: MALICIOUS (Score ~75-85)
Reasons:
- IP address instead of domain
- No HTTPS
- Credential harvesting path
```

---

### 9. Typosquatting (Google)
![Google Fake](https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=https://g00gle-security.ml/verify)

```
URL: https://g00gle-security.ml/verify
Expected: MALICIOUS (Score ~85-95)
Reasons:
- Brand impersonation (g00gle ≈ google)
- Suspicious TLD (.ml)
- Credential path (/verify)
```

---

### 10. @ Symbol Injection
![At Symbol](https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=https://google.com@evil-site.tk/steal)

```
URL: https://google.com@evil-site.tk/steal
Expected: MALICIOUS (Score ~90+)
Reasons:
- @ symbol injection attack
- User sees "google.com" but goes to "evil-site.tk"
- Suspicious TLD
```

---

## 📱 Live Demo

Can't print? Use the live demo:
**[raoof128.github.io/?demo=true](https://raoof128.github.io/?demo=true)**

The demo mode pre-fills a malicious URL for instant testing.

---

## 🔧 Generate Your Own

Create custom QR codes for testing:

```bash
# Using curl + QR Server API
curl "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=YOUR_URL_HERE" -o test_qr.png

# Using Python qrcode library
pip install qrcode
python -c "import qrcode; qrcode.make('YOUR_URL').save('test_qr.png')"
```

---

## Expected Results Summary

| QR # | URL Pattern | Expected Score | Verdict |
|------|-------------|----------------|---------|
| 1 | google.com | 5-10 | ✅ SAFE |
| 2 | github.com/... | 8-12 | ✅ SAFE |
| 3 | wikipedia.org/... | 5-10 | ✅ SAFE |
| 4 | bit.ly/... | 35-45 | ⚠️ SUSPICIOUS |
| 5 | example.xyz | 40-50 | ⚠️ SUSPICIOUS |
| 6 | multi.subdomains | 45-55 | ⚠️ SUSPICIOUS |
| 7 | paypa1-secure.tk | 80-90 | ❌ MALICIOUS |
| 8 | 192.168.x.x | 75-85 | ❌ MALICIOUS |
| 9 | g00gle.ml | 85-95 | ❌ MALICIOUS |
| 10 | @injection.tk | 90+ | ❌ MALICIOUS |

---

*Generated for Mehr Guard Demo — December 2025*
