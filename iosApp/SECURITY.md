# Security Policy (iOS)

## Supported Scope
This policy covers the iOS module under `iosApp/` and related Swift integration scripts.

## Reporting a Vulnerability
Please do not open public issues for vulnerabilities.

Report privately with:
- Vulnerability description
- Affected file(s) and version/build context
- Reproduction steps or proof-of-concept
- Suggested mitigation (optional)

## Response Targets
- Initial acknowledgement: within 72 hours
- Triage decision: within 7 days
- Fix timeline: based on severity and exploitability

## Hardening Principles
- Avoid private APIs and unsupported platform behavior.
- Validate externally sourced input (URLs, QR payloads).
- Minimize permissions and data retention.
- Keep analysis and sensitive processing on-device when possible.
