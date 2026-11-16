
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

private val FidelityPanel = Color(0xFF151F33)
private val FidelityCyan = Color(0xFF52E5F2)
private val FidelityGreen = Color(0xFF62E6A5)
private val FidelityOrange = Color(0xFFFFB86B)
private val FidelityMuted = Color(0xFF9EABC2)
private val FidelityGrid = Color(0xFF34445D)

@Composable
fun MemoryFidelityAnalysis(results: List<ResearchResult>) {
    val valid = results.filter { it.memoryFidelityCurve.isNotEmpty() }
    if (valid.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FidelityPanel),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "MEMORY FIDELITY ANALYSIS",
                color = FidelityCyan,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Fidelity compares each test-cycle memory vector with the original memory vector saved immediately after the common training phase.",
                color = FidelityMuted
            )

            Text(
                "Cosine similarity scale: +1 = same direction, 0 = orthogonal, -1 = opposite direction. This measures pattern direction, not memory quantity.",
                color = FidelityMuted
            )

            listOf(true, false).forEach { decayEnabled ->
                val group = valid.filter {
                    !it.condition.external &&
                        it.condition.decay == decayEnabled
                }

                if (group.isEmpty()) return@forEach

                HorizontalDivider(color = FidelityGrid)

                Text(
                    "External Input OFF • Natural Decay ${if (decayEnabled) "ON" else "OFF"}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                val refreshOn = group.filter { it.condition.refresh }
                val refreshOff = group.filter { !it.condition.refresh }

                FidelityCurveChart(refreshOn, refreshOff)

                FidelityLegend()

                FidelitySummary("Refresh ON", refreshOn, FidelityGreen)
                FidelitySummary("Refresh OFF", refreshOff, FidelityOrange)
            }

            Text(
                "Interpretation: a high fidelity score means the memory vector remains directionally similar to the trained baseline. It does not establish semantic accuracy or prove that information is understood.",
                color = FidelityOrange
            )
        }
    }
}

@Composable
private fun FidelityCurveChart(
    refreshOn: List<ResearchResult>,
    refreshOff: List<ResearchResult>
) {
    val onCurve = averageCurve(refreshOn)
    val offCurve = averageCurve(refreshOff)

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

        // Grid and zero-fidelity reference.
        for (i in 0..4) {
            val y = top + graphHeight * i / 4f
            drawLine(
                FidelityGrid,
                Offset(left, y),
                Offset(right, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        fun drawCurve(values: List<Float>, color: Color) {
            if (values.size < 2) return

            val points = values.mapIndexed { index, value ->
                val x = left + graphWidth * index / (values.size - 1).toFloat()
                val normalized = (value.coerceIn(-1f, 1f) + 1f) / 2f
                val y = bottom - normalized * graphHeight
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

        drawCurve(onCurve, FidelityGreen)
        drawCurve(offCurve, FidelityOrange)
    }
}

private fun averageCurve(trials: List<ResearchResult>): List<Float> {
    if (trials.isEmpty()) return emptyList()

    val length = trials.minOf { it.memoryFidelityCurve.size }
    if (length == 0) return emptyList()

    return List(length) { index ->
        trials.map { it.memoryFidelityCurve[index] }.average().toFloat()
    }
}

@Composable
private fun FidelityLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Text("━━ Refresh ON", color = FidelityGreen)
        Text("━━ Refresh OFF", color = FidelityOrange)
    }
}

@Composable
private fun FidelitySummary(
    label: String,
    trials: List<ResearchResult>,
    color: Color
) {
    if (trials.isEmpty()) return

    val finalValues = trials.map { it.finalMemoryFidelity }
    val meanFinal = finalValues.average()
    val meanAcrossTest = trials.map { it.meanMemoryFidelity }.average()
    val positive = finalValues.count { it > 0f }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = color, fontWeight = FontWeight.Bold)
        Text("Trials: ${trials.size}", color = Color.White)
        Text(
            "Mean final fidelity: ${"%+.3f".format(meanFinal)}",
            color = Color.White
        )
        Text(
            "Mean fidelity across 80 test cycles: ${"%+.3f".format(meanAcrossTest)}",
            color = FidelityMuted
        )
        Text(
            "Positive final similarity: $positive / ${trials.size}",
            color = FidelityMuted
        )
    }
}
