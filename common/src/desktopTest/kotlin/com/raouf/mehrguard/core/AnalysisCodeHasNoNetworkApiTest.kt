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


package com.raouf.mehrguard.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Scans the source of the analysis packages for networking APIs.
 *
 * `PhishingEngine` and everything it calls live in five packages of the shared
 * module: `core`, `engine`, `ml`, `model` and `security`. This test reads every
 * Kotlin file in those packages and fails if one of them names a networking API
 * or imports the two packages of this project that do talk to the network by
 * design (`ota`, the rule-update check, and `network`, the short-link resolver).
 *
 * This is a text scan. It shows that the analysis code does not name a
 * networking API. It is not a network monitor and cannot rule out I/O hidden
 * behind reflection or a dependency.
 */
class AnalysisCodeHasNoNetworkApiTest {

    private val analysisPackages = listOf("core", "engine", "ml", "model", "security")

    private val forbidden = listOf(
        Regex("""\bjava\.net\b"""),
        Regex("""\bjavax\.net\b"""),
        Regex("""\bio\.ktor\b"""),
        Regex("""\bokhttp3\b"""),
        Regex("""\bHttpURLConnection\b"""),
        Regex("""\bHttpClient\b"""),
        Regex("""\bURLConnection\b"""),
        Regex("""\bSocket\b"""),
        Regex("""\bNSURLSession\b"""),
        Regex("""\bXMLHttpRequest\b"""),
        Regex("""\bcom\.raouf\.mehrguard\.ota\b"""),
        Regex("""\bcom\.raouf\.mehrguard\.network\b""")
    )

    private fun locateSharedSources(): File {
        // Gradle runs a module's tests from the module directory; allow the repository root too.
        val relative = "src/commonMain/kotlin/com/raouf/mehrguard"
        return listOf(relative, "common/$relative")
            .map(::File)
            .firstOrNull { it.isDirectory }
            ?: error("Shared sources not found from ${File(".").absolutePath}")
    }

    @Test
    fun `analysis packages name no networking API`() {
        val root = locateSharedSources()
        val files = analysisPackages.flatMap { name ->
            val directory = File(root, name)
            assertTrue(directory.isDirectory, "Missing analysis package directory: $name")
            directory.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        }
        assertTrue(files.size >= 20, "Expected to scan the analysis sources, found only ${files.size} files")

        val hits = files.flatMap { file ->
            file.readLines().mapIndexedNotNull { index, line ->
                val pattern = forbidden.firstOrNull { it.containsMatchIn(line) }
                if (pattern == null) null else "${file.relativeTo(root)}:${index + 1}: ${line.trim()}"
            }
        }

        println("Scanned ${files.size} files in ${analysisPackages.joinToString()}: ${hits.size} networking references")
        assertTrue(hits.isEmpty(), "Networking API named in analysis code:\n" + hits.joinToString("\n"))
    }
}
