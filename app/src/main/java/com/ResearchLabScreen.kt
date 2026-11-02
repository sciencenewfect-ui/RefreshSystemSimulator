package com.refreshsystem.simulator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class ResearchDepartment(
    val title: String,
    val description: String
)

@Composable
fun ResearchLabScreen() {

    val departments = listOf(
        ResearchDepartment(
            "1. Current Understanding",
            "वर्तमान में शरीर, Memory, Identity और I-Feeling को कैसे समझा जाता है।"
        ),
        ResearchDepartment(
            "2. Open System & Stability Research",
            "Open System, Refresh Process, Movement और Stability का वैज्ञानिक अध्ययन।"
        ),
        ResearchDepartment(
            "3. Memory & Consciousness Research",
            "Memory Formation, Retention, Refresh, I-Feeling और Consciousness का अध्ययन।"
        ),
        ResearchDepartment(
            "4. Theory Evaluation",
            "रिसर्च के परिणामों की तुलना वर्तमान वैज्ञानिक धारणाओं और Refresh Theory से करना।"
        ),
        ResearchDepartment(
            "5. Robot System Experiment",
            "मानव और 10 Robots के साझा सिस्टम पर भविष्य के प्रयोगों की योजना।"
        )
    )

    var selectedDepartment by remember {
        mutableStateOf<ResearchDepartment?>(null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        if (selectedDepartment == null) {

            Text(
                text = "Refresh Theory Research Lab",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "यहाँ Refresh Theory के पाँच मुख्य रिसर्च विभाग होंगे।",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            departments.forEach { department ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    onClick = {
                        selectedDepartment = department
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = department.title,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = department.description,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Open Department →",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

        } else {

            Text(
                text = selectedDepartment!!.title,
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = selectedDepartment!!.description,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "इस विभाग का विस्तृत रिसर्च मॉड्यूल अगले चरण में तैयार किया जाएगा।",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    selectedDepartment = null
                }
            ) {
                Text("Back to Research Lab")
            }
        }
    }
}
