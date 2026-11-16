
package com.refreshsystem.simulator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val ExperimentBackground = Color(0xFF10131C)
private val ExperimentPanel = Color(0xFF1A2030)
private val ExperimentCyan = Color(0xFF6BE7F2)
private val ExperimentMuted = Color(0xFFB7C0D3)

@Composable
fun ExternalWorldExperimentScreen() {

    var simulationState by remember {
        mutableStateOf(ExternalWorldEngine.createInitialState())
    }

    var externalEnabled by remember {
        mutableStateOf(true)
    }

    var internalRefreshEnabled by remember {
        mutableStateOf(true)
    }

    var searchCue by remember {
        mutableStateOf("")
    }

    val metrics = ExternalWorldEngine.getMemoryMetrics(
        simulationState
    )

    val retrievedMemories = ExternalWorldEngine.retrieveMemory(
        state = simulationState,
        cue = searchCue
    )

    fun runOneCycle() {
        simulationState = ExternalWorldEngine.step(
            state = simulationState,
            externalInputEnabled = externalEnabled,
            internalRefreshEnabled = internalRefreshEnabled,
            seed = 42
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ExperimentBackground)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        Text(
            text = "OPEN SYSTEM EXPERIMENT",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Text(
            text = "External World • Internal Memory • Dynamic Refresh",
            color = ExperimentMuted
        )

        // INPUT CONTROLS

        ExperimentCard(title = "1. System Input Controls") {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "External Input",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Environment se nayi information",
                        color = ExperimentMuted
                    )
                }

                Switch(
                    checked = externalEnabled,
                    onCheckedChange = { externalEnabled = it }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Internal Refresh",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Existing memory ka refresh process",
                        color = ExperimentMuted
                    )
                }

                Switch(
                    checked = internalRefreshEnabled,
                    onCheckedChange = {
                        internalRefreshEnabled = it
                    }
                )
            }

            Text(
                text = "External: ${if (externalEnabled) "ON" else "OFF"}",
                color = ExperimentCyan
            )

            Text(
                text = "Internal Refresh: ${
                    if (internalRefreshEnabled) "ON" else "OFF"
                }",
                color = ExperimentCyan
            )
        }

        // SIMULATION CONTROL

        ExperimentCard(title = "2. Simulation Control") {

            Text(
                "Simulation Cycle: ${simulationState.cycle}",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Text(
                simulationState.status,
                color = ExperimentMuted
            )

            Button(
                onClick = { runOneCycle() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Run One Cycle")
            }

            OutlinedButton(
                onClick = {
                    repeat(10) {
                        runOneCycle()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Run 10 Cycles")
            }

            OutlinedButton(
                onClick = {
                    simulationState =
                        ExternalWorldEngine.reset()
                    externalEnabled = true
                    internalRefreshEnabled = true
                    searchCue = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset Experiment")
            }
        }

        // EXTERNAL WORLD

        ExperimentCard(title = "3. External World") {

            Text(
                "Total Observed Events: ${simulationState.observedEvents}",
                color = Color.White
            )

            Text(
                "Latest External Event:",
                color = ExperimentCyan,
                fontWeight = FontWeight.Bold
            )

            Text(
                simulationState.lastObservedEvent?.content
                    ?: "No event observed yet.",
                color = Color.White
            )

            Text(
                "External Event History",
                color = ExperimentCyan,
                fontWeight = FontWeight.Bold
            )

            if (simulationState.eventHistory.isEmpty()) {
                Text(
                    "No external events recorded.",
                    color = ExperimentMuted
                )
            } else {
                simulationState.eventHistory
                    .takeLast(10)
                    .reversed()
                    .forEach { event ->
                        Text(
                            "Cycle event ${event.id}: ${event.content}",
                            color = Color.White
                        )
                    }
            }
        }

        // INTERNAL MEMORY

        ExperimentCard(title = "4. Internal Memory Measurements") {

            MetricRow("Memory Records", metrics.totalRecords)
            MetricRow("Total Information Units", metrics.totalTokens)
            MetricRow(
                "Unique Information Units",
                metrics.uniqueInformationUnits
            )
            MetricRow("Information Transfers", metrics.totalTransfers)
            MetricRow("Total Inputs", metrics.totalInputs)
            MetricRow("Total Refreshes", metrics.totalRefreshes)

            Text(
                "Average Memory Strength: ${
                    (metrics.averageRecordStrength * 100).toInt()
                }%",
                color = ExperimentCyan,
                fontWeight = FontWeight.Bold
            )
        }

        // MEMORY RETRIEVAL

        ExperimentCard(title = "5. Memory Retrieval") {

            OutlinedTextField(
                value = searchCue,
                onValueChange = { searchCue = it },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Search memory")
                },
                placeholder = {
                    Text("Example: morning, rain, person")
                },
                singleLine = true
            )

            Text(
                "Retrieved Memories: ${retrievedMemories.size}",
                color = ExperimentCyan,
                fontWeight = FontWeight.Bold
            )

            if (searchCue.isNotBlank()) {
                if (retrievedMemories.isEmpty()) {
                    Text(
                        "No matching memory found.",
                        color = ExperimentMuted
                    )
                } else {
                    retrievedMemories.forEach { memory ->
                        Text(
                            memory.content,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Research Note: Ye provisional computational simulation hai. Memory strength aur refresh measurements model ke parameters hain, consciousness ya human memory ka scientific proof nahi.",
            color = ExperimentMuted,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ExperimentCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = ExperimentPanel
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                title,
                color = ExperimentCyan,
                fontWeight = FontWeight.Bold
            )

            content()
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = ExperimentMuted)
        Text(
            value.toString(),
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}
