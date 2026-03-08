//
// Copyright 2025-2026 Mehr Guard Contributors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

// Services/UnifiedAnalysisService.swift
// Mehr Guard Unified Analysis Engine
//
// This service provides URL analysis using:
// 1. KMP HeuristicsEngine (when common.framework is linked)
// 2. Swift fallback engine (when KMP is not available)
//
// This dual approach ensures:
// - Full KMP integration for competition judging
// - Works in development without building Kotlin

import Foundation
import SwiftUI

#if canImport(common)
import common
#endif

#if os(iOS)

// MARK: - Unified Analysis Service

@MainActor
class UnifiedAnalysisService: ObservableObject {
    static let shared = UnifiedAnalysisService()
    
    @Published var isKMPAvailable: Bool = false
    @Published var lastEngineUsed: String = "None"

    #if canImport(common)
    private let kmpEngine = HeuristicsEngine()
    #endif

    // MARK: - User Settings (read from UserDefaults)

    private var userTrustedDomains: [String] = []
    private var userBlockedDomains: [String] = []
    private var sensitivityLevel: Int = 2

    private var maliciousThreshold: Int {
        switch sensitivityLevel {
        case 1: return 85   // Low — fewer false positives
        case 3: return 50   // Paranoia — catch more threats
        default: return 71  // Balanced (aligned with KMP engine)
        }
    }

    private var suspiciousThreshold: Int {
        switch sensitivityLevel {
        case 1: return 50
        case 3: return 20
        default: return 31  // Aligned with KMP engine
        }
    }

    private init() {
        #if canImport(common)
        isKMPAvailable = true
        #if DEBUG
        print("✅ [Mehr Guard] KMP HeuristicsEngine loaded")
        #endif
        #else
        isKMPAvailable = false
        #if DEBUG
        print("⚠️ [Mehr Guard] Using Swift fallback engine")
        #endif
        #endif

        refreshUserSettings()
    }

    /// Reload user-managed domain lists and sensitivity from UserDefaults.
    /// Called automatically before each analysis.
    func refreshUserSettings() {
        let raw = UserDefaults.standard.integer(forKey: "threatSensitivity")
        sensitivityLevel = (1...3).contains(raw) ? raw : 2

        if let data = UserDefaults.standard.data(forKey: "trustedDomains"),
           let domains = try? JSONDecoder().decode([String].self, from: data) {
            userTrustedDomains = domains.map { $0.lowercased() }
        }
        if let data = UserDefaults.standard.data(forKey: "blockedDomains"),
           let domains = try? JSONDecoder().decode([String].self, from: data) {
            userBlockedDomains = domains.map { $0.lowercased() }
        }
    }
    
    // MARK: - Unified Analysis
    
    /// Analyze a URL using the best available engine
    /// Returns a RiskAssessmentMock for UI compatibility
    func analyze(url: String) -> RiskAssessmentMock {
        refreshUserSettings()
        #if canImport(common)
        return analyzeWithKMP(url: url)
        #else
        return analyzeWithSwift(url: url)
        #endif
    }
    
    // MARK: - KMP Engine (Kotlin Multiplatform)
    
    #if canImport(common)
    private func analyzeWithKMP(url: String) -> RiskAssessmentMock {
        lastEngineUsed = "KMP HeuristicsEngine"
        
        // Call Kotlin engine
        let result = kmpEngine.analyze(url: url)
        
        // Extract triggered flags
        let flags = result.checks.compactMap { check -> String? in
            guard let hCheck = check as? HeuristicsEngine.HeuristicCheck else { return nil }
            return hCheck.triggered ? hCheck.name : nil
        }
        
        // Map Kotlin score to verdict using sensitivity-aware thresholds
        let score = Int(result.score)
        let verdict: VerdictMock
        if score >= maliciousThreshold {
            verdict = .malicious
        } else if score >= suspiciousThreshold {
            verdict = .suspicious
        } else {
            verdict = .safe
        }
        
        return RiskAssessmentMock(
            score: score,
            verdict: verdict,
            flags: flags.isEmpty ? ["Analyzed by KMP Engine"] : flags,
            confidence: Double(100 - abs(50 - score)) / 100.0,
            url: url
        )
    }
    #endif
    
    // MARK: - Swift Fallback Engine
    
    private func analyzeWithSwift(url: String) -> RiskAssessmentMock {
        lastEngineUsed = "Swift Fallback Engine"

        var score = 0
        var flags: [String] = []
        let lowercasedUrl = url.lowercased()

        // Normalize URL
        var normalizedUrl = lowercasedUrl
        if !normalizedUrl.hasPrefix("http://") && !normalizedUrl.hasPrefix("https://") {
            normalizedUrl = "https://" + normalizedUrl
        }

        guard let urlComponents = URLComponents(string: normalizedUrl),
              let host = urlComponents.host else {
            return RiskAssessmentMock(
                score: 50,
                verdict: .suspicious,
                flags: ["Invalid URL Format"],
                confidence: 0.7,
                url: url
            )
        }

        // ============================================
        // FIX #6: UNICODE NFKC NORMALIZATION
        // Normalize the host to catch decomposed homoglyphs
        // ============================================
        let normalizedHost = (host as NSString).precomposedStringWithCompatibilityMapping.lowercased()

        // ============================================
        // FIX #3: USER-BLOCKED DOMAINS — immediate high score
        // ============================================
        let isUserBlocked = userBlockedDomains.contains(normalizedHost)
            || userBlockedDomains.contains(where: { normalizedHost.hasSuffix(".\($0)") })

        if isUserBlocked {
            score += 80
            flags.append("User-Blocked Domain")
        }

        // ============================================
        // UNICODE/CYRILLIC HOMOGRAPH DETECTION
        // ============================================
        let cyrillicCharacters: Set<Character> = Set("абвгдеёжзийклмнопрстуфхцчшщъыьэюяАБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ")
        let latinCharacters: Set<Character> = Set("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")

        var hasCyrillic = false
        var hasLatin = false

        for char in normalizedHost {
            if cyrillicCharacters.contains(char) {
                hasCyrillic = true
            }
            if latinCharacters.contains(char) {
                hasLatin = true
            }
        }

        if hasCyrillic && hasLatin {
            score += 70
            flags.append("Mixed Script Attack (Cyrillic/Latin)")
        } else if hasCyrillic {
            if !normalizedHost.hasSuffix(".ru") && !normalizedHost.hasSuffix(".рф") {
                score += 50
                flags.append("Cyrillic Homograph")
            }
        }

        // ============================================
        // URL SHORTENERS
        // ============================================
        let urlShorteners = [
            "bit.ly", "tinyurl.com", "t.co", "goo.gl", "ow.ly", "is.gd",
            "buff.ly", "rebrand.ly", "cutt.ly", "short.io", "bl.ink",
            "tiny.cc", "s.id", "shorturl.at", "v.gd", "tr.im"
        ]

        for shortener in urlShorteners {
            if normalizedHost == shortener || normalizedHost.hasSuffix(".\(shortener)") {
                score += 40
                flags.append("URL Shortener")
                break
            }
        }

        // ============================================
        // FIX #5: NESTED REDIRECT DETECTION — scan all occurrences
        // ============================================
        let redirectParams = ["url=", "redirect=", "next=", "goto=", "return=", "dest=", "target=", "link="]
        let encodedSchemes = ["http%3A", "https%3A", "http%3a", "https%3a"]
        outer: for param in redirectParams {
            var searchRange = lowercasedUrl.startIndex..<lowercasedUrl.endIndex
            while let paramRange = lowercasedUrl.range(of: param, range: searchRange) {
                let afterParam = lowercasedUrl[paramRange.upperBound...]
                if encodedSchemes.contains(where: { afterParam.hasPrefix($0) })
                    || afterParam.hasPrefix("http://")
                    || afterParam.hasPrefix("https://") {
                    score += 40
                    flags.append("Nested Redirect URL")
                    break outer
                }
                searchRange = paramRange.upperBound..<lowercasedUrl.endIndex
            }
        }

        // ============================================
        // FIX #3: TRUSTED DOMAINS — merge built-in + user lists
        // ============================================
        let builtInTrusted = [
            "google.com", "www.google.com", "google.com.au",
            "apple.com", "www.apple.com",
            "microsoft.com", "www.microsoft.com", "account.microsoft.com",
            "github.com", "www.github.com",
            "amazon.com", "www.amazon.com",
            "paypal.com", "www.paypal.com",
            "facebook.com", "www.facebook.com",
            "twitter.com", "www.twitter.com", "x.com",
            "youtube.com", "www.youtube.com",
            "netflix.com", "www.netflix.com",
            "linkedin.com", "www.linkedin.com",
            "instagram.com", "www.instagram.com",
            "wikipedia.org", "en.wikipedia.org",
            "reddit.com", "www.reddit.com",
            "stackoverflow.com", "www.stackoverflow.com"
        ]
        let allTrusted = builtInTrusted + userTrustedDomains

        let isTrusted = !isUserBlocked && (
            allTrusted.contains(normalizedHost)
            || allTrusted.contains(where: { normalizedHost.hasSuffix(".\($0)") })
        )

        if isTrusted && score < 30 {
            score = max(5, score)
            if !flags.contains(where: { $0.contains("Homograph") || $0.contains("Mixed Script") }) {
                flags.append("Verified Domain")
            }
        } else {
            // FIX #9: Lower base score from 25 → 15 to reduce false positives
            // on unknown but legitimate sites
            if score == 0 {
                score = 15
            }

            // ============================================
            // SUSPICIOUS PATTERNS
            // ============================================

            if lowercasedUrl.contains("login") || lowercasedUrl.contains("signin")
                || lowercasedUrl.contains("verify") || lowercasedUrl.contains("account") {
                score += 15
                flags.append("Login/Verify Keywords")
            }

            if lowercasedUrl.contains("secure") || lowercasedUrl.contains("alert")
                || lowercasedUrl.contains("urgent") || lowercasedUrl.contains("suspended") {
                score += 25
                flags.append("Urgency Language")
            }

            // ============================================
            // ASCII HOMOGRAPH DETECTION
            // ============================================
            let homographPatterns = [
                "paypa1", "paypal1", "paypai", "paypall",
                "amaz0n", "amazom", "arnazon",
                "g00gle", "googie", "go0gle",
                "faceb00k", "facebok", "facebo0k",
                "micros0ft", "mircosoft", "micr0soft",
                "app1e", "appie", "apple1",
                "netf1ix", "netfiix", "n3tflix",
                "bank0f", "bankof-", "bank-of"
            ]

            for pattern in homographPatterns {
                if lowercasedUrl.contains(pattern) {
                    score += 40
                    flags.append("ASCII Homograph Attack")
                    break
                }
            }

            // ============================================
            // HIGH-RISK TLDs
            // ============================================
            let highRiskTLDs = [".tk", ".ml", ".ga", ".cf", ".gq"]
            let mediumRiskTLDs = [".work", ".click", ".xyz", ".top", ".buzz"]

            for tld in highRiskTLDs {
                if normalizedHost.hasSuffix(tld) {
                    score += 50
                    flags.append("High-Risk Free TLD")
                    break
                }
            }

            for tld in mediumRiskTLDs {
                if normalizedHost.hasSuffix(tld) {
                    score += 25
                    flags.append("Suspicious TLD")
                    break
                }
            }

            // ============================================
            // BRAND IMPERSONATION
            // ============================================
            let brandNames = ["paypal", "amazon", "google", "apple", "microsoft", "facebook", "netflix", "bank"]
            for brand in brandNames {
                if normalizedHost.hasPrefix("\(brand).") || normalizedHost.hasPrefix("\(brand)-") {
                    if !normalizedHost.hasSuffix("\(brand).com") && !normalizedHost.hasSuffix("\(brand).net") {
                        score += 40
                        flags.append("Brand Impersonation")
                        break
                    }
                }
            }

            // ============================================
            // STRUCTURAL CHECKS
            // ============================================

            let components = normalizedHost.components(separatedBy: ".")
            if components.count > 4 {
                score += 20
                flags.append("Complex Domain Structure")
            }

            // FIX #4: IP address detection — tightened patterns to avoid false positives
            let ipPatterns = [
                "^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$",              // Standard dotted-quad (full host match)
                "^0x[0-9a-fA-F]{2,8}$",                                      // Hex IP (bounded length)
                "^0[0-7]{1,3}\\.0[0-7]{1,3}\\.0[0-7]{1,3}\\.0[0-7]{1,3}$",  // Octal IP (proper octets)
                "^[1-9]\\d{7,9}$"                                            // Decimal IP (1-digit start, 8-10 total)
            ]

            for pattern in ipPatterns {
                if let regex = try? NSRegularExpression(pattern: pattern),
                   regex.firstMatch(in: normalizedHost, range: NSRange(normalizedHost.startIndex..., in: normalizedHost)) != nil {
                    score += 45
                    flags.append("IP Address URL")
                    break
                }
            }

            if normalizedHost.filter({ $0 == "-" }).count > 2 {
                score += 15
                flags.append("Excessive Hyphens")
            }

            // ============================================
            // @ SYMBOL - CREDENTIAL THEFT
            // ============================================
            if url.contains("@") {
                score += 55
                flags.append("Credential Theft Attempt")
            }

            // ============================================
            // TYPOSQUATTING
            // ============================================
            let typosquattingPatterns = [
                "googl.", "gogle.", "goolge.", "gooogle.", "g00gle.", "googel.",
                "appple.", "aple.", "aplle.", "app1e.",
                "amazn.", "amzon.", "amazom.", "anazon.",
                "paypa.", "paypall.", "payypal.", "pyppal.",
                "microsof.", "mircosoft.", "microsofl.",
                "facebok.", "facbook.", "faceboo.",
                "netfllx.", "netfiix.", "neflix.",
                "bankk.", "bamk."
            ]

            for pattern in typosquattingPatterns {
                if normalizedHost.contains(pattern) {
                    score += 50
                    flags.append("Typosquatting")
                    break
                }
            }
        }

        // ============================================
        // FIX #1: ALIGNED VERDICT THRESHOLDS (sensitivity-aware)
        // ============================================
        let verdict: VerdictMock
        if score >= maliciousThreshold {
            verdict = .malicious
        } else if score >= suspiciousThreshold {
            verdict = .suspicious
        } else {
            verdict = .safe
            if flags.isEmpty {
                flags.append("No Threats Detected")
            }
        }

        return RiskAssessmentMock(
            score: min(score, 100),
            verdict: verdict,
            flags: flags,
            confidence: calculateConfidence(score: score, flagCount: flags.count),
            url: url
        )
    }
    
    // MARK: - Helpers
    
    private func calculateConfidence(score: Int, flagCount: Int) -> Double {
        // FIX #8: Confidence based on score extremity, flag diversity, and threshold distance
        //
        // Score distance from the ambiguous zone (31-71) drives base confidence.
        // Multiple independent flags corroborate the verdict, boosting confidence.
        let ambiguousCenter = 51.0
        let distanceFromCenter = abs(Double(score) - ambiguousCenter)
        // Map distance 0-50 → confidence 0.55-0.90
        let scoreConfidence = 0.55 + (distanceFromCenter / 50.0) * 0.35

        // Each additional flag adds evidence (diminishing returns)
        let flagBonus: Double
        switch flagCount {
        case 0:     flagBonus = 0.0
        case 1:     flagBonus = 0.03
        case 2:     flagBonus = 0.06
        case 3:     flagBonus = 0.08
        default:    flagBonus = 0.10
        }

        return min(scoreConfidence + flagBonus, 0.98)
    }
}

// MARK: - Engine Info View Component

struct EngineInfoBadge: View {
    @StateObject private var service = UnifiedAnalysisService.shared
    
    var body: some View {
        HStack(spacing: 6) {
            Circle()
                .fill(service.isKMPAvailable ? Color.verdictSafe : Color.verdictWarning)
                .frame(width: 6, height: 6)
            
            Text(service.isKMPAvailable ? "KMP Engine" : "Swift Engine")
                .font(.caption2.weight(.medium))
                .foregroundColor(.textSecondary)
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .background(.ultraThinMaterial, in: Capsule())
    }
}

#endif
