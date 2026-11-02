
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
    val infoEn: String,
    val infoHi: String,
    val hypothesisEn: String,
    val hypothesisHi: String,
    val experimentEn: String,
    val experimentHi: String
)

data class ResearchDepartment(
    val title: String,
    val subtitle: String,
    val descriptionEn: String,
    val descriptionHi: String,
    val topics: List<ResearchTopic>
)

private fun t(
    title: String,
    hindi: String,
    infoEn: String,
    infoHi: String,
    hypothesisEn: String,
    hypothesisHi: String,
    experimentEn: String,
    experimentHi: String
) = ResearchTopic(
    title, hindi, infoEn, infoHi,
    hypothesisEn, hypothesisHi,
    experimentEn, experimentHi
)

private val departments = listOf(

    ResearchDepartment(
        "Current Understanding",
        "वर्तमान वैज्ञानिक समझ",
        "Explore established scientific knowledge about the brain, body and consciousness.",
        "मस्तिष्क, शरीर और चेतना से संबंधित स्थापित वैज्ञानिक जानकारी का अध्ययन करें।",
        listOf(
            t(
                "Brain and Body", "मस्तिष्क और शरीर",
                "The brain and body work through connected biological processes. The nervous system receives signals, processes information and coordinates responses.",
                "मस्तिष्क और शरीर आपस में जुड़ी जैविक प्रक्रियाओं से काम करते हैं। तंत्रिका तंत्र संकेत प्राप्त करता है, जानकारी को संसाधित करता है और शरीर की प्रतिक्रियाओं का समन्वय करता है।",
                "The organization of brain and body processes may be important for conscious activity.",
                "मस्तिष्क और शरीर की प्रक्रियाओं का संगठन चेतन गतिविधियों के लिए महत्वपूर्ण हो सकता है।",
                "Study reliable neuroscience material. Record how the brain, nervous system and body processes interact.",
                "विश्वसनीय न्यूरोसाइंस सामग्री का अध्ययन करें। लिखें कि मस्तिष्क, तंत्रिका तंत्र और शरीर की प्रक्रियाएँ किस प्रकार एक-दूसरे से जुड़ी हैं।"
            ),
            t(
                "Memory and Identity", "स्मृति और पहचान",
                "Memory helps retain information from past events. Identity refers to the characteristics and continuity through which a person recognizes themselves.",
                "स्मृति पुरानी घटनाओं की जानकारी को बनाए रखने में मदद करती है। पहचान उन विशेषताओं और निरंतरता से जुड़ी है जिनसे व्यक्ति स्वयं को पहचानता है।",
                "Memory may contribute to a person's sense of identity.",
                "स्मृति किसी व्यक्ति की अपनी पहचान के एहसास में योगदान दे सकती है।",
                "Record neutral daily experiences. Review the records after one day and one week. Compare which details remain.",
                "रोजमर्रा की सामान्य घटनाएँ लिखें। एक दिन और एक सप्ताह बाद रिकॉर्ड दोबारा देखें। तुलना करें कि कौन-सी जानकारियाँ याद या सुरक्षित रहती हैं।"
            ),
            t(
                "Homeostasis", "शरीर का आंतरिक संतुलन",
                "Homeostasis is the regulation of internal conditions, such as temperature and blood glucose, within ranges compatible with life.",
                "होमियोस्टेसिस वह प्रक्रिया है जिसमें शरीर तापमान और रक्त ग्लूकोज जैसी आंतरिक स्थितियों को जीवन के अनुकूल सीमा में बनाए रखता है।",
                "Continuous regulation may help a living system maintain stability despite environmental changes.",
                "लगातार नियंत्रण की प्रक्रिया वातावरण में बदलाव होने के बावजूद जीवित सिस्टम को स्थिरता बनाए रखने में मदद कर सकती है।",
                "Study examples of homeostasis. List the variable, regulation mechanism and resulting response.",
                "होमियोस्टेसिस के उदाहरणों का अध्ययन करें। संबंधित स्थिति, उसे नियंत्रित करने की प्रक्रिया और उससे होने वाली प्रतिक्रिया लिखें।"
            ),
            t(
                "Consciousness Research", "चेतना पर वैज्ञानिक शोध",
                "Consciousness research studies awareness, subjective experience, attention and the relationship between brain activity and experience.",
                "चेतना पर शोध में जागरूकता, व्यक्तिगत अनुभव, ध्यान और मस्तिष्क की गतिविधियों तथा अनुभव के संबंध का अध्ययन किया जाता है।",
                "Consciousness may be related to coordinated activity in an organized living system.",
                "चेतना का संबंध किसी संगठित जीवित सिस्टम की आपस में समन्वित गतिविधियों से हो सकता है।",
                "Compare scientific theories of consciousness. Record their claims, evidence and unanswered questions.",
                "चेतना से जुड़ी वैज्ञानिक थ्योरी की तुलना करें। उनके दावे, उपलब्ध प्रमाण और अनसुलझे सवाल अलग-अलग लिखें।"
            )
        )
    ),

    ResearchDepartment(
        "Open System & Stability Research",
        "ओपन सिस्टम और स्थिरता का शोध",
        "Study systems that exchange matter, energy or information with their surroundings.",
        "ऐसे सिस्टम का अध्ययन करें जो अपने वातावरण से पदार्थ, ऊर्जा या जानकारी का आदान-प्रदान करते हैं।",
        listOf(
            t(
                "Open System", "ओपन सिस्टम",
                "An open system exchanges matter, energy or information with its environment. Living organisms are examples of open systems.",
                "ओपन सिस्टम अपने वातावरण से पदार्थ, ऊर्जा या जानकारी का आदान-प्रदान करता है। जीवित शरीर इसके उदाहरण हैं।",
                "Continuous exchange may be necessary for some systems to maintain their organization.",
                "कुछ सिस्टमों के संगठन को बनाए रखने के लिए लगातार आदान-प्रदान आवश्यक हो सकता है।",
                "Choose a plant as an example. Record what enters, what leaves and what internal processes occur.",
                "उदाहरण के लिए एक पौधे को चुनें। लिखें कि उसमें क्या प्रवेश करता है, क्या बाहर निकलता है और उसके अंदर कौन-सी प्रक्रियाएँ होती हैं।"
            ),
            t(
                "Refresh Process", "रिफ्रेश प्रक्रिया",
                "A refresh process describes ongoing updates or changes in a system's components, states or relationships over time.",
                "रिफ्रेश प्रक्रिया किसी सिस्टम के हिस्सों, स्थितियों या उनके संबंधों में समय के साथ होने वाले लगातार बदलाव और अपडेट को दर्शाती है।",
                "Repeated updating may contribute to the maintenance of a system's organization.",
                "बार-बार होने वाले अपडेट किसी सिस्टम के संगठन को बनाए रखने में योगदान दे सकते हैं।",
                "Create a simple system model. Record its initial state, apply defined updates and note which properties remain unchanged.",
                "एक साधारण सिस्टम मॉडल बनाएँ। उसकी शुरुआती स्थिति लिखें, उसमें निश्चित बदलाव करें और देखें कि कौन-से गुण पहले जैसे बने रहते हैं।"
            ),
            t(
                "Dynamic Stability", "डायनामिक स्थिरता",
                "Dynamic stability means a system can undergo changes while maintaining important properties or functions within a defined range.",
                "डायनामिक स्थिरता का अर्थ है कि बदलाव होते रहने के बावजूद सिस्टम अपने महत्वपूर्ण गुणों या कार्यों को एक निर्धारित सीमा में बनाए रखे।",
                "A system may remain stable through continuous changes rather than by remaining completely unchanged.",
                "कोई सिस्टम बिल्कुल अपरिवर्तित रहने के बजाय लगातार बदलावों के बीच भी स्थिर रह सकता है।",
                "Use a simple simulation or spreadsheet. Change one input at a time and record whether a selected property remains within its defined range.",
                "एक साधारण सिमुलेशन या स्प्रेडशीट का उपयोग करें। एक समय में एक इनपुट बदलें और देखें कि चुना गया गुण अपनी निर्धारित सीमा में बना रहता है या नहीं।"
            ),
            t(
                "Information Retention", "जानकारी को बनाए रखना",
                "Information retention refers to the persistence of information or traces in a system over time.",
                "Information Retention का अर्थ है किसी सिस्टम में जानकारी या उसके निशानों का समय के साथ बने रहना।",
                "Information retention may be one measurable part of a system's stability.",
                "जानकारी को बनाए रखना किसी सिस्टम की स्थिरता का एक मापने योग्य हिस्सा हो सकता है।",
                "Store a defined set of information in a test system. Reopen it later and compare the stored information with the original.",
                "एक टेस्ट सिस्टम में कुछ निश्चित जानकारी सेव करें। बाद में उसे दोबारा खोलें और सेव की गई जानकारी की मूल जानकारी से तुलना करें।"
            )
        )
    ),

    ResearchDepartment(
        "Memory & Consciousness Research",
        "याददाश्त और चेतना का अध्ययन",
        "Explore memory, continuity, self-recognition and the role of information.",
        "स्मृति, निरंतरता, स्वयं की पहचान और जानकारी की भूमिका का अध्ययन करें।",
        listOf(
            t(
                "Memory Formation", "स्मृति का निर्माण",
                "Memory formation involves processes through which information is encoded, stored and later retrieved.",
                "स्मृति निर्माण में जानकारी को ग्रहण करना, सुरक्षित रखना और बाद में दोबारा प्राप्त करना शामिल है।",
                "Information retained over time may influence the later state of a system.",
                "समय के साथ सुरक्षित रहने वाली जानकारी किसी सिस्टम की बाद की स्थिति को प्रभावित कर सकती है।",
                "Record a small set of neutral facts. Test recall after different time intervals and note which facts remain available.",
                "कुछ सामान्य जानकारियाँ लिखें। अलग-अलग समय के अंतराल के बाद उन्हें याद करने की कोशिश करें और लिखें कि कौन-सी जानकारी याद रहती है।"
            ),
            t(
                "Memory Refresh", "स्मृति का रिफ्रेश",
                "Memory can be retrieved, updated and sometimes changed when new information is incorporated.",
                "स्मृति को दोबारा याद किया जा सकता है, अपडेट किया जा सकता है और नई जानकारी जुड़ने पर उसमें बदलाव भी हो सकता है।",
                "Repeated retrieval and updating may affect how information is retained.",
                "बार-बार जानकारी याद करना और अपडेट करना इस बात को प्रभावित कर सकता है कि जानकारी किस प्रकार सुरक्षित रहती है।",
                "Record a short description. Review it after a delay, add new details and compare both versions.",
                "एक छोटा विवरण लिखें। कुछ समय बाद उसे दोबारा पढ़ें, नई जानकारी जोड़ें और पुराने तथा नए विवरण की तुलना करें।"
            ),
            t(
                "I-Feeling", "मैं होने का एहसास",
                "The sense of self is studied through self-awareness, body representation, memory and the feeling of being an individual.",
                "स्वयं का एहसास आत्म-जागरूकता, शरीर की पहचान, स्मृति और एक अलग व्यक्ति होने की अनुभूति के माध्यम से समझा जाता है।",
                "The sense of self may depend on several connected processes rather than one isolated memory.",
                "स्वयं का एहसास केवल एक अलग स्मृति पर नहीं, बल्कि कई आपस में जुड़ी प्रक्रियाओं पर निर्भर हो सकता है।",
                "Review published research on self-awareness. Separate measurable behavior from subjective reports.",
                "आत्म-जागरूकता पर प्रकाशित शोध पढ़ें। मापे जा सकने वाले व्यवहार और व्यक्ति द्वारा बताए गए निजी अनुभवों को अलग-अलग दर्ज करें।"
            ),
            t(
                "Continuity of Self", "स्वयं की निरंतरता",
                "People often experience themselves as the same individual across time, despite changes in their body, knowledge and memories.",
                "शरीर, ज्ञान और यादों में बदलाव होने के बावजूद व्यक्ति समय के साथ स्वयं को एक ही व्यक्ति के रूप में अनुभव कर सकता है।",
                "Continuity of self may involve relationships between memory, body and ongoing processes.",
                "स्वयं की निरंतरता में स्मृति, शरीर और लगातार चलने वाली प्रक्रियाओं के बीच संबंध शामिल हो सकता है।",
                "Maintain a dated learning log. Compare changes in beliefs and memories while identifying what feels continuous.",
                "तारीख के साथ अपनी सीख और विचार लिखें। समय के साथ विचारों और यादों में हुए बदलावों की तुलना करें और देखें कि क्या निरंतर बना रहता है।"
            )
        )
    ),

    ResearchDepartment(
        "Theory Evaluation",
        "थ्योरी का वैज्ञानिक मूल्यांकन",
        "Evaluate Refresh Theory through evidence, testable predictions and possible revisions.",
        "प्रमाणों, जाँची जा सकने वाली भविष्यवाणियों और संभावित सुधारों के माध्यम से Refresh Theory का मूल्यांकन करें।",
        listOf(
            t(
                "Supported Claims", "समर्थित दावे",
                "A claim is supported when reliable evidence is consistent with its predictions. Support does not automatically prove a complete theory.",
                "किसी दावे को तब समर्थन मिलता है जब विश्वसनीय प्रमाण उसकी भविष्यवाणी से मेल खाते हैं। इससे पूरी थ्योरी अपने आप सिद्ध नहीं हो जाती।",
                "Some parts of Refresh Theory may match established findings about open systems and biological regulation.",
                "Refresh Theory के कुछ विचार ओपन सिस्टम और जैविक नियंत्रण से जुड़े स्थापित वैज्ञानिक निष्कर्षों से मेल खा सकते हैं।",
                "Choose one specific claim. Find reliable scientific sources and record evidence, limitations and alternative explanations.",
                "एक निश्चित दावा चुनें। विश्वसनीय वैज्ञानिक स्रोत खोजें और प्रमाण, सीमाएँ तथा दूसरी संभावित व्याख्याएँ लिखें।"
            ),
            t(
                "Contradicted Claims", "विरोधी प्रमाण",
                "A scientific model must be open to evidence that conflicts with its predictions. Contradictions can reveal weaknesses or necessary revisions.",
                "वैज्ञानिक मॉडल को ऐसे प्रमाणों के लिए खुला रहना चाहिए जो उसकी भविष्यवाणियों से मेल न खाते हों। विरोधी प्रमाण कमजोरियाँ या जरूरी सुधार दिखा सकते हैं।",
                "Testing possible contradictions may help identify where Refresh Theory needs revision.",
                "संभावित विरोधी प्रमाणों की जाँच से पता चल सकता है कि Refresh Theory में कहाँ सुधार की आवश्यकता है।",
                "Write one clear prediction. Search for observations that could contradict it. Record conflicting evidence.",
                "एक स्पष्ट भविष्यवाणी लिखें। ऐसे अवलोकन खोजें जो उसका विरोध कर सकते हैं। विरोधी प्रमाणों को भी दर्ज करें।"
            ),
            t(
                "Open Questions", "अनसुलझे सवाल",
                "Open questions are important issues for which current explanations or evidence are incomplete.",
                "अनसुलझे सवाल ऐसे महत्वपूर्ण विषय होते हैं जिनके लिए अभी उपलब्ध प्रमाण या वैज्ञानिक व्याख्याएँ अधूरी हैं।",
                "Identifying unanswered questions can help convert a broad theory into testable research questions.",
                "अनुत्तरित सवालों की पहचान करने से किसी व्यापक थ्योरी को जाँचे जा सकने वाले शोध प्रश्नों में बदला जा सकता है।",
                "Write one unanswered question. Define what evidence could help answer it and what result would remain inconclusive.",
                "एक अनसुलझा सवाल लिखें। तय करें कि उसका उत्तर पाने के लिए कौन-से प्रमाण चाहिए और कौन-सा परिणाम अनिर्णायक रहेगा।"
            ),
            t(
                "Theory Revision", "थ्योरी में सुधार",
                "Scientific theories and models can be refined when new evidence, clearer definitions or better experiments become available.",
                "नए प्रमाण, स्पष्ट परिभाषाएँ या बेहतर प्रयोग उपलब्ध होने पर वैज्ञानिक थ्योरी और मॉडल में सुधार किया जा सकता है।",
                "Refresh Theory may need refinement as its claims are tested.",
                "Refresh Theory के दावों की जाँच होने के साथ इसमें सुधार की आवश्यकता हो सकती है।",
                "Review an earlier research record. Identify unclear terms, missing controls and alternative explanations. Write a revised hypothesis.",
                "पहले के किसी शोध रिकॉर्ड को देखें। अस्पष्ट शब्द, छूटे हुए नियंत्रण और दूसरी संभावित व्याख्याएँ पहचानें। फिर संशोधित परिकल्पना लिखें।"
            )
        )
    ),

    ResearchDepartment(
        "Robot System Experiment",
        "रोबोट सिस्टम प्रयोग",
        "Compare information processing and continuity in artificial and biological systems.",
        "कृत्रिम और जैविक सिस्टम में जानकारी संसाधित करने तथा निरंतरता की तुलना करें।",
        listOf(
            t(
                "Human System", "मानव सिस्टम",
                "A human is a living biological system involving metabolism, nervous activity, sensory processing and regulation.",
                "मनुष्य एक जीवित जैविक सिस्टम है जिसमें मेटाबॉलिज्म, तंत्रिका गतिविधि, इंद्रियों से जानकारी लेना और आंतरिक नियंत्रण शामिल है।",
                "Human system organization may provide useful questions for comparing biological and artificial systems.",
                "मानव सिस्टम का संगठन जैविक और कृत्रिम सिस्टम की तुलना करने के लिए उपयोगी सवाल दे सकता है।",
                "Create a diagram of human system inputs, internal processes and outputs using published information.",
                "प्रकाशित वैज्ञानिक जानकारी के आधार पर मानव सिस्टम के इनपुट, आंतरिक प्रक्रियाओं और आउटपुट का एक चित्र बनाएँ।"
            ),
            t(
                "Robot System", "रोबोट सिस्टम",
                "Robots use sensors, software, processors and actuators to receive information and produce actions.",
                "रोबोट जानकारी प्राप्त करने और उसके अनुसार कार्य करने के लिए सेंसर, सॉफ्टवेयर, प्रोसेसर और मोटर जैसी व्यवस्थाओं का उपयोग करते हैं।",
                "A robot can retain and update information, but this alone does not establish subjective experience.",
                "रोबोट जानकारी को सुरक्षित और अपडेट कर सकता है, लेकिन केवल इससे उसके अंदर निजी अनुभव या चेतना सिद्ध नहीं होती।",
                "Use a simple software simulation. Record its inputs, stored data, updates and outputs.",
                "एक साधारण सॉफ्टवेयर सिमुलेशन का उपयोग करें। उसके इनपुट, सेव किए गए डेटा, अपडेट और आउटपुट दर्ज करें।"
            ),
            t(
                "System Separation", "सिस्टम का अलग होना",
                "Two systems may have similar information while remaining physically and operationally separate systems.",
                "दो सिस्टम में समान जानकारी हो सकती है, फिर भी वे भौतिक और कार्यात्मक रूप से अलग सिस्टम रह सकते हैं।",
                "Copying information from one system to another may not establish continuity of the original system.",
                "एक सिस्टम से दूसरे सिस्टम में जानकारी कॉपी करने से मूल सिस्टम की निरंतरता अपने आप सिद्ध नहीं होती।",
                "Create two independent test records with identical starting data. Update them separately and document how their states diverge.",
                "एक जैसे शुरुआती डेटा वाले दो अलग टेस्ट रिकॉर्ड बनाएँ। दोनों को अलग-अलग अपडेट करें और दर्ज करें कि उनकी स्थितियाँ कैसे अलग होती हैं।"
            ),
            t(
                "Continuity Hypothesis", "निरंतरता की परिकल्पना",
                "Continuity can refer to persistence of organization, information or operation. These meanings should be measured separately.",
                "निरंतरता का अर्थ संगठन, जानकारी या संचालन का बने रहना हो सकता है। इन सभी अर्थों को अलग-अलग मापना चाहिए।",
                "The proposed hypothesis is that some form of system continuity may be relevant to consciousness. This remains unproven.",
                "प्रस्तावित परिकल्पना यह है कि सिस्टम की किसी प्रकार की निरंतरता चेतना से संबंधित हो सकती है। यह अभी सिद्ध नहीं है।",
                "Design a computer simulation that measures information persistence and organizational continuity. Do not treat its result as proof of consciousness.",
                "एक ऐसा कंप्यूटर सिमुलेशन बनाएँ जो जानकारी के बने रहने और संगठन की निरंतरता को मापे। इसके परिणाम को चेतना का प्रमाण न मानें।"
            )
        )
    )
)

@Composable
fun ResearchHomeScreen() {
    var departmentIndex by remember { mutableIntStateOf(-1) }
    var topicIndex by remember { mutableIntStateOf(-1) }

    when {
        departmentIndex >= 0 && topicIndex >= 0 -> {
            TopicScreen(
                department = departments[departmentIndex],
                topic = departments[departmentIndex].topics[topicIndex],
                storageKey = "${departmentIndex}_${topicIndex}",
                onBack = { topicIndex = -1 }
            )
        }

        departmentIndex >= 0 -> {
            DepartmentScreen(
                department = departments[departmentIndex],
                onBack = { departmentIndex = -1 },
                onTopicClick = { topicIndex = it }
            )
        }

        else -> {
            HomeScreen(
                onDepartmentClick = { departmentIndex = it }
            )
        }
    }
}

@Composable
private fun HomeScreen(onDepartmentClick: (Int) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F7FB))
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
            color = Color(0xFF55718A)
        )

        Spacer(Modifier.height(15.dp))

        Text(
            "Explore. Experiment. Evaluate.",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF14283E)
        )
        Text(
            "शोध करें, प्रयोग करें और प्रमाणों के आधार पर मूल्यांकन करें।",
            color = Color.DarkGray,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(22.dp))

        departments.forEachIndexed { index, department ->
            val colors = listOf(
                Color(0xFFE5F3FF),
                Color(0xFFE8F7F0),
                Color(0xFFF2ECFF),
                Color(0xFFFFF1E5),
                Color(0xFFFFEDEF)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 13.dp)
                    .clickable { onDepartmentClick(index) },
                colors = CardDefaults.cardColors(
                    containerColor = colors[index]
                ),
                shape = RoundedCornerShape(16.dp)
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
                        color = Color(0xFF526B80),
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "4 Research Topics  •  Open →",
                        color = Color(0xFF2678A5),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Text(
            "Research note: Experimental observations are not proof of consciousness.",
            fontSize = 12.sp,
            color = Color.DarkGray
        )
        Text(
            "शोध में प्राप्त अवलोकन अपने आप चेतना का प्रमाण नहीं होते।",
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
        Modifier
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

        Text(department.descriptionEn, color = Color.White, fontSize = 14.sp)
        Spacer(Modifier.height(7.dp))
        Text(department.descriptionHi, color = SoftText, fontSize = 14.sp)

        Spacer(Modifier.height(22.dp))

        Text(
            "RESEARCH TOPICS / शोध के विषय",
            color = Accent,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        department.topics.forEachIndexed { index, item ->
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
                        item.title,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        item.hindi,
                        color = SoftText,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Open Research Topic → / विषय खोलें →",
                        color = Accent,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TopicScreen(
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

    var history by remember(storageKey) {
        mutableStateOf(prefs.getString(historyKey, "") ?: "")
    }

    var savedMessage by remember(storageKey) {
        mutableStateOf("")
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Navy)
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        Text(
            "← Back to Topics / वापस जाएँ",
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
        Text(topic.hindi, color = SoftText, fontSize = 17.sp)

        ResearchSection(
            "1. INFORMATION",
            "जानकारी"
        ) {
            BilingualText(topic.infoEn, topic.infoHi)
        }

        ResearchSection(
            "2. RESEARCH HYPOTHESIS",
            "शोध की परिकल्पना"
        ) {
            BilingualText(
                topic.hypothesisEn,
                topic.hypothesisHi
            )

            Spacer(Modifier.height(10.dp))

            Text(
                "Proposed hypothesis — not an established scientific fact.",
                color = Accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "प्रस्तावित परिकल्पना — यह अभी स्थापित वैज्ञानिक तथ्य नहीं है।",
                color = SoftText,
                fontSize = 12.sp
            )
        }

        ResearchSection(
            "3. EXPERIMENT PROTOCOL",
            "प्रयोग की प्रक्रिया"
        ) {
            BilingualText(
                topic.experimentEn,
                topic.experimentHi
            )
        }

        ResearchSection(
            "4. OBSERVATION",
            "अवलोकन और डेटा दर्ज करें"
        ) {
            Text(
                "Record what you observed, including conditions and unexpected results.",
                color = Color.White,
                fontSize = 14.sp
            )
            Text(
                "अपने अवलोकन लिखें। परिस्थितियों और किसी अप्रत्याशित परिणाम को भी दर्ज करें।",
                color = SoftText,
                fontSize = 14.sp
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = observation,
                onValueChange = {
                    observation = it
                    savedMessage = ""
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                label = {
                    Text("Observation / अपना अवलोकन")
                },
                placeholder = {
                    Text("Write your research data / अपना डेटा लिखें")
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

        ResearchSection(
            "5. RESULT EVALUATION",
            "परिणाम का मूल्यांकन"
        ) {
            Text(
                "Select how your observation relates to the hypothesis.",
                color = Color.White,
                fontSize = 14.sp
            )
            Text(
                "चुनें कि आपका अवलोकन परिकल्पना से किस प्रकार संबंधित है।",
                color = SoftText,
                fontSize = 14.sp
            )

            Spacer(Modifier.height(10.dp))

            val options = listOf(
                Triple(
                    "Supports",
                    "परिकल्पना का समर्थन करता है",
                    "Supports"
                ),
                Triple(
                    "Does Not Support",
                    "परिकल्पना का समर्थन नहीं करता",
                    "Does Not Support"
                ),
                Triple(
                    "Inconclusive",
                    "परिणाम अनिर्णायक है",
                    "Inconclusive"
                )
            )

            options.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            evaluation = option.third
                        }
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = evaluation == option.third,
                        onClick = {
                            evaluation = option.third
                        },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Accent,
                            unselectedColor = SoftText
                        )
                    )

                    Column {
                        Text(option.first, color = Color.White)
                        Text(option.second, color = SoftText, fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    val stamp = SimpleDateFormat(
                        "yyyy-MM-dd HH:mm",
                        Locale.getDefault()
                    ).format(Date())

                    val resultHindi = when (evaluation) {
                        "Supports" -> "परिकल्पना का समर्थन करता है"
                        "Does Not Support" -> "परिकल्पना का समर्थन नहीं करता"
                        else -> "परिणाम अनिर्णायक है"
                    }

                    val entry = """
                        [$stamp]
                        Result: $evaluation
                        परिणाम: $resultHindi

                        Observation:
                        $observation
                    """.trimIndent()

                    val updated = if (history.isBlank()) {
                        entry
                    } else {
                        "$entry\n\n--------------------\n\n$history"
                    }

                    prefs.edit()
                        .putString(observationKey, observation)
                        .putString(evaluationKey, evaluation)
                        .putString(historyKey, updated)
                        .apply()

                    history = updated
                    savedMessage =
                        "Saved successfully / डेटा सफलतापूर्वक सेव हुआ।"
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = Navy
                )
            ) {
                Text(
                    "SAVE RESEARCH RECORD / रिकॉर्ड सेव करें",
                    fontWeight = FontWeight.Bold
                )
            }

            if (savedMessage.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    savedMessage,
                    color = Color(0xFF7DE6A2),
                    fontSize = 13.sp
                )
            }
        }

        if (history.isNotBlank()) {
            ResearchSection(
                "6. SAVED RESEARCH HISTORY",
                "पहले सेव किए गए शोध रिकॉर्ड"
            ) {
                Text(history, color = SoftText, fontSize = 13.sp)
            }
        }

        ResearchSection(
            "SCIENTIFIC CAUTION",
            "वैज्ञानिक सावधानी"
        ) {
            BilingualText(
                "An observation or simulation result does not independently prove or disprove consciousness. Results must be interpreted within the limits of the experiment.",
                "किसी अवलोकन या सिमुलेशन का परिणाम अपने आप चेतना को सिद्ध या गलत साबित नहीं करता। परिणामों की व्याख्या प्रयोग की सीमाओं को ध्यान में रखकर करनी चाहिए।"
            )
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun BilingualText(
    english: String,
    hindi: String
) {
    Text(
        english,
        color = Color.White,
        fontSize = 14.sp,
        lineHeight = 21.sp
    )

    Spacer(Modifier.height(10.dp))

    Text(
        hindi,
        color = SoftText,
        fontSize = 14.sp,
        lineHeight = 22.sp
    )
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
            .padding(top = 15.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardDark
        ),
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
