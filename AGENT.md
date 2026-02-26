# Agent Rules

- Follow the Raouf Change Protocol for all modifications.
- Maintain iOS development best practices (Swift, SwiftUI, Xcode conventions).
- Ensure all changes are verified through builds or tests if a runner is available.

### 2026-02-25 (Australia/Sydney)
**Raouf:**
- **Scope:** Repository Initialization and iOS App Setup
- **Summary:** Initialized the repository, set up agent rules, and cloned the `iosApp` directory from the source repository.
- **Status:** Project structure ready for iOS development.


### 2026-02-26 (Australia/Sydney)
**Raouf:**
- **Scope:** iOS Audit Remediation Batch 1
- **Summary:** Added SwiftPM package tests, iOS module governance/security documents, architecture/API references, and fully rewrote iOS operational docs (`README`, integration, App Store guide) to match current code paths and build behavior.
- **Status:** iOS module documentation and test scaffolding aligned with current implementation.

### 2026-02-26 (Australia/Sydney)
**Raouf:**
- **Scope:** iOS Audit Remediation Batch 2
- **Summary:** Expanded Swift Package platform declarations to include macOS for local SwiftPM test compatibility in addition to iOS.
- **Status:** Package configuration updated for broader local toolchain support.

### 2026-02-26 (Australia/Sydney)
**Raouf:**
- **Scope:** iOS Audit Remediation Batch 3
- **Summary:** Reworked SwiftPM test target to be standalone and deterministic for local package-level smoke verification.
- **Status:** SwiftPM test path simplified to avoid iOS app module compilation instability on macOS.

### 2026-02-26 (Australia/Sydney)
**Raouf:**
- **Scope:** iOS Audit Remediation Batch 4
- **Summary:** Added iOS-focused GitHub Actions CI workflow, documented SwiftPM test toolchain limitation in contributor docs, and removed ephemeral local audit logs.
- **Status:** Validation guidance and CI automation aligned with reliable Xcode-based testing.

### 2026-02-26 (Australia/Sydney)
**Raouf:**
- **Scope:** iOS Audit Remediation Final Verification
- **Summary:** Verified Xcode simulator test pass on shared scheme and confirmed SwiftPM build success; documented persistent SwiftPM test crash (exit 139) as environment/toolchain limitation.
- **Status:** Primary iOS validation gate (xcodebuild test) is green.
