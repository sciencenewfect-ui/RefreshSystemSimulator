
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

private data class FactorEffect(
    val factor: String,
    val context: String,
    val trials: Int,
    val stability: Float,
    val memory: Float,
    val continuity: Float
)

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

            val commonSeeds = offBySeed.keys.intersect(onBySeed.keys)

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
                    stability = stabilityDeltas.average().toFloat(),
                    memory = memoryDeltas.average().toFloat(),
                    continuity = continuityDeltas.average().toFloat()
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
                "FACTOR EFFECT ANALYSIS",
                color = Color(0xFF52E5F2),
                fontWeight = FontWeight.Bold
            )

            Text(
                "Each comparison changes only one factor. Trials with the same seed are paired.",
                color = Color(0xFF9EABC2)
            )

            Text(
                "Positive value = factor ON produced a higher metric than factor OFF. Negative value = lower metric.",
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
                        verticalArrangement = Arrangement.spacedBy(5.dp)
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

                        EffectValue("Stability", effect.stability)
                        EffectValue("Memory", effect.memory)
                        EffectValue("Continuity", effect.continuity)
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF34445D))

            Text(
                "Interpretation limit: these are differences produced by the current simulator equations and parameters. They are not proof of real-world causation.",
                color = Color(0xFFFFB86B)
            )
        }
    }
}

@Composable
private fun EffectValue(label: String, delta: Float) {
    val color = when {
        delta > 0.0001f -> Color(0xFF62E6A5)
        delta < -0.0001f -> Color(0xFFFFB86B)
        else -> Color(0xFF9EABC2)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFF9EABC2))
        Text(
            "%+.2f%%".format(delta * 100f),
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
