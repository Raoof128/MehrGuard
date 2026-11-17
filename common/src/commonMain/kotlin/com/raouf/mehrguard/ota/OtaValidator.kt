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

/** Outcome of validating an OTA file: either a value or the reason it was rejected. */
internal sealed class OtaValidation<out T> {
    class Ok<T>(val value: T) : OtaValidation<T>()
    class Rejected(val reason: String) : OtaValidation<Nothing>()
}

/**
 * One data file listed in `version.json`.
 *
 * [sha256] is the lowercase hex digest from a `sha256:<64 hex>` checksum, or null when
 * the manifest has no checksum, a placeholder, or anything not in that form. A null
 * digest means the file is *unverified* and [OtaUpdateManager] will not apply it.
 */
internal class OtaFileEntry(
    val version: Int?,
    val filename: String,
    val sha256: String?,
    val sizeBytes: Long?
)

internal class OtaManifest(
    val version: Int,
    val brandDb: OtaFileEntry,
    val heuristics: OtaFileEntry
)

/**
 * Structural validation of the OTA manifest and data files.
 *
 * Everything here works on the parsed JSON tree from [OtaJson], never on regex
 * matches, and rejects rather than repairs. Each public function returns an
 * [OtaValidation]; internally a failed check throws [Rejection], which [validating]
 * turns into [OtaValidation.Rejected].
 */
internal object OtaValidator {

    const val MAX_VERSION = 1_000_000L
    const val MAX_BRANDS = 2_000
    const val MAX_LIST_ITEMS = 5_000
    const val MAX_STRING_LENGTH = 256
    const val MAX_WEIGHT = 50L

    /** Heuristic weights the engine's config reads; all must be present. */
    val REQUIRED_WEIGHTS = listOf(
        "httpNotHttps", "ipAddress", "urlShortener", "excessiveSubdomains",
        "nonStandardPort", "credentialParams", "punycode"
    )

    private val BRAND_LISTS = listOf("typosquats", "homographs", "combosquats")
    private val HEURISTIC_LISTS = listOf(
        "suspiciousTlds", "highRiskTlds", "urlShorteners", "credentialKeywords", "suspiciousPathKeywords"
    )

    private const val CHECKSUM_PREFIX = "sha256:"
    private val FILENAME = Regex("^[A-Za-z0-9][A-Za-z0-9._-]{0,63}\\.json$")
    private val SHA256_CHECKSUM = Regex("^sha256:[0-9a-fA-F]{64}$")

    private class Rejection(val reason: String) : Exception(reason)

    private fun reject(reason: String): Nothing = throw Rejection(reason)

    private fun <T> validating(block: () -> T): OtaValidation<T> {
        return try {
            OtaValidation.Ok(block())
        } catch (e: Rejection) {
            OtaValidation.Rejected(e.reason)
        }
    }

    /** Lowercase hex digest of a `sha256:<hex>` checksum, or null if it is not in that form. */
    fun parseSha256(checksum: String?): String? {
        if (checksum == null || !SHA256_CHECKSUM.matches(checksum)) return null
        return checksum.substring(CHECKSUM_PREFIX.length).lowercase()
    }

    fun parseManifest(text: String): OtaValidation<OtaManifest> = validating {
        val root = parseObject(text)
        val version = positiveInt(root["version"]) ?: reject("missing or invalid \"version\"")
        val files = root["files"] as? Map<*, *> ?: reject("missing \"files\" object")
        OtaManifest(
            version = version,
            brandDb = parseEntry(files["brand_db"], "brand_db"),
            heuristics = parseEntry(files["heuristics"], "heuristics")
        )
    }

    private fun parseEntry(raw: Any?, name: String): OtaFileEntry {
        val entry = raw as? Map<*, *> ?: reject("\"files.$name\" is missing")
        val filename = (entry["filename"] as? String)?.takeIf { FILENAME.matches(it) && !it.contains("..") }
            ?: reject("\"files.$name.filename\" is missing or not a plain .json file name")
        val version = entry["version"]?.let { positiveInt(it) ?: reject("\"files.$name.version\" is invalid") }
        val checksum = entry["checksum"]
        if (checksum != null && checksum !is String) reject("\"files.$name.checksum\" is not a string")
        val size = entry["sizeBytes"]?.let { it as? Long ?: reject("\"files.$name.sizeBytes\" is not an integer") }
        if (size != null && size < 0) reject("\"files.$name.sizeBytes\" is negative")
        return OtaFileEntry(version, filename, parseSha256(checksum), size)
    }

    /** Check the brand database: `brands` maps names to objects with a non-empty `official_domains`. */
    fun validateBrandDb(text: String, expectedVersion: Int?): OtaValidation<Unit> = validating {
        val root = parseObject(text)
        checkVersion(root, expectedVersion)
        val brands = root["brands"] as? Map<*, *> ?: reject("missing \"brands\" object")
        if (brands.isEmpty() || brands.size > MAX_BRANDS) reject("\"brands\" must hold 1 to $MAX_BRANDS entries")
        for ((key, value) in brands) validateBrand(key, value)
    }

    private fun validateBrand(key: Any?, value: Any?) {
        val name = (key as? String)?.takeIf { it.isNotEmpty() && it.length <= MAX_STRING_LENGTH }
            ?: reject("a brand name is empty or too long")
        val brand = value as? Map<*, *> ?: reject("brand \"$name\" is not an object")
        if (!isStringList(brand["official_domains"], requireNonEmpty = true)) {
            reject("brand \"$name\" has no valid \"official_domains\"")
        }
        BRAND_LISTS.firstOrNull { brand[it] != null && !isStringList(brand[it], requireNonEmpty = false) }
            ?.let { reject("brand \"$name\" has an invalid \"$it\" list") }
    }

    /** Check the heuristics file: `weights` holds every required weight as an integer in 0..[MAX_WEIGHT]. */
    fun validateHeuristics(text: String, expectedVersion: Int?): OtaValidation<Unit> = validating {
        val root = parseObject(text)
        checkVersion(root, expectedVersion)
        val weights = root["weights"] as? Map<*, *> ?: reject("missing \"weights\" object")
        REQUIRED_WEIGHTS.firstOrNull { !weights.containsKey(it) }?.let { reject("weight \"$it\" is missing") }
        if (weights.size > MAX_LIST_ITEMS) reject("too many weights")
        for ((key, value) in weights) {
            if (value !is Long || value !in 0L..MAX_WEIGHT) {
                reject("weight \"$key\" is not an integer from 0 to $MAX_WEIGHT")
            }
        }
        HEURISTIC_LISTS.firstOrNull { root[it] != null && !isStringList(root[it], requireNonEmpty = false) }
            ?.let { reject("\"$it\" is not a list of short strings") }
    }

    // === helpers ===

    private fun parseObject(text: String): Map<*, *> {
        val parsed = try {
            OtaJson.parse(text)
        } catch (e: OtaJsonException) {
            reject("not well-formed JSON: ${e.message}")
        }
        return parsed as? Map<*, *> ?: reject("not a JSON object")
    }

    private fun checkVersion(root: Map<*, *>, expected: Int?) {
        val version = positiveInt(root["version"]) ?: reject("missing or invalid \"version\"")
        if (expected != null && version != expected) {
            reject("\"version\" is $version but the manifest lists $expected")
        }
    }

    private fun positiveInt(value: Any?): Int? {
        return if (value is Long && value in 1..MAX_VERSION) value.toInt() else null
    }

    /** True for a list of at most [MAX_LIST_ITEMS] non-empty strings of at most [MAX_STRING_LENGTH] chars. */
    private fun isStringList(value: Any?, requireNonEmpty: Boolean): Boolean {
        val list = value as? List<*> ?: return false
        if (list.size > MAX_LIST_ITEMS || (requireNonEmpty && list.isEmpty())) return false
        return list.all { it is String && it.isNotEmpty() && it.length <= MAX_STRING_LENGTH }
    }
}
