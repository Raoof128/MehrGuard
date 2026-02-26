# Mehr Guard iOS

Native SwiftUI iOS client for Mehr Guard. The app scans QR codes and evaluates links for phishing risk using a unified analysis layer:
- Kotlin Multiplatform `common.framework` when available.
- A built-in Swift fallback engine when the framework is not linked.

## Requirements
- macOS with Xcode 17+
- iOS 17.0+ deployment target
- Swift 6

## Quick Start
1. Open the Xcode project:
```bash
open iosApp/MehrGuard.xcodeproj
```
2. Select the shared `MehrGuard` scheme.
3. Run on a simulator (for example `iPhone 17`) or a physical iPhone.

## Optional: Build and Link the KMP Framework
If working in the full monorepo (with `gradlew` and `common/` available):
```bash
cd iosApp
./scripts/build_framework.sh
```
This builds and copies:
- `common.framework` -> `iosApp/Frameworks/common.framework`

When the framework is unavailable, the app still runs using Swift fallback logic.

## Testing
### Xcode tests
```bash
xcodebuild \
  -project iosApp/MehrGuard.xcodeproj \
  -scheme MehrGuard \
  -destination 'platform=iOS Simulator,name=iPhone 17,OS=26.2' \
  test
```

### SwiftPM tests (optional)
```bash
cd iosApp
swift test
```

Note: In some Swift 6.2 toolchains, `swift test` may crash with exit code `139` before execution for this package layout. Use `xcodebuild test` as the authoritative iOS validation gate.

## Project Structure
```text
iosApp/
├── MehrGuard.xcodeproj/          # Xcode project and shared scheme
├── MehrGuard/                    # App source (SwiftUI, models, assets)
├── MehrGuardTests/               # XCTest unit tests (Xcode target)
├── MehrGuardUITests/             # UI and performance test suites
├── scripts/                      # Build/import automation scripts
├── Tests/MehrGuardPackageTests/  # SwiftPM package tests
├── INTEGRATION_GUIDE.md          # KMP integration details
└── APP_STORE_REVIEW.md           # App Store review/testing notes
```

## Security and Privacy
- On-device analysis first; no account required.
- Local history storage via `UserDefaults`.
- Explicit iOS permission prompts for camera and photo library access.
- Privacy manifest included: `MehrGuard/PrivacyInfo.xcprivacy`.

## Additional Documentation
- `INTEGRATION_GUIDE.md`
- `APP_STORE_REVIEW.md`
- `ARCHITECTURE.md`
- `API_REFERENCE.md`
- `CONTRIBUTING.md`
- `SECURITY.md`
- `CODE_OF_CONDUCT.md`
- `LICENSE`
