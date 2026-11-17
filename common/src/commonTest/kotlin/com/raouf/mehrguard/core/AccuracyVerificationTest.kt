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
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Accuracy Verification Test
 *
 * Deterministic evaluation of detection accuracy against committed dataset.
 * Run with: `./gradlew :common:desktopTest --tests "*AccuracyVerificationTest*"`
 *
 * ## Purpose
 * Prints precision, recall and F1 on the 41 URLs listed in this file and fails
 * if they drop below the floors asserted further down. The URLs were written
 * by the project's author, so this is a regression guard and not a benchmark.
 * The larger committed set is measured by `DatasetEvaluationTest`.
 *
 * ## Metrics Calculated
 * - **Precision**: TP / (TP + FP) - How many flagged URLs are actually phishing
 * - **Recall**: TP / (TP + FN) - How many actual phishing URLs we catch
 * - **F1 Score**: Harmonic mean of precision and recall
 * - **Accuracy**: (TP + TN) / Total
 *
 * @since 1.2.0
 */
class AccuracyVerificationTest {

    private val engine = PhishingEngine()

    /** Multiplatform-compatible percent formatting */
    private fun formatPercent(value: Double): String {
        val percent = (value * 100 * 10).toInt() / 10.0
        return percent.toString()
    }

    // ==================== Committed Test Dataset ====================

    /**
     * Phishing-shaped URLs (ground truth: MALICIOUS).
     * Written by the author as examples of common phishing URL shapes; they are
     * not taken from a phishing feed.
     * All URLs are defanged for safety.
     */
    private val knownPhishingUrls = listOf(
        // Typosquatting
        "http://paypa1-secure.tk/login",
        "http://amaz0n-verify.ml/account",
        "http://micros0ft-login.ga/signin",
        "http://g00gle-security.cf/verify",
        "http://app1e-id.gq/unlock",

        // Brand + suspicious TLD
        "https://paypal-security.tk/verify",
        "https://amazon-account.ml/update",
        "https://netflix-billing.ga/payment",
        "https://facebook-secure.cf/login",
        "https://instagram-verify.gq/account",

        // IP address phishing
        "http://192.168.1.1/paypal/login",
        "http://10.0.0.1/amazon/signin",
        "http://172.16.0.1/banking/verify",

        // Credential harvesting paths
        "https://secure-login.tk/verify-account/update-password",
        "https://account-verify.ml/confirm-identity/signin",

        // URL shortener abuse
        "https://bit.ly/secure-paypal-login",
        "https://tinyurl.com/verify-amazon",

        // Excessive subdomains
        "http://secure.login.verify.paypal.account.tk/signin",
        "http://update.password.account.amazon.verify.ml/login",

        // @ symbol injection
        "http://paypal.com@evil.tk/login",
        "http://amazon.com@phish.ml/signin"
    )

    /**
     * Known legitimate URLs (ground truth: SAFE).
     * Verified safe domains from major organizations.
     */
    private val knownLegitimateUrls = listOf(
        "https://www.google.com",
        "https://www.amazon.com",
        "https://www.paypal.com",
        "https://www.microsoft.com",
        "https://www.apple.com",
        "https://www.facebook.com",
        "https://www.github.com",
        "https://www.stackoverflow.com",
        "https://www.wikipedia.org",
        "https://www.bbc.com",
        "https://www.nytimes.com",
        "https://www.reddit.com",
        "https://www.linkedin.com",
        "https://www.twitter.com",
        "https://www.netflix.com",
        "https://www.spotify.com",
        "https://www.dropbox.com",
        "https://www.slack.com",
        "https://www.zoom.us",
        "https://www.adobe.com"
    )

    // ==================== Metric Calculations ====================

    data class Metrics(
        val truePositives: Int,
        val falsePositives: Int,
        val trueNegatives: Int,
        val falseNegatives: Int
    ) {
        val total: Int = truePositives + falsePositives + trueNegatives + falseNegatives

        val precision: Double = if (truePositives + falsePositives > 0) {
            truePositives.toDouble() / (truePositives + falsePositives)
        } else 0.0

        val recall: Double = if (truePositives + falseNegatives > 0) {
            truePositives.toDouble() / (truePositives + falseNegatives)
        } else 0.0

        val f1Score: Double = if (precision + recall > 0) {
            2 * (precision * recall) / (precision + recall)
        } else 0.0

        val accuracy: Double = if (total > 0) {
            (truePositives + trueNegatives).toDouble() / total
        } else 0.0
    }

    private fun calculateMetrics(): Metrics {
        var tp = 0  // Correctly identified phishing
        var fp = 0  // Incorrectly flagged legitimate
        var tn = 0  // Correctly identified legitimate
        var fn = 0  // Missed phishing

        // Test phishing URLs (expecting MALICIOUS or SUSPICIOUS)
        knownPhishingUrls.forEach { url ->
            val result = engine.analyzeBlocking(url)
            if (result.verdict == Verdict.MALICIOUS || result.verdict == Verdict.SUSPICIOUS) {
                tp++
            } else {
                fn++
            }
        }

        // Test legitimate URLs (expecting SAFE)
        knownLegitimateUrls.forEach { url ->
            val result = engine.analyzeBlocking(url)
            if (result.verdict == Verdict.SAFE) {
                tn++
            } else {
                fp++
            }
        }

        return Metrics(tp, fp, tn, fn)
    }

    // ==================== Verification Tests ====================

    @Test
    fun `VERIFY precision is above 80 percent`() {
        val metrics = calculateMetrics()
        println("📊 Precision: ${(metrics.precision * 100).toInt()}%")
        assertTrue(
            metrics.precision >= 0.80,
            "Precision ${metrics.precision} is below 80% threshold"
        )
    }

    @Test
    fun `VERIFY recall is above 85 percent`() {
        val metrics = calculateMetrics()
        println("📊 Recall: ${(metrics.recall * 100).toInt()}%")
        assertTrue(
            metrics.recall >= 0.85,
            "Recall ${metrics.recall} is below 85% threshold"
        )
    }

    @Test
    fun `VERIFY F1 score is above 80 percent`() {
        val metrics = calculateMetrics()
        println("📊 F1 Score: ${(metrics.f1Score * 100).toInt()}%")
        assertTrue(
            metrics.f1Score >= 0.80,
            "F1 Score ${metrics.f1Score} is below 80% threshold"
        )
    }

    @Test
    fun `VERIFY overall accuracy is above 85 percent`() {
        val metrics = calculateMetrics()
        println("📊 Accuracy: ${(metrics.accuracy * 100).toInt()}%")
        assertTrue(
            metrics.accuracy >= 0.85,
            "Accuracy ${metrics.accuracy} is below 85% threshold"
        )
    }

    @Test
    fun `VERIFY complete metrics summary`() {
        val metrics = calculateMetrics()

        println("""
            |
            |╔══════════════════════════════════════════════════════════════╗
            |║           Mehr Guard ACCURACY VERIFICATION REPORT            ║
            |╠══════════════════════════════════════════════════════════════╣
            |║  Dataset Size: ${metrics.total} URLs                                      ║
            |║  - Known Phishing: ${knownPhishingUrls.size}                                      ║
            |║  - Known Legitimate: ${knownLegitimateUrls.size}                                    ║
            |╠══════════════════════════════════════════════════════════════╣
            |║  CONFUSION MATRIX:                                          ║
            |║  ┌─────────────┬─────────────┬─────────────┐                ║
            |║  │             │ Pred PHISH  │ Pred SAFE   │                ║
            |║  ├─────────────┼─────────────┼─────────────┤                ║
            |║  │ Actual PHISH│ TP: ${metrics.truePositives.toString().padEnd(7)}│ FN: ${metrics.falseNegatives.toString().padEnd(7)}│                ║
            |║  │ Actual SAFE │ FP: ${metrics.falsePositives.toString().padEnd(7)}│ TN: ${metrics.trueNegatives.toString().padEnd(7)}│                ║
            |║  └─────────────┴─────────────┴─────────────┘                ║
            |╠══════════════════════════════════════════════════════════════╣
            |║  METRICS:                                                   ║
            |║  • Precision: ${formatPercent(metrics.precision)}%                                          ║
            |║  • Recall:    ${formatPercent(metrics.recall)}%                                          ║
            |║  • F1 Score:  ${formatPercent(metrics.f1Score)}%                                          ║
            |║  • Accuracy:  ${formatPercent(metrics.accuracy)}%                                          ║
            |╚══════════════════════════════════════════════════════════════╝
            |
        """.trimMargin())

        // All thresholds must pass
        assertTrue(metrics.precision >= 0.80, "Precision below threshold")
        assertTrue(metrics.recall >= 0.85, "Recall below threshold")
        assertTrue(metrics.f1Score >= 0.80, "F1 below threshold")
        assertTrue(metrics.accuracy >= 0.85, "Accuracy below threshold")
    }

    // ==================== Determinism Verification ====================

    @Test
    fun `VERIFY results are deterministic across runs`() {
        val firstRun = knownPhishingUrls.map { url ->
            val result = engine.analyzeBlocking(url)
            url to result.score
        }

        val secondRun = knownPhishingUrls.map { url ->
            val result = engine.analyzeBlocking(url)
            url to result.score
        }

        firstRun.zip(secondRun).forEach { (first, second) ->
            assertEquals(
                first.second, second.second,
                "Non-deterministic result for ${first.first}"
            )
        }

        println("✅ All ${knownPhishingUrls.size} URLs produced deterministic scores")
    }
}
