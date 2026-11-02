
package com.refreshsystem.simulator

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

data class ResearchDepartment(
    val title: String,
    val subtitle: String,
    val description: String,
    val focus: List<String>,
    val status: String
)

private val researchDepartments = listOf(
    ResearchDepartment(
        "Current Understanding",
        "वर्तमान वैज्ञानिक समझ",
        "शरीर, मस्तिष्क, Memory और Consciousness पर उपलब्ध वैज्ञानिक जानकारी का अध्ययन।",
        listOf("Brain and Body", "Memory and Identity", "Homeostasis", "Consciousness Research"),
        "Research Foundation"
    ),
    ResearchDepartment(
        "Open System & Stability Research",
        "मूल वैज्ञानिक अध्ययन",
        "Open System, Refresh Process, Movement और Dynamic Stability के संबंधों का अध्ययन।",
        listOf("Open System", "Refresh Process", "Dynamic Stability", "Information Retention"),
        "Core Research"
    ),
    ResearchDepartment(
        "Memory & Consciousness Research",
        "स्मृति और चेतना का अध्ययन",
        "Memory, I-Feeling, Identity और सिस्टम की निरंतरता के बीच संभावित संबंधों की जाँच।",
        listOf("Memory Formation", "Memory Refresh", "I-Feeling", "Continuity of Self"),
        "Research Module"
    ),
    ResearchDepartment(
        "Theory Evaluation",
        "थ्योरी का पुनर्मूल्यांकन",
        "Refresh Theory की वर्तमान धारणाओं की वैज्ञानिक प्रमाणों के साथ तुलना और जाँच।",
        listOf("Supported Claims", "Contradicted Claims", "Open Questions", "Theory Revision"),
        "Evaluation Stage"
    ),
    ResearchDepartment(
        "Robot System Experiment",
        "रोबोट सिस्टम प्रयोग",
        "मानव और रोबोट सिस्टम की Stability, Refresh और Continuity से जुड़े भविष्य के प्रयोगों की योजना।",
        listOf("Human System", "Robot System", "System Separation", "Continuity Hypothesis"),
        "Planned — Not Started"
    )
)

private val labBackground = Color(0xFF081321)
private val panelColor = Color(0xFF12243A)
private val accentBlue = Color(0xFF48C8FF)
private val softText = Color(0xFFB7C9DA)

@Composable
fun ResearchHomeScreen() {
    var selectedDepartment by remember { mutableIntStateOf(-1) }

    if (selectedDepartment >= 0) {
        DepartmentDetailScreen(
            department = researchDepartments[selectedDepartment],
            onBack = { selectedDepartment = -1 }
        )
        return
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = labBackground
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "REFRESH THEORY",
                    color = accentBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    "RESEARCH LAB",
                    color = Color.White,
                    fontSize = 29.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "A structured research environment",
                    color = softText,
                    fontSize = 14.sp
                )
            }

            item {
                ResearchBanner(
                    title = "THE SCIENCE OF STABILITY",
                    message = "Understanding Open Systems, Refresh Processes and the continuity of information."
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "RESEARCH DEPARTMENTS",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "05 MODULES",
                        color = accentBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            itemsIndexed(researchDepartments) { index, department ->
                DepartmentCard(
                    number = index + 1,
                    department = department,
                    onClick = { selectedDepartment = index }
                )
            }

            item {
                ResearchBanner(
                    title = "RESEARCH PRINCIPLE",
                    message = "Scientific facts, proposed hypotheses and experimental observations will be recorded separately."
                )
            }

            item {
                Text(
                    "REFRESH THEORY  •  RESEARCH FRAMEWORK",
                    color = softText,
                    fontSize = 10.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ResearchBanner(title: String, message: String) {
    val transition = rememberInfiniteTransition(label = "bannerLight")
    val lightPosition by transition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "lightPosition"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF123B59), Color(0xFF142B49), Color(0xFF202D54))
                )
            )
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(end = 8.dp)) {
            Text(title, color = accentBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(message, color = Color.White, fontSize = 15.sp, lineHeight = 22.sp)
        }

        BoxWithConstraints(modifier = Modifier.matchParentSize()) {
            Box(
                modifier = Modifier
                    .offset(x = maxWidth * lightPosition)
                    .width(34.dp)
                    .fillMaxHeight()
                    .graphicsLayer { alpha = 0.55f }
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Color(0xFF8CE6FF), Color.Transparent)
                        )
                    )
            )
        }
    }
}

@Composable
private fun DepartmentCard(
    number: Int,
    department: ResearchDepartment,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = panelColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1D4562)),
                contentAlignment = Alignment.Center
            ) {
                Text(number.toString().padStart(2, '0'), color = accentBlue, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(department.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(4.dp))
                Text(department.subtitle, color = softText, fontSize = 12.sp)
                Spacer(Modifier.height(7.dp))
                Text(department.status.uppercase(), color = accentBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Text("›", color = accentBlue, fontSize = 28.sp)
        }
    }
}

@Composable
private fun DepartmentDetailScreen(
    department: ResearchDepartment,
    onBack: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = labBackground) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            TextButton(onClick = onBack) {
                Text("← BACK TO RESEARCH LAB", color = accentBlue)
            }
            Spacer(Modifier.height(18.dp))
            ResearchBanner(department.title.uppercase(), department.subtitle)
            Spacer(Modifier.height(22.dp))
            Text("DEPARTMENT OVERVIEW", color = accentBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Text(department.description, color = Color.White, fontSize = 16.sp, lineHeight = 25.sp)
            Spacer(Modifier.height(24.dp))
            Text("RESEARCH AREAS", color = accentBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            department.focus.forEach { topic ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 9.dp),
                    colors = CardDefaults.cardColors(containerColor = panelColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("•  $topic", color = Color.White, modifier = Modifier.padding(15.dp), fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            ResearchBanner(
                department.status.uppercase(),
                "यह विभाग अभी research framework के रूप में व्यवस्थित किया जा रहा है। यहाँ कोई अप्रमाणित परिणाम वैज्ञानिक तथ्य के रूप में नहीं दिखाया जाएगा।"
            )
        }
    }
}
