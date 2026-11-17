
package com.refreshsystem.simulator

import android.graphics.Paint
import android.graphics.Typeface
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
import androidx.compose.ui.graphics.nativeCanvas
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
private val WorldLine = Color(0xFF52647D)

private val GroupColors = listOf(
    WorldPurple,
    WorldCyan,
    WorldGreen,
    WorldOrange
)

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
        Text(
            "SYSTEM KA ANDARUNI NAKSHA",
            color = WorldCyan
        )

        Text(
            "Har samooh mein 4 ikaiyan hain. Ikaiyan aapas mein judkar " +
                "ek bada Internal World banati hain.",
            color = WorldMuted
        )

        Canvas(
            Modifier
                .fillMaxWidth()
                .height(350.dp)
                .background(WorldBg, RoundedCornerShape(14.dp))
                .padding(8.dp)
        ) {
            if (count == 0) return@Canvas

            val center = Offset(
                size.width / 2f,
                size.height / 2f
            )

            val minSide = size.minDimension
            val groupRadius = minSide * 0.275f
            val localRadius = minSide * 0.095f
            val nodeRadius = minSide * 0.043f
            val movementRange = minSide * 0.035f

            var maximumValue = 0.05f

            for (i in 0 until count) {
                maximumValue = maxOf(
                    maximumValue,
                    abs(pattern[i]),
                    abs(memory[i])
                )
            }

            val normalizedPattern = List(count) {
                (pattern[it] / maximumValue).coerceIn(-1f, 1f)
            }

            val normalizedMemory = List(count) {
                (memory[it] / maximumValue).coerceIn(-1f, 1f)
            }

            // Chaar samoohon ke fixed centres.
            val groupCenters = List(4) { group ->
                val angle =
                    group * (Math.PI / 2.0) - Math.PI / 2.0

                Offset(
                    center.x + cos(angle).toFloat() * groupRadius,
                    center.y + sin(angle).toFloat() * groupRadius
                )
            }

            // Har samooh mein chaar numbered ikaiyan.
            val points = List(count) { index ->
                val group = index / 4
                val member = index % 4

                val memberAngle =
                    member * (Math.PI / 2.0) - Math.PI / 2.0

                val p = normalizedPattern[index]
                val m = normalizedMemory[index]

                val radialChange =
                    (p * 0.65f + m * 0.35f) * movementRange

                val sideChange =
                    (p - m) * movementRange * 0.25f

                val radius = localRadius + radialChange

                val gx = groupCenters[group].x
                val gy = groupCenters[group].y

                Offset(
                    gx +
                        cos(memberAngle).toFloat() * radius -
                        sin(memberAngle).toFloat() * sideChange,

                    gy +
                        sin(memberAngle).toFloat() * radius +
                        cos(memberAngle).toFloat() * sideChange
                )
            }

            // Group ke background aur boundary.
            for (group in 0 until 4) {
                val first = group * 4
                val last = minOf(first + 4, count)

                if (first < last) {
                    val members = (first until last).toList()

                    val gx = members.map { points[it].x }
                        .average().toFloat()

                    val gy = members.map { points[it].y }
                        .average().toFloat()

                    drawCircle(
                        color = GroupColors[group].copy(alpha = 0.10f),
                        radius = minSide * 0.145f,
                        center = Offset(gx, gy)
                    )

                    drawCircle(
                        color = GroupColors[group].copy(alpha = 0.45f),
                        radius = minSide * 0.115f,
                        center = Offset(gx, gy),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 1.5.dp.toPx()
                        )
                    )
                }
            }

            // Har group ke andar sirf chaar ring connections.
            for (group in 0 until 4) {
                val first = group * 4
                val last = minOf(first + 4, count)

                if (last - first < 2) continue

                for (member in 0 until (last - first)) {
                    val a = first + member
                    val b = first + (member + 1) % (last - first)

                    val strength =
                        if (
                            a < weights.size &&
                            b < weights[a].size
                        ) {
                            abs(weights[a][b])
                        } else {
                            0f
                        }

                    val relative =
                        (strength / 0.02f).coerceIn(0f, 1f)

                    drawLine(
                        color = GroupColors[group].copy(
                            alpha = 0.30f + relative * 0.60f
                        ),
                        start = points[a],
                        end = points[b],
                        strokeWidth =
                            1.5.dp.toPx() + relative * 1.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // Groups ke beech har pair ka ek strongest
            // actual learned connection.
            for (groupA in 0 until 4) {
                for (groupB in groupA + 1 until 4) {
                    var bestStrength = 0f
                    var bestA = -1
                    var bestB = -1

                    val startA = groupA * 4
                    val endA = minOf(startA + 4, count)
                    val startB = groupB * 4
                    val endB = minOf(startB + 4, count)

                    for (i in startA until endA) {
                        for (j in startB until endB) {
                            if (
                                i < weights.size &&
                                j < weights[i].size
                            ) {
                                val strength = abs(weights[i][j])

                                if (strength > bestStrength) {
                                    bestStrength = strength
                                    bestA = i
                                    bestB = j
                                }
                            }
                        }
                    }

                    if (bestA >= 0 && bestB >= 0) {
                        val relative =
                            (bestStrength / 0.02f).coerceIn(0f, 1f)

                        drawLine(
                            color = WorldLine.copy(
                                alpha = 0.25f + relative * 0.65f
                            ),
                            start = points[bestA],
                            end = points[bestB],
                            strokeWidth =
                                1.dp.toPx() + relative * 1.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // Centre se har group ka connection.
            for (group in 0 until 4) {
                drawLine(
                    color = GroupColors[group].copy(alpha = 0.55f),
                    start = center,
                    end = groupCenters[group],
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Nodes aur unka visual response.
            for (i in 0 until count) {
                val p = abs(normalizedPattern[i])
                val m = abs(normalizedMemory[i])
                val point = points[i]
                val groupColor = GroupColors[i / 4]

                drawCircle(
                    color = WorldGreen.copy(alpha = 0.12f + m * 0.35f),
                    radius = nodeRadius * (1.5f + m * 0.5f),
                    center = point
                )

                drawCircle(
                    color = groupColor.copy(alpha = 0.55f + p * 0.45f),
                    radius = nodeRadius * (0.85f + p * 0.25f),
                    center = point
                )

                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = point
                )
            }

            // Har node ke upar uska number.
            val labelPaint = Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 11.dp.toPx()
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(
                    Typeface.DEFAULT,
                    Typeface.BOLD
                )
                isAntiAlias = true
            }

            for (i in 0 until count) {
                val point = points[i]

                drawContext.canvas.nativeCanvas.drawText(
                    "${i + 1}",
                    point.x,
                    point.y - nodeRadius * 1.45f,
                    labelPaint
                )
            }

            // Shared central point.
            drawCircle(
                color = WorldPurple.copy(alpha = 0.20f),
                radius = nodeRadius * 2.1f,
                center = center
            )

            drawCircle(
                color = WorldPurple,
                radius = nodeRadius * 0.8f,
                center = center
            )
        }

        Text(
            "4 SAMOOH AUR UNKE AAPASI SAMBANDH",
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
            "PATTERN AUR MEMORY",
            color = Color.White
        )

        WorldLegend("Node ka rang: samooh aur pattern", WorldCyan)
        WorldLegend("Hara glow: memory ki matra", WorldGreen)
        WorldLegend("Lakeerein: learned connections", WorldLine)
        WorldLegend("Beech ka bindu: shared system", WorldPurple)

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
            "Network ki taakat",
            snapshot.networkStrength,
            WorldGreen
        )

        Text(
            "SYSTEM KI VARTAMAN STHITI",
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
            "Cycle: ${snapshot.cycle}  |  Refresh: ${snapshot.refreshCount}",
            color = WorldMuted
        )

        Text(
            "Position mein badlav pattern aur memory ke relative values " +
                "se aata hai. Group ke andar ring connections aur groups " +
                "ke beech sabse mazboot learned connection dikhaya gaya hai. " +
                "Relative visual scaling ka matlab actual physical distance nahi hai.",
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

        Text(
            label,
            color = WorldMuted
        )
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
