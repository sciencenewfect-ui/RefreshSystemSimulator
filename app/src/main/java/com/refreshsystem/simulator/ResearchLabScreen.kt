
package com.refreshsystem.simulator

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

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

data class ResearchQuestion(
    val title: String,
    val hindiTitle: String,
    val englishAnswer: String,
    val hindiAnswer: String,
    val colors: List<Color>
)

private val researchDepartments = listOf(
    ResearchDepartment(
        "Current Understanding",
        "वर्तमान वैज्ञानिक समझ",
        "Study of established scientific knowledge about the body, brain, memory and consciousness.",
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
        "Study of Open Systems, Refresh Process, change and Dynamic Stability.",
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
        "Explore possible relationships between memory, I-Feeling, identity and continuity.",
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
        "Compare and evaluate the ideas of Refresh Theory against scientific evidence.",
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
        "Plan future experiments related to stability, refresh and continuity in human and robot systems.",
        listOf(
            ResearchTopic("Human System", "मानव प्रणाली"),
            ResearchTopic("Robot System", "रोबोट प्रणाली"),
            ResearchTopic("System Separation", "दो अलग-अलग प्रणालियों का अंतर"),
            ResearchTopic("Continuity Hypothesis", "निरंतरता से जुड़ी परिकल्पना")
        ),
        "Planned — Not Started"
    )
)

private val researchQuestions = listOf(
    ResearchQuestion(
        title = "WHAT HAPPENS AFTER DEATH?",
        hindiTitle = "मृत्यु के बाद हमारे अस्तित्व का क्या होता है?",
        englishAnswer = """
Death raises one of the deepest questions about existence: what happens to the information, identity and continuity associated with a living system?

Refresh Theory proposes studying consciousness in relation to the Dynamic Stability of an Open System. A living system continuously exchanges matter and energy with its environment. Its components interact, change and maintain an organized structure.

An important research question is: when this organization can no longer be maintained, what happens to the information and continuity associated with that system?

The idea of a Zero Dimension or another state is a speculative possibility raised within this research. It is not an established scientific finding.

This question requires clear definitions, testable hypotheses and scientific evidence. This research does not claim that consciousness survives death.
        """.trimIndent(),
        hindiAnswer = """
मृत्यु हमारे अस्तित्व से जुड़ा एक बहुत बड़ा सवाल खड़ा करती है। जीवित सिस्टम से जुड़ी जानकारी, पहचान और निरंतरता का क्या होता है?

Refresh Theory चेतना को Open System की Dynamic Stability के संबंध में समझने का प्रस्ताव रखती है। जीवित सिस्टम अपने वातावरण से लगातार पदार्थ और ऊर्जा का आदान-प्रदान करता है। उसके अलग-अलग हिस्से आपस में जुड़कर बदलते रहते हैं और एक संगठन बनाए रखते हैं।

यहाँ एक महत्वपूर्ण शोध प्रश्न है: जब यह संगठन बनाए रखना संभव नहीं रहता, तो उस सिस्टम से जुड़ी जानकारी और निरंतरता का क्या होता है?

Zero Dimension या किसी दूसरी अवस्था का विचार इस शोध में उठाई गई एक परिकल्पना है। यह अभी स्थापित वैज्ञानिक तथ्य नहीं है।

इस सवाल की जाँच के लिए स्पष्ट परिभाषाएँ, जाँची जा सकने वाली परिकल्पनाएँ और वैज्ञानिक प्रमाण जरूरी हैं। यह शोध यह दावा नहीं करता कि चेतना मृत्यु के बाद भी बनी रहती है।
        """.trimIndent(),
        listOf(Color(0xFF542039), Color(0xFF251A3F))
    ),
    ResearchQuestion(
        title = "WHY DO WE NEED REFRESH THEORY?",
        hindiTitle = "Refresh Theory पर शोध क्यों जरूरी है?",
        englishAnswer = """
We still have important questions about how consciousness, memory, identity and the feeling of being “I” are related.

Refresh Theory is a proposed framework for exploring these questions through Open Systems, Refresh Processes, Information Retention and Dynamic Stability.

The research aims to examine how a system changes while maintaining its organization, how information is retained, and whether these processes can help us understand continuity.

It also raises a long-term question: could some aspects of memory, identity or system organization ever be continued in another human or robot system?

This is a research goal, not a proven technology. The purpose of this project is to define the ideas clearly, build experiments and compare results with scientific evidence.
        """.trimIndent(),
        hindiAnswer = """
चेतना, याददाश्त, पहचान और “मैं हूँ” की अनुभूति के आपसी संबंधों को लेकर आज भी कई महत्वपूर्ण सवाल मौजूद हैं।

Refresh Theory इन सवालों को Open System, Refresh Process, Information Retention और Dynamic Stability के माध्यम से समझने का एक प्रस्तावित ढाँचा है।

इस शोध का उद्देश्य यह जाँचना है कि कोई सिस्टम बदलते हुए अपना संगठन कैसे बनाए रखता है, जानकारी किस तरह सुरक्षित रहती है और ये प्रक्रियाएँ निरंतरता को समझने में किस तरह मदद कर सकती हैं।

इसके साथ एक भविष्य से जुड़ा सवाल भी है: क्या याददाश्त, पहचान या सिस्टम के संगठन के कुछ पहलुओं को कभी किसी दूसरे मानव या रोबोट सिस्टम में जारी रखा जा सकता है?

यह एक शोध का लक्ष्य है, सिद्ध तकनीक नहीं। इस परियोजना का उद्देश्य विचारों को स्पष्ट करना, प्रयोग बनाना और परिणामों की वैज्ञानिक प्रमाणों से तुलना करना है।
        """.trimIndent(),
        listOf(Color(0xFF173C59), Color(0xFF172A49))
    ),
    ResearchQuestion(
        title = "CAN CONSCIOUSNESS CONTINUE IN A ROBOT?",
        hindiTitle = "क्या चेतना रोबोट में जारी रह सकती है?",
        englishAnswer = """
Artificial Intelligence and robotics are developing rapidly. This makes it important to study the difference between intelligent behaviour, memory, identity and conscious experience.

A robot may store information, respond to its environment and maintain its internal processes. However, these abilities alone do not prove that it has subjective experience or a human-like sense of “I”.

Refresh Theory proposes investigating whether the organization and continuity of an Open System can help us understand consciousness, and whether any aspects of a human system could be continued in a robotic system.

A major challenge is distinguishing a copy of memories or behaviour from the continuation of first-person experience.

At present, transferring human consciousness into a robot has not been scientifically demonstrated. This remains an open research question.
        """.trimIndent(),
        hindiAnswer = """
Artificial Intelligence और Robotics तेजी से विकसित हो रहे हैं। इसलिए बुद्धिमान व्यवहार, याददाश्त, पहचान और सचेत अनुभव के बीच अंतर को समझना महत्वपूर्ण हो गया है।

कोई रोबोट जानकारी सुरक्षित रख सकता है, अपने वातावरण के अनुसार प्रतिक्रिया दे सकता है और अपनी आंतरिक प्रक्रियाएँ चला सकता है। लेकिन केवल इन क्षमताओं से यह साबित नहीं होता कि उसके पास सचेत अनुभव या इंसान जैसी “मैं” की अनुभूति है।

Refresh Theory यह जाँचने का प्रस्ताव रखती है कि Open System का संगठन और निरंतरता चेतना को समझने में किस तरह मदद कर सकते हैं और क्या मानव सिस्टम के कुछ पहलुओं को रोबोट सिस्टम में जारी रखा जा सकता है।

एक बड़ी चुनौती यह समझना है कि याददाश्त या व्यवहार की कॉपी बनना और पहले व्यक्ति के अनुभव की निरंतरता — क्या ये दोनों एक ही बात हैं?

फिलहाल इंसान की चेतना को रोबोट में ट्रांसफर करना वैज्ञानिक रूप से प्रदर्शित नहीं हुआ है। यह अभी एक खुला शोध प्रश्न है।
        """.trimIndent(),
        listOf(Color(0xFF174538), Color(0xFF16313C))
    )
)

private val homeBackground = Color(0xFFF7F9FC)
private val labBackground = Color(0xFF081321)
private val panelColor = Color(0xFF12243A)
private val accentBlue = Color(0xFF48C8FF)
private val softText = Color(0xFFB7C9DA)

private val departmentColors = listOf(
    Color(0xFFEAF4FF),
    Color(0xFFE9F8F1),
    Color(0xFFF2EDFF),
    Color(0xFFFFF5E7),
    Color(0xFFE9F6FA)
)

private val departmentBorders = listOf(
    Color(0xFFB8D9FA),
    Color(0xFFB8E5CE),
    Color(0xFFD4C5FA),
    Color(0xFFF4D7A8),
    Color(0xFFB7DFEA)
)

private val departmentTextColors = listOf(
    Color(0xFF245A91),
    Color(0xFF23734E),
    Color(0xFF6546A3),
    Color(0xFF94601C),
    Color(0xFF24677C)
)

@Composable
fun ResearchHomeScreen() {
    var selectedDepartment by remember { mutableIntStateOf(-1) }
    var selectedQuestion by remember { mutableIntStateOf(-1) }

    when {
        selectedDepartment >= 0 -> {
            DepartmentDetailScreen(
                department = researchDepartments[selectedDepartment],
                onBack = { selectedDepartment = -1 }
            )
        }

        selectedQuestion >= 0 -> {
            QuestionDetailScreen(
                question = researchQuestions[selectedQuestion],
                onBack = { selectedQuestion = -1 }
            )
        }

        else -> {
            HomeScreenContent(
                onDepartmentClick = { selectedDepartment = it },
                onQuestionClick = { selectedQuestion = it }
            )
        }
    }
}

@Composable
private fun HomeScreenContent(
    onDepartmentClick: (Int) -> Unit,
    onQuestionClick: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = homeBackground
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 22.dp,
                bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            item {
                Column {
                    Text(
                        "REFRESH THEORY",
                        color = Color(0xFF245A91),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "RESEARCH LAB",
                        color = Color(0xFF172033),
                        fontSize = 29.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "Explore • Experiment • Evaluate",
                        color = Color(0xFF64748B),
                        fontSize = 13.sp
                    )
                }
            }

            item {
                ResearchQuestionTicker(onQuestionClick)
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "RESEARCH DEPARTMENTS",
                        color = Color(0xFF172033),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "अपना रिसर्च विभाग चुनें",
                        color = Color(0xFF64748B),
                        fontSize = 13.sp
                    )
                }
            }

            itemsIndexed(researchDepartments) { index, department ->
                HomeDepartmentCard(
                    index = index,
                    department = department,
                    onClick = { onDepartmentClick(index) }
                )
            }

            item {
                Text(
                    "REFRESH THEORY • RESEARCH FRAMEWORK",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ResearchQuestionTicker(
    onQuestionClick: (Int) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        while (true) {
            delay(900)
            listState.animateScrollToItem(index = 3)
            listState.scrollToItem(0)
        }
    }

    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        contentPadding = PaddingValues(end = 12.dp)
    ) {
        itemsIndexed(listOf(0, 1, 2, 0, 1, 2)) { _, questionIndex ->
            val question = researchQuestions[questionIndex]

            Box(
                modifier = Modifier
                    .width(285.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(Brush.horizontalGradient(question.colors))
                    .clickable { onQuestionClick(questionIndex) }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        question.hindiTitle,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "›",
                        color = Color.White,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeDepartmentCard(
    index: Int,
    department: ResearchDepartment,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            departmentBorders[index]
        ),
        colors = CardDefaults.cardColors(
            containerColor = departmentColors[index]
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(43.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    (index + 1).toString().padStart(2, '0'),
                    color = departmentTextColors[index],
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(Modifier.width(13.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    department.title,
                    color = departmentTextColors[index],
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    department.subtitle,
                    color = Color(0xFF536579),
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    department.status.uppercase(),
                    color = departmentTextColors[index],
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                "›",
                color = departmentTextColors[index],
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun QuestionDetailScreen(
    question: ResearchQuestion,
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = labBackground
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            item {
                TextButton(onClick = onBack) {
                    Text(
                        "← BACK TO RESEARCH LAB",
                        color = accentBlue
                    )
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.horizontalGradient(question.colors))
                        .padding(18.dp)
                ) {
                    Column {
                        Text(
                            question.title,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            question.hindiTitle,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 27.sp
                        )
                    }
                }
            }

            item {
                AnswerSection(
                    heading = "ENGLISH EXPLANATION",
                    text = question.englishAnswer
                )
            }

            item {
                AnswerSection(
                    heading = "हिंदी में व्याख्या",
                    text = question.hindiAnswer
                )
            }

            item {
                Text(
                    "REFRESH THEORY • RESEARCH QUESTION",
                    color = softText,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun AnswerSection(
    heading: String,
    text: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = panelColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(17.dp)
        ) {
            Text(
                heading,
                color = accentBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text,
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 23.sp
            )
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                TextButton(onClick = onBack) {
                    Text(
                        "← BACK TO RESEARCH LAB",
                        color = accentBlue
                    )
                }
            }

            item {
                Text(
                    department.title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 29.sp
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    department.subtitle,
                    color = accentBlue,
                    fontSize = 14.sp
                )
            }

            item {
                Text(
                    "DEPARTMENT OVERVIEW",
                    color = accentBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    department.description,
                    color = Color.White,
                    fontSize = 15.sp,
                    lineHeight = 24.sp
                )
            }

            item {
                Text(
                    "RESEARCH AREAS • शोध के विषय",
                    color = accentBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            itemsIndexed(department.focus) { _, topic ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = panelColor
                    ),
                    shape = RoundedCornerShape(13.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color(0xFF29445F)
                    )
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
                        Spacer(Modifier.height(5.dp))
                        Text(
                            topic.hindi,
                            color = softText,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = panelColor
                    ),
                    shape = RoundedCornerShape(13.dp)
                ) {
                    Column(Modifier.padding(15.dp)) {
                        Text(
                            department.status.uppercase(),
                            color = accentBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(7.dp))
                        Text(
                            "यह विभाग Refresh Theory के शोध ढाँचे का हिस्सा है। यहाँ अप्रमाणित परिणामों को वैज्ञानिक तथ्य के रूप में प्रस्तुत नहीं किया जाएगा।",
                            color = Color.White,
                            fontSize = 14.sp,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }
    }
}
