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

package com.raouf.mehrguard.core

import com.raouf.mehrguard.model.Verdict
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.measureTime
import kotlin.time.TimeSource

/**
 * Sanity checks that analysis is plain local computation.
 *
 * What these tests check:
 * 1. Analysis returns a bounded score for every kind of input, including malformed ones.
 * 2. Each component (heuristics, brand detector, TLD scorer, scoring model,
 *    homograph detector) runs when constructed on its own, with no setup.
 * 3. Analysing the same URL repeatedly gives the same result.
 * 4. Analysis stays within a generous time bound.
 *
 * What they do not check: network activity. A unit test cannot observe sockets
 * or DNS lookups. The evidence that analysis makes no network calls is the
 * source code, which `AnalysisCodeHasNoNetworkApiTest` scans for networking APIs.
 *
 * @since 1.2.0
 */
class LocalAnalysisSanityTest {

    private val engine = PhishingEngine()
    private val timeSource = TimeSource.Monotonic

    /** Multiplatform-compatible percent formatting */
    private fun formatDouble(value: Double, decimals: Int = 1): String {
        val multiplier = if (decimals == 1) 10.0 else if (decimals == 2) 100.0 else 1.0
        return ((value * multiplier).toInt() / multiplier).toString()
    }

    // ==================== Analysis completes ====================

    @Test
    fun `analysis completes for every URL pattern within the time bound`() {
        val testUrls = listOf(
            // Standard URLs
            "https://google.com",
            "http://evil.tk/phish",

            // IP addresses
            "http://192.168.1.1/login",
            "http://10.0.0.1:8080/admin",
            "http://[::1]/test",

            // Punycode
            "https://xn--80ak6aa.com/test",

            // Very long URLs
            "https://example.com/" + "a".repeat(500),

            // Unicode domains
            "https://gооgle.com",  // Cyrillic o
            "https://раураl.com",  // Cyrillic

            // Complex redirect patterns
            "https://redirect.com?url=https://evil.com?next=https://phish.tk",

            // Data URIs
            "data:text/html,<script>alert(1)</script>",

            // Edge cases
            "",
            "invalid",
            "http://",
            "https://"
        )

        val mark = timeSource.markNow()

        testUrls.forEach { url ->
            val result = engine.analyzeBlocking(url)
            assertNotNull(result, "Result should not be null for: $url")
            assertTrue(result.score in 0..100, "Score out of bounds for: $url")
        }

        val elapsed = mark.elapsedNow().inWholeMilliseconds

        println("""
            |
            |LOCAL ANALYSIS SANITY CHECK
            |   URLs analyzed: ${testUrls.size}
            |   Total time: ${elapsed}ms
            |   Average per URL: ${elapsed / testUrls.size}ms
            |   
            |   This is a time bound, not a network monitor.
            |
        """.trimMargin())

        // A generous bound for local computation
        assertTrue(
            elapsed / testUrls.size < 500,
            "Analysis too slow for local computation (${elapsed / testUrls.size}ms/URL)"
        )
    }

    @Test
    fun `heuristics engine runs standalone`() {
        val heuristics = com.raouf.mehrguard.engine.HeuristicsEngine()

        val result = heuristics.analyze("http://paypal-secure.tk/login")

        assertNotNull(result)
        assertTrue(result.score >= 0, "Heuristic score should be non-negative")
        assertTrue(result.flags.isNotEmpty(), "Should report at least one flag")

        println("Heuristics engine: ${result.flags.size} flags")
    }

    @Test
    fun `brand detector runs standalone`() {
        val detector = com.raouf.mehrguard.engine.BrandDetector()

        val result = detector.detect("http://paypa1-secure.tk/login")

        assertNotNull(result)
        // The detector returns a result; what it matches is covered by BrandDetectorTest
        assertTrue(
            result.match != null || result.score >= 0,
            "Brand detector should return a result"
        )

        println("Brand detector: ${result.match ?: "No match"} (score: ${result.score})")
    }

    @Test
    fun `TLD scorer runs standalone`() {
        val scorer = com.raouf.mehrguard.engine.TldScorer()

        // Test with full URL format
        val highRiskUrls = listOf(
            "http://example.tk/path",
            "http://example.ml/path",
            "http://example.ga/path"
        )
        val lowRiskUrls = listOf(
            "https://example.com",
            "https://example.org",
            "https://example.edu"
        )

        highRiskUrls.forEach { url ->
            val result = scorer.score(url)
            assertTrue(result.score > 0, "High-risk TLD in $url should have score > 0")
        }

        lowRiskUrls.forEach { url ->
            val result = scorer.score(url)
            assertTrue(result.score <= 35, "Low-risk TLD in $url should have score <= 35")
        }

        println("TLD scorer: all TLDs scored")
    }

    @Test
    fun `scoring model runs standalone`() {
        val model = com.raouf.mehrguard.ml.LogisticRegressionModel.default()
        val extractor = com.raouf.mehrguard.ml.FeatureExtractor()

        val features = extractor.extract("http://paypal-secure.tk/login/verify")
        val score = model.predict(features)

        assertTrue(score in 0.0f..1.0f, "ML score should be between 0 and 1")

        println("Scoring model: prediction score = ${formatDouble(score.toDouble(), 2)}")
    }

    @Test
    fun `homograph detector runs standalone`() {
        val detector = com.raouf.mehrguard.engine.HomographDetector()

        // Cyrillic 'a' looks like Latin 'a'
        val result = detector.detect("gооgle.com")  // Cyrillic o's

        assertTrue(result.isHomograph, "Should detect the homograph")
        assertTrue(result.detectedCharacters.isNotEmpty(), "Should find confusable characters")

        println("Homograph detector: ${result.detectedCharacters.size} confusables found")
    }

    // ==================== Consistency Verification ====================

    @Test
    fun `repeated analysis of one URL gives identical results`() {
        val url = "http://paypal-secure.tk/verify-account/login"

        // Analyze same URL 100 times
        val results = (1..100).map { engine.analyzeBlocking(url) }

        val firstScore = results.first().score
        val firstVerdict = results.first().verdict

        results.forEachIndexed { index, result ->
            assertTrue(
                result.score == firstScore,
                "Inconsistent score at iteration $index: expected $firstScore, got ${result.score}"
            )
            assertTrue(
                result.verdict == firstVerdict,
                "Inconsistent verdict at iteration $index"
            )
        }

        println("Consistency: 100 iterations produced identical results")
    }

    // ==================== Timing Analysis ====================

    @Test
    fun `repeated analysis stays within the local time bound`() {
        val url = "http://suspicious-domain.tk/phishing/login"

        // Warm up
        repeat(10) { engine.analyzeBlocking(url) }

        // Measure
        val times = mutableListOf<Long>()
        repeat(50) {
            val mark = timeSource.markNow()
            engine.analyzeBlocking(url)
            times.add(mark.elapsedNow().inWholeMilliseconds)
        }

        val avgTime = times.average()
        val maxTime = times.maxOrNull() ?: 0
        val minTime = times.minOrNull() ?: 0

        println("""
            |
            |📊 TIMING ANALYSIS (50 iterations)
            |   Average: ${formatDouble(avgTime)}ms
            |   Min: ${minTime}ms
            |   Max: ${maxTime}ms
            |   
            |   A time bound only; this test cannot observe network activity.
            |
        """.trimMargin())

        // Generous bounds for local computation
        assertTrue(maxTime < 1000, "Max time too high for local computation")
        assertTrue(avgTime < 100, "Average time too high for local computation")
    }
}
