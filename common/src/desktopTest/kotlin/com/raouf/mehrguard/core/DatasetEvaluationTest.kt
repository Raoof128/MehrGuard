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

import com.raouf.mehrguard.model.Verdict
import java.io.File
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Measures the engine on the committed dataset, `data/test_urls.csv`.
 *
 * Run with: `./gradlew :common:desktopTest --tests "*DatasetEvaluationTest*" --info`
 *
 * Every figure this test prints is computed here, from the engine's verdict on
 * each row. Nothing is read from documentation.
 *
 * What the numbers mean, and what they do not:
 * - The dataset is 100 URLs written by the project's author: 50 legitimate
 *   sites and 50 constructed phishing patterns on fake or defanged domains.
 * - The author also wrote the heuristics, so this is a check that the rules
 *   fire on the patterns they were written for. It is not a held-out benchmark
 *   and says nothing about live phishing feeds.
 * - A phishing row counts as detected when the verdict is MALICIOUS or
 *   SUSPICIOUS. A legitimate row counts as correct only when it is SAFE.
 */
class DatasetEvaluationTest {

    private val engine = PhishingEngine()

    private data class Row(val url: String, val phishing: Boolean, val category: String)

    private data class Outcome(val row: Row, val verdict: Verdict, val score: Int, val flags: List<String>) {
        val flagged: Boolean = verdict == Verdict.MALICIOUS || verdict == Verdict.SUSPICIOUS
        val correct: Boolean = if (row.phishing) flagged else verdict == Verdict.SAFE
    }

    private val rowPattern = Regex("^(.+?),([01]),([^,]*),(.*)$")

    private fun locateDataset(): File {
        // Gradle runs a module's tests from the module directory; allow the repository root too.
        return listOf("../data/test_urls.csv", "data/test_urls.csv")
            .map(::File)
            .firstOrNull { it.isFile }
            ?: error("data/test_urls.csv not found from ${File(".").absolutePath}")
    }

    private fun loadRows(): List<Row> = locateDataset().readLines()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("url,") }
        .map { line ->
            val match = rowPattern.matchEntire(line) ?: error("Unparseable dataset row: $line")
            Row(
                url = match.groupValues[1],
                phishing = match.groupValues[2] == "1",
                category = match.groupValues[3]
            )
        }

    private fun percent(numerator: Int, denominator: Int): String =
        if (denominator == 0) "n/a" else String.format(Locale.ROOT, "%.1f%%", 100.0 * numerator / denominator)

    @Test
    fun `MEASURE precision recall and F1 on the committed dataset`() {
        val rows = loadRows()
        assertEquals(100, rows.size, "data/test_urls.csv should hold 100 labelled URLs")
        assertEquals(50, rows.count { it.phishing }, "data/test_urls.csv should hold 50 phishing URLs")

        val outcomes = rows.map { row ->
            val assessment = engine.analyzeBlocking(row.url)
            Outcome(row, assessment.verdict, assessment.score, assessment.flags)
        }

        val tp = outcomes.count { it.row.phishing && it.flagged }
        val fn = outcomes.count { it.row.phishing && !it.flagged }
        val tn = outcomes.count { !it.row.phishing && it.verdict == Verdict.SAFE }
        val fp = outcomes.count { !it.row.phishing && it.verdict != Verdict.SAFE }

        val precision = if (tp + fp == 0) 0.0 else tp.toDouble() / (tp + fp)
        val recall = if (tp + fn == 0) 0.0 else tp.toDouble() / (tp + fn)
        val f1 = if (precision + recall == 0.0) 0.0 else 2 * precision * recall / (precision + recall)

        val report = buildString {
            appendLine("MEHR GUARD DATASET EVALUATION (data/test_urls.csv)")
            appendLine("rows=${rows.size} phishing=${tp + fn} legitimate=${tn + fp}")
            appendLine("TP=$tp FN=$fn TN=$tn FP=$fp")
            appendLine("precision=${percent(tp, tp + fp)} recall=${percent(tp, tp + fn)} " +
                "f1=${String.format(Locale.ROOT, "%.1f%%", 100.0 * f1)} accuracy=${percent(tp + tn, rows.size)}")
            appendLine("phishing rows by verdict: " + Verdict.entries.joinToString(" ") { v ->
                "$v=${outcomes.count { it.row.phishing && it.verdict == v }}"
            })
            appendLine("legitimate rows by verdict: " + Verdict.entries.joinToString(" ") { v ->
                "$v=${outcomes.count { !it.row.phishing && it.verdict == v }}"
            })
            appendLine("by category (correct/total):")
            outcomes.groupBy { it.row.category }.toSortedMap().forEach { (category, group) ->
                appendLine("  $category ${group.count { it.correct }}/${group.size}")
            }
            val misses = outcomes.filterNot { it.correct }
            appendLine("misclassified rows: ${misses.size}")
            misses.forEach {
                appendLine("  ${if (it.row.phishing) "MISSED" else "FALSE-ALARM"} ${it.verdict} ${it.row.category} ${it.row.url}")
                appendLine("    score=${it.score} signals=${it.flags.joinToString(" | ").ifEmpty { "(none)" }}")
            }
            append("END MEHR GUARD DATASET EVALUATION")
        }

        println(report)
        File("build/reports").mkdirs()
        File("build/reports/dataset-evaluation.txt").writeText(report + "\n")
    }
}
