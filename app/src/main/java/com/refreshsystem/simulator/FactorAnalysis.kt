
package com.refreshsystem.simulator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

private val AnalysisPanel = Color(0xFF151F33)
private val AnalysisCyan = Color(0xFF52E5F2)
private val AnalysisGreen = Color(0xFF62E6A5)
private val AnalysisOrange = Color(0xFFFFB86B)
private val AnalysisMuted = Color(0xFF9EABC2)
private val AnalysisGrid = Color(0xFF34445D)

private data class FactorEffect(
    val factor: String,
    val context: String,
    val trials: Int,
    val stabilityMean: Float,
    val stabilitySd: Float,
    val stabilityPositive: Float,
    val memoryMean: Float,
    val memorySd: Float,
    val memoryPositive: Float,
    val continuityMean: Float,
    val continuitySd: Float,
    val continuityPositive: Float
)

private data class PairedMemoryTrial(
    val seed: Int,
    val decay: Boolean,
    val refreshOnCurve: List<Float>,
    val refreshOffCurve: List<Float>
) {
    val differenceCurve: List<Float>
        get() {
            val length = minOf(refreshOnCurve.size, refreshOffCurve.size)
            return List(length) { index ->
                (refreshOnCurve[index] - refreshOffCurve[index]) * 100f
            }
        }

    val finalDifference: Float
        get() = differenceCurve.lastOrNull() ?: 0f
}

private fun mean(values: List<Float>): Float =
    if (values.isEmpty()) 0f else values.average().toFloat()

private fun sampleSd(values: List<Float>): Float {
    if (values.size < 2) return 0f

    val average = mean(values)
    val variance = values.sumOf {
        val difference = (it - average).toDouble()
        difference * difference
    } / (values.size - 1)

    return sqrt(variance).toFloat()
}

private fun positiveRate(values: List<Float>): Float {
    if (values.isEmpty()) return 0f
    return values.count { it > 0f }.toFloat() / values.size
}

private fun calculateFactorEffects(
    results: List<ResearchResult>
): List<FactorEffect> {
    val effects = mutableListOf<FactorEffect>()

    val factors = listOf(
        "External Input" to 0,
        "Internal Refresh" to 1,
        "Natural Decay" to 2
    )

    for ((factorName, factorIndex) in factors) {
        for (off in ResearchExperimentEngine.conditions) {
            val offValue = when (factorIndex) {
                0 -> off.external
                1 -> off.refresh
                else -> off.decay
            }

            if (offValue) continue

            val on = ResearchExperimentEngine.conditions.firstOrNull { candidate ->
                val candidateValue = when (factorIndex) {
                    0 -> candidate.external
                    1 -> candidate.refresh
                    else -> candidate.decay
                }

                val sameOtherSettings = when (factorIndex) {
                    0 -> candidate.refresh == off.refresh &&
                        candidate.decay == off.decay
                    1 -> candidate.external == off.external &&
                        candidate.decay == off.decay
                    else -> candidate.external == off.external &&
                        candidate.refresh == off.refresh
                }

                candidateValue && sameOtherSettings
            } ?: continue

            val offBySeed = results
                .filter { it.condition.id == off.id }
                .associateBy { it.seed }

            val onBySeed = results
                .filter { it.condition.id == on.id }
                .associateBy { it.seed }

            val commonSeeds = offBySeed.keys
                .intersect(onBySeed.keys)
                .sorted()

            if (commonSeeds.isEmpty()) continue

            val stabilityDeltas = commonSeeds.map { seed ->
                onBySeed.getValue(seed).finalStability -
                    offBySeed.getValue(seed).finalStability
            }

            val memoryDeltas = commonSeeds.map { seed ->
                onBySeed.getValue(seed).finalMemory -
                    offBySeed.getValue(seed).finalMemory
            }

            val continuityDeltas = commonSeeds.map { seed ->
                onBySeed.getValue(seed).finalContinuity -
                    offBySeed.getValue(seed).finalContinuity
            }

            val context = when (factorIndex) {
                0 -> "Refresh ${if (off.refresh) "ON" else "OFF"} • Decay ${if (off.decay) "ON" else "OFF"}"
                1 -> "External ${if (off.external) "ON" else "OFF"} • Decay ${if (off.decay) "ON" else "OFF"}"
                else -> "External ${if (off.external) "ON" else "OFF"} • Refresh ${if (off.refresh) "ON" else "OFF"}"
            }

            effects.add(
                FactorEffect(
                    factor = factorName,
                    context = context,
                    trials = commonSeeds.size,
                    stabilityMean = mean(stabilityDeltas),
                    stabilitySd = sampleSd(stabilityDeltas),
                    stabilityPositive = positiveRate(stabilityDeltas),
                    memoryMean = mean(memoryDeltas),
                    memorySd = sampleSd(memoryDeltas),
                    memoryPositive = positiveRate(memoryDeltas),
                    continuityMean = mean(continuityDeltas),
                    continuitySd = sampleSd(continuityDeltas),
                    continuityPositive = positiveRate(continuityDeltas)
                )
            )
        }
    }

    return effects
}

private fun pairedMemoryTrials(
    results: List<ResearchResult>
): List<PairedMemoryTrial> {
    val output = mutableListOf<PairedMemoryTrial>()

    for (decayEnabled in listOf(true, false)) {
        val refreshOn = results.filter {
            !it.condition.external &&
                it.condition.refresh &&
                it.condition.decay == decayEnabled
        }.associateBy { it.seed }

        val refreshOff = results.filter {
            !it.condition.external &&
                !it.condition.refresh &&
                it.condition.decay == decayEnabled
        }.associateBy { it.seed }

        val commonSeeds = refreshOn.keys.intersect(refreshOff.keys).sorted()

        for (seed in commonSeeds) {
            val on = refreshOn.getValue(seed)
            val off = refreshOff.getValue(seed)

            if (on.trainingEndMemory <= 0.000001f ||
                off.trainingEndMemory <= 0.000001f
            ) continue

            if (on.memoryCurve.isEmpty() || off.memoryCurve.isEmpty()) continue

            val length = minOf(on.memoryCurve.size, off.memoryCurve.size)
            if (length < 2) continue

            val onCurve = List(length) { index ->
                on.memoryCurve[index] / on.trainingEndMemory
            }

            val offCurve = List(length) { index ->
                off.memoryCurve[index] / off.trainingEndMemory
            }

            output.add(
                PairedMemoryTrial(
                    seed = seed,
                    decay = decayEnabled,
                    refreshOnCurve = onCurve,
                    refreshOffCurve = offCurve
                )
            )
        }
    }

    return output
}

@Composable
fun FactorAnalysisPanel(results: List<ResearchResult>) {
    if (results.isEmpty()) return

    val effects = calculateFactorEffects(results)

    val factors = listOf(
        "External Input",
        "Internal Refresh",
        "Natural Decay"
    )

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        MemoryRetentionPanel(results)
        PairedMemoryRetentionPanel(results)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = AnalysisPanel
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "STATISTICAL RELIABILITY ANALYSIS",
                    color = AnalysisCyan,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Paired comparisons use the same seed for factor ON and factor OFF.",
                    color = AnalysisMuted
                )

                Text(
                    "Mean Effect = average ON minus OFF. SD = sample standard deviation. Positive Trial % = percentage of paired trials with a positive difference.",
                    color = AnalysisMuted
                )

                factors.forEach { factor ->
                    val group = effects.filter { it.factor == factor }

                    HorizontalDivider(color = AnalysisGrid)

                    Text(
                        factor,
                        color = AnalysisCyan,
                        fontWeight = FontWeight.Bold
                    )

                    group.forEach { effect ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Text(
                                effect.context,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )

                            Text(
                                "Paired trials: ${effect.trials}",
                                color = AnalysisMuted
                            )

                            ReliabilityMetric(
                                "Stability",
                                effect.stabilityMean,
                                effect.stabilitySd,
                                effect.stabilityPositive
                            )

                            ReliabilityMetric(
                                "Memory",
                                effect.memoryMean,
                                effect.memorySd,
                                effect.memoryPositive
                            )

                            ReliabilityMetric(
                                "Continuity",
                                effect.continuityMean,
                                effect.continuitySd,
                                effect.continuityPositive
                            )
                        }
                    }
                }

                HorizontalDivider(color = AnalysisGrid)

                Text(
                    "Positive Trial % measures consistency of direction, not statistical significance. These results describe this computational model only.",
                    color = AnalysisOrange
                )
            }
        }
    }
}

@Composable
private fun MemoryRetentionPanel(results: List<ResearchResult>) {
    val externalOff = results.filter { !it.condition.external }
    if (externalOff.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AnalysisPanel
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "MEMORY RETENTION CURVE ANALYSIS",
                color = AnalysisCyan,
                fontWeight = FontWeight.Bold
            )

            Text(
                "External input is OFF during these test conditions. Curves show mean memory relative to the memory level measured immediately after training.",
                color = AnalysisMuted
            )

            listOf(true, false).forEach { decayEnabled ->
                HorizontalDivider(color = AnalysisGrid)

                Text(
                    "Natural Decay ${if (decayEnabled) "ON" else "OFF"}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                val refreshOn = externalOff.filter {
                    it.condition.refresh && it.condition.decay == decayEnabled
                }

                val refreshOff = externalOff.filter {
                    !it.condition.refresh && it.condition.decay == decayEnabled
                }

                MemoryCurveChart(refreshOn, refreshOff)
                CurveLegend()

                RetentionSummary(
                    "Internal Refresh ON",
                    refreshOn,
                    AnalysisGreen
                )

                RetentionSummary(
                    "Internal Refresh OFF",
                    refreshOff,
                    AnalysisOrange
                )
            }

            HorizontalDivider(color = AnalysisGrid)

            Text(
                "50% threshold: first test cycle where memory falls below half of its post-training baseline. 'Not crossed' means it stayed at or above that level for all recorded test cycles.",
                color = AnalysisMuted
            )

            Text(
                "This is an operational model threshold, not a universal biological memory limit. A ratio above 100% means measured memory magnitude increased relative to the training baseline; it does not prove information fidelity.",
                color = AnalysisOrange
            )
        }
    }
}

@Composable
private fun PairedMemoryRetentionPanel(results: List<ResearchResult>) {
    val paired = pairedMemoryTrials(results)
    if (paired.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AnalysisPanel
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "PAIRED REFRESH EFFECT ON MEMORY",
                color = AnalysisCyan,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Each pair uses the same seed and training baseline. External input is OFF. Difference = normalized memory with Refresh ON minus normalized memory with Refresh OFF.",
                color = AnalysisMuted
            )

            listOf(true, false).forEach { decayEnabled ->
                val group = paired.filter { it.decay == decayEnabled }
                if (group.isEmpty()) return@forEach

                HorizontalDivider(color = AnalysisGrid)

                Text(
                    "Natural Decay ${if (decayEnabled) "ON" else "OFF"}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                PairedDifferenceChart(group)

                val finalDifferences = group.map { it.finalDifference }
                val earlyDifferences = group.mapNotNull {
                    val curve = it.differenceCurve
                    if (curve.isEmpty()) null
                    else mean(curve.take(minOf(20, curve.size)))
                }

                Text("Valid paired seeds: ${group.size}", color = AnalysisMuted)

                Text(
                    "Mean final difference: ${"%+.2f".format(mean(finalDifferences))} percentage points",
                    color = AnalysisGreen
                )

                Text(
                    "Across-pair SD: ${"%.2f".format(sampleSd(finalDifferences))} percentage points",
                    color = AnalysisMuted
                )

                Text(
                    "Refresh ON higher: ${(positiveRate(finalDifferences) * 100f).toInt()}% of pairs",
                    color = AnalysisMuted
                )

                Text(
                    "Mean difference over first 20 cycles: ${"%+.2f".format(mean(earlyDifferences))} percentage points",
                    color = AnalysisMuted
                )
            }

            Text(
                "Descriptive paired comparison only. A positive difference indicates greater measured memory magnitude, not necessarily better information accuracy or fidelity. Small trial counts do not establish statistical significance.",
                color = AnalysisOrange
            )
        }
    }
}

@Composable
private fun PairedDifferenceChart(trials: List<PairedMemoryTrial>) {
    if (trials.isEmpty()) return

    val length = trials.minOf { it.differenceCurve.size }
    if (length < 2) return

    val averageCurve = List(length) { index ->
        mean(trials.map { it.differenceCurve[index] })
    }

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        val left = 8.dp.toPx()
        val right = size.width - 8.dp.toPx()
        val top = 12.dp.toPx()
        val bottom = size.height - 12.dp.toPx()
        val height = bottom - top
        val width = right - left

        val maxAbs = (averageCurve.maxOf { kotlin.math.abs(it) } * 1.15f)
            .coerceAtLeast(1f)

        val zeroY = top + height / 2f

        drawLine(
            AnalysisGrid,
            Offset(left, zeroY),
            Offset(right, zeroY),
            strokeWidth = 1.5.dp.toPx()
        )

        val points = averageCurve.mapIndexed { index, value ->
            val x = left + width * index / (length - 1).toFloat()
            val y = zeroY - (value / maxAbs) * (height / 2f)
            Offset(x, y)
        }

        for (index in 0 until points.lastIndex) {
            drawLine(
                AnalysisGreen,
                points[index],
                points[index + 1],
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }

    Text(
        "Above zero = Refresh ON has higher normalized memory magnitude.",
        color = AnalysisMuted
    )
}

@Composable
private fun MemoryCurveChart(
    refreshOn: List<ResearchResult>,
    refreshOff: List<ResearchResult>
) {
    val onCurve = averageNormalizedCurve(refreshOn)
    val offCurve = averageNormalizedCurve(refreshOff)

    if (onCurve.isEmpty() && offCurve.isEmpty()) return

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(190.dp)
    ) {
        val left = 8.dp.toPx()
        val right = size.width - 8.dp.toPx()
        val top = 12.dp.toPx()
        val bottom = size.height - 12.dp.toPx()
        val graphHeight = bottom - top
        val graphWidth = right - left

        for (i in 0..4) {
            val y = top + graphHeight * i / 4f
            drawLine(
                AnalysisGrid,
                Offset(left, y),
                Offset(right, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val thresholdY = bottom - 0.5f * graphHeight
        drawLine(
            AnalysisOrange.copy(alpha = 0.8f),
            Offset(left, thresholdY),
            Offset(right, thresholdY),
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round
        )

        fun drawCurve(values: List<Float>, color: Color) {
            if (values.size < 2) return

            val points = values.mapIndexed { index, value ->
                val x = left + graphWidth * index / (values.size - 1).toFloat()
                val y = bottom - value.coerceIn(0f, 2f) / 2f * graphHeight
                Offset(x, y)
            }

            for (i in 0 until points.lastIndex) {
                drawLine(
                    color,
                    points[i],
                    points[i + 1],
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        drawCurve(onCurve, AnalysisGreen)
        drawCurve(offCurve, AnalysisOrange)
    }
}

private fun averageNormalizedCurve(
    trials: List<ResearchResult>
): List<Float> {
    val valid = trials.filter {
        it.trainingEndMemory > 0.000001f && it.memoryCurve.isNotEmpty()
    }

    if (valid.isEmpty()) return emptyList()

    val length = valid.minOf { it.memoryCurve.size }

    return List(length) { index ->
        valid.map { trial ->
            trial.memoryCurve[index] / trial.trainingEndMemory
        }.average().toFloat()
    }
}

@Composable
private fun CurveLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("━━", color = AnalysisGreen)
            Text("Refresh ON", color = AnalysisMuted)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("━━", color = AnalysisOrange)
            Text("Refresh OFF", color = AnalysisMuted)
        }
    }
}

@Composable
private fun RetentionSummary(
    label: String,
    trials: List<ResearchResult>,
    color: Color
) {
    if (trials.isEmpty()) return

    val valid = trials.filter { it.trainingEndMemory > 0.000001f }

    val finalRatios = valid.map {
        it.finalMemory / it.trainingEndMemory
    }

    val thresholdCycles = valid.map { trial ->
        val threshold = trial.trainingEndMemory * 0.5f
        val crossingIndex = trial.memoryCurve.indexOfFirst { it < threshold }

        if (crossingIndex < 0) null else crossingIndex + 1
    }

    val crossed = thresholdCycles.filterNotNull()
    val notCrossed = thresholdCycles.size - crossed.size

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = color, fontWeight = FontWeight.Bold)

        Text(
            "Trials: ${trials.size} | Mean final/baseline: ${
                if (finalRatios.isEmpty()) "N/A"
                else "%.1f%%".format(finalRatios.average() * 100.0)
            }",
            color = Color.White
        )

        Text(
            "50% threshold crossed: ${crossed.size} / ${thresholdCycles.size}",
            color = AnalysisMuted
        )

        Text(
            if (crossed.isEmpty()) {
                "First crossing cycle: Not crossed"
            } else {
                "Mean first crossing cycle: %.1f".format(crossed.average())
            } + " | Not crossed: $notCrossed",
            color = AnalysisMuted
        )
    }
}

@Composable
private fun ReliabilityMetric(
    label: String,
    mean: Float,
    sd: Float,
    positive: Float
) {
    val color = when {
        mean > 0.0001f -> AnalysisGreen
        mean < -0.0001f -> AnalysisOrange
        else -> AnalysisMuted
    }

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = Color.White, fontWeight = FontWeight.Medium)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Mean effect", color = AnalysisMuted)
            Text(
                "%+.3f%%".format(mean * 100f),
                color = color,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Standard deviation", color = AnalysisMuted)
            Text("%.3f%%".format(sd * 100f), color = Color.White)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Positive trials", color = AnalysisMuted)
            Text(
                "${(positive * 100f).toInt()}%",
                color = AnalysisCyan
            )
        }
    }
}
