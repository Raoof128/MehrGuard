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
 * Hosts are split on their public suffix ([PublicSuffixList]) and internationalised
 * hosts are decoded first ([Punycode]). A host on a brand's official domain is not
 * checked against that brand; it is still checked against the others. Hosts under
 * shared hosting suffixes are never treated as official.
 *
 * SECURITY NOTES:
 * - All inputs are bounded to prevent DoS
 * - Brand database is immutable
 * - Thread-safe for concurrent detection
 * - Includes Australian banking institutions
 *
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

        /**
         * Hosting services under which anyone can register a name. A host under one
         * of these is never treated as a brand's own site, even where the suffix sits
         * under an official domain (cloudapp.azure.com under azure.com).
         */
        private val USER_CONTENT_SUFFIXES = setOf(
            "cloudapp.azure.com", "azurewebsites.net", "sharepoint.com",
            "blogspot.com", "appspot.com", "web.app", "firebaseapp.com",
            "github.io", "herokuapp.com", "amazonaws.com",
            "pages.dev", "workers.dev", "vercel.app", "netlify.app"
        )

        // === SCORING ===

        const val SCORE_EXACT_SUBDOMAIN = 30
        const val SCORE_TYPOSQUAT = 35
        const val SCORE_HOMOGRAPH = 40
        const val SCORE_COMBOSQUAT = 25
        const val SCORE_FUZZY_MATCH = 20
        const val SCORE_EXACT_NAME = 30
    }


    /**
     * Match type classification.
     */
    enum class MatchType {
        EXACT_IN_SUBDOMAIN,
        TYPOSQUAT,
        HOMOGRAPH,
        COMBO_SQUAT,
        FUZZY_MATCH,

        /** The brand's name is the site name, or a hyphen-separated part of it, on a domain the brand does not own */
        EXACT_NAME
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

        // Decode xn-- labels first: the homograph patterns are Unicode text, and
        // links normally carry an internationalised host in its ASCII form.
        val hostLower = Punycode.toUnicodeHost(host.lowercase()).lowercase()

        // Split the host on its public suffix, so that in "www.example.com.au"
        // the site name is "example" and not "com".
        val parsedHost = PublicSuffixList.default.parse(hostLower)

        // Anyone can register a name under a hosting service, so no host there
        // counts as a brand's own site.
        val userContentHost = USER_CONTENT_SUFFIXES.any { isOnDomain(hostLower, it) }

        // Whether the host is the official site of some listed brand. Such a site
        // name is a real brand's name, so it is not also a misspelling of another
        // brand (spotify.com is not a typo of shopify). Only the fuzzy check is
        // skipped; every other check still runs for the other brands.
        val onListedOfficialSite = !userContentHost &&
            BrandDatabase.brands.values.any { isOfficialFor(it, hostLower) }

        for ((brand, config) in BrandDatabase.brands) {
            // A brand's own site does not impersonate that brand. It is still
            // checked against every other brand.
            if (!userContentHost && isOfficialFor(config, hostLower)) continue

            val match = matchBrand(brand, config, hostLower, parsedHost, allowFuzzy = !onListedOfficialSite)
            if (match != null) return match
        }

        return DetectionResult(score = 0, match = null, details = null)
    }

    /**
     * Check one brand against the host, strongest evidence first.
     *
     * 1. the brand name in a subdomain
     * 2. a listed homograph, 3. a listed typosquat, 4. a listed brand-plus-keyword name
     * 5. the brand name as the site name, or as one hyphen-separated part of it
     *    (paypal-security.com)
     * 6. a site name within a small edit distance of the brand
     */
    private fun matchBrand(
        brand: String,
        config: BrandDatabase.BrandConfig,
        hostLower: String,
        parsedHost: PublicSuffixList.ParseResult,
        allowFuzzy: Boolean
    ): DetectionResult? {
        val siteName = parsedHost.registrableDomain.substringBefore('.')
        val homograph = config.homographs.firstOrNull { hostLower.contains(it.lowercase()) }
        val typosquat = config.typosquats.firstOrNull { hostLower.contains(it.lowercase()) }
        val combosquat = config.combosquats.firstOrNull { hostLower.contains(it.lowercase()) }

        val found: Pair<MatchType, String>? = when {
            isBrandInSubdomain(parsedHost, brand) -> MatchType.EXACT_IN_SUBDOMAIN to brand
            homograph != null -> MatchType.HOMOGRAPH to homograph
            typosquat != null -> MatchType.TYPOSQUAT to typosquat
            combosquat != null -> MatchType.COMBO_SQUAT to combosquat
            hostLower.contains('.') && isBrandName(siteName, brand) -> MatchType.EXACT_NAME to siteName
            allowFuzzy && isFuzzyMatch(parsedHost, brand) -> MatchType.FUZZY_MATCH to brand
            else -> null
        }

        return found?.let { (type, pattern) ->
            DetectionResult(
                score = scoreFor(type),
                match = brand,
                details = BrandMatch(brand, type, pattern, config.category)
            )
        }
    }

    private fun scoreFor(type: MatchType): Int = when (type) {
        MatchType.EXACT_IN_SUBDOMAIN -> SCORE_EXACT_SUBDOMAIN
        MatchType.HOMOGRAPH -> SCORE_HOMOGRAPH
        MatchType.TYPOSQUAT -> SCORE_TYPOSQUAT
        MatchType.COMBO_SQUAT -> SCORE_COMBOSQUAT
        MatchType.EXACT_NAME -> SCORE_EXACT_NAME
        MatchType.FUZZY_MATCH -> SCORE_FUZZY_MATCH
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
     * True when [host] is [domain] or a subdomain of it. The match is on whole
     * labels: "evilpaypal.com" is not on "paypal.com".
     */
    private fun isOnDomain(host: String, domain: String): Boolean {
        return host == domain || host.endsWith(".$domain")
    }

    /**
     * True when [host] is on one of the brand's official domains.
     *
     * Official entries are matched as whole-label suffixes and not by registrable
     * domain, because some entries are narrower than a registrable domain
     * (weixin.qq.com) and some are public suffixes themselves (gov.uk).
     */
    private fun isOfficialFor(config: BrandDatabase.BrandConfig, host: String): Boolean {
        return config.officialDomains.any { isOnDomain(host, it.lowercase()) }
    }

    /**
     * True when [brand] is the whole site name or one hyphen-separated part of it:
     * "paypal" in "paypal" and in "secure-paypal-login", but not in "paypalobjects".
     */
    private fun isBrandName(siteName: String, brand: String): Boolean {
        return siteName == brand || siteName.split('-').any { it == brand }
    }

    /**
     * Check if the brand name appears in the labels to the left of the registrable domain.
     * The brand's own official domains are excluded before this is called.
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
