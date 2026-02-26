import XCTest
@testable import MehrGuard

final class SmokeTests: XCTestCase {
    func testContainsAfterMarker() {
        let value = "redirect=https://example.com"
        XCTAssertTrue(value.contains("https://", after: "redirect="))
        XCTAssertFalse(value.contains("ftp://", after: "redirect="))
    }
}
