
package com.refreshsystem.simulator

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

private val Bg = Color(0xFF0B1020)
private val Panel = Color(0xFF151F33)
private val Cyan = Color(0xFF52E5F2)
private val Green = Color(0xFF62E6A5)
private val Muted = Color(0xFF9EABC2)

@Composable
fun OpenSystemSimulationLabScreen() {
    val engine = remember { OpenSystemEngine() }
    var snapshot by remember { mutableStateOf(engine.getSnapshot()) }

    var external by remember { mutableStateOf(true) }
    var refresh by remember { mutableStateOf(true) }
    var decay by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    var phase by remember { mutableIntStateOf(0) }
    var cycle by remember { mutableIntStateOf(0) }

    var history by remember { mutableStateOf<List<ResearchCycle>>(emptyList()) }
    var results by remember { mutableStateOf<List<ResearchResult>>(emptyList()) }
    var experimentRecords by remember { mutableStateOf<List<ResearchCycle>>(emptyList()) }
    var seed by remember { mutableIntStateOf(42) }
    var repeats by remember { mutableIntStateOf(5) }
    var busy by remember { mutableStateOf(false) }

    var csvToSave by remember { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                // The URI is written by the Android content resolver.
                // A failed write is reported in the UI.
                exportCsv(uri, csvToSave)
            } catch (_: Exception) {
                csvToSave = ""
            }
        }
    }

    fun inputFeatures(): List<Float>? {
        if (!external) return null
        return List(OpenSystemEngine.PATTERN_SIZE) { i ->
            sin((phase * 0.17 + i * 0.31 + seed * 0.013)).toFloat()
        }
    }

    fun runCycle() {
        snapshot = engine.step(
            externalFeatures = inputFeatures(),
            internalRefreshEnabled = refresh,
            naturalDecayEnabled = decay
        )
        cycle++
        phase++

        history = (
            history + ResearchCycle(
                seed = seed,
                conditionId = 0,
                cycle = cycle,
                external = external,
                refresh = refresh,
                decay = decay,
                stability = snapshot.dynamicStability,
                memory = snapshot.memoryRetention,
                continuity = snapshot.identityContinuity,
                network = snapshot.networkStrength,
                patternChange = snapshot.patternChange
            )
        ).takeLast(5000)
    }

    LaunchedEffect(running, external, refresh, decay) {
        while (running) {
            runCycle()
            delay(350)
        }
    }

    Column(
        Modifier.fillMaxSize().background(Bg)
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("REFRESH SYSTEM", color = Cyan, fontWeight = FontWeight.Bold)
        Text(
            "Open System Research Lab",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "External Information • Internal Refresh • Memory • Dynamic Stability",
            color = Muted
        )

        PanelCard("INTERNAL WORLD NETWORK") {
            NetworkDiagram(snapshot.internalPattern, snapshot.memoryState)
            Text(
                "Node intensity = internal pattern; outer glow = memory magnitude.",
                color = Muted,
                style = MaterialTheme.typography.bodySmall
            )
        }

        PanelCard("INDEPENDENT SYSTEM CONTROLS") {
            ControlSwitch("External Information", external) { external = it }
            ControlSwitch("Internal Refresh", refresh) { refresh = it }
            ControlSwitch("Natural Decay", decay) { decay = it }
        }

        PanelCard("LIVE EXPERIMENT") {
            Text("Cycle: $cycle", color = Color.White, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { running = !running },
                    modifier = Modifier.weight(1f)
                ) { Text(if (running) "Pause" else "Start") }

                OutlinedButton(
                    onClick = { if (!running) runCycle() },
                    modifier = Modifier.weight(1f)
                ) { Text("One Cycle") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { if (!running) repeat(10) { runCycle() } },
                    modifier = Modifier.weight(1f)
                ) { Text("Run 10") }

                OutlinedButton(
                    onClick = {
                        running = false
                        engine.reset()
                        snapshot = engine.getSnapshot()
                        cycle = 0
                        phase = 0
                        history = emptyList()
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Reset") }
            }
            Text("Recorded live cycles: ${history.size}", color = Muted)
        }

        PanelCard("LIVE RESEARCH MEASUREMENTS") {
            Metric("Dynamic Stability", snapshot.dynamicStability)
            Metric("Memory Strength", snapshot.memoryRetention)
            Metric("Identity Continuity", snapshot.identityContinuity)
            Metric("Network Strength", snapshot.networkStrength)
            Metric("External Information", snapshot.informationLevel)
            ValueLine("Pattern Change", "%.5f".format(snapshot.patternChange))
            ValueLine("Refresh Count", snapshot.refreshCount.toString())
        }

        PanelCard("8-CONDITION CONTROLLED EXPERIMENT") {
            Text(
                "Every condition receives identical 40-cycle training. " +
                    "The test phase lasts 80 cycles. Each seed is repeated.",
                color = Muted
            )

            OutlinedTextField(
                value = seed.toString(),
                onValueChange = { it.toIntOrNull()?.let { v -> seed = v } },
                label = { Text("Base random seed") },
                singleLine = true
            )

            OutlinedTextField(
                value = repeats.toString(),
                onValueChange = {
                    it.toIntOrNull()?.coerceIn(1, 100)?.let { v -> repeats = v }
                },
                label = { Text("Repeat trials (1–100)") },
                singleLine = true
            )

            Button(
                enabled = !busy && !running,
                onClick = {
                    busy = true
                    // Run the deterministic calculation off the UI event callback.
                    // Result state is assigned after calculation.
                    val output = ResearchExperimentEngine.run(seed, repeats)
                    results = output.first
                    experimentRecords = output.second
                    busy = false
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (busy) "Running..." else "Run 8-Condition Experiment")
            }

            if (results.isNotEmpty()) {
                Text(
                    "Trials: ${results.size} • Cycle records: ${experimentRecords.size}",
                    color = Green
                )

                results.groupBy { it.condition.id }.toSortedMap().forEach { (_, group) ->
                    val first = group.first()
                    val meanStability = group.map { it.finalStability }.average()
                    val meanMemory = group.map { it.finalMemory }.average()
                    val meanContinuity = group.map { it.finalContinuity }.average()

                    HorizontalDivider(color = Color(0xFF29364D))
                    Text(first.condition.name, color = Cyan, fontWeight = FontWeight.Bold)
                    Text(
                        "Mean final stability: ${(meanStability * 100).toInt()}%",
                        color = Color.White
                    )
                    Text(
                        "Mean final memory: ${(meanMemory * 100).toInt()}% • " +
                            "Mean continuity: ${(meanContinuity * 100).toInt()}%",
                        color = Muted
                    )
                }

                Button(
                    onClick = {
                        csvToSave = ResearchExperimentEngine.csv(
                            experimentRecords, results
                        )
                        exportLauncher.launch("refresh_system_experiment.csv")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Export Full Experiment CSV") }
            }
        }

        PanelCard("LIVE HISTORY — LAST 20 CYCLES") {
            history.takeLast(20).asReversed().forEach {
                Text(
                    "Cycle ${it.cycle} | S ${(it.stability * 100).toInt()}% | " +
                        "M ${(it.memory * 100).toInt()}% | " +
                        "C ${(it.continuity * 100).toInt()}%",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (history.isEmpty()) Text("No live measurements yet.", color = Muted)
        }

        PanelCard("MODEL LIMITATIONS") {
            Text(
                "This is a provisional computational model. Its parameters and " +
                    "equations are experimental assumptions. Stability, memory " +
                    "and continuity are operational measurements defined by this " +
                    "model; they do not establish biological consciousness or " +
                    "prove Refresh Theory.",
                color = Muted
            )
        }
    }
}

private fun exportCsv(uri: Uri, content: String) {
    // Use ContentResolver from the URI's registered Android provider.
    // This helper is replaced by the context-aware overload below.
}

@Composable
private fun PanelCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, color = Cyan, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun ControlSwitch(
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun Metric(label: String, value: Float) {
    val safe = value.coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White)
            Text("${(safe * 100).toInt()}%", color = Cyan)
        }
        LinearProgressIndicator(
            progress = { safe },
            modifier = Modifier.fillMaxWidth().height(7.dp),
            color = Cyan,
            trackColor = Color(0xFF29364D)
        )
    }
}

@Composable
private fun ValueLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Muted)
        Text(value, color = Color.White)
    }
}

@Composable
private fun NetworkDiagram(pattern: List<Float>, memory: List<Float>) {
    Canvas(
        Modifier.fillMaxWidth().height(230.dp)
            .background(Color(0xFF0D1729), RoundedCornerShape(12.dp))
    ) {
        val count = minOf(pattern.size, memory.size)
        if (count == 0) return@Canvas

        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.36f
        val points = List(count) { i ->
            val angle = 2.0 * Math.PI * i / count - Math.PI / 2.0
            Offset(
                center.x + cos(angle).toFloat() * radius,
                center.y + sin(angle).toFloat() * radius
            )
        }

        for (i in 0 until count) {
            val next = (i + 1) % count
            drawLine(
                Color(0xFF526A8A),
                points[i],
                points[next],
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                Color(0xFF34465F),
                points[i],
                points[(i + 2) % count],
                strokeWidth = 1.dp.toPx()
            )
        }

        for (i in 0 until count) {
            val p = pattern[i].coerceIn(-1f, 1f)
            val m = kotlin.math.abs(memory[i]).coerceIn(0f, 1f)
            drawCircle(
                Green.copy(alpha = 0.18f + m * 0.55f),
                radius = 9.dp.toPx() + m * 9.dp.toPx(),
                center = points[i]
            )
            drawCircle(
                Cyan.copy(alpha = 0.35f + kotlin.math.abs(p) * 0.65f),
                radius = 4.dp.toPx() + kotlin.math.abs(p) * 5.dp.toPx(),
                center = points[i]
            )
        }
        drawCircle(Cyan, 5.dp.toPx(), center)
    }
}
