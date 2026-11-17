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
 * Checks the Punycode decoder against RFC 3492 sample strings and known host names.
 * Expected values are written as escapes so the non-ASCII letters are unambiguous.
 */
class PunycodeTest {

    @Test
    fun `decodes a label with one non-ASCII letter`() {
        assertEquals("m\u00FCnchen", Punycode.decode("mnchen-3ya"))
        assertEquals("b\u00FCcher", Punycode.decode("bcher-kva"))
    }

    @Test
    fun `decodes a label made only of non-ASCII letters`() {
        // The well-known all-Cyrillic lookalike of "apple".
        assertEquals("\u0430\u0440\u0440\u04CF\u0435", Punycode.decode("80ak6aa92e"))
    }

    @Test
    fun `decodes RFC 3492 sample strings`() {
        // Section 7.1 (A) Arabic and (B) Chinese (simplified).
        assertEquals(
            "\u0644\u064A\u0647\u0645\u0627\u0628\u062A\u0643\u0644\u0645\u0648\u0634\u0639\u0631\u0628\u064A\u061F",
            Punycode.decode("egbpdaj6bu4bxfgehfvwxn")
        )
        assertEquals(
            "\u4ED6\u4EEC\u4E3A\u4EC0\u4E48\u4E0D\u8BF4\u4E2D\u6587",
            Punycode.decode("ihqwcrb4cv8a8dqg056pqjye")
        )
    }

    @Test
    fun `keeps the ASCII part of a mixed label in place`() {
        // Latin "p", Cyrillic "a" (U+0430), then Latin "ypal".
        assertEquals("p\u0430ypal", Punycode.decode("pypal-4ve"))
    }

    @Test
    fun `rejects input that is not Punycode`() {
        assertNull(Punycode.decode("abc!"))
        assertNull(Punycode.decode("-abc"))
        assertNull(Punycode.decode("99999999999999999999"))
    }

    @Test
    fun `decodes only the encoded labels of a host`() {
        assertEquals("www.m\u00FCnchen.de", Punycode.toUnicodeHost("www.xn--mnchen-3ya.de"))
        assertEquals("p\u0430ypal.com", Punycode.toUnicodeHost("XN--pypal-4ve.com"))
    }

    @Test
    fun `leaves ordinary and malformed hosts unchanged`() {
        assertEquals("www.example.com", Punycode.toUnicodeHost("www.example.com"))
        assertEquals("xn--abc!.com", Punycode.toUnicodeHost("xn--abc!.com"))
        assertEquals("xn--.com", Punycode.toUnicodeHost("xn--.com"))
    }
}
