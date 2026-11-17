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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val Bg = Color(0xFF0B1020)
private val Panel = Color(0xFF151F33)
private val Cyan = Color(0xFF52E5F2)
private val Green = Color(0xFF62E6A5)
private val Orange = Color(0xFFFFB86B)
private val Purple = Color(0xFFB69CFF)
private val Muted = Color(0xFF9EABC2)
private val GridColor = Color(0xFF34445D)

@Composable
fun OpenSystemSimulationLabScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
    var exportMessage by remember { mutableStateOf("") }
    var experimentMessage by remember { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val output = context.contentResolver.openOutputStream(uri)
                    ?: throw IllegalStateException("File could not be opened")
                output.bufferedWriter().use { it.write(csvToSave) }
                exportMessage = "CSV export completed."
            } catch (e: Exception) {
                exportMessage = "CSV export failed: ${e.message}"
            }
        }
    }

    fun inputFeatures(): List<Float>? {
        if (!external) return null
        return List(OpenSystemEngine.PATTERN_SIZE) { i ->
            sin(phase * 0.17 + i * 0.31 + seed * 0.013).toFloat()
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
        Modifier
            .fillMaxSize()
            .background(Bg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "REFRESH SYSTEM",
            color = Cyan,
            fontWeight = FontWeight.Bold
        )

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
            InternalWorldVisualization(
                snapshot = snapshot,
                weights = engine.currentWeights()
            )
        }

        PanelCard("INDEPENDENT SYSTEM CONTROLS") {
            ControlSwitch("External Information", external) {
                external = it
            }
            ControlSwitch("Internal Refresh", refresh) {
                refresh = it
            }
            ControlSwitch("Natural Decay", decay) {
                decay = it
            }
        }

        PanelCard("LIVE EXPERIMENT") {
            Text(
                "Cycle: $cycle",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { running = !running },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (running) "Pause" else "Start")
                }

                OutlinedButton(
                    onClick = { if (!running) runCycle() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("One Cycle")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        if (!running) repeat(10) { runCycle() }
                    },
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
                        phase = 0
                        history = emptyList()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset")
                }
            }

            Text("Recorded live cycles: ${history.size}", color = Muted)
        }

        PanelCard("LIVE RESEARCH MEASUREMENTS") {
            Metric("Dynamic Stability", snapshot.dynamicStability, Cyan)
            Metric("Memory Strength", snapshot.memoryRetention, Green)
            Metric("Identity Continuity", snapshot.identityContinuity, Purple)
            Metric("Network Strength", snapshot.networkStrength, Orange)
            Metric("External Information", snapshot.informationLevel, Cyan)

            ValueLine("Pattern Change", "%.5f".format(snapshot.patternChange))
            ValueLine("Refresh Count", snapshot.refreshCount.toString())
        }

        PanelCard("LIVE TREND GRAPH") {
            Text(
                "Cycle-wise measurements from the live simulation.",
                color = Muted
            )

            TrendChart(history)

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem("Stability", Cyan)
                LegendItem("Memory", Green)
                LegendItem("Continuity", Purple)
            }

            Text(
                "Showing latest 100 recorded cycles. Vertical scale: 0–100%.",
                color = Muted,
                style = MaterialTheme.typography.bodySmall
            )
        }

        PanelCard("8-CONDITION CONTROLLED EXPERIMENT") {
            Text(
                "Each condition receives identical 40-cycle training followed by 80 test cycles. " +
                    "Each trial uses the selected seed sequence.",
                color = Muted
            )

            OutlinedTextField(
                value = seed.toString(),
                onValueChange = {
                    it.toIntOrNull()?.let { value -> seed = value }
                },
                label = { Text("Base random seed") },
                singleLine = true
            )

            OutlinedTextField(
                value = repeats.toString(),
                onValueChange = {
                    it.toIntOrNull()?.coerceIn(1, 100)?.let { value ->
                        repeats = value
                    }
                },
                label = { Text("Repeat trials (1–100)") },
                singleLine = true
            )

            Button(
                enabled = !busy && !running,
                onClick = {
                    busy = true
                    experimentMessage = "Experiment running in background..."

                    scope.launch {
                        try {
                            val output = withContext(Dispatchers.Default) {
                                ResearchExperimentEngine.run(seed, repeats)
                            }

                            results = output.first
                            experimentRecords = output.second
                            experimentMessage = "Experiment completed successfully."
                        } catch (e: Exception) {
                            experimentMessage = "Experiment failed: ${e.message}"
                        } finally {
                            busy = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    when {
                        busy -> "Running experiment..."
                        else -> "Run 8-Condition Experiment"
                    }
                )
            }

            if (busy) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Cyan
                )
            }

            if (experimentMessage.isNotEmpty()) {
                Text(
                    experimentMessage,
                    color = if (busy) Orange else Green
                )
            }

            if (results.isNotEmpty()) {
                ValueLine("Total trials", results.size.toString())
                ValueLine("Test cycle records", experimentRecords.size.toString())

                Text(
                    "CONDITION COMPARISON GRAPH",
                    color = Cyan,
                    fontWeight = FontWeight.Bold
                )

                ConditionComparisonChart(results)

                FactorAnalysisPanel(results)
                MemoryFidelityAnalysis(results)

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendItem("Stability", Cyan)
                    LegendItem("Memory", Green)
                }

                Text(
                    "C1–C8 labels correspond to the condition details below.",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall
                )

                results.groupBy { it.condition.id }
                    .toSortedMap()
                    .forEach { (_, group) ->
                        val first = group.first()
                        val meanStability =
                            group.map { it.finalStability }.average()
                        val meanMemory =
                            group.map { it.finalMemory }.average()
                        val meanContinuity =
                            group.map { it.finalContinuity }.average()
                        val meanNetwork =
                            group.map { it.finalNetwork }.average()

                        HorizontalDivider(color = GridColor)

                        Text(
                            "C${first.condition.id} — ${first.condition.name}",
                            color = Cyan,
                            fontWeight = FontWeight.Bold
                        )

                        Metric(
                            "Mean final stability",
                            meanStability.toFloat(),
                            Cyan
                        )
                        Metric(
                            "Mean final memory",
                            meanMemory.toFloat(),
                            Green
                        )

                        ValueLine(
                            "Mean continuity",
                            "${(meanContinuity * 100).toInt()}%"
                        )
                        ValueLine(
                            "Mean network strength",
                            "${(meanNetwork * 100).toInt()}%"
                        )
                    }

                Button(
                    onClick = {
                        csvToSave = ResearchExperimentEngine.csv(
                            experimentRecords,
                            results
                        )
                        exportMessage = ""
                        exportLauncher.launch("refresh_system_experiment.csv")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export Full Experiment CSV")
                }

                if (exportMessage.isNotEmpty()) {
                    Text(exportMessage, color = Green)
                }
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

            if (history.isEmpty()) {
                Text("No live measurements yet.", color = Muted)
            }
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

@Composable
private fun TrendChart(history: List<ResearchCycle>) {
    val data = history.takeLast(100)

    if (data.size < 2) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(190.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Start the simulation to generate graph data.", color = Muted)
        }
        return
    }

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(210.dp)
            .background(Color(0xFF0D1729), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        val left = 8.dp.toPx()
        val right = size.width - 8.dp.toPx()
        val top = 8.dp.toPx()
        val bottom = size.height - 8.dp.toPx()
        val graphHeight = bottom - top
        val graphWidth = right - left

        for (i in 0..4) {
            val y = top + graphHeight * i / 4f
            drawLine(
                GridColor,
                Offset(left, y),
                Offset(right, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        fun drawSeries(values: List<Float>, color: Color) {
            val points = values.mapIndexed { index, value ->
                val x = left + graphWidth * index / (values.size - 1).toFloat()
                val y = bottom - value.coerceIn(0f, 1f) * graphHeight
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

        drawSeries(data.map { it.stability }, Cyan)
        drawSeries(data.map { it.memory }, Green)
        drawSeries(data.map { it.continuity }, Purple)
    }
}

@Composable
private fun ConditionComparisonChart(results: List<ResearchResult>) {
    val groups = results.groupBy { it.condition.id }.toSortedMap()

    if (groups.isEmpty()) return

    val values = groups.map { (_, group) ->
        val stability = group.map { it.finalStability }.average().toFloat()
        val memory = group.map { it.finalMemory }.average().toFloat()
        stability to memory
    }

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(230.dp)
            .background(Color(0xFF0D1729), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        val left = 8.dp.toPx()
        val right = size.width - 8.dp.toPx()
        val top = 12.dp.toPx()
        val bottom = size.height - 24.dp.toPx()
        val height = bottom - top
        val width = right - left

        for (i in 0..4) {
            val y = top + height * i / 4f
            drawLine(
                GridColor,
                Offset(left, y),
                Offset(right, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val groupWidth = width / values.size
        val barWidth = (groupWidth * 0.27f).coerceAtLeast(3.dp.toPx())

        values.forEachIndexed { index, pair ->
            val centerX = left + groupWidth * (index + 0.5f)

            val stabilityHeight = pair.first.coerceIn(0f, 1f) * height
            val memoryHeight = pair.second.coerceIn(0f, 1f) * height

            drawRect(
                Cyan,
                topLeft = Offset(
                    centerX - barWidth - 2.dp.toPx(),
                    bottom - stabilityHeight
                ),
                size = androidx.compose.ui.geometry.Size(
                    barWidth,
                    stabilityHeight
                )
            )

            drawRect(
                Green,
                topLeft = Offset(
                    centerX + 2.dp.toPx(),
                    bottom - memoryHeight
                ),
                size = androidx.compose.ui.geometry.Size(
                    barWidth,
                    memoryHeight
                )
            )
        }
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        groups.keys.forEach { id ->
            Text(
                "C$id",
                color = Muted,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            Modifier
                .size(9.dp)
                .background(color, RoundedCornerShape(50))
        )
        Text(label, color = Muted, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun PanelCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
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
private fun Metric(
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
            Text(label, color = Color.White)
            Text("${(safe * 100).toInt()}%", color = color)
        }

        LinearProgressIndicator(
            progress = safe,
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp),
            color = color,
            trackColor = GridColor
        )
    }
}

@Composable
private fun ValueLine(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Muted)
        Text(value, color = Color.White)
    }
}
