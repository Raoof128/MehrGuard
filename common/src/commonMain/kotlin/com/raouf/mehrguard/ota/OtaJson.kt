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

package com.raouf.mehrguard.ota

/** Thrown by [OtaJson.parse] when the text is not well-formed JSON. */
internal class OtaJsonException(message: String) : Exception(message)

/**
 * Small strict JSON parser (RFC 8259) used to check downloaded OTA files.
 *
 * The common module has no JSON library, and the earlier regex extraction accepted
 * any text that happened to contain the right substrings. This parser rejects
 * trailing content, duplicate object keys, control characters in strings, numbers
 * that are not valid JSON, and nesting deeper than [MAX_DEPTH].
 *
 * Result types: `Map<String, Any?>`, `List<Any?>`, `String`, `Long` (integers),
 * `Double` (numbers with a fraction or exponent), `Boolean` and `null`.
 */
internal object OtaJson {

    const val MAX_DEPTH = 16

    private const val WHITESPACE = " \t\n\r"
    private const val UNICODE_DIGITS = 4
    private const val HEX_RADIX = 16

    /** Parse [text]. @throws OtaJsonException if it is not well-formed. */
    fun parse(text: String): Any? {
        val parser = Parser(text)
        parser.skipWhitespace()
        val value = parser.readValue(0)
        parser.skipWhitespace()
        if (!parser.atEnd()) parser.fail("unexpected content after the JSON value")
        return value
    }

    private class Parser(private val s: String) {
        private var i = 0

        fun atEnd(): Boolean = i >= s.length

        fun fail(message: String): Nothing = throw OtaJsonException("$message at position $i")

        fun skipWhitespace() {
            while (!atEnd() && s[i] in WHITESPACE) i++
        }

        fun readValue(depth: Int): Any? {
            if (depth > MAX_DEPTH) fail("nesting deeper than $MAX_DEPTH")
            if (atEnd()) fail("unexpected end of input")
            return when (val c = s[i]) {
                '{' -> readObject(depth)
                '[' -> readArray(depth)
                '"' -> readString()
                't' -> readLiteral("true", true)
                'f' -> readLiteral("false", false)
                'n' -> readLiteral("null", null)
                '-', in '0'..'9' -> readNumber()
                else -> fail("unexpected character '$c'")
            }
        }

        private fun readLiteral(word: String, value: Any?): Any? {
            if (!s.startsWith(word, i)) fail("invalid literal")
            i += word.length
            return value
        }

        private fun expect(c: Char) {
            if (atEnd() || s[i] != c) fail("expected '$c'")
            i++
        }

        /** After a member or element: consume ',' and return true, or consume [close] and return false. */
        private fun nextOrClose(close: Char): Boolean {
            skipWhitespace()
            if (atEnd()) fail("unterminated value")
            val c = s[i++]
            if (c != ',' && c != close) fail("expected ',' or '$close'")
            return c == ','
        }

        private fun readObject(depth: Int): Map<String, Any?> {
            i++ // {
            val map = LinkedHashMap<String, Any?>()
            skipWhitespace()
            if (!atEnd() && s[i] == '}') {
                i++
                return map
            }
            do {
                skipWhitespace()
                if (atEnd() || s[i] != '"') fail("expected a string key")
                val key = readString()
                if (map.containsKey(key)) fail("duplicate key \"$key\"")
                skipWhitespace()
                expect(':')
                skipWhitespace()
                map[key] = readValue(depth + 1)
            } while (nextOrClose('}'))
            return map
        }

        private fun readArray(depth: Int): List<Any?> {
            i++ // [
            val list = ArrayList<Any?>()
            skipWhitespace()
            if (!atEnd() && s[i] == ']') {
                i++
                return list
            }
            do {
                skipWhitespace()
                list.add(readValue(depth + 1))
            } while (nextOrClose(']'))
            return list
        }

        private fun readString(): String {
            i++ // opening quote
            val out = StringBuilder()
            while (true) {
                if (atEnd()) fail("unterminated string")
                val c = s[i++]
                if (c == '"') return out.toString()
                if (c < ' ') fail("control character in string")
                out.append(if (c == '\\') readEscape() else c)
            }
        }

        private fun readEscape(): Char {
            if (atEnd()) fail("unterminated escape")
            return when (val e = s[i++]) {
                '"', '\\', '/' -> e
                'b' -> '\b'
                'f' -> '\u000C'
                'n' -> '\n'
                'r' -> '\r'
                't' -> '\t'
                'u' -> readUnicodeEscape()
                else -> fail("invalid escape '\\$e'")
            }
        }

        private fun readUnicodeEscape(): Char {
            if (i + UNICODE_DIGITS > s.length) fail("truncated \\u escape")
            val hex = s.substring(i, i + UNICODE_DIGITS)
            if (!hex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) fail("invalid \\u escape")
            i += UNICODE_DIGITS
            return hex.toInt(HEX_RADIX).toChar()
        }

        private fun readNumber(): Any {
            val start = i
            if (s[i] == '-') i++
            readIntegerPart()
            val hasFraction = readFraction()
            val hasExponent = readExponent()
            val token = s.substring(start, i)
            return if (hasFraction || hasExponent) {
                token.toDoubleOrNull() ?: fail("invalid number")
            } else {
                token.toLongOrNull() ?: fail("integer out of range")
            }
        }

        private fun readIntegerPart() {
            if (atEnd()) fail("invalid number")
            when (s[i]) {
                '0' -> i++
                in '1'..'9' -> skipDigits()
                else -> fail("invalid number")
            }
        }

        private fun readFraction(): Boolean {
            if (atEnd() || s[i] != '.') return false
            i++
            if (skipDigits() == 0) fail("invalid number")
            return true
        }

        private fun readExponent(): Boolean {
            if (atEnd() || (s[i] != 'e' && s[i] != 'E')) return false
            i++
            if (!atEnd() && (s[i] == '+' || s[i] == '-')) i++
            if (skipDigits() == 0) fail("invalid number")
            return true
        }

        /** Skip ASCII digits and return how many there were. */
        private fun skipDigits(): Int {
            val start = i
            while (!atEnd() && s[i] in '0'..'9') i++
            return i - start
        }
    }
}
