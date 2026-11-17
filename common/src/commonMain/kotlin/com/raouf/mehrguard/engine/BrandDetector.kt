/*
 * Copyright 2025-2026 Mehr Guard Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.raouf.mehrguard.engine

/**
 * Brand Impersonation Detector for Mehr Guard
 *
 * Detects when URLs attempt to impersonate well-known brands
 * using typosquatting, homographs, subdomain abuse, and fuzzy matching.
 *
 * Hosts are split on their public suffix ([PublicSuffixList]), internationalised
 * hosts are decoded first ([Punycode]), and a host on any listed brand's official
 * domain is never reported.
 *
 * SECURITY NOTES:
 * - All inputs are bounded to prevent DoS
 * - Brand database is immutable
 * - Thread-safe for concurrent detection
 * - Includes Australian banking institutions
 *
 * @author Mehr Guard Security Team
 * @since 1.0.0
 */
class BrandDetector {

    companion object {
        // === CONFIGURATION ===

        /** Maximum URL length to process */
        private const val MAX_URL_LENGTH = 2048

        /** Maximum host length to process */
        private const val MAX_HOST_LENGTH = 255

        /** Largest edit distance that fuzzy matching ever accepts */
        private const val FUZZY_MATCH_THRESHOLD = 2

        /**
         * A fuzzy match may differ from the brand in at most one character per
         * this many characters of the brand name. With 3, brands of three to five
         * letters allow one edit and longer ones allow two. Without this scaling a
         * three-letter brand matched any three-letter name that shared one letter
         * with it ("cnn" against "anz", "ibm" against "irs").
         */
        private const val FUZZY_CHARS_PER_EDIT = 3

        /** Official domains of every brand in the database, lower case. */
        private val ALL_OFFICIAL_DOMAINS: Set<String> by lazy {
            BrandDatabase.brands.values
                .flatMap { it.officialDomains }
                .map { it.lowercase() }
                .toSet()
        }

        // === SCORING ===

        const val SCORE_EXACT_SUBDOMAIN = 30
        const val SCORE_TYPOSQUAT = 35
        const val SCORE_HOMOGRAPH = 40
        const val SCORE_COMBOSQUAT = 25
        const val SCORE_FUZZY_MATCH = 20
    }


    /**
     * Match type classification.
     */
    enum class MatchType {
        EXACT_IN_SUBDOMAIN,
        TYPOSQUAT,
        HOMOGRAPH,
        COMBO_SQUAT,
        FUZZY_MATCH
    }

    /**
     * Brand match details.
     */
    data class BrandMatch(
        val brand: String,
        val matchType: MatchType,
        val matchedPattern: String,
        val category: BrandDatabase.BrandCategory
    )

    /**
     * Detection result.
     */
    data class DetectionResult(
        val score: Int,
        val match: String?,
        val details: BrandMatch?
    ) {
        val isImpersonation: Boolean get() = match != null

        val severity: String
            get() = when {
                score >= SCORE_HOMOGRAPH -> "CRITICAL"
                score >= SCORE_TYPOSQUAT -> "HIGH"
                score >= SCORE_EXACT_SUBDOMAIN -> "MEDIUM"
                score > 0 -> "LOW"
                else -> "NONE"
            }
    }

    /**
     * Detect brand impersonation in URL.
     *
     * @param url The URL to analyze
     * @return DetectionResult with score and match details
     */
    fun detect(url: String): DetectionResult {
        // SECURITY: Validate input length
        if (url.isBlank() || url.length > MAX_URL_LENGTH) {
            return DetectionResult(score = 0, match = null, details = null)
        }

        val host = extractHost(url)

        // SECURITY: Validate extracted host
        if (host.isBlank() || host.length > MAX_HOST_LENGTH) {
            return DetectionResult(score = 0, match = null, details = null)
        }

        // Decode xn-- labels first: the homograph patterns below are Unicode text,
        // and links normally carry an internationalised host in its ASCII form.
        val hostLower = Punycode.toUnicodeHost(host.lowercase()).lowercase()

        // A site on a listed brand's own domain is not impersonating that brand,
        // and it is not a misspelling of some other brand either.
        if (isOfficialDomain(hostLower)) {
            return DetectionResult(score = 0, match = null, details = null)
        }

        // Split the host on its public suffix, so that in "www.example.com.au"
        // the site name is "example" and not "com".
        val parsedHost = PublicSuffixList.default.parse(hostLower)

        // Check each brand from the database
        for ((brand, config) in BrandDatabase.brands) {
            // 1. EXACT_IN_SUBDOMAIN - brand appears in subdomain
            if (isBrandInSubdomain(parsedHost, brand)) {
                return DetectionResult(
                    score = SCORE_EXACT_SUBDOMAIN,
                    match = brand,
                    details = BrandMatch(brand, MatchType.EXACT_IN_SUBDOMAIN, brand, config.category)
                )
            }

            // 2. HOMOGRAPH - highest severity, check first
            for (variant in config.homographs) {
                if (hostLower.contains(variant.lowercase())) {
                    return DetectionResult(
                        score = SCORE_HOMOGRAPH,
                        match = brand,
                        details = BrandMatch(brand, MatchType.HOMOGRAPH, variant, config.category)
                    )
                }
            }

            // 3. TYPOSQUAT - character substitution
            for (variant in config.typosquats) {
                if (hostLower.contains(variant.lowercase())) {
                    return DetectionResult(
                        score = SCORE_TYPOSQUAT,
                        match = brand,
                        details = BrandMatch(brand, MatchType.TYPOSQUAT, variant, config.category)
                    )
                }
            }

            // 4. COMBOSQUAT - brand + keyword
            for (combo in config.combosquats) {
                if (hostLower.contains(combo.lowercase())) {
                    return DetectionResult(
                        score = SCORE_COMBOSQUAT,
                        match = brand,
                        details = BrandMatch(brand, MatchType.COMBO_SQUAT, combo, config.category)
                    )
                }
            }

            // 5. FUZZY MATCH - edit distance (expensive, check last)
            if (isFuzzyMatch(parsedHost, brand)) {
                return DetectionResult(
                    score = SCORE_FUZZY_MATCH,
                    match = brand,
                    details = BrandMatch(brand, MatchType.FUZZY_MATCH, brand, config.category)
                )
            }
        }

        return DetectionResult(score = 0, match = null, details = null)
    }

    /**
     * Check multiple URLs in batch.
     *
     * @param urls List of URLs to check
     * @return Map of URL to DetectionResult
     */
    fun detectBatch(urls: List<String>): Map<String, DetectionResult> {
        // SECURITY: Limit batch size
        val bounded = urls.take(100)
        return bounded.associateWith { detect(it) }
    }

    /**
     * True when [host] is an official domain of any listed brand, or a subdomain of one.
     * The match is on whole labels: "evilpaypal.com" is not "paypal.com".
     */
    private fun isOfficialDomain(host: String): Boolean {
        return ALL_OFFICIAL_DOMAINS.any { host == it || host.endsWith(".$it") }
    }

    /**
     * Check if the brand name appears in the labels to the left of the registrable domain.
     * Official domains are excluded before this is called.
     */
    private fun isBrandInSubdomain(parsedHost: PublicSuffixList.ParseResult, brand: String): Boolean {
        if (parsedHost.subdomains.isEmpty()) return false
        return parsedHost.subdomains.joinToString(".").contains(brand)
    }

    /**
     * Calculate Levenshtein distance for fuzzy matching.
     * Limited to short strings for performance.
     */
    private fun isFuzzyMatch(parsedHost: PublicSuffixList.ParseResult, brand: String): Boolean {
        // A host with a single label has no site name to compare
        if (!parsedHost.host.contains('.')) return false

        // Compare the site name only: the first label of the registrable domain
        val domain = parsedHost.registrableDomain.substringBefore('.')

        // Shorter brands tolerate fewer edits (see FUZZY_CHARS_PER_EDIT)
        val maxDistance = minOf(FUZZY_MATCH_THRESHOLD, brand.length / FUZZY_CHARS_PER_EDIT)

        // Skip if lengths are too different
        if (kotlin.math.abs(domain.length - brand.length) > maxDistance) {
            return false
        }

        // Calculate edit distance
        val distance = levenshteinDistance(domain, brand)
        return distance in 1..maxDistance
    }

    /**
     * Calculate Levenshtein edit distance.
     *
     * Bounded implementation to prevent DoS on very long strings.
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        // SECURITY: Limit input size
        val a = s1.take(50)
        val b = s2.take(50)

        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        // OPTIMIZATION: Always make 'a' the shorter string
        val (shorter, longer) = if (a.length <= b.length) a to b else b to a

        // OPTIMIZATION: Early exit if length difference exceeds threshold
        val lengthDiff = longer.length - shorter.length
        if (lengthDiff > 3) return lengthDiff

        // Single-row DP optimization: O(min(m,n)) space
        var previousRow = IntArray(shorter.length + 1) { it }
        var currentRow = IntArray(shorter.length + 1)

        for (i in 1..longer.length) {
            currentRow[0] = i

            for (j in 1..shorter.length) {
                val cost = if (longer[i - 1] == shorter[j - 1]) 0 else 1
                currentRow[j] = minOf(
                    currentRow[j - 1] + 1,      // insertion
                    previousRow[j] + 1,          // deletion
                    previousRow[j - 1] + cost    // substitution
                )
            }

            // Swap rows
            val temp = previousRow
            previousRow = currentRow
            currentRow = temp
        }

        return previousRow[shorter.length]
    }

    /**
     * Safely extract host from URL.
     * URL-decodes percent-encoded characters to catch obfuscation attacks.
     */
    private fun extractHost(url: String): String {
        val bounded = url.take(MAX_URL_LENGTH)

        val withoutProtocol = bounded
            .removePrefix("https://")
            .removePrefix("http://")

        val endIndex = withoutProtocol.indexOfFirst { it == '/' || it == '?' || it == '#' || it == ':' }

        val rawHost = when {
            endIndex > 0 -> withoutProtocol.substring(0, endIndex)
            else -> withoutProtocol
        }.take(MAX_HOST_LENGTH)
        
        // URL-decode to catch encoded obfuscation like %20 (space), %2E (dot)
        return decodePercentEncoding(rawHost)
    }
    
    /**
     * Decode percent-encoded characters in a string.
     * Handles common URL obfuscation techniques.
     */
    private fun decodePercentEncoding(input: String): String {
        return input.replace(Regex("%([0-9A-Fa-f]{2})")) { match ->
            val code = match.groupValues[1].toInt(16)
            if (code in 0x20..0x7E) { // Printable ASCII
                code.toChar().toString()
            } else {
                match.value // Keep non-printable as-is
            }
        }
    }
}
