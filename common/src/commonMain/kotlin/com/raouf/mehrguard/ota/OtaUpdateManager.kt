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

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * OTA (Over-the-Air) Update Manager for the Living Engine
 *
 * Manages remote updates for the detection engine's databases:
 * - Brand database (brand_db_v2.json)
 * - Heuristics weights (heuristics_v2.json)
 *
 * ## Architecture
 * - **Versioning**: the origin hosts `version.json` with a version number and, per data
 *   file, a filename, a `sha256:<hex>` checksum and a size in bytes
 * - **Offline-First**: the app works with bundled data if the network is unavailable
 *   or an update is rejected
 * - **Cache Layer**: accepted updates are saved to local storage
 * - **Priority Loading**: a cached file is used only if it still passes every check
 *   below at load time; otherwise the bundled defaults are used
 *
 * ## What is checked before an update is applied
 * 1. The manifest is well-formed JSON of the expected shape (strict parse, required
 *    fields, plain `.json` filenames, version within range).
 * 2. The manifest version is newer than the current one (no downgrade).
 * 3. Every listed file has a real `sha256:<64 hex>` checksum. A missing or placeholder
 *    checksum means the file is unverified, and the whole update is refused (fail
 *    closed) with the reason logged.
 * 4. Each download is at most [MAX_FILE_SIZE] bytes, its byte size equals `sizeBytes`,
 *    and its SHA-256 equals the listed checksum.
 * 5. Each file parses as JSON of the expected shape (see [OtaValidator]).
 *
 * Only when all of that passes for both files are they written to the cache, together
 * with the manifest. A failed or invalid download never replaces a cached file. The
 * cached manifest is written last, so an interrupted write leaves a cache that fails
 * the load-time check and is ignored.
 *
 * ## What this does and does not protect against
 * The checksum comes from the same origin as the data. It detects corruption and
 * truncation in transit or at rest, and a data file that does not match its manifest.
 * It does NOT protect against a compromised origin: whoever can change the data file
 * can change the manifest to match. Signed manifests (for example Ed25519, with the
 * public key shipped in the app) are not implemented. The SHA-256 is implemented in
 * Kotlin in this module ([Sha256]) so it needs no dependency and runs on every target.
 *
 * Transport is HTTPS (TLS). The size cap limits memory use on a hostile response.
 *
 * @see OtaStorage for platform-specific storage implementation
 *
 * @author Mehr Guard Security Team
 * @since 1.3.0
 */
class OtaUpdateManager(
    private val storage: OtaStorage,
    private val httpClient: OtaHttpClient,
    /** Receives the reason whenever an update is refused or a cached file is ignored. */
    private val log: (String) -> Unit = {}
) {

    companion object {
        /**
         * Base URL for OTA updates (GitHub Pages).
         *
         * Checked on 17 Nov 2026: `<this URL>/version.json` and
         * `https://raoof128.github.io/data/updates/version.json` both return HTTP 404,
         * so the update check currently has no effect and the app runs on bundled data.
         * The path names a repository that no longer exists; the files now live in
         * Raoof128/Raoof128.github.io under `data/updates/` but are not served by its
         * Pages site. Change this constant only once a URL is confirmed to serve the file.
         */
        const val UPDATE_BASE_URL = "https://raoof128.github.io/QDKMP-KotlinConf-2026-/data/updates"

        /** Version manifest filename */
        const val VERSION_MANIFEST = "version.json"

        /** Maximum file size for downloads (500KB), measured in bytes */
        const val MAX_FILE_SIZE = 500 * 1024

        /** Current bundled version */
        const val BUNDLED_VERSION = 1

        /** Cache file names */
        const val CACHED_BRAND_DB = "brand_db_cached.json"
        const val CACHED_HEURISTICS = "heuristics_cached.json"
        const val CACHED_VERSION = "version_cached.json"
    }

    /**
     * Update state for tracking progress
     */
    sealed class UpdateState {
        data object Idle : UpdateState()
        data object Checking : UpdateState()
        data object Downloading : UpdateState()
        data class Success(val version: Int) : UpdateState()
        data class Error(val message: String) : UpdateState()
        data object NoUpdateNeeded : UpdateState()
    }

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val _currentVersion = MutableStateFlow(BUNDLED_VERSION)
    val currentVersion: StateFlow<Int> = _currentVersion.asStateFlow()

    /** Thrown inside an update attempt to refuse it; [checkAndUpdate] turns it into [UpdateState.Error]. */
    private class UpdateRejected(message: String) : Exception(message)

    private fun rejectUpdate(message: String): Nothing = throw UpdateRejected(message)

    /**
     * Check for and apply updates.
     *
     * Call this from Application.onCreate() in a background coroutine.
     */
    suspend fun checkAndUpdate() {
        _updateState.value = UpdateState.Checking

        _updateState.value = try {
            performUpdate()
        } catch (e: UpdateRejected) {
            failed(e.message ?: "update rejected")
        } catch (e: Exception) {
            failed("update failed: ${e.message ?: "unknown error"}")
        }
    }

    private fun failed(message: String): UpdateState {
        log("OTA $message")
        return UpdateState.Error(message)
    }

    private suspend fun performUpdate(): UpdateState {
        // 1. Load the cached version if it is still trustworthy
        loadCachedVersion()

        // 2. Fetch and validate the remote manifest
        val manifestText = fetchVersionManifest() ?: return UpdateState.NoUpdateNeeded
        val manifest = when (val parsed = OtaValidator.parseManifest(manifestText)) {
            is OtaValidation.Ok -> parsed.value
            is OtaValidation.Rejected -> rejectUpdate("manifest rejected: ${parsed.reason}")
        }

        // 3. Compare versions
        if (manifest.version <= _currentVersion.value) return UpdateState.NoUpdateNeeded

        // 4. Fail closed: refuse before downloading if either file is unverified
        requireChecksums(manifest)

        // 5. Download and verify both files in memory; nothing is cached yet
        _updateState.value = UpdateState.Downloading
        val brandDb = downloadVerified("brand_db", manifest.brandDb) {
            OtaValidator.validateBrandDb(it, manifest.brandDb.version)
        }
        val heuristics = downloadVerified("heuristics", manifest.heuristics) {
            OtaValidator.validateHeuristics(it, manifest.heuristics.version)
        }

        // 6. Cache both files, the manifest last
        storage.write(CACHED_BRAND_DB, brandDb)
        storage.write(CACHED_HEURISTICS, heuristics)
        storage.write(CACHED_VERSION, manifestText)
        _currentVersion.value = manifest.version
        return UpdateState.Success(manifest.version)
    }

    private fun requireChecksums(manifest: OtaManifest) {
        val unverified = listOf("brand_db" to manifest.brandDb, "heuristics" to manifest.heuristics)
            .firstOrNull { it.second.sha256 == null }
        if (unverified != null) {
            rejectUpdate(
                "update to version ${manifest.version} not applied: " +
                    "${unverified.first} has no valid sha256 checksum (unverified)"
            )
        }
    }

    /**
     * Get cached brand database JSON, or null if none is cached or the cached copy
     * fails validation (the caller then uses the bundled data).
     */
    fun getCachedBrandDb(): String? {
        return loadVerifiedCache(CACHED_BRAND_DB, { it.brandDb }) { text, entry ->
            OtaValidator.validateBrandDb(text, entry.version)
        }
    }

    /**
     * Get cached heuristics JSON, or null if none is cached or the cached copy
     * fails validation (the caller then uses the bundled data).
     */
    fun getCachedHeuristics(): String? {
        return loadVerifiedCache(CACHED_HEURISTICS, { it.heuristics }) { text, entry ->
            OtaValidator.validateHeuristics(text, entry.version)
        }
    }

    /**
     * Check if usable cached data is available.
     */
    fun hasCachedData(): Boolean {
        return getCachedBrandDb() != null || getCachedHeuristics() != null
    }

    /**
     * Clear all cached data (for testing/reset).
     */
    fun clearCache() {
        storage.delete(CACHED_BRAND_DB)
        storage.delete(CACHED_HEURISTICS)
        storage.delete(CACHED_VERSION)
        _currentVersion.value = BUNDLED_VERSION
    }

    // === PRIVATE METHODS ===

    /** The cached manifest if it is valid and lists a real checksum for both files, else null. */
    private fun readTrustedCachedManifest(): OtaManifest? {
        val cached = storage.read(CACHED_VERSION) ?: return null
        val manifest = (OtaValidator.parseManifest(cached) as? OtaValidation.Ok)?.value
        // A manifest without real checksums comes from a build that cached unverified downloads.
        val trusted = manifest?.takeIf { it.brandDb.sha256 != null && it.heuristics.sha256 != null }
        if (trusted == null) log("OTA cached manifest is invalid or unverified, ignoring the cache")
        return trusted
    }

    private fun loadCachedVersion() {
        _currentVersion.value = readTrustedCachedManifest()?.version ?: BUNDLED_VERSION
    }

    private suspend fun fetchVersionManifest(): String? {
        return try {
            val response = httpClient.get("$UPDATE_BASE_URL/$VERSION_MANIFEST")
            response.takeIf { it.encodeToByteArray().size <= MAX_FILE_SIZE }
                ?: run {
                    log("OTA manifest ignored: larger than $MAX_FILE_SIZE bytes")
                    null
                }
        } catch (e: Exception) {
            null // Network error, use cached/bundled
        }
    }

    /**
     * Download one data file and check size, checksum and shape. Returns the content or
     * throws [UpdateRejected]. Never touches the cache.
     */
    private suspend fun downloadVerified(
        name: String,
        entry: OtaFileEntry,
        validate: (String) -> OtaValidation<Unit>
    ): String {
        val content = try {
            httpClient.get("$UPDATE_BASE_URL/${entry.filename}")
        } catch (e: Exception) {
            rejectUpdate("$name download failed: ${e.message ?: "network error"}")
        }

        val bytes = content.encodeToByteArray()
        if (bytes.size > MAX_FILE_SIZE) {
            rejectUpdate("$name rejected: ${bytes.size} bytes is over the $MAX_FILE_SIZE byte limit")
        }
        val declared = entry.sizeBytes
        if (declared == null || declared <= 0L || declared != bytes.size.toLong()) {
            rejectUpdate("$name rejected: manifest sizeBytes ($declared) does not match ${bytes.size} bytes downloaded")
        }
        if (Sha256.hexDigest(bytes) != entry.sha256) {
            rejectUpdate("$name rejected: SHA-256 does not match the manifest")
        }
        (validate(content) as? OtaValidation.Rejected)?.let { rejectUpdate("$name rejected: ${it.reason}") }
        return content
    }

    /**
     * Read a cached data file only if the cached manifest is valid, the file's SHA-256
     * and size match it, and its content still passes validation.
     */
    private fun loadVerifiedCache(
        cacheFile: String,
        pick: (OtaManifest) -> OtaFileEntry,
        validate: (String, OtaFileEntry) -> OtaValidation<Unit>
    ): String? {
        return try {
            readVerifiedCache(cacheFile, pick, validate)
        } catch (e: Exception) {
            log("OTA cached $cacheFile ignored: ${e.message ?: "read error"}")
            null
        }
    }

    private fun readVerifiedCache(
        cacheFile: String,
        pick: (OtaManifest) -> OtaFileEntry,
        validate: (String, OtaFileEntry) -> OtaValidation<Unit>
    ): String? {
        val entry = readTrustedCachedManifest()?.let(pick) ?: return null
        val content = storage.read(cacheFile) ?: return null
        val bytes = content.encodeToByteArray()
        val problem = if (bytes.size > MAX_FILE_SIZE || Sha256.hexDigest(bytes) != entry.sha256) {
            "does not match the cached manifest"
        } else {
            (validate(content, entry) as? OtaValidation.Rejected)?.reason
        }
        if (problem != null) log("OTA cached $cacheFile ignored: $problem")
        return content.takeIf { problem == null }
    }
}

/**
 * Platform-specific storage interface for OTA files.
 *
 * Each platform implements this to store/retrieve cached update files:
 * - Android: Context.filesDir
 * - iOS: NSDocumentDirectory  
 * - Desktop: User home directory
 * - Web: localStorage
 */
interface OtaStorage {
    /**
     * Read file contents from cache.
     * @return File contents or null if not found
     */
    fun read(filename: String): String?

    /**
     * Write content to cache file.
     */
    fun write(filename: String, content: String)

    /**
     * Check if cached file exists.
     */
    fun exists(filename: String): Boolean

    /**
     * Delete cached file.
     */
    fun delete(filename: String)
}

/**
 * HTTP client interface for OTA downloads.
 *
 * Each platform provides its own HTTP implementation:
 * - Android: OkHttp or HttpURLConnection
 * - iOS: NSURLSession
 * - Desktop: java.net.HttpURLConnection
 * - Web: fetch() API
 */
interface OtaHttpClient {
    /**
     * Perform HTTP GET request.
     * @param url The URL to fetch
     * @return Response body as string
     * @throws Exception on network error
     */
    suspend fun get(url: String): String
}
