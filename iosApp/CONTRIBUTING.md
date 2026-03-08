# Contributing to Mehr Guard

Thank you for taking the time to contribute. This document explains how to set up your environment, what checks must pass before merging, and how to submit a good pull request.

---

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Help](#getting-help)
- [Before You Start](#before-you-start)
- [Development Setup](#development-setup)
- [Making Changes](#making-changes)
- [Required Checks](#required-checks)
- [Submitting a Pull Request](#submitting-a-pull-request)
- [Coding Standards](#coding-standards)
- [Commit Messages](#commit-messages)
- [Documentation](#documentation)

---

## Code of Conduct

All contributors must follow the [Code of Conduct](CODE_OF_CONDUCT.md). Be respectful, constructive, and professional in all interactions.

---

## Getting Help

- **Bug reports / feature requests** — Open a GitHub Issue using the appropriate template.
- **Questions** — Open a Discussion or a draft PR with your approach outlined.

---

## Before You Start

For anything beyond a small typo fix, please **open an issue first** to discuss your intended change. This avoids wasted effort if the change does not align with the project roadmap.

For security vulnerabilities, follow the [Security Policy](SECURITY.md) — do **not** open a public issue.

---

## Development Setup

### Prerequisites

| Tool | Version |
|------|---------|
| macOS | Ventura 13+ |
| Xcode | 17+ |
| Swift | 6.0 |
| SwiftLint | Any current brew version |

### First-time setup

```bash
# Install SwiftLint
brew install swiftlint

# Clone and open the project
git clone https://github.com/Raoof128/MehrGuard.git
cd MehrGuard/iosApp
open MehrGuard.xcodeproj
```

Select the **MehrGuard** shared scheme. Run on any available iPhone simulator (iOS 17+).

### Optional: KMP Framework

The Kotlin Multiplatform engine is not required for development. The Swift fallback engine is always available.

```bash
# From repo root (requires Java / Gradle)
cd iosApp
./scripts/build_framework.sh
```

See [`INTEGRATION_GUIDE.md`](INTEGRATION_GUIDE.md) for details.

---

## Making Changes

1. Fork the repository and create a branch from `master`:
   ```bash
   git checkout -b fix/descriptive-branch-name
   ```
2. Make your changes following the [Coding Standards](#coding-standards) below.
3. Run all [Required Checks](#required-checks) locally before pushing.
4. Update `AGENT.md` and `CHANGELOG.md` with a `Raouf:` entry describing your changes.
5. Push and open a pull request using the PR template.

---

## Required Checks

All checks must pass before a PR can be merged. CI enforces these automatically, but run them locally first.

### 1. Build

```bash
xcodebuild \
  -project MehrGuard.xcodeproj \
  -scheme MehrGuard \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' \
  build
```

Zero errors and zero warnings required.

### 2. Unit Tests (CI gate)

```bash
xcodebuild \
  -project MehrGuard.xcodeproj \
  -scheme MehrGuard \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' \
  test
```

All tests in `MehrGuardTests` must pass.

### 3. SwiftLint

```bash
cd iosApp
swiftlint lint
```

Zero errors. Address any warnings you introduced.

### 4. UI Smoke Test (recommended)

```bash
xcodebuild \
  -project MehrGuard.xcodeproj \
  -scheme MehrGuard \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' \
  test \
  -only-testing:MehrGuardUITests/MehrGuardUITests/testAppLaunches
```

### 5. Full UI Suite (required for UI-impacting changes)

```bash
xcodebuild \
  -project MehrGuard.xcodeproj \
  -scheme MehrGuard \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' \
  test \
  -only-testing:MehrGuardUITests
```

---

## Submitting a Pull Request

- Keep PRs focused — one logical change per PR.
- Fill in the PR template completely.
- Link the related issue with `Closes #123`.
- PRs that fail CI will not be reviewed until green.

---

## Coding Standards

### Swift

- Target **Swift 6** strict concurrency compliance (`@MainActor`, `Sendable`, `async/await`).
- Use `#if canImport(common)` guards around all KMP imports.
- Prefer `guard` early-exit over nested `if` chains.
- Use `@Observable` + `@MainActor` for all ViewModels (iOS 17+).
- Use `MARK: -` sections to organise types longer than ~50 lines.
- Avoid force-unwrap (`!`) in production code; use `guard let` or default values.
- All new public/internal APIs must have a doc comment (`///`).

### Architecture

- Keep the service layer (`Models/`) free of SwiftUI imports.
- All URL analysis must go through `UnifiedAnalysisService`.
- New UI screens belong under `UI/<FeatureName>/`.

### Tests

- New behaviour must ship with at least one unit test in `MehrGuardTests`.
- Test names should describe the scenario: `testHighRiskTLDScoresMalicious()`.
- Do not use `sleep` or fixed delays in tests; use XCTest expectations.

---

## Commit Messages

Use conventional commits:

```
<type>(<scope>): <short summary>
```

**Types:** `feat`, `fix`, `docs`, `test`, `refactor`, `chore`, `ci`

**Examples:**

```
feat(scanner): add gallery import from Files app
fix(engine): align Swift and KMP suspicious threshold to 31
test(engine): add homograph and IP-obfuscation smoke tests
docs: expand CONTRIBUTING with commit message guide
```

---

## Documentation

When your change affects user-facing behaviour or the API, update:

| Change type | Document |
|-------------|----------|
| New feature / changed UX | `README.md` |
| Engine or scoring logic | `ARCHITECTURE.md`, `API_REFERENCE.md` |
| KMP integration | `INTEGRATION_GUIDE.md` |
| Any code change | `CHANGELOG.md` + `AGENT.md` (Raouf: entry) |

Thank you for contributing to Mehr Guard.
