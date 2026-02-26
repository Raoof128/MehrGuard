# iOS API Reference

## Core Types

### `UnifiedAnalysisService`
File: `MehrGuard/Models/UnifiedAnalysisService.swift`

Responsibilities:
- Detect KMP framework availability.
- Analyze URL input through KMP or Swift fallback.
- Publish engine availability metadata for UI badges.

Key API:
- `func analyze(url: String) -> RiskAssessmentMock`

### `SettingsManager`
File: `MehrGuard/Models/SettingsManager.swift`

Responsibilities:
- Centralized access to user preferences.
- Haptic/sound feedback control.
- Notification permission and local alert dispatch.

Key API:
- `func triggerHaptic(_ type: HapticType)`
- `func playSound(_ type: SoundType)`
- `func requestNotificationPermission() async -> Bool`
- `func showSecurityNotification(title:body:verdict:) async`

### `HistoryStore`
File: `MehrGuard/Models/HistoryStore.swift`

Responsibilities:
- Store and retrieve scan history items for the History UI.
- Persist history entries in local storage.

### `LanguageManager`
File: `MehrGuard/Models/LanguageManager.swift`

Responsibilities:
- Control language selection and localization behavior.
- Persist preferred language for app sessions.

## App-level Entry

### `MehrGuardApp`
File: `MehrGuard/App/MehrGuardApp.swift`

Responsibilities:
- Initialize app appearance and tab root.
- Handle deep links (`mehrguard://scan`).
- Manage onboarding state.
