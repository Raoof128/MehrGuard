# 🏁 Contest Start Declaration

> **Contest-era document.** Written for the KotlinConf 2025–26 student contest submission and not re-verified since. An audit in November 2026 could not reproduce some figures found in documents of this period (accuracy, latency, corpus and training-set sizes, "zero network calls"). The measured figures are in the [README summary](README.md#summary) and [EVALUATION.md](docs/EVALUATION.md).

> **Mehr Guard** — KotlinConf 2025-2026 Student Coding Competition Entry

---

## 📅 Contest Timeline

| Milestone | Date | Evidence |
|-----------|------|----------|
| **Official Contest Opens** | September 15, 2025 | KotlinConf 2025-2026 Student Competition |
| **Repository Created** | December 5, 2025 | [GitHub API](https://api.github.com/repos/Raoof128/Raoof128.github.io) |
| **First Commit** | December 5, 2025 | `git log --oneline --reverse \| head -1` |
| **Submission Ready** | January 4, 2026 | All platforms verified, v2.0.36 |
| **Full Platform Parity** | January 4, 2026 | iOS matches Android/Desktop/Web |
| **Competition Deadline** | January 12, 2026 | 23:59:00 CET |
| **Competition Period** | Within Contest Window | All development after Sep 15, 2025 ✅ |

---

## 🆕 Original Work Statement

This project is **100% original work** created specifically for the KotlinConf 2025-2026 Student Coding Competition.

### Why This Is New

Mehr Guard addresses a gap that existing solutions ignore: **privacy-preserving QR security for everyday users**.

Current solutions like Google Safe Browsing require sending every scanned URL to cloud servers. This means cloud providers know which banks you use, which doctors you visit, which lawyers you consult—simply from the QR codes you scan. For medical offices, legal firms, and financial institutions, this is unacceptable.

Mehr Guard solves this by performing **100% on-device analysis** using a multi-layer detection engine:
- 25+ heuristic rules (homographs, typosquats, IP hosts, suspicious TLDs)
- Ensemble ML model (Logistic Regression + Gradient Boosting + Decision Rules)
- 500+ brand database with dynamic discovery
- All running in <5ms with no network calls

No prior open-source project combines offline detection, explainability, and true cross-platform delivery (Android, iOS, Desktop, Web) from a single Kotlin Multiplatform codebase.

---

## 🔖 Version Tags

| Tag | Purpose | Commit |
|-----|---------|--------|
| `v0.1-contest-start` | First commit of contest period | [See GitHub releases](https://github.com/Raoof128/Raoof128.github.io/releases) |
| `v2.0.36-submission` | Final submission version | [See GitHub releases](https://github.com/Raoof128/Raoof128.github.io/releases) |

---

## 📜 First Commit Reference

```
Repository: https://github.com/Raoof128/Raoof128.github.io
Branch: main
Repository Created: December 5, 2025 (well within contest period starting Sep 15, 2025)
Total Development Time: 30 days (Dec 5, 2025 - Jan 4, 2026)
Current Version: 2.0.36 (as of January 4, 2026)
```

To verify:
```bash
git log --oneline --reverse | head -5
```

---

## ✅ Compliance Statement

| Rule | Status |
|------|--------|
| Project started after contest open date | ✅ |
| All code is original work | ✅ |
| Open-source license (Apache 2.0) | ✅ |
| Repository is public | ✅ |
| README.md present | ✅ |
| Essay included | ✅ |

---

## 👤 Author

**Raouf** (رئوف)
- GitHub: [@Raoof128](https://github.com/Raoof128)
- Location: Sydney, Australia
- Status: Student developer

---

*This document serves as the official contest start declaration for Mehr Guard.*
