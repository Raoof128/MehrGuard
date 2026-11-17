/*
 * Copyright 2025-2026 Mehr Guard Contributors
 * Licensed under the Apache License, Version 2.0
 */

package com.raouf.mehrguard.ota

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Checks [Sha256] against the published FIPS 180-4 / NIST test vectors, so the OTA
 * checksum comparison is anchored to values that do not come from this code.
 */
class Sha256Test {

    private fun hash(text: String) = Sha256.hexDigest(text.encodeToByteArray())

    @Test
    fun `empty input`() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hash(""))
    }

    @Test
    fun `abc`() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", hash("abc"))
    }

    @Test
    fun `two block message`() {
        assertEquals(
            "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1",
            hash("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq")
        )
    }

    @Test
    fun `one million a`() {
        val data = ByteArray(1_000_000) { 'a'.code.toByte() }
        assertEquals("cdc76e5c9914fb9281a1c7e284d73e67f1809a48a497200e046d39ccc7112cd0", Sha256.hexDigest(data))
    }

    @Test
    fun `padding boundaries around 55 56 and 64 bytes`() {
        // Known digests of 'a' repeated n times for the lengths where padding changes block count.
        assertEquals("9f4390f8d30c2dd92ec9f095b65e2b9ae9b0a925a5258e241c9f1e910f734318", hash("a".repeat(55)))
        assertEquals("b35439a4ac6f0948b6d6f9e3c6af0f5f590ce20f1bde7090ef7970686ec6738a", hash("a".repeat(56)))
        assertEquals("ffe054fe7ae0cb6dc65c3af9b61d5209f439851db43d0ba5997337df154668eb", hash("a".repeat(64)))
    }

    @Test
    fun `digest is 32 bytes`() {
        assertEquals(32, Sha256.digest(ByteArray(0)).size)
    }
}
