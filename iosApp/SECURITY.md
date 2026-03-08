# Security Policy

## Supported Versions

Only the latest release of Mehr Guard receives security fixes.

| Version | Supported |
|---------|-----------|
| Latest  | ✅ Yes |
| Older   | ❌ No |

---

## Scope

This policy covers:

- The iOS app source under `iosApp/`
- The Swift analysis engine (`UnifiedAnalysisService`, `KMPBridge`)
- The KMP common module (`common/`) when present
- WidgetKit extension (`MehrGuardWidget/`)
- All CI/CD configuration and scripts

**Out of scope:**

- Third-party dependencies (report to their maintainers directly)
- Issues in unreleased or experimental branches
- Denial-of-service attacks requiring physical device access

---

## Reporting a Vulnerability

**Do not open a public GitHub issue for security vulnerabilities.**

Please report vulnerabilities by emailing **security@mehrguard.app**.

Include as much of the following as possible:

- A clear description of the vulnerability and its potential impact
- The component or file affected (e.g., `UnifiedAnalysisService.swift`)
- Steps to reproduce or a proof-of-concept (private, not posted publicly)
- iOS version, device model, and app version where the issue was observed
- Any suggested mitigations you are aware of

You may encrypt your report using our PGP key (available on request).

---

## Response Timeline

| Milestone | Target |
|-----------|--------|
| Acknowledgement | Within 72 hours of receipt |
| Severity triage | Within 7 days |
| Fix or mitigation | Dependent on severity (see below) |
| Public disclosure | After a fix is available and released |

**Severity-based fix targets:**

| CVSS Score | Severity | Target Fix |
|------------|----------|------------|
| 9.0–10.0 | Critical | 14 days |
| 7.0–8.9 | High | 30 days |
| 4.0–6.9 | Medium | 60 days |
| 0.1–3.9 | Low | 90 days |

We will work with you to agree a disclosure date. We ask that you allow us reasonable time to issue a fix before any public disclosure.

---

## Coordinated Disclosure

We follow a coordinated disclosure model. We commit to:

1. Acknowledging your report promptly.
2. Keeping you informed of our progress.
3. Crediting you in the release notes (if you wish).
4. Not taking legal action against researchers who follow this policy in good faith.

---

## Security Design Principles

The following principles guide Mehr Guard's security architecture:

- **On-device processing** — All URL analysis runs locally; no data is sent to external servers.
- **Minimal permissions** — Only camera and photo library access are requested, and only at the moment they are needed.
- **Input validation** — All URL inputs are sanitised (length, scheme, control characters) before analysis.
- **No private APIs** — The app uses only documented, App Store-approved APIs.
- **Principle of least privilege** — Each component accesses only the data it requires.
- **Offline-first** — The app is designed to function fully without network access.

---

## Known Limitations

- **Heuristic-based detection** — The engine uses rule-based scoring, not a real-time threat feed. Novel phishing techniques may not be detected until heuristics are updated.
- **URL shorteners** — The engine flags shorteners (bit.ly, tinyurl, etc.) but cannot follow them to their destination in offline mode.
- **QR codes with encoded payloads** — Obfuscated or multi-stage payloads may not be fully decoded.

These are known design constraints, not security vulnerabilities.
