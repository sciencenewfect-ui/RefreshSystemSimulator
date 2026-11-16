package com.refreshsystem.simulator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

private val LabBg = Color(0xFF0B1020)
private val LabCard = Color(0xFF151F33)
private val LabCyan = Color(0xFF52E5F2)
private val LabGreen = Color(0xFF62E6A5)
private val LabMuted = Color(0xFF9EABC2)

@Composable
fun OpenSystemSimulationLabScreen() {
    val engine = remember { OpenSystemEngine() }
    var snapshot by remember { mutableStateOf(engine.getSnapshot()) }

    var externalOn by remember { mutableStateOf(true) }
    var refreshOn by remember { mutableStateOf(true) }
    var decayOn by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    var cycle by remember { mutableIntStateOf(0) }
    var inputPhase by remember { mutableIntStateOf(0) }

    fun runCycle() {
        val features = if (externalOn) {
            List(OpenSystemEngine.PATTERN_SIZE) { i ->
                val wave = sin((inputPhase + i * 3) * 0.17).toFloat()
                ((wave + 1f) / 2f * 0.85f + 0.05f).coerceIn(0f, 1f)
            }
        } else null

        snapshot = engine.step(
            externalFeatures = features,
            internalRefreshEnabled = refreshOn,
            naturalDecayEnabled = decayOn
        )
        inputPhase += 1
        cycle += 1
    }

    LaunchedEffect(running, externalOn, refreshOn, decayOn) {
        while (running) {
            runCycle()
            delay(350)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LabBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "REFRESH SYSTEM",
                color = LabCyan,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Open System Simulation Lab",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "External Information • Internal World • Dynamic Stability",
                color = LabMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }

        LabPanel("SYSTEM NETWORK — INTERNAL WORLD") {
            NetworkView(
                pattern = snapshot.internalPattern,
                memory = snapshot.memoryState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LegendDot("Pattern", LabCyan)
                LegendDot("Memory", LabGreen)
                LegendDot("Connections", Color(0xFF566783))
            }
            Text(
                "16 interacting internal nodes. Node intensity represents current pattern and memory values.",
                color = LabMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }

        LabPanel("SYSTEM CONTROL") {
            LabSwitch(
                "External Information Input",
                "Environment se nayi information receive karna",
                externalOn
            ) { externalOn = it }

            HorizontalDivider(color = Color(0xFF29364D))

            LabSwitch(
                "Internal Refresh",
                "Internal pattern aur memory ko update karna",
                refreshOn
            ) { refreshOn = it }

            HorizontalDivider(color = Color(0xFF29364D))

            LabSwitch(
                "Natural Decay",
                "Memory aur pattern ki gradual weakening",
                decayOn
            ) { decayOn = it }
        }

        LabPanel("EXPERIMENT CONTROL") {
            Text("Cycle: $cycle", color = Color.White, fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { running = !running },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (running) "Pause" else "Start")
                }
                OutlinedButton(
                    onClick = { runCycle() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("One Cycle")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { repeat(10) { runCycle() } },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Run 10")
                }
                OutlinedButton(
                    onClick = {
                        running = false
                        engine.reset()
                        snapshot = engine.getSnapshot()
                        cycle = 0
                        inputPhase = 0
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset")
                }
            }
        }

        LabPanel("LIVE RESEARCH METRICS") {
            MetricBar("Dynamic Stability", snapshot.dynamicStability)
            MetricBar("Memory Strength", snapshot.memoryRetention)
            MetricBar("Pattern Continuity", snapshot.identityContinuity)
            MetricBar("Network Strength", snapshot.networkStrength)
            MetricBar("Information Input", snapshot.informationLevel)

            MetricValue("Refresh Cycles", snapshot.refreshCount.toString())
            MetricValue(
                "Pattern Change",
                "%.4f".format(snapshot.patternChange)
            )
        }

        LabPanel("CURRENT SYSTEM STATE") {
            StateLine("External Input", if (externalOn) "ACTIVE" else "STOPPED")
            StateLine("Internal Refresh", if (refreshOn) "ACTIVE" else "STOPPED")
            StateLine("Natural Decay", if (decayOn) "ACTIVE" else "OFF")
            StateLine("Simulation", if (running) "RUNNING" else "PAUSED")
        }

        LabPanel("EXPERIMENT NOTE") {
            Text(
                "Experiment: Internal World ko pehle external information se develop karo. Phir External Input OFF karke observe karo ki internal refresh ke saath pattern aur memory kitni der sustain karte hain. Natural Decay ko independently ON/OFF karke results compare karo.",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Research model: Ye provisional computational model hai. Metrics model ke mathematical outputs hain, biological consciousness ka proof nahi.",
                color = LabMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun NetworkView(
    pattern: List<Float>,
    memory: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.background(
            Color(0xFF0D1729),
            RoundedCornerShape(14.dp)
        )
    ) {
        val count = 16
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.36f

        val points = List(count) { i ->
            val angle = (2.0 * Math.PI * i / count) - Math.PI / 2.0
            Offset(
                center.x + cos(angle).toFloat() * radius,
                center.y + sin(angle).toFloat() * radius
            )
        }

        for (i in 0 until count) {
            for (j in i + 1 until count) {
                val distance = (j - i)
                if (distance == 1 || distance == 2 || distance == 8) {
                    val strength = (
                        (abs(pattern.getOrElse(i) { 0f }) +
                         abs(pattern.getOrElse(j) { 0f })) / 2f
                    ).coerceIn(0f, 1f)

                    drawLine(
                        color = Color(0xFF526A8A).copy(
                            alpha = 0.18f + strength * 0.55f
                        ),
                        start = points[i],
                        end = points[j],
                        strokeWidth = 1.dp.toPx() + strength * 1.5.dp.toPx()
                    )
                }
            }
        }

        for (i in 0 until count) {
            val p = abs(pattern.getOrElse(i) { 0f }).coerceIn(0f, 1f)
            val m = abs(memory.getOrElse(i) { 0f }).coerceIn(0f, 1f)
            val point = points[i]

            drawCircle(
                color = LabGreen.copy(alpha = 0.15f + m * 0.45f),
                radius = 13.dp.toPx() + m * 9.dp.toPx(),
                center = point
            )
            drawCircle(
                color = LabCyan.copy(alpha = 0.35f + p * 0.65f),
                radius = 5.dp.toPx() + p * 7.dp.toPx(),
                center = point
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                radius = 2.dp.toPx(),
                center = point
            )
        }

        drawCircle(
            color = LabCyan.copy(alpha = 0.10f),
            radius = 25.dp.toPx(),
            center = center
        )
        drawCircle(
            color = LabCyan.copy(alpha = 0.8f),
            radius = 5.dp.toPx(),
            center = center
        )
    }
}

@Composable
private fun LabPanel(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LabCard),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        ) {
            Text(
                title,
                color = LabCyan,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )
            content()
        }
    }
}

@Composable
private fun LabSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(description, color = LabMuted, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun MetricBar(label: String, value: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White)
            Text("${(value * 100).toInt()}%", color = LabCyan, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { value.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(7.dp),
            color = LabCyan,
            trackColor = Color(0xFF2B3850)
        )
    }
}

@Composable
private fun MetricValue(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = LabMuted)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StateLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = LabMuted)
        Text(value, color = LabGreen, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(
            Modifier.size(8.dp).background(color, RoundedCornerShape(50))
        )
        Text(label, color = LabMuted, style = MaterialTheme.typography.labelSmall)
    }
}
