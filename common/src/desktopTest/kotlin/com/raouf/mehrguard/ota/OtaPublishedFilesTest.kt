/*
 * Copyright 2025-2026 Mehr Guard Contributors
 * Licensed under the Apache License, Version 2.0
 */

package com.raouf.mehrguard.ota

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Guards the files committed under `data/updates/`: the manifest must carry the real
 * SHA-256 and size of each data file, and each data file must pass the same validation
 * the app applies. Without this, editing a data file and forgetting the manifest would
 * ship an update that every client rejects.
 */
class OtaPublishedFilesTest {

    private fun updatesDir(): File {
        val candidates = listOf("../data/updates", "data/updates")
        return candidates.map { File(it) }.firstOrNull { File(it, "version.json").exists() }
            ?: error("data/updates not found from ${File(".").absolutePath}")
    }

    private fun read(dir: File, name: String): ByteArray = File(dir, name).readBytes()

    @Test
    fun `committed manifest matches the committed data files`() {
        val dir = updatesDir()
        val manifest = when (val parsed = OtaValidator.parseManifest(read(dir, "version.json").decodeToString())) {
            is OtaValidation.Ok -> parsed.value
            is OtaValidation.Rejected -> fail("version.json rejected: ${parsed.reason}")
        }

        for ((name, entry) in listOf("brand_db" to manifest.brandDb, "heuristics" to manifest.heuristics)) {
            val bytes = read(dir, entry.filename)
            assertNotNull(entry.sha256, "$name has no real sha256 checksum in version.json")
            assertEquals(entry.sha256, Sha256.hexDigest(bytes), "$name checksum does not match ${entry.filename}")
            assertEquals(bytes.size.toLong(), entry.sizeBytes, "$name sizeBytes does not match ${entry.filename}")
            assertTrue(bytes.size <= OtaUpdateManager.MAX_FILE_SIZE, "${entry.filename} is over the download limit")
        }
    }

    @Test
    fun `committed data files pass validation`() {
        val dir = updatesDir()
        val manifest = (OtaValidator.parseManifest(read(dir, "version.json").decodeToString()) as OtaValidation.Ok).value

        val brand = OtaValidator.validateBrandDb(read(dir, manifest.brandDb.filename).decodeToString(), manifest.brandDb.version)
        if (brand is OtaValidation.Rejected) fail("brand database rejected: ${brand.reason}")

        val heuristics = OtaValidator.validateHeuristics(read(dir, manifest.heuristics.filename).decodeToString(), manifest.heuristics.version)
        if (heuristics is OtaValidation.Rejected) fail("heuristics rejected: ${heuristics.reason}")
    }
}
