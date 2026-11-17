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

import com.raouf.mehrguard.engine.DynamicBrandDiscovery
import com.raouf.mehrguard.engine.HeuristicsEngine
import com.raouf.mehrguard.model.Verdict
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Checks that a host is split on its public suffix everywhere in the analysis
 * path, so that in `www.commbank.com.au` the site is `commbank.com.au` and the
 * only subdomain is `www`. Before November 2026 three components took the last
 * two labels as the site, which turned `commbank` into a "subdomain" of `com.au`.
 */
class PublicSuffixHostSplitTest {

    @Test
    fun `the URL parser splits a com-au host on its public suffix`() {
        val parsed = assertNotNull(UrlAnalyzer().parse("https://www.commbank.com.au/netbank"))

        assertEquals("commbank.com.au", parsed.domain)
        assertEquals(listOf("www"), parsed.subdomains)
        assertEquals("au", parsed.tld)
    }

    @Test
    fun `the URL parser still splits a single-label suffix host as before`() {
        val parsed = assertNotNull(UrlAnalyzer().parse("https://login.secure.example.com/path"))

        assertEquals("example.com", parsed.domain)
        assertEquals(listOf("login", "secure"), parsed.subdomains)
        assertEquals("com", parsed.tld)
    }

    @Test
    fun `a site name under com-au is not treated as a brand-like subdomain`() {
        val result = DynamicBrandDiscovery.analyze("https://www.woolworths.com.au")

        assertTrue(result.findings.isEmpty(), "Unexpected findings: ${result.findings}")
    }

    @Test
    fun `a brand-like subdomain is still found under com-au`() {
        val result = DynamicBrandDiscovery.analyze("https://woolworths.rewards-centre.com.au")

        assertTrue(
            result.findings.any { it.type == DynamicBrandDiscovery.FindingType.BRAND_LIKE_SUBDOMAIN },
            "Expected a brand-like subdomain finding, got: ${result.findings}"
        )
    }

    @Test
    fun `a co-uk suffix is not counted as two top-level domains`() {
        val result = HeuristicsEngine().analyze("https://www.bbc.co.uk/news")

        assertFalse("MULTIPLE_TLD_SEGMENTS" in result.details, "Signals: ${result.details}")
    }

    @Test
    fun `a domain name embedded before a co-uk suffix is still counted`() {
        val result = HeuristicsEngine().analyze("https://paypal.com.account-check.co.uk/login")

        assertTrue("MULTIPLE_TLD_SEGMENTS" in result.details, "Signals: ${result.details}")
    }

    @Test
    fun `legitimate sites under two-label public suffixes are rated SAFE`() {
        val engine = PhishingEngine()
        val sites = listOf(
            "https://www.commbank.com.au",
            "https://www.nab.com.au",
            "https://www.westpac.com.au",
            "https://www.anz.com.au",
            "https://www.woolworths.com.au",
            "https://www.bbc.co.uk"
        )
        for (site in sites) {
            val assessment = engine.analyzeBlocking(site)
            assertEquals(
                Verdict.SAFE,
                assessment.verdict,
                "$site: score=${assessment.score} signals=${assessment.flags}"
            )
        }
    }

    @Test
    fun `an internationalised PayPal lookalike in ASCII form is rated MALICIOUS`() {
        val assessment = PhishingEngine().analyzeBlocking("https://xn--pypal-4ve.com/login")

        assertEquals(
            Verdict.MALICIOUS,
            assessment.verdict,
            "score=${assessment.score} signals=${assessment.flags}"
        )
    }
}
