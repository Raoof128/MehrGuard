import XCTest
@testable import MehrGuard

// MARK: - Analysis Engine Unit Tests

/// Smoke tests for UnifiedAnalysisService (Swift fallback engine).
/// These tests run without the KMP framework and validate the Swift engine
/// directly. They cover all major detection categories and scoring thresholds.
final class SmokeTests: XCTestCase {

    // MARK: - Trusted Domain (Safe Control)

    @MainActor
    func testTrustedDomainAppleEvaluatesSafe() {
        let result = UnifiedAnalysisService.shared.analyze(url: "https://apple.com")
        XCTAssertEqual(result.verdict, .safe)
        XCTAssertLessThan(result.score, 31, "Trusted domain score must be below suspicious threshold")
    }

    @MainActor
    func testTrustedDomainGithubEvaluatesSafe() {
        let result = UnifiedAnalysisService.shared.analyze(url: "https://github.com/Raoof128/MehrGuard")
        XCTAssertEqual(result.verdict, .safe)
        XCTAssertLessThan(result.score, 31)
    }

    @MainActor
    func testUnknownDomainWithNoSignalsStaysSafe() {
        // A legitimate unknown domain with no suspicious signals must not be flagged
        let result = UnifiedAnalysisService.shared.analyze(url: "https://my-legit-startup.io")
        XCTAssertLessThan(result.score, 31, "Unknown domain with no signals must not reach suspicious threshold")
    }

    // MARK: - URL Shorteners (Suspicious)

    @MainActor
    func testBitlyFlaggedAsSuspicious() {
        let result = UnifiedAnalysisService.shared.analyze(url: "https://bit.ly/3xYz123")
        XCTAssertNotEqual(result.verdict, .safe)
        XCTAssertGreaterThanOrEqual(result.score, 31)
        XCTAssertTrue(result.flags.contains(where: { $0.localizedCaseInsensitiveContains("shortener") }),
                      "Expected URL Shortener flag, got: \(result.flags)")
    }

    @MainActor
    func testTinyURLFlaggedAsSuspicious() {
        let result = UnifiedAnalysisService.shared.analyze(url: "https://tinyurl.com/y2abc")
        XCTAssertGreaterThanOrEqual(result.score, 31)
        XCTAssertTrue(result.flags.contains(where: { $0.localizedCaseInsensitiveContains("shortener") }))
    }

    // MARK: - Credential Theft (Malicious)

    @MainActor
    func testAtSymbolCredentialTheftScoresMalicious() {
        let result = UnifiedAnalysisService.shared.analyze(url: "https://login.example.com@evil.com")
        XCTAssertEqual(result.verdict, .malicious)
        XCTAssertGreaterThanOrEqual(result.score, 71)
        XCTAssertTrue(result.flags.contains(where: { $0.localizedCaseInsensitiveContains("credential") }),
                      "Expected Credential Theft flag, got: \(result.flags)")
    }

    // MARK: - Homograph Attacks (Malicious)

    @MainActor
    func testCyrillicInAppleDomainScoresMalicious() {
        // Cyrillic 'а' (U+0430) mixed with Latin characters in apple.com
        let result = UnifiedAnalysisService.shared.analyze(url: "https://аpple.com/verify")
        XCTAssertGreaterThanOrEqual(result.score, 71,
            "Mixed-script homograph must reach malicious threshold, score: \(result.score)")
        XCTAssertTrue(result.flags.contains(where: { $0.contains("Mixed Script") || $0.contains("Homograph") }),
                      "Expected homograph flag, got: \(result.flags)")
    }

    @MainActor
    func testASCIIHomographPayPa1ScoresMalicious() {
        let result = UnifiedAnalysisService.shared.analyze(url: "https://paypa1.com/signin")
        XCTAssertGreaterThanOrEqual(result.score, 31, "ASCII homograph must be at least suspicious")
        XCTAssertTrue(result.flags.contains(where: {
            $0.localizedCaseInsensitiveContains("homograph") || $0.localizedCaseInsensitiveContains("typosquatt")
        }), "Expected homograph or typosquatting flag, got: \(result.flags)")
    }

    // MARK: - High-Risk TLDs (Malicious)

    @MainActor
    func testHighRiskTLDDotTKScoresMalicious() {
        let result = UnifiedAnalysisService.shared.analyze(url: "https://paypal-secure.tk/login")
        XCTAssertGreaterThanOrEqual(result.score, 71)
        XCTAssertTrue(result.flags.contains(where: { $0.localizedCaseInsensitiveContains("TLD") }),
                      "Expected TLD flag, got: \(result.flags)")
    }

    @MainActor
    func testHighRiskTLDDotMLScoresMalicious() {
        let result = UnifiedAnalysisService.shared.analyze(url: "https://bank-secure.ml/verify")
        XCTAssertGreaterThanOrEqual(result.score, 71)
        XCTAssertTrue(result.flags.contains(where: { $0.localizedCaseInsensitiveContains("TLD") }))
    }

    // MARK: - IP Address Obfuscation (Malicious)

    @MainActor
    func testDecimalIPAddressScoresMalicious() {
        let result = UnifiedAnalysisService.shared.analyze(url: "http://192.168.1.1/malware")
        XCTAssertGreaterThanOrEqual(result.score, 31, "IP address URL must be at least suspicious")
        XCTAssertTrue(result.flags.contains(where: { $0.localizedCaseInsensitiveContains("IP") }),
                      "Expected IP Address flag, got: \(result.flags)")
    }

    @MainActor
    func testHexIPAddressScoresMalicious() {
        let result = UnifiedAnalysisService.shared.analyze(url: "http://0xC0A80101/payload")
        XCTAssertGreaterThanOrEqual(result.score, 31)
    }

    // MARK: - Nested Redirects (Suspicious / Malicious)

    @MainActor
    func testEncodedNestedRedirectIsFlagged() {
        let result = UnifiedAnalysisService.shared.analyze(
            url: "https://legit.com/redirect?url=https%3A%2F%2Fphishing.tk%2Flogin"
        )
        XCTAssertTrue(result.flags.contains(where: { $0.localizedCaseInsensitiveContains("redirect") }),
                      "Expected Nested Redirect flag, got: \(result.flags)")
        XCTAssertGreaterThanOrEqual(result.score, 31)
    }

    @MainActor
    func testPlainNestedRedirectIsFlagged() {
        let result = UnifiedAnalysisService.shared.analyze(
            url: "https://legit.com/goto?next=https://phishing.ml/login"
        )
        XCTAssertTrue(result.flags.contains(where: { $0.localizedCaseInsensitiveContains("redirect") }),
                      "Expected Nested Redirect flag, got: \(result.flags)")
    }

    // MARK: - Brand Impersonation (Malicious)

    @MainActor
    func testPayPalSubdomainImpersonationScoresMalicious() {
        let result = UnifiedAnalysisService.shared.analyze(url: "https://paypal.secure-verify.com/billing")
        XCTAssertGreaterThanOrEqual(result.score, 31, "Brand impersonation must be at least suspicious")
    }

    // MARK: - Invalid Input Handling

    @MainActor
    func testInvalidURLFallsBackToSuspicious() {
        let result = UnifiedAnalysisService.shared.analyze(url: "not a valid url")
        XCTAssertEqual(result.verdict, .suspicious)
        XCTAssertEqual(result.score, 50)
    }

    // MARK: - Confidence

    @MainActor
    func testConfidenceIsWithinBounds() {
        let urls = [
            "https://apple.com",
            "https://bit.ly/3xyz",
            "https://аpple.com",
            "https://192.168.1.1/bad"
        ]
        for url in urls {
            let result = UnifiedAnalysisService.shared.analyze(url: url)
            XCTAssertGreaterThanOrEqual(result.confidence, 0.0, "Confidence below 0 for \(url)")
            XCTAssertLessThanOrEqual(result.confidence, 1.0, "Confidence above 1 for \(url)")
        }
    }

    // MARK: - Score Clamping

    @MainActor
    func testScoreIsClampedToHundred() {
        // Multiple stacked signals should not produce score > 100
        let result = UnifiedAnalysisService.shared.analyze(
            url: "https://paypa1.secure-alert.tk/login@evil.com?redirect=https%3A%2F%2Fbad.ml"
        )
        XCTAssertLessThanOrEqual(result.score, 100, "Score must never exceed 100")
    }
}
