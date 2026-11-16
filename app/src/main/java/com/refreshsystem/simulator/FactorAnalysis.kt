
package com.refreshsystem.simulator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

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
        val conditions = ResearchExperimentEngine.conditions

        for (off in conditions) {
            val offValue = when (factorIndex) {
                0 -> off.external
                1 -> off.refresh
                else -> off.decay
            }

            if (offValue) continue

            val on = conditions.firstOrNull { candidate ->
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

@Composable
fun FactorAnalysisPanel(results: List<ResearchResult>) {
    if (results.isEmpty()) return

    val effects = calculateFactorEffects(results)

    val factors = listOf(
        "External Input",
        "Internal Refresh",
        "Natural Decay"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF151F33)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "STATISTICAL RELIABILITY ANALYSIS",
                color = Color(0xFF52E5F2),
                fontWeight = FontWeight.Bold
            )

            Text(
                "Paired comparisons use the same seed for factor ON and factor OFF.",
                color = Color(0xFF9EABC2)
            )

            Text(
                "Mean Effect = average ON minus OFF. SD = sample standard deviation. Positive Trial % = percentage of paired trials with a positive difference.",
                color = Color(0xFF9EABC2)
            )

            factors.forEach { factor ->
                val group = effects.filter { it.factor == factor }

                HorizontalDivider(color = Color(0xFF34445D))

                Text(
                    factor,
                    color = Color(0xFF52E5F2),
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
                            color = Color(0xFF9EABC2)
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

            HorizontalDivider(color = Color(0xFF34445D))

            Text(
                "Important: Positive Trial % measures consistency of direction, not statistical significance. These results describe this computational model only.",
                color = Color(0xFFFFB86B)
            )
        }
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
        mean > 0.0001f -> Color(0xFF62E6A5)
        mean < -0.0001f -> Color(0xFFFFB86B)
        else -> Color(0xFF9EABC2)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.Medium)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Mean effect", color = Color(0xFF9EABC2))
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
            Text("Standard deviation", color = Color(0xFF9EABC2))
            Text(
                "%.3f%%".format(sd * 100f),
                color = Color.White
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Positive trials", color = Color(0xFF9EABC2))
            Text(
                "${(positive * 100f).toInt()}%",
                color = Color(0xFF52E5F2)
            )
        }
    }
}
