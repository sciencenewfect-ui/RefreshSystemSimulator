
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val WorldBg = Color(0xFF0D1729)
private val WorldPanel = Color(0xFF18243A)
private val WorldCyan = Color(0xFF52E5F2)
private val WorldGreen = Color(0xFF62E6A5)
private val WorldPurple = Color(0xFFB69CFF)
private val WorldOrange = Color(0xFFFFB86B)
private val WorldMuted = Color(0xFF9EABC2)
private val WorldLine = Color(0xFF52647D)

@Composable
fun InternalWorldVisualization(
    snapshot: OpenSystemSnapshot,
    weights: List<List<Float>>
) {
    val pattern = snapshot.internalPattern
    val memory = snapshot.memoryState
    val count = minOf(pattern.size, memory.size, 16)
    val cycle = snapshot.cycle

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Internal world ka mukhya drishya
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(WorldBg, RoundedCornerShape(14.dp))
                .padding(10.dp)
        ) {
            if (count == 0) return@Canvas

            val center = Offset(size.width / 2f, size.height / 2f)
            val clusterRadius = size.minDimension * 0.29f
            val localRadius = size.minDimension * 0.075f
            val nodeRadius = size.minDimension * 0.035f

            // 4 samooh, har samooh mein 4 ikaiyan
            val points = List(count) { index ->
                val group = index / 4
                val member = index % 4

                val groupAngle =
                    group * (Math.PI / 2.0) - Math.PI / 2.0

                val memberAngle =
                    member * (Math.PI / 2.0) - Math.PI / 2.0

                val gx = center.x +
                    cos(groupAngle).toFloat() * clusterRadius

                val gy = center.y +
                    sin(groupAngle).toFloat() * clusterRadius

                Offset(
                    gx + cos(memberAngle).toFloat() * localRadius,
                    gy + sin(memberAngle).toFloat() * localRadius
                )
            }

            // Har samooh ka alag kshetra
            for (group in 0 until 4) {
                val first = group * 4
                val last = minOf(first + 4, count)

                if (first < last) {
                    val members = (first until last).toList()
                    val gx = members.map { points[it].x }.average().toFloat()
                    val gy = members.map { points[it].y }.average().toFloat()

                    drawCircle(
                        color = WorldPurple.copy(alpha = 0.10f),
                        radius = size.minDimension * 0.145f,
                        center = Offset(gx, gy)
                    )
                }
            }

            // Ek samooh ke andar sambandh
            for (group in 0 until 4) {
                val start = group * 4
                val end = minOf(start + 4, count)

                for (i in start until end) {
                    for (j in i + 1 until end) {
                        val w = if (
                            i < weights.size &&
                            j < weights[i].size
                        ) abs(weights[i][j]) else 0f

                        val strength = w.coerceIn(0f, 1f)

                        drawLine(
                            color = WorldGreen.copy(
                                alpha = 0.20f + strength * 0.65f
                            ),
                            start = points[i],
                            end = points[j],
                            strokeWidth = 1.2.dp.toPx() +
                                strength * 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // Alag samoohon ke beech chuninda sambandh
            for (i in 0 until count) {
                for (j in i + 1 until count) {
                    if (i / 4 == j / 4) continue

                    val w = if (
                        i < weights.size &&
                        j < weights[i].size
                    ) abs(weights[i][j]) else 0f

                    if (w > 0.003f) {
                        val strength = w.coerceIn(0f, 1f)

                        drawLine(
                            color = WorldCyan.copy(
                                alpha = 0.08f + strength * 0.55f
                            ),
                            start = points[i],
                            end = points[j],
                            strokeWidth = 0.5.dp.toPx() +
                                strength * 1.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // Ikaiyon ka vartaman pattern aur memory
            for (i in 0 until count) {
                val p = abs(pattern[i]).coerceIn(0f, 1f)
                val m = abs(memory[i]).coerceIn(0f, 1f)
                val point = points[i]

                // Memory ka bahari glow
                drawCircle(
                    color = WorldGreen.copy(
                        alpha = 0.12f + m * 0.42f
                    ),
                    radius = nodeRadius * (1.8f + m),
                    center = point
                )

                // Vartaman pattern
                drawCircle(
                    color = WorldCyan.copy(
                        alpha = 0.25f + p * 0.75f
                    ),
                    radius = nodeRadius * (0.65f + p * 0.55f),
                    center = point
                )

                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = 2.dp.toPx(),
                    center = point
                )
            }

            // Poore internal world ka kendr
            drawCircle(
                color = WorldPurple.copy(alpha = 0.20f),
                radius = nodeRadius * 1.8f,
                center = center
            )

            drawCircle(
                color = WorldPurple,
                radius = nodeRadius * 0.65f,
                center = center
            )
        }

        // Samoohon ki pehchan
        Text(
            "INTERNAL WORLD KE 4 SAMOOH",
            color = WorldCyan
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            WorldLegend("Samooh 1", WorldPurple)
            WorldLegend("Samooh 2", WorldCyan)
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            WorldLegend("Samooh 3", WorldGreen)
            WorldLegend("Samooh 4", WorldOrange)
        }

        Text(
            "Rangon ka arth",
            color = Color.White
        )

        WorldLegend("Neela: vartaman pattern", WorldCyan)
        WorldLegend("Hara: memory ki matra", WorldGreen)
        WorldLegend("Baingani: samooh aur kendr", WorldPurple)
        WorldLegend("Lakeerein: seekhe hue sambandh", WorldLine)

        // Badlav aur nirantarata
        Text(
            "SYSTEM MEIN KYA BADAL RAHA HAI?",
            color = WorldCyan
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
            "Pattern ki nirantarata",
            snapshot.identityContinuity,
            WorldPurple
        )

        WorldBar(
            "Dynamic Stability",
            snapshot.dynamicStability,
            WorldCyan
        )

        WorldBar(
            "Sambandhon ki taakat",
            snapshot.networkStrength,
            WorldGreen
        )

        // System ki vartaman sthiti
        Text(
            "ABHI SYSTEM KI STHITI",
            color = Color.White
        )

        WorldStatus(
            "External Information",
            snapshot.externalInputActive
        )

        WorldStatus(
            "Internal Refresh",
            snapshot.internalRefreshActive
        )

        WorldStatus(
            "Natural Decay",
            snapshot.naturalDecayActive
        )

        Text(
            "Cycle: $cycle  |  Refresh: ${snapshot.refreshCount}",
            color = WorldMuted
        )

        Text(
            "Dhyan dein: Jab simulation chalegi, tab pattern, memory " +
                "aur sambandhon mein hone wale badlav ko observe karein. " +
                "Ruki hui simulation mein ye drishya bhi ruk jayega.",
            color = WorldMuted
        )
    }
}

@Composable
private fun WorldLegend(
    label: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier
                .size(10.dp)
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

    Column(
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = WorldMuted)
            Text(
                "${(safe * 100).toInt()}%",
                color = color
            )
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

@Composable
private fun WorldStatus(
    label: String,
    active: Boolean
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = WorldMuted)
        Text(
            if (active) "CHALU" else "BAND",
            color = if (active) WorldGreen else WorldOrange
        )
    }
}
