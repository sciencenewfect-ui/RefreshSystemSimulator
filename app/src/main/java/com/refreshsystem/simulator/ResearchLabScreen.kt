
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun ResearchHomeScreen() {
    MaterialTheme {
        RefreshExperimentLab()
    }
}

@Composable
private fun RefreshExperimentLab() {

    var simulation by remember {
        mutableStateOf(
            RefreshSimulationEngine.createInitialState(seed = 42)
        )
    }

    var selectedCondition by remember {
        mutableStateOf(ExperimentCondition.A_BOTH_ON)
    }

    var currentContext by remember {
        mutableStateOf("daily_life")
    }

    var running by remember {
        mutableStateOf(false)
    }

    var status by remember {
        mutableStateOf("Experiment तैयार है।")
    }

    LaunchedEffect(running, selectedCondition, currentContext) {
        while (running) {
            delay(900L)

            simulation = RefreshSimulationEngine.step(
                state = simulation,
                input = SimulationInput(
                    condition = selectedCondition,
                    currentContextKey = currentContext
                )
            )

            status = "Simulation cycle ${simulation.step} complete."
        }
    }

    val latest = simulation.history.lastOrNull()

    val selectedPattern = latest?.selectedPatternId?.let { id ->
        simulation.patterns.firstOrNull { it.id == id }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        Text(
            text = "REFRESH SYSTEM",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Dynamic Stability & Memory Experiment Lab",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "यह simulator External Input, Internal Input, Refresh, " +
                "Dynamic Stability और Memory Patterns के संबंधों को test करता है।",
            style = MaterialTheme.typography.bodyMedium
        )

        // EXPERIMENT CONDITIONS

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Text(
                    "1. Experiment Condition",
                    style = MaterialTheme.typography.titleLarge
                )

                ExperimentCondition.values().forEach { condition ->

                    FilterChip(
                        selected = selectedCondition == condition,
                        onClick = {
                            selectedCondition = condition
                            status = "Condition: ${condition.label}"
                        },
                        label = {
                            Text(condition.label)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text(
                    "Selected: ${selectedCondition.label}",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // CURRENT CONTEXT

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    "2. Current System Context",
                    style = MaterialTheme.typography.titleLarge
                )

                OutlinedTextField(
                    value = currentContext,
                    onValueChange = {
                        currentContext = it
                    },
                    label = {
                        Text("Context Key")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(
                    "Examples: daily_life, social, past_experience, other",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // LIVE SYSTEM STATUS

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {

                Text(
                    "3. Live System Measurements",
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    "Simulation Step: ${simulation.step}",
                    fontWeight = FontWeight.Bold
                )

                ResultMetric(
                    "Global Dynamic Stability",
                    simulation.globalDynamicStability
                )

                ResultMetric(
                    "Memory Dynamic Stability",
                    simulation.memoryDynamicStability
                )

                ResultMetric(
                    "Average Memory Strength",
                    latest?.averageMemoryStrength
                        ?: simulation.patterns.map { it.strength }.average()
                )

                ResultMetric(
                    "Average Pattern Continuity",
                    latest?.averagePatternContinuity
                        ?: simulation.patterns.map { it.continuity }.average()
                )

                Text(
                    "Status: $status",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // SIMULATION CONTROLS

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Text(
                    "4. Simulation Controls",
                    style = MaterialTheme.typography.titleLarge
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Button(
                        onClick = {
                            running = true
                            status = "Simulation running."
                        },
                        enabled = !running,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Start")
                    }

                    OutlinedButton(
                        onClick = {
                            running = false
                            status = "Simulation paused."
                        },
                        enabled = running,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Pause")
                    }
                }

                OutlinedButton(
                    onClick = {
                        simulation = RefreshSimulationEngine.step(
                            state = simulation,
                            input = SimulationInput(
                                condition = selectedCondition,
                                currentContextKey = currentContext
                            )
                        )

                        status = "One simulation cycle completed."
                    },
                    enabled = !running,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Run One Cycle")
                }

                OutlinedButton(
                    onClick = {
                        running = false

                        simulation =
                            RefreshSimulationEngine.createInitialState(
                                seed = 42
                            )

                        status = "Experiment reset. Initial state restored."
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset Experiment")
                }
            }
        }

        // MEMORY ACTIVATION

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    "5. Memory Pattern Activation",
                    style = MaterialTheme.typography.titleLarge
                )

                if (selectedPattern != null) {

                    Text(
                        "Recently Selected Pattern",
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(selectedPattern.name)

                    Text(
                        "Selection Probability: ${
                            percent(latest?.selectedPatternProbability ?: 0.0)
                        }"
                    )

                    Text(
                        "Total Activations: ${selectedPattern.activationCount}"
                    )

                } else {
                    Text("अभी कोई memory pattern activate नहीं हुआ है।")
                }

                HorizontalDivider()

                simulation.patterns.forEach { pattern ->

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {

                        Text(
                            pattern.name,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            "Context: ${pattern.contextKey} | " +
                                "Activations: ${pattern.activationCount}",
                            style = MaterialTheme.typography.bodySmall
                        )

                        ResultMetric(
                            "Strength",
                            pattern.strength
                        )

                        ResultMetric(
                            "Continuity",
                            pattern.continuity
                        )

                        Text(
                            "Activation Probability: ${
                                percent(pattern.activationProbability)
                            }",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    HorizontalDivider()
                }
            }
        }

        // LIVE GRAPH

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    "6. Dynamic Stability Graph",
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    "Blue: Global Dynamic Stability | " +
                        "Green: Memory Dynamic Stability",
                    style = MaterialTheme.typography.bodySmall
                )

                StabilityGraph(
                    history = simulation.history,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                )

                Text(
                    "Recorded cycles: ${simulation.history.size}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // EXPERIMENT HISTORY

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    "7. Experiment History",
                    style = MaterialTheme.typography.titleLarge
                )

                simulation.history.takeLast(20).reversed().forEach { record ->

                    HorizontalDivider()

                    Text(
                        "Step ${record.step}",
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        "Global DS: ${percent(record.globalDynamicStability)}"
                    )

                    Text(
                        "Memory DS: ${percent(record.memoryDynamicStability)}"
                    )

                    Text(
                        "Memory Strength: ${percent(record.averageMemoryStrength)}"
                    )

                    Text(
                        "Selected Pattern ID: ${record.selectedPatternId}"
                    )
                }

                if (simulation.history.isEmpty()) {
                    Text("अभी कोई experiment cycle record नहीं हुआ है।")
                }
            }
        }

        // MODEL LIMITATIONS

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    "Research Notes",
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    "• सभी Memory Patterns shared Refresh से update होते हैं।"
                )

                Text(
                    "• Context और randomness memory selection को प्रभावित करते हैं।"
                )

                Text(
                    "• Numerical rates अभी provisional assumptions हैं।"
                )

                Text(
                    "• Memory connections और related-pattern reactivation " +
                        "का पूरा model अभी implement नहीं हुआ है।"
                )

                Text(
                    "• Memory Dynamic Stability अभी average pattern continuity " +
                        "से estimate की जाती है।"
                )
            }
        }
    }
}

@Composable
private fun ResultMetric(
    label: String,
    value: Double
) {

    val safeValue = value.coerceIn(0.0, 1.0).toFloat()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        Text(
            "$label: ${percent(value)}",
            style = MaterialTheme.typography.bodyMedium
        )

        LinearProgressIndicator(
            progress = { safeValue },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
        )
    }
}

@Composable
private fun StabilityGraph(
    history: List<SimulationMetrics>,
    modifier: Modifier = Modifier
) {

    Canvas(
        modifier = modifier
            .background(
                color = Color(0xFF101B29),
                shape = RoundedCornerShape(12.dp)
            )
    ) {

        val left = 28.dp.toPx()
        val right = size.width - 12.dp.toPx()
        val top = 14.dp.toPx()
        val bottom = size.height - 24.dp.toPx()

        val graphWidth = right - left
        val graphHeight = bottom - top

        // Horizontal reference lines

        for (i in 0..4) {

            val y = top + graphHeight * i / 4f

            drawLine(
                color = Color.Gray.copy(alpha = 0.35f),
                start = Offset(left, y),
                end = Offset(right, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        if (history.isEmpty()) return@Canvas

        val visible = history.takeLast(60)

        fun drawSeries(
            values: List<Double>,
            color: Color
        ) {

            if (values.size < 2) return

            val path = Path()

            values.forEachIndexed { index, value ->

                val x = left +
                    graphWidth * index / (values.size - 1).toFloat()

                val normalized =
                    value.coerceIn(0.0, 1.0).toFloat()

                val y = bottom - normalized * graphHeight

                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 3.dp.toPx())
            )
        }

        drawSeries(
            visible.map { it.globalDynamicStability },
            Color(0xFF38BDF8)
        )

        drawSeries(
            visible.map { it.memoryDynamicStability },
            Color(0xFF4ADE80)
        )
    }
}

private fun percent(value: Double): String {
    return String.format(
        Locale.US,
        "%.1f%%",
        value.coerceIn(0.0, 1.0) * 100.0
    )
}
