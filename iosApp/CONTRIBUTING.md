# Contributing (iOS)

## Development Setup
1. Install Xcode 17+.
2. Clone repository and open `iosApp/MehrGuard.xcodeproj`.
3. Use the shared `MehrGuard` scheme.

## Branch and PR Expectations
- Keep PR scope focused.
- Include tests for behavior changes.
- Document user-facing changes in `CHANGELOG.md`.
- Ensure `xcodebuild test` passes before requesting review.

## Required Checks
- Build: `xcodebuild -project iosApp/MehrGuard.xcodeproj -scheme MehrGuard -destination 'platform=iOS Simulator,name=iPhone 17,OS=26.2' build`
- Baseline tests: `xcodebuild ... test` (same destination; shared-scheme stable gate)
- UI smoke: `xcodebuild ... test -only-testing:MehrGuardUITests/MehrGuardUITests/testAppLaunches`
- Optional full UI suite: `xcodebuild ... test -only-testing:MehrGuardUITests`
- SwiftPM checks: `cd iosApp && swift build`
- Optional SwiftPM test smoke: `cd iosApp && swift test` (may be affected by Swift toolchain issue; Xcode tests are the required gate)
- Lint (if installed): `swiftlint --strict`

## Coding Standards
- Keep code Swift 6 compatible.
- Use meaningful names and concise comments for non-obvious logic.
- Guard iOS-only APIs with `#if os(iOS)` where applicable.
- Guard optional shared framework imports with `#if canImport(common)`.
- Keep `AGENT.md` and `CHANGELOG.md` updated for every repository change.
