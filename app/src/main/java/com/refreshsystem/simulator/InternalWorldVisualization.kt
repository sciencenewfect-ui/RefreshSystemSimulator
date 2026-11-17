
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
import kotlin.math.sqrt

private val WorldBg = Color(0xFF0D1729)
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

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(WorldBg, RoundedCornerShape(14.dp))
                .padding(10.dp)
        ) {
            if (count == 0) return@Canvas

            val center = Offset(size.width / 2f, size.height / 2f)
            val minSide = size.minDimension

            val groupRadius = minSide * 0.255f
            val localRadius = minSide * 0.078f
            val movementRange = minSide * 0.065f
            val nodeRadius = minSide * 0.033f

            // Chaar samoohon ke kendr apni jagah par bane rahte hain.
            // Unke andar ki ikaiyan pattern aur memory ke anusaar chalti hain.
            val groupCenters = List(4) { group ->
                val angle = group * (Math.PI / 2.0) - Math.PI / 2.0

                Offset(
                    center.x + cos(angle).toFloat() * groupRadius,
                    center.y + sin(angle).toFloat() * groupRadius
                )
            }

            // Har node ki position current pattern aur memory se nikalti hai.
            val points = List(count) { index ->
                val group = index / 4
                val member = index % 4

                val groupAngle =
                    group * (Math.PI / 2.0) - Math.PI / 2.0

                val memberAngle =
                    member * (Math.PI / 2.0) - Math.PI / 2.0

                val p = pattern[index].coerceIn(-1f, 1f)
                val m = memory[index].coerceIn(-1f, 1f)

                val first = group * 4
                val last = minOf(first + 4, count)

                var groupPattern = 0f
                var groupMemory = 0f

                for (i in first until last) {
                    groupPattern += pattern[i]
                    groupMemory += memory[i]
                }

                val members = (last - first).coerceAtLeast(1)
                groupPattern /= members
                groupMemory /= members

                // Poore samooh ka halka movement.
                val groupShift =
                    (groupPattern * 0.55f + groupMemory * 0.30f) *
                        movementRange * 0.45f

                val groupSideShift =
                    (groupPattern - groupMemory) *
                        movementRange * 0.25f

                val gx = groupCenters[group].x +
                    cos(groupAngle).toFloat() * groupShift -
                    sin(groupAngle).toFloat() * groupSideShift

                val gy = groupCenters[group].y +
                    sin(groupAngle).toFloat() * groupShift +
                    cos(groupAngle).toFloat() * groupSideShift

                // Ikai ki apni position mein badlav.
                val radialChange =
                    (p * 0.55f + m * 0.35f) * movementRange

                val sideChange =
                    (p - m) * 0.30f * movementRange

                val radius = localRadius + radialChange

                val baseX = cos(memberAngle).toFloat() * radius
                val baseY = sin(memberAngle).toFloat() * radius

                val shiftX =
                    cos(memberAngle).toFloat() * sideChange

                val shiftY =
                    sin(memberAngle).toFloat() * sideChange

                Offset(
                    gx + baseX - shiftY,
                    gy + baseY + shiftX
                )
            }

            // Samoohon ka kshetra unki current activity ke saath badalta hai.
            for (group in 0 until 4) {
                val first = group * 4
                val last = minOf(first + 4, count)

                if (first < last) {
                    var averageActivity = 0f

                    for (i in first until last) {
                        averageActivity +=
                            abs(pattern[i]) + abs(memory[i])
                    }

                    averageActivity /= ((last - first) * 2f)

                    val members = (first until last).toList()
                    val gx = members.map { points[it].x }
                        .average().toFloat()
                    val gy = members.map { points[it].y }
                        .average().toFloat()

                    drawCircle(
                        color = WorldPurple.copy(
                            alpha = 0.06f + averageActivity * 0.10f
                        ),
                        radius = minSide * (
                            0.115f + averageActivity * 0.045f
                        ),
                        center = Offset(gx, gy)
                    )
                }
            }

            // Seekhe hue sambandhon ki maximum strength.
            var maximumWeight = 0f

            for (i in 0 until minOf(count, weights.size)) {
                for (j in 0 until minOf(count, weights[i].size)) {
                    maximumWeight = maxOf(
                        maximumWeight,
                        abs(weights[i][j])
                    )
                }
            }

            // Connections: strength badalne par lakeeron ki motai
            // aur visibility bhi badalti hai.
            for (i in 0 until count) {
                for (j in i + 1 until count) {
                    if (
                        i >= weights.size ||
                        j >= weights[i].size
                    ) continue

                    val strength = abs(weights[i][j])

                    if (strength < 0.0005f) continue

                    val relativeStrength =
                        if (maximumWeight > 0f) {
                            (strength / maximumWeight).coerceIn(0f, 1f)
                        } else {
                            0f
                        }

                    val sameGroup = i / 4 == j / 4

                    val lineColor =
                        if (sameGroup) WorldGreen else WorldCyan

                    val alpha =
                        if (sameGroup) {
                            0.12f + relativeStrength * 0.70f
                        } else {
                            0.06f + relativeStrength * 0.55f
                        }

                    drawLine(
                        color = lineColor.copy(alpha = alpha),
                        start = points[i],
                        end = points[j],
                        strokeWidth =
                            0.5.dp.toPx() +
                                relativeStrength * 2.4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // Nodes: current pattern aur memory dono position
            // aur node ke size ko prabhavit karte hain.
            for (i in 0 until count) {
                val p = abs(pattern[i]).coerceIn(0f, 1f)
                val m = abs(memory[i]).coerceIn(0f, 1f)
                val point = points[i]

                drawCircle(
                    color = WorldGreen.copy(alpha = 0.10f + m * 0.42f),
                    radius = nodeRadius * (1.5f + m),
                    center = point
                )

                drawCircle(
                    color = WorldCyan.copy(alpha = 0.30f + p * 0.70f),
                    radius = nodeRadius * (0.65f + p * 0.55f),
                    center = point
                )

                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = 2.dp.toPx(),
                    center = point
                )
            }

            // Kendr: poore Internal World ka common connection.
            drawCircle(
                color = WorldPurple.copy(alpha = 0.20f),
                radius = nodeRadius * 2f,
                center = center
            )

            drawCircle(
                color = WorldPurple,
                radius = nodeRadius * 0.65f,
                center = center
            )
        }

        Text(
            "INTERNAL WORLD: 16 IKAIYAN, 4 SAMOOH",
            color = WorldCyan
        )

        Text(
            "Chaar samooh alag hain, lekin unke sambandh poore system ko jodte hain.",
            color = WorldMuted
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

        Text("RANGON AUR LAKEERON KA ARTH", color = Color.White)

        WorldLegend("Neela: vartaman pattern", WorldCyan)
        WorldLegend("Hara: memory ki matra", WorldGreen)
        WorldLegend("Baingani: samooh aur kendr", WorldPurple)
        WorldLegend("Lakeerein: seekhe hue sambandh", WorldLine)

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

        Text("ABHI SYSTEM KI STHITI", color = Color.White)

        WorldStatus("External Information", snapshot.externalInputActive)
        WorldStatus("Internal Refresh", snapshot.internalRefreshActive)
        WorldStatus("Natural Decay", snapshot.naturalDecayActive)

        Text(
            "Cycle: ${snapshot.cycle}  |  Refresh: ${snapshot.refreshCount}",
            color = WorldMuted
        )

        Text(
            "Ikaiyon ki position current pattern aur memory ke anusaar " +
                "badalti hai. Lakeerein engine ke seekhe hue sambandhon " +
                "ko dikhati hain. Chaar samoohon ka overall arrangement " +
                "bana rehta hai.",
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
