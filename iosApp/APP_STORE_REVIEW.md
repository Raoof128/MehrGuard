# App Store Review Guide (iOS)

## App Summary
Mehr Guard is an iOS utility that scans QR payloads and evaluates URL phishing risk on-device.

## Build Metadata
- Bundle identifier: `com.raouf.mehrguard.ios`
- Marketing version: `$(MARKETING_VERSION)`
- Build number: `$(CURRENT_PROJECT_VERSION)`
- Minimum iOS: 17.0

## Permissions
- Camera (`NSCameraUsageDescription`): QR scanning.
- Photo library (`NSPhotoLibraryUsageDescription`): analyze QR images from gallery.

## Privacy Position
- No account is required.
- No user tracking.
- Local history is stored on device.
- Privacy manifest included at `MehrGuard/PrivacyInfo.xcprivacy`.

## Reviewer Walkthrough
1. Launch app and finish onboarding.
2. Grant camera permission.
3. Scan a QR code containing a URL.
4. Confirm verdict output and flagged reasons.
5. Open History and verify the event is recorded.
6. Open Settings and validate toggles (haptics/sound/theme/history).

## Test URLs
- Safe examples:
  - `https://apple.com`
  - `https://github.com`
- Suspicious examples:
  - URL shorteners (`https://bit.ly/...`)
  - Redirect-style parameters (`...?redirect=https://...`)

## Additional Notes for Review
- Simulator can validate UI and non-camera paths.
- Physical device is required for full live camera scan flow.
- App supports deep link `mehrguard://scan`.
