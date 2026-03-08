import XCTest
@testable import MehrGuard

final class SmokeTests: XCTestCase {

    @MainActor
    func testTrustedDomainEvaluatesSafe() {
        let result = UnifiedAnalysisService.shared.analyze(url: "https://apple.com")
        XCTAssertEqual(result.verdict, .safe)
        XCTAssertLessThan(result.score, 31)
    }

    @MainActor
    func testUrlShortenerIsFlagged() {
        let result = UnifiedAnalysisService.shared.analyze(url: "https://bit.ly/suspicious-path")
        XCTAssertNotEqual(result.verdict, .safe)
        XCTAssertGreaterThanOrEqual(result.score, 31)
        XCTAssertTrue(result.flags.contains(where: { $0.localizedCaseInsensitiveContains("shortener") }))
    }

    @MainActor
    func testAtSymbolCredentialTheftPatternIsFlagged() {
        let result = UnifiedAnalysisService.shared.analyze(url: "https://login.example.com@evil.com")
        XCTAssertEqual(result.verdict, .malicious)
        XCTAssertGreaterThanOrEqual(result.score, 71)
        XCTAssertTrue(result.flags.contains(where: { $0.localizedCaseInsensitiveContains("credential") }))
    }

    @MainActor
    func testInvalidUrlFallsBackToSuspicious() {
        let result = UnifiedAnalysisService.shared.analyze(url: "not a valid url")
        XCTAssertEqual(result.verdict, .suspicious)
        XCTAssertEqual(result.score, 50)
    }

    @MainActor
    func testNestedRedirectDetection() {
        let result = UnifiedAnalysisService.shared.analyze(url: "https://legit.com/redirect?url=https%3A%2F%2Fphishing.tk%2Flogin")
        XCTAssertTrue(result.flags.contains(where: { $0.localizedCaseInsensitiveContains("redirect") }))
        XCTAssertGreaterThanOrEqual(result.score, 31)
    }

    @MainActor
    func testUnknownDomainBaseScoreDoesNotFalsePositive() {
        // An unknown domain with no suspicious signals should stay safe
        let result = UnifiedAnalysisService.shared.analyze(url: "https://my-legit-startup.io")
        XCTAssertLessThan(result.score, 31, "Unknown domain with no signals should not be suspicious")
    }
}
