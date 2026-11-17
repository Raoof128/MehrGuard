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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Regression tests for three defects found in November 2026, when all four
 * Australian bank sites in `data/test_urls.csv` were reported as suspicious:
 *
 * 1. The site name was taken to be the second-to-last label of the host. For
 *    `www.commbank.com.au` that label is `com`, which is within two edits of the
 *    brand `zoom`, so every site under `.com.au`, `.co.uk` and similar suffixes
 *    was reported as impersonating a brand.
 * 2. A brand's own site could be reported as a misspelling of another brand
 *    (`spotify.com` against `shopify`).
 * 3. Two edits were allowed whatever the length of the brand, so three- and
 *    four-letter names matched almost anything (`cnn` against `anz`).
 *
 * Most of the hosts below are not in the evaluation dataset.
 */
class BrandDetectorFalseAlarmTest {

    private val detector = BrandDetector()

    @Test
    fun `sites under a two-label public suffix are not reported`() {
        val sites = listOf(
            "https://www.commbank.com.au",
            "https://www.nab.com.au",
            "https://www.westpac.com.au",
            "https://www.anz.com.au",
            "https://www.woolworths.com.au",
            "https://www.abc.net.au",
            "https://www.bbc.co.uk/news",
            "https://www.cam.ac.uk",
            "https://www.sony.co.jp"
        )
        for (site in sites) {
            assertNull(detector.detect(site).match, "Reported a brand for $site")
        }
    }

    @Test
    fun `the own site of a listed brand is not reported as another brand`() {
        val sites = listOf(
            "https://www.spotify.com",
            "https://www.shopify.com",
            "https://www.irs.gov",
            "https://mail.yahoo.com",
            "https://accounts.google.com/signin"
        )
        for (site in sites) {
            assertNull(detector.detect(site).match, "Reported a brand for $site")
        }
    }

    @Test
    fun `short unrelated names are not fuzzy matches`() {
        val sites = listOf(
            "https://www.cnn.com",
            "https://www.bbc.com",
            "https://www.etsy.com",
            "https://www.nba.com",
            "https://www.ibm.com",
            "https://t.co/abc123"
        )
        for (site in sites) {
            assertNull(detector.detect(site).match, "Reported a brand for $site")
        }
    }

    @Test
    fun `one edit from a short brand is still a fuzzy match`() {
        val result = detector.detect("https://nabb.com.au/login")

        assertEquals("nab", result.match)
        assertEquals(BrandDetector.MatchType.FUZZY_MATCH, result.details?.matchType)
    }

    @Test
    fun `two edits from a long brand are still a fuzzy match`() {
        assertEquals("spotify", detector.detect("https://spotifi.com/login").match)
        assertEquals("linkedin", detector.detect("https://linkedim.com").match)
    }

    @Test
    fun `listed patterns are still found under a two-label public suffix`() {
        assertEquals("commbank", detector.detect("https://cornmbank.com.au/netbank").match)
        assertEquals("amazon", detector.detect("https://amazom.com.au").match)
        assertEquals("google", detector.detect("https://gooogle.co.uk").match)
    }

    @Test
    fun `a brand name used as a subdomain is still found under a two-label public suffix`() {
        val result = detector.detect("https://commbank.evil.com.au/login")

        assertEquals("commbank", result.match)
        assertEquals(BrandDetector.MatchType.EXACT_IN_SUBDOMAIN, result.details?.matchType)
    }

    @Test
    fun `a host that only ends with the text of an official domain is not treated as official`() {
        // "evilpaypal.com" ends with the characters "paypal.com" but is a different domain.
        val result = detector.detect("https://paypal.evilpaypal.com/login")

        assertEquals("paypal", result.match)
    }

    @Test
    fun `an internationalised host in ASCII form is checked against the homograph list`() {
        // xn--pypal-4ve.com is "paypal.com" with a Cyrillic "a" as the second letter.
        val result = detector.detect("https://xn--pypal-4ve.com/login")

        assertEquals("paypal", result.match)
        assertEquals(BrandDetector.MatchType.HOMOGRAPH, result.details?.matchType)
    }

    @Test
    fun `an ordinary internationalised host is not reported`() {
        // xn--mnchen-3ya.de is the German city name with u-umlaut.
        assertNull(detector.detect("https://xn--mnchen-3ya.de").match)
    }
}
