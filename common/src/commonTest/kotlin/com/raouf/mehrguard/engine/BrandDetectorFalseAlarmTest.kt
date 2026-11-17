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
 * It also covers the rules that keep those fixes from opening a gap: a brand's
 * own site is exempt only from checks for that brand, hosts under shared hosting
 * suffixes are never exempt, and a brand's exact name on a domain it does not
 * own is reported.
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
    fun `a brand name under a shared hosting suffix is reported`() {
        val hosts = listOf(
            "paypal.cloudapp.azure.com",
            "paypal.azurewebsites.net",
            "paypal.sharepoint.com",
            "paypal.blogspot.com",
            "paypal.appspot.com",
            "paypal.web.app",
            "paypal.firebaseapp.com",
            "paypal.github.io",
            "paypal.herokuapp.com",
            "paypal.s3.amazonaws.com",
            "paypal.pages.dev",
            "paypal.workers.dev",
            "paypal.vercel.app",
            "paypal.netlify.app"
        )
        for (host in hosts) {
            assertEquals("paypal", detector.detect("https://$host/login").match, "Not reported: $host")
        }
    }

    @Test
    fun `a hosting suffix under an official domain does not exempt the brand that owns it`() {
        // azure.com is an official Microsoft domain, but anyone can register a
        // name under cloudapp.azure.com.
        assertEquals("microsoft", detector.detect("https://microsoft.cloudapp.azure.com/login").match)
    }

    @Test
    fun `the name of one brand on the official domain of another is reported`() {
        val result = detector.detect("https://paypal.google.com/login")

        assertEquals("paypal", result.match)
        assertEquals(BrandDetector.MatchType.EXACT_IN_SUBDOMAIN, result.details?.matchType)
    }

    @Test
    fun `the exact name of a brand on a domain it does not own is reported`() {
        val cases = mapOf(
            "https://commbank.co.uk/login" to "commbank",
            "https://westpac.tk" to "westpac",
            "https://paypal.co.nz" to "paypal",
            "https://paypal-security.com" to "paypal",
            "https://secure-paypal-verification.com/confirm" to "paypal",
            "https://microsoft-update.com" to "microsoft",
            "https://amazon-verify.com" to "amazon"
        )
        for ((url, brand) in cases) {
            val result = detector.detect(url)
            assertEquals(brand, result.match, "Wrong brand for $url")
            assertEquals(BrandDetector.MatchType.EXACT_NAME, result.details?.matchType, "Wrong match type for $url")
        }
    }

    @Test
    fun `a brand name inside a longer word is not an exact-name match`() {
        val sites = listOf(
            "https://www.pineapple.com",
            "https://www.snapple.com",
            "https://www.paypalobjects.com/checkout.js"
        )
        for (site in sites) {
            assertNull(detector.detect(site).match, "Reported a brand for $site")
        }
    }

    @Test
    fun `listed regional sites of a brand are not reported`() {
        val sites = listOf(
            "https://www.amazon.de/gp/cart",
            "https://www.google.de",
            "https://www.dhl.de",
            "https://discord.gg/abc",
            "https://www.medicare.gov"
        )
        for (site in sites) {
            assertNull(detector.detect(site).match, "Reported a brand for $site")
        }
    }

    @Test
    fun `an ordinary internationalised host is not reported`() {
        // xn--mnchen-3ya.de is the German city name with u-umlaut.
        assertNull(detector.detect("https://xn--mnchen-3ya.de").match)
    }
}
