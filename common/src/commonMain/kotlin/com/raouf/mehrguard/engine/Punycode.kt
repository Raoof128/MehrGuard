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
 * Decodes internationalised host names from their ASCII form (RFC 3492 Punycode).
 *
 * A host such as `xn--pypal-4ve.com` is the ASCII encoding of a name that contains
 * a Cyrillic letter. QR codes and links normally carry this ASCII form, so the
 * brand and homograph checks, which compare Unicode text, have to see the decoded
 * name or they never fire.
 *
 * Decoding only. Nothing here validates the name against IDNA rules.
 */
// decode() follows the RFC 3492 pseudo-code step by step, so it keeps the RFC's
// early exits, nested loop and literal code-point values.
@Suppress(
    "MagicNumber",
    "ReturnCount",
    "CyclomaticComplexMethod",
    "NestedBlockDepth",
    "LoopWithTooManyJumpStatements",
    "ComplexCondition"
)
object Punycode {

    private const val BASE = 36
    private const val T_MIN = 1
    private const val T_MAX = 26
    private const val SKEW = 38
    private const val DAMP = 700
    private const val INITIAL_BIAS = 72
    private const val INITIAL_N = 128
    private const val MAX_CODE_POINT = 0x10FFFF
    private const val ACE_PREFIX = "xn--"
    private const val MAX_LABEL_LENGTH = 63

    /**
     * Returns [host] with every `xn--` label decoded to Unicode.
     * A label that is not valid Punycode is left as it is.
     */
    fun toUnicodeHost(host: String): String {
        if (!host.contains(ACE_PREFIX, ignoreCase = true)) return host
        return host.split(".").joinToString(".") { label ->
            if (label.length in (ACE_PREFIX.length + 1)..MAX_LABEL_LENGTH &&
                label.startsWith(ACE_PREFIX, ignoreCase = true)
            ) {
                decode(label.substring(ACE_PREFIX.length)) ?: label
            } else {
                label
            }
        }
    }

    /**
     * Decodes one Punycode string (a label without its `xn--` prefix).
     * Returns null if the input is not valid Punycode.
     */
    fun decode(input: String): String? {
        val output = mutableListOf<Int>()
        val delimiter = input.lastIndexOf('-')
        if (delimiter > 0) {
            for (index in 0 until delimiter) {
                val code = input[index].code
                if (code >= INITIAL_N) return null
                output.add(code)
            }
        }

        var position = if (delimiter > 0) delimiter + 1 else 0
        var n = INITIAL_N
        var i = 0
        var bias = INITIAL_BIAS

        while (position < input.length) {
            val oldI = i
            var weight = 1
            var k = BASE
            while (true) {
                if (position >= input.length) return null
                val digit = digitOf(input[position])
                position++
                if (digit < 0) return null
                if (digit > (Int.MAX_VALUE - i) / weight) return null
                i += digit * weight
                val threshold = when {
                    k <= bias -> T_MIN
                    k >= bias + T_MAX -> T_MAX
                    else -> k - bias
                }
                if (digit < threshold) break
                if (weight > Int.MAX_VALUE / (BASE - threshold)) return null
                weight *= BASE - threshold
                k += BASE
            }
            val length = output.size + 1
            bias = adapt(i - oldI, length, oldI == 0)
            if (i / length > MAX_CODE_POINT - n) return null
            n += i / length
            i %= length
            if (n in 0xD800..0xDFFF) return null
            output.add(i, n)
            i++
        }

        return buildString {
            for (codePoint in output) {
                if (codePoint < 0x10000) {
                    append(codePoint.toChar())
                } else {
                    val offset = codePoint - 0x10000
                    append((0xD800 + (offset shr 10)).toChar())
                    append((0xDC00 + (offset and 0x3FF)).toChar())
                }
            }
        }
    }

    private fun adapt(delta: Int, numPoints: Int, firstTime: Boolean): Int {
        var d = if (firstTime) delta / DAMP else delta / 2
        d += d / numPoints
        var k = 0
        while (d > ((BASE - T_MIN) * T_MAX) / 2) {
            d /= BASE - T_MIN
            k += BASE
        }
        return k + (BASE - T_MIN + 1) * d / (d + SKEW)
    }

    private fun digitOf(char: Char): Int = when (char) {
        in 'a'..'z' -> char - 'a'
        in 'A'..'Z' -> char - 'A'
        in '0'..'9' -> char - '0' + 26
        else -> -1
    }
}
