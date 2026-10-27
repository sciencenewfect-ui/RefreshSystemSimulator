package com.refreshsystem.simulator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Night = Color(0xFF06101F)
private val Panel = Color(0xFF101F33)
private val Cyan = Color(0xFF64E8FF)
private val Mint = Color(0xFF65E6C4)

class MainActivity : ComponentActivity() {
override fun onCreate(savedInstanceState: Bundle?) {
super.onCreate(savedInstanceState)
setContent {
MaterialTheme(colorScheme = darkColorScheme(primary = Cyan, background = Night, surface = Panel)) {
RefreshApp()
}
}
}
}

private data class Lesson(val title: String, val subtitle: String, val explanation: String, val stage: Int)

private val lessons = listOf(
Lesson("Nature creates a system", "प्रकृति में शुरुआत", "ऊर्जा, पानी, खनिज और अणुओं की परस्पर क्रियाएँ जटिल संरचनाएँ बनने की संभावनाएँ पैदा कर सकती हैं। जीवन की उत्पत्ति की पूरी प्रक्रिया अभी शोध का विषय है।", 0),
Lesson("Open system", "आदान-प्रदान", "Open System अपने वातावरण से ऊर्जा और/या पदार्थ का आदान-प्रदान करता है। इस उदाहरण में कण अंदर आते-जाते हैं और अंदर की प्रक्रियाएँ जारी रहती हैं।", 1),
Lesson("Continuous refresh", "लगातार बदलाव", "सिस्टम के भीतर प्रतिक्रियाएँ और पदार्थों का प्रवाह लगातार चल सकता है। घटक बदलने पर भी व्यापक संगठन कुछ समय तक बना रह सकता है।", 2),
Lesson("Stability & connections", "संगठन का बने रहना", "यहाँ Stability का अर्थ है कि बदलाव के बीच सिस्टम का व्यापक पैटर्न बना रहता है। समय के साथ कुछ कनेक्शन बदल सकते हैं।", 3),
Lesson("Information & memory", "तुम्हारे मॉडल का विचार", "Refresh Theory में कनेक्शनों के पैटर्न को जानकारी बनाए रखने की संभावित नींव माना जाता है। यह विचार है, स्थापित वैज्ञानिक निष्कर्ष नहीं।", 4),
Lesson("Consciousness model", "चेतना से संभावित संबंध", "तुम्हारी परिकल्पना में Stability को चेतना समझने की शुरुआत माना गया है। Stability से चेतना उत्पन्न होती है या नहीं, यह अभी वैज्ञानिक रूप से सिद्ध नहीं है।", 5)
)

@Composable
private fun RefreshApp() {
var selected by remember { mutableIntStateOf(0) }
var refreshCount by remember { mutableIntStateOf(0) }
var running by remember { mutableStateOf(true) }
var longTime by remember { mutableStateOf(false) }
val lesson = lessons[selected]

Column(  
    modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0B1A2E), Night))).verticalScroll(rememberScrollState()).padding(18.dp),  
    verticalArrangement = Arrangement.spacedBy(14.dp)  
) {  
    Text("REFRESH SYSTEM", color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)  
    Text("Explore how nature\norganizes systems.", color = Color.White, fontSize = 29.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)  
    Text("प्रकृति → Open System → Refresh → Stability → Memory", color = Color(0xFFB8C8DA), fontSize = 13.sp)  

    Box(modifier = Modifier.fillMaxWidth().height(230.dp).background(Panel, RoundedCornerShape(22.dp)).border(1.dp, Color(0xFF24445F), RoundedCornerShape(22.dp)).padding(8.dp)) {  
        NetworkCanvas(stage = selected, running = running, longTime = longTime, modifier = Modifier.fillMaxSize())  
        Column(Modifier.align(Alignment.TopStart).padding(10.dp)) {  
            Text(if (running) "SIMULATION RUNNING" else "SIMULATION PAUSED", color = if (running) Mint else Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)  
            Text(if (longTime) "LONG TIMESCALE" else "SHORT TIMESCALE", color = Color(0xFF8FAFC9), fontSize = 9.sp)  
        }  
    }  

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {  
        MetricCard("Refresh cycles", refreshCount.toString(), Modifier.weight(1f))  
        MetricCard("System phase", "${selected + 1}/6", Modifier.weight(1f))  
    }  

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {  
        Button(onClick = { running = !running }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF163B55))) {  
            Text(if (running) "Pause" else "Resume")  
        }  
        Button(onClick = { refreshCount++ }, modifier = Modifier.weight(1f)) { Text("Refresh +1") }  
    }  
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {  
        Text("Time scale", color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))  
        Text(if (longTime) "Long" else "Short", color = Cyan, modifier = Modifier.clickable { longTime = !longTime }.padding(8.dp))  
        Text("⇄", color = Cyan, modifier = Modifier.clickable { longTime = !longTime }.padding(8.dp))  
    }  

    Text("LEARN STEP BY STEP", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)  
    lessons.forEachIndexed { index, item ->  
        Row(  
            modifier = Modifier.fillMaxWidth().background(if (selected == index) Color(0xFF18344B) else Panel, RoundedCornerShape(15.dp))  
                .border(1.dp, if (selected == index) Color(0xFF3C9FBD) else Color(0xFF1A3047), RoundedCornerShape(15.dp))  
                .clickable { selected = index }.padding(14.dp),  
            verticalAlignment = Alignment.CenterVertically  
        ) {  
            Box(Modifier.size(34.dp).background(if (selected == index) Cyan else Color(0xFF20364B), RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {  
                Text("${index + 1}", color = if (selected == index) Night else Color.White, fontWeight = FontWeight.Bold)  
            }  
            Spacer(Modifier.width(12.dp))  
            Column(Modifier.weight(1f)) {  
                Text(item.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)  
                Text(item.subtitle, color = Color(0xFF9DB2C8), fontSize = 12.sp)  
            }  
            Text("›", color = Cyan, fontSize = 25.sp)  
        }  
    }  

    Column(Modifier.fillMaxWidth().background(Color(0xFF101D2D), RoundedCornerShape(18.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {  
        Text("${selected + 1}. ${lesson.title}", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 17.sp)  
        Text(lesson.explanation, color = Color(0xFFD5E0EC), fontSize = 14.sp, lineHeight = 21.sp)  
        Text("NOTE: Refresh Theory is a proposed model. This simulation illustrates ideas and is not proof of consciousness.", color = Color(0xFF91A5B9), fontSize = 11.sp, lineHeight = 16.sp)  
    }  
    Spacer(Modifier.height(12.dp))  
}

}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
Column(modifier.background(Panel, RoundedCornerShape(16.dp)).border(1.dp, Color(0xFF1C3A54), RoundedCornerShape(16.dp)).padding(15.dp)) {
Text(label, color = Color(0xFF9DB2C8), fontSize = 11.sp)
Spacer(Modifier.height(5.dp))
Text(value, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
}
}

@Composable
private fun NetworkCanvas(stage: Int, running: Boolean, longTime: Boolean, modifier: Modifier = Modifier) {
val transition = rememberInfiniteTransition(label = "network")
val pulse by transition.animateFloat(initialValue = 0.25f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1700, easing = LinearEasing), RepeatMode.Reverse), label = "pulse")
Canvas(modifier) {
val w = size.width; val h = size.height
val points = listOf(
Offset(w*.12f,h*.30f), Offset(w*.28f,h*.18f), Offset(w*.45f,h*.35f), Offset(w*.66f,h*.19f), Offset(w*.84f,h*.34f),
Offset(w*.19f,h*.66f), Offset(w*.39f,h*.77f), Offset(w*.59f,h*.63f), Offset(w*.78f,h*.77f), Offset(w*.91f,h*.59f), Offset(w*.51f,h*.48f)
)
val edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 0 to 5, 5 to 6, 6 to 7, 7 to 8, 8 to 9, 2 to 10, 10 to 7, 1 to 6, 3 to 8, 5 to 2, 7 to 9)
edges.forEachIndexed { i, e ->
val visible = stage == 0 && i > 6 || longTime && i % 4 == 0
drawLine(if (visible) Color(0xFF2D617A) else Color(0xFF269FC8).copy(alpha = if (running) .35f + pulse*.45f else .2f), points[e.first], points[e.second], strokeWidth = if (visible) 1.3f else 2f, cap = StrokeCap.Round)
}
points.forEachIndexed { i, p ->
val radius = if (running) 4f + pulse2.2f else 4f
drawCircle(Color(0xFF56E2FF).copy(alpha = .15f), radius = radius2.6f, center = p)
drawCircle(if (i % 3 == 0) Mint else Cyan, radius = radius, center = p)
}
}
}
