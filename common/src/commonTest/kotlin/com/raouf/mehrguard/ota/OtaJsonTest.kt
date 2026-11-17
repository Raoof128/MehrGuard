/*
 * Copyright 2025-2026 Mehr Guard Contributors
 * Licensed under the Apache License, Version 2.0
 */

package com.raouf.mehrguard.ota

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Tests for the strict JSON parser used on downloaded OTA files. */
class OtaJsonTest {

    private fun rejects(text: String) {
        assertFailsWith<OtaJsonException>("should reject: $text") { OtaJson.parse(text) }
    }

    @Test
    fun `parses objects arrays and scalars`() {
        val parsed = OtaJson.parse("""{"a":1,"b":[true,false,null,"x"],"c":{"d":-2.5e1},"e":""}""") as Map<*, *>
        assertEquals(1L, parsed["a"])
        assertEquals(listOf(true, false, null, "x"), parsed["b"])
        assertEquals(-25.0, (parsed["c"] as Map<*, *>)["d"])
        assertEquals("", parsed["e"])
    }

    @Test
    fun `parses string escapes`() {
        val parsed = OtaJson.parse("\"a\\n\\t\\\"\\\\\\/\\u0041\\u00e9\"")
        assertEquals("a\n\t\"\\/A\u00e9", parsed)
    }

    @Test
    fun `accepts whitespace around values`() {
        assertEquals(listOf(1L, 2L), OtaJson.parse(" \n\t[ 1 , 2 ]\r\n"))
    }

    @Test
    fun `rejects truncated input`() {
        rejects("""{"a":1""")
        rejects("""[1,2""")
        rejects("\"abc")
        rejects("")
    }

    @Test
    fun `rejects trailing content`() {
        rejects("""{"a":1} x""")
        rejects("""{"a":1}{"b":2}""")
    }

    @Test
    fun `rejects non json syntax`() {
        rejects("{a:1}")
        rejects("{'a':1}")
        rejects("""{"a":1,}""")
        rejects("[1,]")
        rejects("""{"a" 1}""")
        rejects("nul")
        rejects("True")
        rejects("<html><body>404</body></html>")
    }

    @Test
    fun `rejects invalid numbers`() {
        rejects("01")
        rejects("1.")
        rejects("-")
        rejects("1e")
        rejects("+1")
        rejects("99999999999999999999")
    }

    @Test
    fun `rejects duplicate keys`() {
        rejects("""{"a":1,"a":2}""")
    }

    @Test
    fun `rejects bad escapes and control characters`() {
        rejects("\"\\x\"")
        rejects("\"\\u12\"")
        rejects("\"\\u+123\"")
        rejects("\"a\nb\"")
    }

    @Test
    fun `rejects excessive nesting`() {
        val deep = "[".repeat(OtaJson.MAX_DEPTH + 2) + "]".repeat(OtaJson.MAX_DEPTH + 2)
        rejects(deep)
        val ok = "[".repeat(OtaJson.MAX_DEPTH) + "]".repeat(OtaJson.MAX_DEPTH)
        assertTrue(OtaJson.parse(ok) is List<*>)
    }

    @Test
    fun `null literal parses to null`() {
        assertNull(OtaJson.parse("null"))
    }
}
