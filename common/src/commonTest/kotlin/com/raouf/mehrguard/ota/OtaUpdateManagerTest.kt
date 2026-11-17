/*
 * Copyright 2025-2026 Mehr Guard Contributors
 * Licensed under the Apache License, Version 2.0
 */

package com.raouf.mehrguard.ota

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Tests for the OTA download path using in-memory fakes for storage and network.
 *
 * Each rejection test also checks that the previously cached copy is untouched.
 */
class OtaUpdateManagerTest {

    // === fakes ===

    private class FakeStorage : OtaStorage {
        val files = mutableMapOf<String, String>()
        var writes = 0

        override fun read(filename: String): String? = files[filename]
        override fun write(filename: String, content: String) {
            writes++
            files[filename] = content
        }
        override fun exists(filename: String): Boolean = files.containsKey(filename)
        override fun delete(filename: String) {
            files.remove(filename)
        }
    }

    private class FakeHttp : OtaHttpClient {
        val responses = mutableMapOf<String, String>()
        val requested = mutableListOf<String>()

        override suspend fun get(url: String): String {
            requested.add(url)
            return responses[url] ?: throw RuntimeException("HTTP 404")
        }
    }

    // === fixtures ===

    private fun brandDb(version: Int, domain: String = "paypal.com") =
        """{"version":$version,"brands":{"paypal":{"official_domains":["$domain"],"typosquats":["paypa1"]}}}"""

    private fun heuristics(version: Int, httpNotHttps: Int = 15) =
        """{"version":$version,"weights":{"httpNotHttps":$httpNotHttps,"ipAddress":25,"urlShortener":10,""" +
            """"excessiveSubdomains":12,"nonStandardPort":10,"credentialParams":20,"punycode":18},""" +
            """"suspiciousTlds":["tk","ml"]}"""

    private fun checksumOf(content: String) = "sha256:" + Sha256.hexDigest(content.encodeToByteArray())

    private fun manifest(
        version: Int,
        brand: String,
        heur: String,
        brandChecksum: String? = checksumOf(brand),
        heurChecksum: String? = checksumOf(heur),
        brandSize: Int = brand.encodeToByteArray().size,
        heurSize: Int = heur.encodeToByteArray().size,
        brandFilename: String = "brand_db_v$version.json"
    ): String {
        fun entry(filename: String, checksum: String?, size: Int) = buildString {
            append("""{"version":$version,"filename":"$filename"""")
            if (checksum != null) append(""","checksum":"$checksum"""")
            append(""","sizeBytes":$size}""")
        }
        return """{"version":$version,"files":{"brand_db":${entry(brandFilename, brandChecksum, brandSize)},""" +
            """"heuristics":${entry("heuristics_v$version.json", heurChecksum, heurSize)}}}"""
    }

    private fun url(file: String) = "${OtaUpdateManager.UPDATE_BASE_URL}/$file"

    private class Env {
        val storage = FakeStorage()
        val http = FakeHttp()
        val logs = mutableListOf<String>()
        val manager = OtaUpdateManager(storage, http) { logs.add(it) }
    }

    private fun Env.serve(version: Int, brand: String, heur: String, manifestText: String) {
        http.responses[url("version.json")] = manifestText
        http.responses[url("brand_db_v$version.json")] = brand
        http.responses[url("heuristics_v$version.json")] = heur
    }

    /** Applies a good version 2 so later tests have a cached copy to protect. */
    private suspend fun Env.applyGoodV2() {
        val brand = brandDb(2)
        val heur = heuristics(2)
        serve(2, brand, heur, manifest(2, brand, heur))
        manager.checkAndUpdate()
        assertEquals(OtaUpdateManager.UpdateState.Success(2), manager.updateState.value)
    }

    private fun Env.assertV2StillCached() {
        assertEquals(brandDb(2), manager.getCachedBrandDb())
        assertEquals(heuristics(2), manager.getCachedHeuristics())
        assertEquals(2, manager.currentVersion.value)
    }

    private fun Env.assertRejected(containing: String) {
        val state = manager.updateState.value as? OtaUpdateManager.UpdateState.Error
            ?: fail("expected Error but was ${manager.updateState.value}")
        assertTrue(state.message.contains(containing), "'${state.message}' should contain '$containing'")
        assertTrue(logs.any { it.contains(containing) }, "the reason should be logged: $logs")
    }

    // === valid update ===

    @Test
    fun `valid update is cached`() = runTest {
        val env = Env()
        env.applyGoodV2()

        assertEquals(brandDb(2), env.manager.getCachedBrandDb())
        assertEquals(heuristics(2), env.manager.getCachedHeuristics())
        assertTrue(env.manager.hasCachedData())
        assertEquals(2, env.manager.currentVersion.value)
        assertTrue(env.storage.exists(OtaUpdateManager.CACHED_VERSION))
    }

    @Test
    fun `cached update survives a restart`() = runTest {
        val env = Env()
        env.applyGoodV2()

        val restarted = OtaUpdateManager(env.storage, FakeHttp())
        restarted.checkAndUpdate() // network returns 404, so no update
        assertEquals(brandDb(2), restarted.getCachedBrandDb())
        assertEquals(2, restarted.currentVersion.value)
    }

    @Test
    fun `same or older version is not applied`() = runTest {
        val env = Env()
        env.applyGoodV2()
        val writesBefore = env.storage.writes

        val brand = brandDb(2, "evil.example")
        val heur = heuristics(2, 1)
        env.serve(2, brand, heur, manifest(2, brand, heur))
        env.manager.checkAndUpdate()

        assertEquals(OtaUpdateManager.UpdateState.NoUpdateNeeded, env.manager.updateState.value)
        assertEquals(writesBefore, env.storage.writes)
        env.assertV2StillCached()
    }

    // === malformed or wrongly shaped content ===

    @Test
    fun `malformed json is rejected and the old cache is kept`() = runTest {
        val env = Env()
        env.applyGoodV2()

        val badBrand = """{"version":3,"brands":{"paypal":{"official_domains":["paypal.com"]""" // truncated
        val heur = heuristics(3)
        // Checksum and size match the bad bytes, so only the JSON check can stop it.
        env.serve(3, badBrand, heur, manifest(3, badBrand, heur))
        env.manager.checkAndUpdate()

        env.assertRejected("brand_db rejected")
        env.assertV2StillCached()
    }

    @Test
    fun `valid json of the wrong shape is rejected and the old cache is kept`() = runTest {
        val env = Env()
        env.applyGoodV2()

        val brand = brandDb(3)
        val badHeur = """{"version":3,"weights":{"httpNotHttps":15}}""" // required weights missing
        env.serve(3, brand, badHeur, manifest(3, brand, badHeur))
        env.manager.checkAndUpdate()

        env.assertRejected("heuristics rejected")
        env.assertV2StillCached()
    }

    @Test
    fun `out of range weight is rejected`() = runTest {
        val env = Env()
        val brand = brandDb(2)
        val badHeur = heuristics(2, httpNotHttps = 9999)
        env.serve(2, brand, badHeur, manifest(2, brand, badHeur))
        env.manager.checkAndUpdate()

        env.assertRejected("heuristics rejected")
        assertNull(env.manager.getCachedHeuristics())
    }

    @Test
    fun `a bad second file means the first is not cached either`() = runTest {
        val env = Env()
        val brand = brandDb(2)
        val badHeur = "not json at all"
        env.serve(2, brand, badHeur, manifest(2, brand, badHeur))
        env.manager.checkAndUpdate()

        env.assertRejected("heuristics rejected")
        assertEquals(0, env.storage.writes)
        assertNull(env.manager.getCachedBrandDb())
        assertEquals(OtaUpdateManager.BUNDLED_VERSION, env.manager.currentVersion.value)
    }

    @Test
    fun `failed download keeps the old cache`() = runTest {
        val env = Env()
        env.applyGoodV2()

        val brand = brandDb(3)
        val heur = heuristics(3)
        env.http.responses[url("version.json")] = manifest(3, brand, heur)
        env.http.responses[url("brand_db_v3.json")] = brand
        // heuristics_v3.json is not served: the fake answers 404
        env.manager.checkAndUpdate()

        env.assertRejected("heuristics download failed")
        env.assertV2StillCached()
    }

    @Test
    fun `manifest filename that is not a plain json name is rejected`() = runTest {
        val env = Env()
        val brand = brandDb(2)
        val heur = heuristics(2)
        env.serve(2, brand, heur, manifest(2, brand, heur, brandFilename = "../../evil.json"))
        env.manager.checkAndUpdate()

        env.assertRejected("manifest rejected")
        assertEquals(listOf(url("version.json")), env.http.requested)
    }

    // === size ===

    @Test
    fun `oversize response is rejected and the old cache is kept`() = runTest {
        val env = Env()
        env.applyGoodV2()

        val padding = "x".repeat(OtaUpdateManager.MAX_FILE_SIZE)
        val bigBrand = """{"version":3,"brands":{"paypal":{"official_domains":["paypal.com"]}},"pad":"$padding"}"""
        val heur = heuristics(3)
        // Checksum and declared size are honest, so only the size limit can stop it.
        env.serve(3, bigBrand, heur, manifest(3, bigBrand, heur))
        env.manager.checkAndUpdate()

        env.assertRejected("byte limit")
        env.assertV2StillCached()
    }

    @Test
    fun `size is measured in bytes not characters`() = runTest {
        val env = Env()
        // Each of these characters is 3 bytes in UTF-8, so the text is under the limit in
        // characters and over it in bytes.
        val padding = "€".repeat(OtaUpdateManager.MAX_FILE_SIZE / 3 + 10)
        val bigBrand = """{"version":2,"brands":{"paypal":{"official_domains":["paypal.com"]}},"pad":"$padding"}"""
        assertTrue(bigBrand.length < OtaUpdateManager.MAX_FILE_SIZE)
        assertTrue(bigBrand.encodeToByteArray().size > OtaUpdateManager.MAX_FILE_SIZE)
        val heur = heuristics(2)
        env.serve(2, bigBrand, heur, manifest(2, bigBrand, heur))
        env.manager.checkAndUpdate()

        env.assertRejected("byte limit")
    }

    @Test
    fun `declared size that does not match is rejected`() = runTest {
        val env = Env()
        val brand = brandDb(2)
        val heur = heuristics(2)
        env.serve(2, brand, heur, manifest(2, brand, heur, brandSize = brand.length + 1))
        env.manager.checkAndUpdate()

        env.assertRejected("sizeBytes")
        assertNull(env.manager.getCachedBrandDb())
    }

    @Test
    fun `declared size of zero is rejected`() = runTest {
        val env = Env()
        val brand = brandDb(2)
        val heur = heuristics(2)
        env.serve(2, brand, heur, manifest(2, brand, heur, brandSize = 0))
        env.manager.checkAndUpdate()

        env.assertRejected("sizeBytes")
        assertEquals(0, env.storage.writes)
    }

    // === checksum ===

    @Test
    fun `checksum mismatch is rejected and the old cache is kept`() = runTest {
        val env = Env()
        env.applyGoodV2()

        val brand = brandDb(3)
        val heur = heuristics(3)
        val tamperedBrand = brandDb(3, "evil.example") // the manifest still lists the hash of the original
        env.serve(3, tamperedBrand, heur, manifest(3, brand, heur, brandSize = tamperedBrand.encodeToByteArray().size))
        env.manager.checkAndUpdate()

        env.assertRejected("SHA-256 does not match")
        env.assertV2StillCached()
    }

    @Test
    fun `checksum with upper case hex is accepted`() = runTest {
        val env = Env()
        val brand = brandDb(2)
        val heur = heuristics(2)
        val upper = "sha256:" + Sha256.hexDigest(brand.encodeToByteArray()).uppercase()
        env.serve(2, brand, heur, manifest(2, brand, heur, brandChecksum = upper))
        env.manager.checkAndUpdate()

        assertEquals(OtaUpdateManager.UpdateState.Success(2), env.manager.updateState.value)
    }

    @Test
    fun `placeholder checksum is not applied and nothing is downloaded`() = runTest {
        val env = Env()
        env.applyGoodV2()
        env.http.requested.clear()
        val writesBefore = env.storage.writes

        val brand = brandDb(3)
        val heur = heuristics(3)
        env.serve(3, brand, heur, manifest(3, brand, heur, brandChecksum = "sha256:placeholder"))
        env.manager.checkAndUpdate()

        env.assertRejected("unverified")
        assertEquals(listOf(url("version.json")), env.http.requested)
        assertEquals(writesBefore, env.storage.writes)
        env.assertV2StillCached()
    }

    @Test
    fun `missing checksum is not applied`() = runTest {
        val env = Env()
        val brand = brandDb(2)
        val heur = heuristics(2)
        env.serve(2, brand, heur, manifest(2, brand, heur, heurChecksum = null))
        env.manager.checkAndUpdate()

        env.assertRejected("unverified")
        assertEquals(0, env.storage.writes)
        assertFalse(env.manager.hasCachedData())
    }

    @Test
    fun `checksum of the wrong length is treated as unverified`() = runTest {
        val env = Env()
        val brand = brandDb(2)
        val heur = heuristics(2)
        env.serve(2, brand, heur, manifest(2, brand, heur, brandChecksum = "sha256:abc123"))
        env.manager.checkAndUpdate()

        env.assertRejected("unverified")
        assertEquals(0, env.storage.writes)
    }

    // === load-time checks ===

    @Test
    fun `a cached file that no longer matches is ignored in favour of bundled data`() = runTest {
        val env = Env()
        env.applyGoodV2()

        env.storage.files[OtaUpdateManager.CACHED_BRAND_DB] = brandDb(2, "evil.example")
        assertNull(env.manager.getCachedBrandDb())
        assertNotNull(env.manager.getCachedHeuristics()) // the untouched file is still used
    }

    @Test
    fun `a cached file that is not valid json is ignored`() = runTest {
        val env = Env()
        env.applyGoodV2()

        env.storage.files[OtaUpdateManager.CACHED_HEURISTICS] = "{ truncated"
        assertNull(env.manager.getCachedHeuristics())
        assertNotNull(env.manager.getCachedBrandDb())
    }

    @Test
    fun `cache from before validation existed is ignored`() = runTest {
        // Older builds stored whatever they downloaded, with the placeholder-checksum manifest.
        val env = Env()
        env.storage.files[OtaUpdateManager.CACHED_BRAND_DB] = brandDb(2)
        env.storage.files[OtaUpdateManager.CACHED_HEURISTICS] = heuristics(2)
        env.storage.files[OtaUpdateManager.CACHED_VERSION] =
            manifest(2, brandDb(2), heuristics(2), brandChecksum = "sha256:placeholder", heurChecksum = "sha256:placeholder")

        assertNull(env.manager.getCachedBrandDb())
        assertNull(env.manager.getCachedHeuristics())
        assertFalse(env.manager.hasCachedData())

        // And it must not block a properly verified update to the same version number.
        val brand = brandDb(2)
        val heur = heuristics(2)
        env.serve(2, brand, heur, manifest(2, brand, heur))
        env.manager.checkAndUpdate()
        assertEquals(OtaUpdateManager.UpdateState.Success(2), env.manager.updateState.value)
        assertEquals(brand, env.manager.getCachedBrandDb())
    }

    @Test
    fun `clearCache removes everything`() = runTest {
        val env = Env()
        env.applyGoodV2()
        env.manager.clearCache()

        assertFalse(env.manager.hasCachedData())
        assertEquals(OtaUpdateManager.BUNDLED_VERSION, env.manager.currentVersion.value)
    }

    // === manifest ===

    @Test
    fun `malformed manifest is rejected`() = runTest {
        val env = Env()
        env.http.responses[url("version.json")] = """{"version": 2, "files": """
        env.manager.checkAndUpdate()

        env.assertRejected("manifest rejected")
        assertEquals(0, env.storage.writes)
    }

    @Test
    fun `unreachable origin leaves bundled data in place`() = runTest {
        val env = Env() // the fake answers 404 for everything
        env.manager.checkAndUpdate()

        assertEquals(OtaUpdateManager.UpdateState.NoUpdateNeeded, env.manager.updateState.value)
        assertEquals(0, env.storage.writes)
        assertFalse(env.manager.hasCachedData())
    }
}
