import XCTest
@testable import MehrGuard

final class SmokeTests: XCTestCase {
    func testContainsAfterMarker() {
        let value = "redirect=https://example.com"
        XCTAssertTrue(value.contains("https://", after: "redirect="))
        XCTAssertFalse(value.contains("ftp://", after: "redirect="))
    }

    @MainActor
    func testTrustedDomainEvaluatesSafe() {
        let result = UnifiedAnalysisService.shared.analyze(url: "https://apple.com")
        XCTAssertEqual(result.verdict, .safe)
        XCTAssertLessThan(result.score, 35)
    }

    @MainActor
    func testUrlShortenerIsFlagged() {
        let result = UnifiedAnalysisService.shared.analyze(url: "https://bit.ly/suspicious-path")
        XCTAssertNotEqual(result.verdict, .safe)
        XCTAssertGreaterThanOrEqual(result.score, 35)
        XCTAssertTrue(result.flags.contains(where: { $0.localizedCaseInsensitiveContains("shortener") }))
    }

    @MainActor
    func testAtSymbolCredentialTheftPatternIsFlagged() {
        let result = UnifiedAnalysisService.shared.analyze(url: "https://login.example.com@evil.com")
        XCTAssertEqual(result.verdict, .malicious)
        XCTAssertGreaterThanOrEqual(result.score, 60)
        XCTAssertTrue(result.flags.contains(where: { $0.localizedCaseInsensitiveContains("credential") }))
    }

    @MainActor
    func testInvalidUrlFallsBackToSuspicious() {
        let result = UnifiedAnalysisService.shared.analyze(url: "not a valid url")
        XCTAssertEqual(result.verdict, .suspicious)
        XCTAssertEqual(result.score, 50)
    }
}
