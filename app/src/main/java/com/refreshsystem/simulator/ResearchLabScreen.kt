
package com.refreshsystem.simulator

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Navy = Color(0xFF081321)
private val CardDark = Color(0xFF12243A)
private val Accent = Color(0xFF48C8FF)
private val SoftText = Color(0xFFB7C9DA)
private val LightBg = Color(0xFFF4F7FB)

data class ResearchTopic(
    val title: String,
    val hindi: String,
    val info: String,
    val hindiInfo: String,
    val hypothesis: String,
    val experiment: String
)

data class ResearchDepartment(
    val title: String,
    val subtitle: String,
    val description: String,
    val topics: List<ResearchTopic>
)

private fun topic(
    title: String,
    hindi: String,
    info: String,
    hindiInfo: String,
    hypothesis: String,
    experiment: String
) = ResearchTopic(title, hindi, info, hindiInfo, hypothesis, experiment)

private val departments = listOf(

    ResearchDepartment(
        "Current Understanding",
        "वर्तमान वैज्ञानिक समझ",
        "Explore established scientific knowledge related to the brain, body and consciousness.",
        listOf(
            topic(
                "Brain and Body", "मस्तिष्क और शरीर",
                "The brain and body work through connected biological processes. The nervous system receives signals, processes information and coordinates responses.",
                "मस्तिष्क और शरीर आपस में जुड़ी जैविक प्रक्रियाओं से काम करते हैं। तंत्रिका तंत्र संकेत प्राप्त करता है और शरीर की प्रतिक्रियाओं को नियंत्रित करता है।",
                "The organization of brain and body processes may be important for conscious activity.",
                "Study reliable neuroscience material. Record how brain, nervous system and body processes interact. Do not make medical conclusions from this exercise."
            ),
            topic(
                "Memory and Identity", "स्मृति और पहचान",
                "Memory helps retain information from past events. Identity refers to the characteristics and continuity through which a person recognizes themselves.",
                "स्मृति पुराने अनुभवों की जानकारी को बनाए रखने में मदद करती है। पहचान उन विशेषताओं और निरंतरता से जुड़ी है जिनसे व्यक्ति स्वयं को पहचानता है।",
                "Memory may contribute to a person's sense of identity.",
                "Record a few neutral daily experiences and note which details remain after one day and one week. Compare the records."
            ),
            topic(
                "Homeostasis", "शरीर का आंतरिक संतुलन",
                "Homeostasis is the regulation of internal conditions, such as temperature and blood glucose, within ranges compatible with life.",
                "होमियोस्टेसिस शरीर के अंदर तापमान और ग्लूकोज जैसी स्थितियों को जीवन के अनुकूल सीमा में बनाए रखने की प्रक्रिया है।",
                "Continuous regulation may help a living system maintain stability despite environmental changes.",
                "Study examples of homeostasis from biology textbooks. List the variable, the regulation mechanism and the response. Do not perform medical experiments."
            ),
            topic(
                "Consciousness Research", "चेतना पर वैज्ञानिक शोध",
                "Consciousness research studies awareness, subjective experience, attention and the relationship between brain activity and experience.",
                "चेतना पर शोध में जागरूकता, अनुभव, ध्यान और मस्तिष्क की गतिविधियों के बीच संबंध का अध्ययन किया जाता है।",
                "Consciousness may be related to coordinated activity in an organized living system.",
                "Compare different scientific theories of consciousness. Record their claims, evidence and unanswered questions separately."
            )
        )
    ),

    ResearchDepartment(
        "Open System & Stability Research",
        "ओपन सिस्टम और स्थिरता का शोध",
        "Study systems that exchange matter, energy or information with their surroundings.",
        listOf(
            topic(
                "Open System", "ओपन सिस्टम",
                "An open system exchanges matter, energy or information with its environment. Living organisms are examples of open systems.",
                "ओपन सिस्टम अपने वातावरण से पदार्थ, ऊर्जा या जानकारी का आदान-प्रदान करता है। जीवित शरीर इसके उदाहरण हैं।",
                "Continuous exchange may be necessary for some systems to maintain their organization.",
                "Choose a simple example such as a plant. Record what enters, what leaves and what internal processes occur."
            ),
            topic(
                "Refresh Process", "रिफ्रेश प्रक्रिया",
                "A refresh process describes ongoing updates or changes in a system's components, states or relationships over time.",
                "रिफ्रेश प्रक्रिया का अर्थ है किसी सिस्टम के हिस्सों, स्थितियों या उनके संबंधों में समय के साथ लगातार बदलाव और अपडेट होना।",
                "Repeated updating may contribute to the maintenance of a system's organization.",
                "Create a simple system model. Record its initial state, apply defined updates and note which properties remain unchanged."
            ),
            topic(
                "Dynamic Stability", "डायनामिक स्थिरता",
                "Dynamic stability means a system can undergo changes while maintaining important properties or functions within a defined range.",
                "डायनामिक स्थिरता का अर्थ है कि बदलाव होते रहने के बावजूद सिस्टम अपने जरूरी गुणों या कार्यों को एक सीमा में बनाए रखता है।",
                "A system may remain stable through continuous changes rather than by remaining completely unchanged.",
                "Use a simple simulation or spreadsheet. Change one input at a time and record whether the selected system property remains within its defined range."
            ),
            topic(
                "Information Retention", "जानकारी को बनाए रखना",
                "Information retention refers to the persistence of information or traces in a system over time.",
                "Information Retention का अर्थ है किसी सिस्टम में जानकारी या उसके निशान का समय के साथ बने रहना।",
                "Information retention may be one measurable part of a system's stability.",
                "Store a defined set of information in a test system. Reopen it later and compare the stored information with the original."
            )
        )
    ),

    ResearchDepartment(
        "Memory & Consciousness Research",
        "याददाश्त और चेतना का अध्ययन",
        "Explore memory, continuity, self-recognition and the proposed role of information in consciousness.",
        listOf(
            topic(
                "Memory Formation", "स्मृति का निर्माण",
                "Memory formation involves processes through which information is encoded, stored and later retrieved.",
                "स्मृति निर्माण में जानकारी को ग्रहण करना, सुरक्षित रखना और बाद में वापस प्राप्त करना शामिल है।",
                "Information retained over time may influence the later state of a system.",
                "Record a small set of neutral facts, test recall after different time intervals and note which facts remain available."
            ),
            topic(
                "Memory Refresh", "स्मृति का रिफ्रेश",
                "Memory can be retrieved, updated and sometimes changed when new information is incorporated.",
                "स्मृति को दोबारा याद किया जा सकता है, अपडेट किया जा सकता है और नई जानकारी मिलने पर उसमें बदलाव भी हो सकता है।",
                "Repeated retrieval and updating may affect how information is retained.",
                "Record a short neutral description. Review it after a delay, add new details and compare the original and updated versions."
            ),
            topic(
                "I-Feeling", "मैं होने का एहसास",
                "The sense of self is studied through self-awareness, body representation, memory and the feeling of being an individual.",
                "स्वयं का एहसास आत्म-जागरूकता, शरीर की पहचान, स्मृति और एक व्यक्ति होने की अनुभूति से जुड़ा विषय है।",
                "The sense of self may depend on several connected processes rather than one isolated memory.",
                "Review published research on self-awareness. Separate measurable behavior from subjective reports. Do not treat a questionnaire as proof of consciousness."
            ),
            topic(
                "Continuity of Self", "स्वयं की निरंतरता",
                "People often experience themselves as the same individual across time, despite changes in their body, knowledge and memories.",
                "शरीर, ज्ञान और यादों में बदलाव होने के बावजूद व्यक्ति समय के साथ स्वयं को एक ही व्यक्ति के रूप में अनुभव कर सकता है।",
                "Continuity of self may involve relationships between memory, body and ongoing processes.",
                "Maintain a dated personal learning log for a limited period. Compare changes in beliefs and memories while identifying what feels continuous."
            )
        )
    ),

    ResearchDepartment(
        "Theory Evaluation",
        "थ्योरी का वैज्ञानिक मूल्यांकन",
        "Evaluate Refresh Theory through evidence, testable predictions and possible revisions.",
        listOf(
            topic(
                "Supported Claims", "समर्थित दावे",
                "A claim is supported when reliable evidence is consistent with its predictions. Support does not automatically prove a complete theory.",
                "किसी दावे को तब समर्थन मिलता है जब विश्वसनीय प्रमाण उसकी भविष्यवाणी से मेल खाते हैं। समर्थन मिलना पूरी थ्योरी को सिद्ध नहीं करता।",
                "Some parts of Refresh Theory may match established findings about open systems and biological regulation.",
                "Choose one specific claim. Find reliable scientific sources and record the evidence, limitations and alternative explanations."
            ),
            topic(
                "Contradicted Claims", "विरोधी प्रमाण",
                "A scientific model must be open to evidence that conflicts with its predictions. Contradictions can reveal weaknesses or necessary revisions.",
                "वैज्ञानिक मॉडल को ऐसे प्रमाणों के लिए खुला रहना चाहिए जो उसकी भविष्यवाणियों से मेल नहीं खाते। विरोधी प्रमाण थ्योरी की कमजोरियाँ दिखा सकते हैं।",
                "Testing possible contradictions may help identify where Refresh Theory needs revision.",
                "Write one clear prediction. Search for observations that could contradict it. Record conflicting evidence without ignoring it."
            ),
            topic(
                "Open Questions", "अनसुलझे सवाल",
                "Open questions are important issues for which current explanations or evidence are incomplete.",
                "अनसुलझे सवाल वे विषय हैं जिनके उत्तर के लिए अभी पर्याप्त प्रमाण या स्पष्ट व्याख्या उपलब्ध नहीं है।",
                "Identifying unanswered questions can help convert a broad theory into testable research questions.",
                "Write one unanswered question. Define what evidence would help answer it and what result would remain inconclusive."
            ),
            topic(
                "Theory Revision", "थ्योरी में सुधार",
                "Scientific theories and models can be refined when new evidence, clearer definitions or better experiments become available.",
                "नए प्रमाण, स्पष्ट परिभाषाएँ या बेहतर प्रयोग मिलने पर वैज्ञानिक थ्योरी और मॉडल में सुधार किया जा सकता है।",
                "Refresh Theory may need refinement as its claims are tested.",
                "Review one previous research record. Identify unclear terms, missing controls and alternative explanations. Write a revised version of the hypothesis."
            )
        )
    ),

    ResearchDepartment(
        "Robot System Experiment",
        "रोबोट सिस्टम प्रयोग",
        "Compare information processing and continuity in artificial and biological systems without assuming that robots are conscious.",
        listOf(
            topic(
                "Human System", "मानव सिस्टम",
                "A human is a living biological system involving metabolism, nervous activity, sensory processing and many forms of regulation.",
                "मनुष्य एक जीवित जैविक सिस्टम है जिसमें मेटाबॉलिज्म, तंत्रिका गतिविधि, इंद्रियों से जानकारी लेना और कई प्रकार का नियंत्रण शामिल है।",
                "Human system organization may provide useful questions for comparing biological and artificial systems.",
                "Create a non-invasive diagram of human system inputs, internal processes and outputs using published information."
            ),
            topic(
                "Robot System", "रोबोट सिस्टम",
                "Robots use sensors, software, processors and actuators to receive information and produce actions.",
                "रोबोट सेंसर, सॉफ्टवेयर, प्रोसेसर और मोटर जैसी व्यवस्थाओं से जानकारी लेते हैं और उसके अनुसार कार्य करते हैं।",
                "A robot can retain and update information, but this alone does not establish subjective experience.",
                "Use a simple software simulation. Record its inputs, stored data, updates and outputs. Do not label its behavior as consciousness."
            ),
            topic(
                "System Separation", "सिस्टम का अलग होना",
                "Two systems may have similar information while remaining physically and operationally separate systems.",
                "दो सिस्टम में समान जानकारी हो सकती है, फिर भी वे अलग-अलग भौतिक और कार्यात्मक सिस्टम रह सकते हैं।",
                "Copying information from one system to another may not establish continuity of the original system.",
                "Create two independent test records with identical starting data. Update them separately and document how their states diverge."
            ),
            topic(
                "Continuity Hypothesis", "निरंतरता की परिकल्पना",
                "Continuity in a system can refer to persistence of organization, information or operation. These meanings should be measured separately.",
                "किसी सिस्टम की निरंतरता का अर्थ उसके संगठन, जानकारी या संचालन का बने रहना हो सकता है। इन सभी को अलग-अलग मापना चाहिए।",
                "The proposed hypothesis is that some form of system continuity may be relevant to consciousness. This remains unproven.",
                "Design a computer simulation that measures information persistence and organizational continuity. The result cannot establish or disprove subjective consciousness by itself."
            )
        )
    )
)

@Composable
fun ResearchHomeScreen() {
    var selectedDepartment by remember { mutableIntStateOf(-1) }
    var selectedTopic by remember { mutableIntStateOf(-1) }

    if (selectedDepartment >= 0 && selectedTopic >= 0) {
        TopicResearchScreen(
            department = departments[selectedDepartment],
            topic = departments[selectedDepartment].topics[selectedTopic],
            storageKey = "${selectedDepartment}_${selectedTopic}",
            onBack = { selectedTopic = -1 }
        )
    } else if (selectedDepartment >= 0) {
        DepartmentScreen(
            department = departments[selectedDepartment],
            onBack = { selectedDepartment = -1 },
            onTopicClick = { selectedTopic = it }
        )
    } else {
        HomeScreen(
            onDepartmentClick = { selectedDepartment = it }
        )
    }
}

@Composable
private fun HomeScreen(onDepartmentClick: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBg)
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        Text(
            "REFRESH THEORY",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF14283E)
        )
        Text(
            "RESEARCH LAB",
            fontSize = 15.sp,
            color = Color(0xFF55718A),
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(18.dp))

        Text(
            "Explore. Experiment. Evaluate.",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF14283E)
        )
        Text(
            "शोध करें, प्रयोग करें और प्रमाणों के आधार पर मूल्यांकन करें।",
            fontSize = 13.sp,
            color = Color.DarkGray
        )

        Spacer(Modifier.height(22.dp))

        departments.forEachIndexed { index, department ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 13.dp)
                    .clickable { onDepartmentClick(index) },
                shape = RoundedCornerShape(17.dp),
                colors = CardDefaults.cardColors(
                    containerColor = listOf(
                        Color(0xFFE5F3FF),
                        Color(0xFFE8F7F0),
                        Color(0xFFF2ECFF),
                        Color(0xFFFFF1E5),
                        Color(0xFFFFEDEF)
                    )[index]
                ),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.padding(17.dp)) {
                    Text(
                        department.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF172E46)
                    )
                    Text(
                        department.subtitle,
                        fontSize = 14.sp,
                        color = Color(0xFF526B80)
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        "${department.topics.size} Research Topics  •  Open Department →",
                        fontSize = 12.sp,
                        color = Color(0xFF2678A5)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Research note: This app is an experimental research tool. Saved results are observations, not proof of consciousness.",
            fontSize = 12.sp,
            color = Color.DarkGray
        )
    }
}

@Composable
private fun DepartmentScreen(
    department: ResearchDepartment,
    onBack: () -> Unit,
    onTopicClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy)
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        Text(
            "← Back to Research Lab",
            color = Accent,
            modifier = Modifier.clickable { onBack() }
        )

        Spacer(Modifier.height(18.dp))

        Text(
            department.title,
            color = Color.White,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            department.subtitle,
            color = Accent,
            fontSize = 16.sp
        )

        Spacer(Modifier.height(12.dp))

        Text(
            department.description,
            color = SoftText,
            fontSize = 14.sp
        )

        Spacer(Modifier.height(22.dp))

        Text(
            "RESEARCH TOPICS",
            color = Accent,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(10.dp))

        department.topics.forEachIndexed { index, topic ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clickable { onTopicClick(index) },
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        topic.title,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        topic.hindi,
                        color = SoftText,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        "Open Research Topic →",
                        color = Accent,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TopicResearchScreen(
    department: ResearchDepartment,
    topic: ResearchTopic,
    storageKey: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(
            "refresh_research_records",
            Context.MODE_PRIVATE
        )
    }

    val observationKey = "observation_$storageKey"
    val evaluationKey = "evaluation_$storageKey"
    val historyKey = "history_$storageKey"

    var observation by remember(storageKey) {
        mutableStateOf(prefs.getString(observationKey, "") ?: "")
    }

    var evaluation by remember(storageKey) {
        mutableStateOf(
            prefs.getString(evaluationKey, "Inconclusive")
                ?: "Inconclusive"
        )
    }

    var savedMessage by remember(storageKey) {
        mutableStateOf("")
    }

    var history by remember(storageKey) {
        mutableStateOf(prefs.getString(historyKey, "") ?: "")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy)
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        Text(
            "← Back to Topics",
            color = Accent,
            modifier = Modifier.clickable { onBack() }
        )

        Spacer(Modifier.height(17.dp))

        Text(
            department.title,
            color = Accent,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(7.dp))

        Text(
            topic.title,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            topic.hindi,
            color = SoftText,
            fontSize = 17.sp
        )

        Spacer(Modifier.height(20.dp))

        ResearchSection("1. INFORMATION", "जानकारी") {
            Text(topic.info, color = Color.White, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            Text(topic.hindiInfo, color = SoftText, fontSize = 14.sp)
        }

        ResearchSection("2. RESEARCH HYPOTHESIS", "शोध की परिकल्पना") {
            Text(
                "Proposed hypothesis — not an established scientific fact.",
                color = Accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(topic.hypothesis, color = Color.White, fontSize = 14.sp)
        }

        ResearchSection("3. EXPERIMENT PROTOCOL", "प्रयोग की प्रक्रिया") {
            Text(topic.experiment, color = Color.White, fontSize = 14.sp)
        }

        ResearchSection("4. OBSERVATION", "अपना डेटा दर्ज करें") {
            Text(
                "Record what you observed. Include conditions, measurements and unexpected results.",
                color = SoftText,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = observation,
                onValueChange = {
                    observation = it
                    savedMessage = ""
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                label = { Text("Write your observation") },
                placeholder = {
                    Text("What did you observe?")
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = SoftText,
                    focusedLabelColor = Accent,
                    unfocusedLabelColor = SoftText,
                    cursorColor = Accent
                )
            )
        }

        ResearchSection("5. RESULT EVALUATION", "परिणाम का मूल्यांकन") {
            Text(
                "Select how your observation relates to the hypothesis.",
                color = SoftText,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(10.dp))

            listOf(
                "Supports",
                "Does Not Support",
                "Inconclusive"
            ).forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { evaluation = option }
                        .padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = evaluation == option,
                        onClick = { evaluation = option },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Accent,
                            unselectedColor = SoftText
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(option, color = Color.White, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    val stamp = SimpleDateFormat(
                        "yyyy-MM-dd HH:mm",
                        Locale.getDefault()
                    ).format(Date())

                    val entry = "[$stamp]\nResult: $evaluation\n$observation"
                    val updatedHistory = if (history.isBlank()) {
                        entry
                    } else {
                        "$entry\n\n--------------------\n\n$history"
                    }

                    prefs.edit()
                        .putString(observationKey, observation)
                        .putString(evaluationKey, evaluation)
                        .putString(historyKey, updatedHistory)
                        .apply()

                    history = updatedHistory
                    savedMessage = "Research record saved on this device."
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = Navy
                )
            ) {
                Text("SAVE RESEARCH RECORD", fontWeight = FontWeight.Bold)
            }

            if (savedMessage.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(savedMessage, color = Color(0xFF7DE6A2), fontSize = 13.sp)
            }
        }

        if (history.isNotBlank()) {
            ResearchSection("6. SAVED RESEARCH HISTORY", "पहले सेव किए गए रिकॉर्ड") {
                Text(
                    history,
                    color = SoftText,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "Scientific caution: A result in this experiment does not prove or disprove consciousness by itself. Interpret observations within the limits of the experiment.",
            color = SoftText,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun ResearchSection(
    title: String,
    hindi: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(15.dp)
    ) {
        Column(Modifier.padding(15.dp)) {
            Text(
                title,
                color = Accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                hindi,
                color = SoftText,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}
