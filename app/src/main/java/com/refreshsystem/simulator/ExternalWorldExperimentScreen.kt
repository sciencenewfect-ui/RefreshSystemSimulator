
package com.refreshsystem.simulator

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.viewinterop.AndroidView

private val ExperimentBackground = Color(0xFF10131C)
private val ExperimentPanel = Color(0xFF1A2030)
private val ExperimentCyan = Color(0xFF6BE7F2)
private val ExperimentMuted = Color(0xFFB7C0D3)

@Composable
fun ExternalWorldExperimentScreen() {

    var simulationState by remember {
        mutableStateOf(ExternalWorldEngine.createInitialState())
    }

    var externalEnabled by remember { mutableStateOf(true) }
    var internalRefreshEnabled by remember { mutableStateOf(true) }
    var naturalDecayEnabled by remember { mutableStateOf(false) }
    var searchCue by remember { mutableStateOf("") }

    val metrics = ExternalWorldEngine.getMemoryMetrics(simulationState)

    val retrievedMemories = ExternalWorldEngine.retrieveMemory(
        state = simulationState,
        cue = searchCue
    )

    fun runOneCycle() {
        simulationState = ExternalWorldEngine.step(
            state = simulationState,
            externalInputEnabled = externalEnabled,
            internalRefreshEnabled = internalRefreshEnabled,
            seed = 42,
            naturalDecayEnabled = naturalDecayEnabled
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ExperimentBackground)
            .verticalScroll(rememberScrollState())
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

        // 1. HUMAN OPEN SYSTEM 3D MODEL

        ExperimentCard(title = "1. Human Open System 3D Model") {

            Text(
                text = "Interactive human system visualization",
                color = ExperimentMuted
            )

            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(540.dp),
                factory = { context ->
                    HumanOpenSystem3DView(context).apply {
                        updateSignals(
                            camera = false,
                            microphone = false,
                            audio = 0f,
                            stability = metrics.averageRecordStrength
                                .coerceIn(0f, 1f)
                        )
                    }
                },
                update = { view ->
                    view.updateSignals(
                        camera = false,
                        microphone = false,
                        audio = 0f,
                        stability = metrics.averageRecordStrength
                            .coerceIn(0f, 1f)
                    )
                }
            )

            Text(
                text = "Drag the model to rotate it. Brain, heart, lungs, memory region and refresh particles are conceptual visual elements.",
                color = ExperimentMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // 2. SYSTEM INPUT CONTROLS

        ExperimentCard(title = "2. System Input Controls") {

            ControlRow(
                title = "External Input",
                description = "Environment se nayi information",
                checked = externalEnabled,
                onCheckedChange = { externalEnabled = it }
            )

            ControlRow(
                title = "Internal Refresh",
                description = "Existing memory ka refresh process",
                checked = internalRefreshEnabled,
                onCheckedChange = { internalRefreshEnabled = it }
            )

            ControlRow(
                title = "Natural Decay",
                description = "Memory strength ka natural reduction",
                checked = naturalDecayEnabled,
                onCheckedChange = { naturalDecayEnabled = it }
            )

            Text(
                "External Input: ${if (externalEnabled) "ON" else "OFF"}",
                color = ExperimentCyan
            )

            Text(
                "Internal Refresh: ${if (internalRefreshEnabled) "ON" else "OFF"}",
                color = ExperimentCyan
            )

            Text(
                "Natural Decay: ${if (naturalDecayEnabled) "ON" else "OFF"}",
                color = ExperimentCyan
            )
        }

        // 3. SIMULATION CONTROL

        ExperimentCard(title = "3. Simulation Control") {

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
                    repeat(10) { runOneCycle() }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Run 10 Cycles")
            }

            OutlinedButton(
                onClick = {
                    simulationState = ExternalWorldEngine.reset()
                    externalEnabled = true
                    internalRefreshEnabled = true
                    naturalDecayEnabled = false
                    searchCue = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset Experiment")
            }
        }

        // 4. EXTERNAL WORLD

        ExperimentCard(title = "4. External World") {

            MetricRow(
                "Total Observed Events",
                simulationState.observedEvents
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
                            "Event ${event.id}: ${event.content}",
                            color = Color.White
                        )
                    }
            }
        }

        // 5. INTERNAL MEMORY MEASUREMENTS

        ExperimentCard(title = "5. Internal Memory Measurements") {

            MetricRow("Memory Records", metrics.totalRecords)
            MetricRow("Total Information Units", metrics.totalTokens)
            MetricRow("Unique Information Units", metrics.uniqueInformationUnits)
            MetricRow("Information Transfers", metrics.totalTransfers)
            MetricRow("Total Inputs", metrics.totalInputs)
            MetricRow("Total Refreshes", metrics.totalRefreshes)
            MetricRow("Total Decay Cycles", metrics.totalDecayCycles)

            Spacer(Modifier.height(4.dp))

            Text(
                "Average Memory Strength: ${(metrics.averageRecordStrength * 100).toInt()}%",
                color = ExperimentCyan,
                fontWeight = FontWeight.Bold
            )
        }

        // 6. MEMORY RETRIEVAL

        ExperimentCard(title = "6. Memory Retrieval") {

            OutlinedTextField(
                value = searchCue,
                onValueChange = { searchCue = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search memory") },
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
                        Column(
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(memory.content, color = Color.White)

                            Text(
                                "Strength: ${(memory.strength * 100).toInt()}%",
                                color = ExperimentCyan,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Research Note: Ye provisional computational simulation hai. Memory strength, refresh aur decay values model parameters hain. Ye human memory ya consciousness ka scientific proof nahi hai.",
            color = ExperimentMuted,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ControlRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Text(description, color = ExperimentMuted)
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
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
