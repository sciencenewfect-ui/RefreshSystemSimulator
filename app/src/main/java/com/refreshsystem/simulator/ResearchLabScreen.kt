
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ResearchTopic(
    val english: String,
    val hindi: String
)

data class ResearchDepartment(
    val title: String,
    val subtitle: String,
    val description: String,
    val focus: List<ResearchTopic>,
    val status: String
)

private val researchDepartments = listOf(
    ResearchDepartment(
        "Current Understanding",
        "वर्तमान वैज्ञानिक समझ",
        "शरीर, मस्तिष्क, याददाश्त और चेतना के बारे में उपलब्ध वैज्ञानिक जानकारी का अध्ययन।",
        listOf(
            ResearchTopic("Brain and Body", "मस्तिष्क और शरीर"),
            ResearchTopic("Memory and Identity", "याददाश्त और पहचान"),
            ResearchTopic("Homeostasis", "शरीर का आंतरिक संतुलन"),
            ResearchTopic("Consciousness Research", "चेतना से जुड़ा वैज्ञानिक शोध")
        ),
        "Research Foundation"
    ),
    ResearchDepartment(
        "Open System & Stability Research",
        "ओपन सिस्टम और स्थिरता का शोध",
        "Open System, Refresh Process, Movement और Dynamic Stability के संबंधों का अध्ययन।",
        listOf(
            ResearchTopic("Open System", "खुली प्रणाली"),
            ResearchTopic("Refresh Process", "लगातार अपडेट होने की प्रक्रिया"),
            ResearchTopic("Dynamic Stability", "बदलाव के बीच संगठन बनाए रखना"),
            ResearchTopic("Information Retention", "जानकारी को सुरक्षित बनाए रखना")
        ),
        "Core Research"
    ),
    ResearchDepartment(
        "Memory & Consciousness Research",
        "याददाश्त और चेतना का अध्ययन",
        "Memory, I-Feeling, Identity और सिस्टम की निरंतरता के बीच संभावित संबंधों की जाँच।",
        listOf(
            ResearchTopic("Memory Formation", "याददाश्त बनने की प्रक्रिया"),
            ResearchTopic("Memory Refresh", "याददाश्त का लगातार अपडेट होना"),
            ResearchTopic("I-Feeling", "मैं मौजूद हूँ — यह अनुभूति"),
            ResearchTopic("Continuity of Self", "अपने अस्तित्व की निरंतरता")
        ),
        "Research Module"
    ),
    ResearchDepartment(
        "Theory Evaluation",
        "थ्योरी का वैज्ञानिक मूल्यांकन",
        "Refresh Theory की धारणाओं की वैज्ञानिक प्रमाणों के साथ तुलना और जाँच।",
        listOf(
            ResearchTopic("Supported Claims", "प्रमाणों से समर्थित दावे"),
            ResearchTopic("Contradicted Claims", "प्रमाणों से मेल न खाने वाले दावे"),
            ResearchTopic("Open Questions", "ऐसे सवाल जिनके जवाब अभी नहीं मिले"),
            ResearchTopic("Theory Revision", "नए प्रमाणों के आधार पर थ्योरी में सुधार")
        ),
        "Evaluation Stage"
    ),
    ResearchDepartment(
        "Robot System Experiment",
        "रोबोट सिस्टम प्रयोग",
        "मानव और रोबोट सिस्टम की Stability, Refresh और Continuity से जुड़े भविष्य के प्रयोगों की योजना।",
        listOf(
            ResearchTopic("Human System", "मानव प्रणाली"),
            ResearchTopic("Robot System", "रोबोट प्रणाली"),
            ResearchTopic("System Separation", "दो अलग-अलग प्रणालियों का अंतर"),
            ResearchTopic("Continuity Hypothesis", "निरंतरता से जुड़ी परिकल्पना")
        ),
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

            // Banner 1: Research challenge
            item {
                ResearchBanner(
                    title = "THE QUESTION OF EXISTENCE",
                    hindiTitle = "मृत्यु के बाद हमारे अस्तित्व का क्या होगा?",
                    message = "Could death mean the end of our existence, or is there another possibility?",
                    hindiMessage = "क्या मृत्यु के बाद हमारा अस्तित्व पूरी तरह समाप्त हो जाता है? Zero Dimension जैसी अवस्था का विचार एक परिकल्पना है, जिसका कोई प्रमाण अभी उपलब्ध नहीं है। लेकिन यह हमारे सामने एक बड़ी चुनौती और शोध का सवाल रखता है।",
                    colors = listOf(
                        Color(0xFF54202F),
                        Color(0xFF351C45),
                        Color(0xFF172A50)
                    )
                )
            }

            // Banner 2: Why Refresh Theory?
            item {
                ResearchBanner(
                    title = "WHY DO WE NEED REFRESH THEORY?",
                    hindiTitle = "Refresh Theory पर शोध क्यों जरूरी है?",
                    message = "To explore consciousness, memory, identity and the continuity of existence.",
                    hindiMessage = "यदि हम अपने अस्तित्व को लगातार बनाए रखने की संभावना समझना चाहते हैं, तो पहले चेतना, याददाश्त और पहचान को समझना होगा। ये कैसे काम करते हैं? इनके बीच क्या संबंध है? क्या इनकी निरंतरता किसी दूसरे मानव या रोबोट सिस्टम में संभव हो सकती है? Refresh Theory इन्हीं सवालों की जाँच करने का एक प्रयास है।",
                    colors = listOf(
                        Color(0xFF123B59),
                        Color(0xFF143A48),
                        Color(0xFF202D54)
                    )
                )
            }

            item {
                ResearchBanner(
                    title = "THE FUTURE OF HUMANITY",
                    hindiTitle = "आने वाली तकनीक और मानवता का भविष्य",
                    message = "AI and robotics are advancing. Understanding consciousness and identity may become an important scientific challenge.",
                    hindiMessage = "आने वाले समय में AI और Robotics की क्षमता और बढ़ सकती है। इसलिए चेतना, याददाश्त और पहचान को समझने से जुड़े सवालों पर अभी से गंभीर शोध करना महत्वपूर्ण है।",
                    colors = listOf(
                        Color(0xFF123B59),
                        Color(0xFF172B50),
                        Color(0xFF30214D)
                    )
                )
            }

            item {
                Text(
                    "RESEARCH DEPARTMENTS",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "05 RESEARCH MODULES • पाँच शोध विभाग",
                    color = accentBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
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
                    title = "JOIN THE RESEARCH",
                    hindiTitle = "इस शोध का हिस्सा बनें",
                    message = "Learn, explore, experiment and contribute to research updates.",
                    hindiMessage = "इस Application के माध्यम से आप Refresh Theory को समझ सकते हैं, अपने प्रयोग कर सकते हैं और अपने अनुभव तथा शोध के परिणाम साझा कर सकते हैं। हमारा उद्देश्य मिलकर सवालों की जाँच करना और नए प्रमाणों के आधार पर शोध को आगे बढ़ाना है।",
                    colors = listOf(
                        Color(0xFF104A49),
                        Color(0xFF123B59),
                        Color(0xFF142B49)
                    )
                )
            }

            item {
                Text(
                    "REFRESH THEORY • RESEARCH FRAMEWORK",
                    color = softText,
                    fontSize = 10.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ResearchBanner(
    title: String,
    hindiTitle: String,
    message: String,
    hindiMessage: String,
    colors: List<Color>
) {
    val transition = rememberInfiniteTransition(label = "bannerLight")
    val lightPosition by transition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "lightPosition"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(colors))
            .padding(17.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 8.dp)
        ) {
            Text(
                title,
                color = accentBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(7.dp))

            Text(
                hindiTitle,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 25.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                message,
                color = Color(0xFFD5E9F8),
                fontSize = 13.sp,
                lineHeight = 19.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                hindiMessage,
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 23.sp
            )
        }

        BoxWithConstraints(modifier = Modifier.matchParentSize()) {
            Box(
                modifier = Modifier
                    .offset(x = maxWidth * lightPosition)
                    .width(30.dp)
                    .fillMaxHeight()
                    .graphicsLayer { alpha = 0.30f }
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color(0xFF8CE6FF),
                                Color.Transparent
                            )
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = panelColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1D4562)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    number.toString().padStart(2, '0'),
                    color = accentBlue,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    department.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    department.subtitle,
                    color = softText,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    department.status.uppercase(),
                    color = accentBlue,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
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
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = labBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            TextButton(onClick = onBack) {
                Text("← BACK TO RESEARCH LAB", color = accentBlue)
            }

            Spacer(Modifier.height(18.dp))

            Text(
                department.title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                department.subtitle,
                color = accentBlue,
                fontSize = 14.sp
            )

            Spacer(Modifier.height(22.dp))

            Text(
                "DEPARTMENT OVERVIEW",
                color = accentBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                department.description,
                color = Color.White,
                fontSize = 16.sp,
                lineHeight = 25.sp
            )

            Spacer(Modifier.height(24.dp))

            Text(
                "RESEARCH AREAS • शोध के विषय",
                color = accentBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(10.dp))

            department.focus.forEach { topic ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 9.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = panelColor
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(15.dp)
                    ) {
                        Text(
                            topic.english,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            topic.hindi,
                            color = softText,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            ResearchBanner(
                title = department.status.uppercase(),
                hindiTitle = "विभाग की वर्तमान स्थिति",
                message = "This department is part of the research framework.",
                hindiMessage = "यह विभाग अभी शोध के ढाँचे के रूप में व्यवस्थित किया जा रहा है। यहाँ अप्रमाणित परिणामों को वैज्ञानिक तथ्य के रूप में प्रस्तुत नहीं किया जाएगा।",
                colors = listOf(
                    Color(0xFF123B59),
                    Color(0xFF142B49),
                    Color(0xFF202D54)
                )
            )
        }
    }
}
