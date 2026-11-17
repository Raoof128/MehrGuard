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

/**
 * SHA-256 (FIPS 180-4) in pure Kotlin, so the OTA check works unchanged on every
 * target without a new dependency or a JVM-only API.
 *
 * Only the OTA path uses it, on files of at most [OtaUpdateManager.MAX_FILE_SIZE]
 * bytes. It is checked against the FIPS test vectors in `Sha256Test`. The numeric
 * literals are the constants and shift distances the standard defines.
 */
@Suppress("MagicNumber")
internal object Sha256 {

    private val K = longArrayOf(
        0x428a2f98L, 0x71374491L, 0xb5c0fbcfL, 0xe9b5dba5L, 0x3956c25bL, 0x59f111f1L, 0x923f82a4L, 0xab1c5ed5L,
        0xd807aa98L, 0x12835b01L, 0x243185beL, 0x550c7dc3L, 0x72be5d74L, 0x80deb1feL, 0x9bdc06a7L, 0xc19bf174L,
        0xe49b69c1L, 0xefbe4786L, 0x0fc19dc6L, 0x240ca1ccL, 0x2de92c6fL, 0x4a7484aaL, 0x5cb0a9dcL, 0x76f988daL,
        0x983e5152L, 0xa831c66dL, 0xb00327c8L, 0xbf597fc7L, 0xc6e00bf3L, 0xd5a79147L, 0x06ca6351L, 0x14292967L,
        0x27b70a85L, 0x2e1b2138L, 0x4d2c6dfcL, 0x53380d13L, 0x650a7354L, 0x766a0abbL, 0x81c2c92eL, 0x92722c85L,
        0xa2bfe8a1L, 0xa81a664bL, 0xc24b8b70L, 0xc76c51a3L, 0xd192e819L, 0xd6990624L, 0xf40e3585L, 0x106aa070L,
        0x19a4c116L, 0x1e376c08L, 0x2748774cL, 0x34b0bcb5L, 0x391c0cb3L, 0x4ed8aa4aL, 0x5b9cca4fL, 0x682e6ff3L,
        0x748f82eeL, 0x78a5636fL, 0x84c87814L, 0x8cc70208L, 0x90befffaL, 0xa4506cebL, 0xbef9a3f7L, 0xc67178f2L
    )

    private val INITIAL = longArrayOf(
        0x6a09e667L, 0xbb67ae85L, 0x3c6ef372L, 0xa54ff53aL, 0x510e527fL, 0x9b05688cL, 0x1f83d9abL, 0x5be0cd19L
    )

    private const val HEX = "0123456789abcdef"
    private const val BLOCK_SIZE = 64

    /** SHA-256 of [data] as 64 lowercase hex characters. */
    fun hexDigest(data: ByteArray): String {
        val out = StringBuilder()
        for (b in digest(data)) {
            val v = b.toInt() and 0xFF
            out.append(HEX[v ushr 4])
            out.append(HEX[v and 0x0F])
        }
        return out.toString()
    }

    /** SHA-256 of [data] as 32 bytes. */
    fun digest(data: ByteArray): ByteArray {
        val padded = pad(data)
        val h = IntArray(INITIAL.size) { INITIAL[it].toInt() }
        val w = IntArray(64)
        for (offset in padded.indices step BLOCK_SIZE) {
            loadSchedule(padded, offset, w)
            compress(h, w)
        }
        return toBytes(h)
    }

    /** Append 0x80, zeros, then the message length in bits as a big-endian 64-bit value. */
    private fun pad(data: ByteArray): ByteArray {
        val paddedSize = ((data.size + 9 + 63) / BLOCK_SIZE) * BLOCK_SIZE
        val padded = ByteArray(paddedSize)
        data.copyInto(padded)
        padded[data.size] = 0x80.toByte()
        val bitLength = data.size.toLong() * 8L
        for (i in 0 until 8) {
            padded[paddedSize - 1 - i] = (bitLength ushr (8 * i)).toByte()
        }
        return padded
    }

    /** Expand one 64-byte block into the 64-word message schedule [w]. */
    private fun loadSchedule(padded: ByteArray, offset: Int, w: IntArray) {
        for (i in 0 until 16) {
            val j = offset + i * 4
            w[i] = ((padded[j].toInt() and 0xFF) shl 24) or
                ((padded[j + 1].toInt() and 0xFF) shl 16) or
                ((padded[j + 2].toInt() and 0xFF) shl 8) or
                (padded[j + 3].toInt() and 0xFF)
        }
        for (i in 16 until 64) {
            val s0 = rotr(w[i - 15], 7) xor rotr(w[i - 15], 18) xor (w[i - 15] ushr 3)
            val s1 = rotr(w[i - 2], 17) xor rotr(w[i - 2], 19) xor (w[i - 2] ushr 10)
            w[i] = w[i - 16] + s0 + w[i - 7] + s1
        }
    }

    /** Run the 64 rounds over schedule [w] and add the result into the state [h]. */
    private fun compress(h: IntArray, w: IntArray) {
        var a = h[0]
        var b = h[1]
        var c = h[2]
        var d = h[3]
        var e = h[4]
        var f = h[5]
        var g = h[6]
        var hh = h[7]

        for (i in 0 until 64) {
            val bigS1 = rotr(e, 6) xor rotr(e, 11) xor rotr(e, 25)
            val ch = (e and f) xor (e.inv() and g)
            val t1 = hh + bigS1 + ch + K[i].toInt() + w[i]
            val bigS0 = rotr(a, 2) xor rotr(a, 13) xor rotr(a, 22)
            val maj = (a and b) xor (a and c) xor (b and c)
            val t2 = bigS0 + maj

            hh = g
            g = f
            f = e
            e = d + t1
            d = c
            c = b
            b = a
            a = t1 + t2
        }

        h[0] += a
        h[1] += b
        h[2] += c
        h[3] += d
        h[4] += e
        h[5] += f
        h[6] += g
        h[7] += hh
    }

    private fun toBytes(h: IntArray): ByteArray {
        val out = ByteArray(32)
        for (i in 0 until 8) {
            out[i * 4] = (h[i] ushr 24).toByte()
            out[i * 4 + 1] = (h[i] ushr 16).toByte()
            out[i * 4 + 2] = (h[i] ushr 8).toByte()
            out[i * 4 + 3] = h[i].toByte()
        }
        return out
    }

    private fun rotr(x: Int, n: Int): Int = (x ushr n) or (x shl (32 - n))
}
