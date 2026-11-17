
package com.refreshsystem.simulator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val WorldBg = Color(0xFF0D1729)
private val WorldCyan = Color(0xFF52E5F2)
private val WorldGreen = Color(0xFF62E6A5)
private val WorldPurple = Color(0xFFB69CFF)
private val WorldOrange = Color(0xFFFFB86B)
private val WorldMuted = Color(0xFF9EABC2)

@Composable
fun InternalWorldVisualization(
    snapshot: OpenSystemSnapshot,
    weights: List<List<Float>>
) {
    val pattern = snapshot.internalPattern
    val memory = snapshot.memoryState
    val count = minOf(pattern.size, memory.size)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(310.dp)
                .background(WorldBg, RoundedCornerShape(14.dp))
                .padding(8.dp)
        ) {
            if (count == 0) return@Canvas

            val center = Offset(size.width / 2f, size.height / 2f)
            val clusterRadius = size.minDimension * 0.235f
            val nodeRadius = size.minDimension * 0.055f
            val innerRadius = size.minDimension * 0.075f

            val points = List(count) { index ->
                val cluster = index / 4
                val positionInCluster = index % 4

                val clusterAngle =
                    cluster * (2.0 * Math.PI / 4.0) - Math.PI / 2.0

                val localAngle =
                    positionInCluster * (2.0 * Math.PI / 4.0) -
                        Math.PI / 2.0

                val clusterX =
                    center.x + cos(clusterAngle).toFloat() * clusterRadius

                val clusterY =
                    center.y + sin(clusterAngle).toFloat() * clusterRadius

                Offset(
                    clusterX + cos(localAngle).toFloat() * innerRadius,
                    clusterY + sin(localAngle).toFloat() * innerRadius
                )
            }

            // Chaar samooh dikhana
            for (cluster in 0 until (count + 3) / 4) {
                val members =
                    (cluster * 4 until minOf(cluster * 4 + 4, count))

                // FIX: Range khaali hai ya nahi, iski sahi jaanch
                if (members.first <= members.last) {
                    val x =
                        members.map { points[it].x }.average().toFloat()

                    val y =
                        members.map { points[it].y }.average().toFloat()

                    drawCircle(
                        color = WorldPurple.copy(alpha = 0.07f),
                        radius = size.minDimension * 0.15f,
                        center = Offset(x, y)
                    )
                }
            }

            // Seekhe hue sambandh dikhana
            var maximumWeight = 0f

            for (i in 0 until minOf(count, weights.size)) {
                for (j in 0 until minOf(count, weights[i].size)) {
                    maximumWeight =
                        maxOf(maximumWeight, abs(weights[i][j]))
                }
            }

            for (i in 0 until minOf(count, weights.size)) {
                for (j in i + 1 until minOf(count, weights[i].size)) {
                    val strength = abs(weights[i][j])

                    val relativeStrength =
                        if (maximumWeight > 0f) {
                            strength / maximumWeight
                        } else {
                            0f
                        }

                    drawLine(
                        color = WorldGreen.copy(
                            alpha = 0.10f + relativeStrength * 0.70f
                        ),
                        start = points[i],
                        end = points[j],
                        strokeWidth = 0.7.dp.toPx() +
                            relativeStrength * 2.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // Neela gola = vartaman pattern
            // Hara glow = memory
            for (i in 0 until count) {
                val p = abs(pattern[i]).coerceIn(0f, 1f)
                val m = abs(memory[i]).coerceIn(0f, 1f)
                val point = points[i]

                drawCircle(
                    color = WorldGreen.copy(alpha = 0.10f + m * 0.55f),
                    radius = nodeRadius * (1.2f + m),
                    center = point
                )

                drawCircle(
                    color = WorldCyan.copy(alpha = 0.25f + p * 0.75f),
                    radius = nodeRadius * (0.45f + p * 0.55f),
                    center = point
                )

                drawCircle(
                    color = Color.White.copy(alpha = 0.65f),
                    radius = 2.dp.toPx(),
                    center = point
                )
            }

            // Poore internal world ka centre
            drawCircle(
                color = WorldPurple.copy(alpha = 0.20f),
                radius = nodeRadius * 0.85f,
                center = center
            )

            drawCircle(
                color = WorldPurple,
                radius = nodeRadius * 0.35f,
                center = center
            )
        }

        Text(
            "16 ikaiyan • 4 samooh • Asli sambandh",
            color = WorldMuted
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            WorldLegend("Pattern", WorldCyan)
            WorldLegend("Memory", WorldGreen)
            WorldLegend("Sambandh", WorldPurple)
        }

        Text(
            "Andar ka badlav",
            color = Color.White
        )

        WorldBar(
            "Pattern mein badlav",
            (snapshot.patternChange / 0.25f).coerceIn(0f, 1f),
            WorldOrange
        )

        WorldBar(
            "Memory ki matra",
            snapshot.memoryRetention,
            WorldGreen
        )

        WorldBar(
            "Poore pattern ki nirantarata",
            snapshot.identityContinuity,
            WorldPurple
        )

        WorldBar(
            "Dynamic Stability",
            snapshot.dynamicStability,
            WorldCyan
        )

        Text(
            "Cycle: ${snapshot.cycle}  |  Refresh: ${snapshot.refreshCount}",
            color = WorldMuted
        )

        Text(
            "Lakeeron ki chamak sambandhon ki taakat batati hai. " +
                "Golon ka badalta rang pattern aur memory ki sthiti dikhata hai.",
            color = WorldMuted
        )
    }
}

@Composable
private fun WorldLegend(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            Modifier
                .size(9.dp)
                .background(color, RoundedCornerShape(50))
        )

        Text(label, color = WorldMuted)
    }
}

@Composable
private fun WorldBar(
    label: String,
    value: Float,
    color: Color
) {
    val safe = value.coerceIn(0f, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = WorldMuted)
            Text("${(safe * 100).toInt()}%", color = color)
        }

        LinearProgressIndicator(
            progress = safe,
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp),
            color = color,
            trackColor = Color(0xFF34445D)
        )
    }
}
