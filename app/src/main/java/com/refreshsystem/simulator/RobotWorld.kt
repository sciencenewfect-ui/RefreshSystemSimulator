
package com.refreshsystem.simulator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val RobotBackground = Color(0xFF10131C)
private val RobotPanel = Color(0xFF1A2030)
private val RobotCyan = Color(0xFF6BE7F2)
private val RobotMuted = Color(0xFFB7C0D3)

@Composable
fun RobotWorldScreen() {
    var cameraEnabled by remember { mutableStateOf(false) }
    var observation by remember {
        mutableStateOf("अभी कोई नया observation नहीं है।")
    }
    var faceCount by remember { mutableIntStateOf(0) }
    var refreshCount by remember { mutableIntStateOf(0) }
    var environment by remember { mutableFloatStateOf(0.5f) }
    var internalState by remember { mutableFloatStateOf(0.5f) }
    var stability by remember { mutableFloatStateOf(0.75f) }
    var memoryInput by remember { mutableStateOf("") }

    val memories = remember { mutableStateListOf<String>() }

    fun refreshSystem() {
        refreshCount++

        internalState = (
            internalState * 0.65f +
                environment * 0.35f
            ).coerceIn(0f, 1f)

        stability = (
            1f - kotlin.math.abs(internalState - environment)
            ).coerceIn(0f, 1f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                RobotBackground,
                RoundedCornerShape(20.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "ROBOT WORLD",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = Color.White
        )

        Text(
            text = "Open System • Observation • Memory • Refresh",
            color = RobotMuted,
            style = MaterialTheme.typography.bodySmall
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = RobotPanel
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "1. Camera Observation",
                    fontWeight = FontWeight.Bold,
                    color = RobotCyan
                )

                Text(
                    "कैमरा चालू करने पर ऐप चेहरे और दृश्य के कुछ labels पहचानने की कोशिश करेगा।",
                    color = Color.White
                )

                CameraPanel(
                    enabled = cameraEnabled,
                    onEnabledChange = { cameraEnabled = it },
                    onObservation = { result, faces ->
                        observation = result
                        faceCount = faces
                        refreshSystem()
                    }
                )

                Text(
                    text = "Latest observation: $observation",
                    color = RobotMuted,
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = "Detected faces: $faceCount",
                    color = Color.White
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = RobotPanel
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "2. Refresh System",
                    fontWeight = FontWeight.Bold,
                    color = RobotCyan
                )

                Text(
                    "Refresh Count: $refreshCount",
                    color = Color.White
                )

                Text(
                    "Internal State: ${(internalState * 100).toInt()}%",
                    color = Color.White
                )

                Text(
                    "Stability Score: ${(stability * 100).toInt()}%",
                    color = Color.White
                )

                LinearProgressIndicator(
                    progress = { stability },
                    modifier = Modifier.fillMaxWidth(),
                    color = RobotCyan
                )

                Text(
                    "External Environment: ${(environment * 100).toInt()}%",
                    color = RobotMuted
                )

                Slider(
                    value = environment,
                    onValueChange = { environment = it },
                    valueRange = 0f..1f
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { refreshSystem() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Refresh Now")
                    }

                    OutlinedButton(
                        onClick = {
                            refreshCount = 0
                            internalState = 0.5f
                            stability = 0.75f
                            environment = 0.5f
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset")
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = RobotPanel
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "3. Information Retention",
                    fontWeight = FontWeight.Bold,
                    color = RobotCyan
                )

                Text(
                    "यहाँ तुम जानकारी जोड़ सकते हो। यह इस स्क्रीन की अस्थायी memory list में रहेगी।",
                    color = RobotMuted
                )

                OutlinedTextField(
                    value = memoryInput,
                    onValueChange = { memoryInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("जानकारी लिखें") },
                    placeholder = { Text("उदाहरण: सामने एक चेहरा दिखा") },
                    singleLine = false
                )

                Button(
                    onClick = {
                        val entry = memoryInput.trim()
                        if (entry.isNotEmpty()) {
                            memories.add(0, entry)
                            memoryInput = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Information")
                }

                Text(
                    "Saved entries: ${memories.size}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                if (memories.isEmpty()) {
                    Text(
                        "अभी कोई जानकारी सेव नहीं है।",
                        color = RobotMuted
                    )
                } else {
                    memories.take(10).forEachIndexed { index, item ->
                        Text(
                            "${index + 1}. $item",
                            color = Color.White
                        )
                    }

                    OutlinedButton(
                        onClick = { memories.clear() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Clear Memory List")
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = "महत्वपूर्ण: यह Refresh Theory पर आधारित एक प्रयोगात्मक simulation है। Stability Score और memory list चेतना का वैज्ञानिक प्रमाण नहीं हैं। Memory list अभी ऐप बंद होने पर स्थायी रूप से सुरक्षित नहीं रहती।",
            color = RobotMuted,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
