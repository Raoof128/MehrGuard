# 🔧 Code Quality

> **Contest-era document.** Written for the KotlinConf 2025–26 student contest submission and not re-verified since. An audit in November 2026 could not reproduce some figures found in documents of this period (accuracy, latency, corpus and training-set sizes, "zero network calls"). The measured figures are in the [README summary](README.md#summary) and [EVALUATION.md](docs/EVALUATION.md).

> **Overview of static analysis, linting, and quality gates.**

---

## Detekt (Kotlin Static Analysis)

This project uses [Detekt](https://detekt.dev/) for static code analysis.

### Current Status

| Metric | Value |
|--------|-------|
| **Weighted Issues** | 5,829 (baselined) |
| **New Issues** | 0 |
| **Baseline File** | `detekt-baseline.xml` |

### How We Handle Issues

1. **Baseline Tracking** — All existing issues are tracked in `detekt-baseline.xml` to avoid noise during development.
2. **No New Issues** — New code must not introduce additional issues beyond the baseline.
3. **Gradual Improvement** — Issues are addressed incrementally as code is modified.

### Running Detekt

```bash
# Run detekt (will fail if new issues beyond baseline)
./gradlew detekt

# Update baseline (after fixing issues)
./gradlew detektBaseline
```

### Configuration

- **Config File:** `detekt.yml`
- **Baseline:** `detekt-baseline.xml`
- **Rules:** Standard Detekt ruleset with project-specific overrides

---

## Android Lint

Android-specific linting is enabled with standard rules.

```bash
./gradlew :androidApp:lint
```

---

## Code Formatting

- **Kotlin Style:** [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- **EditorConfig:** Standard `.editorconfig` settings

---

*Note: The high issue count reflects the codebase history. All issues are stable and tracked. No security-critical issues are present.*
