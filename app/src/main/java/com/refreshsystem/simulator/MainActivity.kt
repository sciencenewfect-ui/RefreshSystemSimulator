package com.refreshsystem.simulator

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val Ink = Color(0xFF10131C)
private val Panel = Color(0xFF1A2030)
private val Red = Color(0xFFFF3B45)
private val Cyan = Color(0xFF6BE7F2)
private val Muted = Color(0xFFB7C0D3)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                background = Ink, surface = Panel, primary = Red,
                onBackground = Color.White, onSurface = Color.White
            )) { RefreshTheoryApp() }
        }
    }
}

@Composable
private fun RefreshTheoryApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("refresh_theory", Context.MODE_PRIVATE) }
    var tab by remember { mutableIntStateOf(0) }
    var photoUri by remember { mutableStateOf(prefs.getString("photo_uri", null)?.let(Uri::parse)) }
    var photoBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var name by remember { mutableStateOf(prefs.getString("user_name", "") ?: "") }
    var memoryInput by remember { mutableStateOf("") }
    val memories = remember {
        mutableStateListOf<String>().apply {
            addAll((prefs.getString("memories", "") ?: "").split("\n").filter { it.isNotBlank() })
        }
    }
    val events = remember { mutableStateListOf<String>() }
    var tick by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(true) }
    var environment by remember { mutableFloatStateOf(.5f) }
    var internalState by remember { mutableFloatStateOf(.5f) }
    var stability by remember { mutableFloatStateOf(.72f) }
    var activeLesson by remember { mutableIntStateOf(0) }
    val lessons = remember {
        listOf(
            "REFRESH PROCESS" to "सिस्टम की स्थिति समय के साथ अपडेट होती है। बदलाव होना और संगठन बने रहना एक साथ संभव हैं।",
            "OPEN SYSTEM" to "Open System वातावरण से ऊर्जा, पदार्थ या संकेत ले सकता है और बाहर भेज सकता है।",
            "STABILITY" to "इस मॉडल में Stability का मतलब है—बदलाव के बावजूद जरूरी संगठन और कार्यक्षमता कितनी बनी रहती है।",
            "INFORMATION RETENTION" to "रिकॉर्ड की गई जानकारी बाद में खोजी जा सकती है और वर्तमान निर्णय को प्रभावित कर सकती है।",
            "IDENTITY & MEMORY" to "यादें व्यवहार को प्रभावित कर सकती हैं; इससे अपने-आप यह सिद्ध नहीं होता कि सिस्टम सचमुच अनुभव करता है।",
            "RESEARCH QUESTION" to "क्या Stability और जानकारी बनाए रखने के बीच ऐसा संबंध है जो चेतना को समझने में मदद करे? इसे प्रयोग से जाँचना होगा।"
        )
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) { }
            photoUri = uri
            prefs.edit().putString("photo_uri", uri.toString()).apply()
        }
    }

    LaunchedEffect(photoUri) {
        photoBitmap = null
        val uri = photoUri ?: return@LaunchedEffect
        try { context.contentResolver.openInputStream(uri)?.use { photoBitmap = BitmapFactory.decodeStream(it) } } catch (_: Exception) { }
    }

    LaunchedEffect(activeLesson) {
        delay(6000)
        activeLesson = (activeLesson + 1) % lessons.size
    }

    LaunchedEffect(running) {
        while (running) {
            delay(1100)
            tick++
            internalState = (internalState * .62f + environment * .23f + Random.nextFloat() * .15f).coerceIn(0f, 1f)
            val event = if (Random.nextBoolean()) "एक connection टूटा; सिस्टम में नया संबंध बनने की संभावना बनी।" else "दो हिस्सों के बीच नया connection बना।"
            events.add(0, "Refresh $tick: $event")
            while (events.size > 40) events.removeAt(events.lastIndex)
            val connectivity = .58f + Random.nextFloat() * .25f
            stability = (connectivity * .55f + (1f - kotlin.math.abs(internalState - .5f)) * .45f).coerceIn(.05f, 1f)
        }
    }

    Column(Modifier.fillMaxSize().background(Ink).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("REFRESH THEORY", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color.White)
        Text("एक बदलता हुआ Open System • प्रयोगात्मक मॉडल", color = Muted)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("रोबोट", "प्रयोग", "सीखें").forEachIndexed { i, label ->
                FilterChip(selected = tab == i, onClick = { tab = i }, label = { Text(label) })
            }
        }
        Spacer(Modifier.height(14.dp))
        when (tab) {
            0 -> {
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("बॉक्स 1 • मेरा डिजिटल रोबोट", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(190.dp)) {
                            Canvas(Modifier.fillMaxSize()) {
                                drawCircle(Red.copy(alpha = .10f), size.minDimension * .48f)
                                drawCircle(Red.copy(alpha = .38f), size.minDimension * .42f, style = androidx.compose.ui.graphics.drawscope.Stroke(4.dp.toPx()))
                            }
                            if (photoBitmap != null) {
                                Image(photoBitmap!!.asImageBitmap(), contentDescription = "आपकी चुनी हुई फोटो", modifier = Modifier.size(142.dp).clip(CircleShape))
                            } else {
                                Box(Modifier.size(142.dp).clip(CircleShape).background(Color(0xFF30394C)), contentAlignment = Alignment.Center) {
                                    Text("अपनी\nफोटो चुनें", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Button(onClick = { photoPicker.launch(arrayOf("image/*")) }) { Text("अपनी फोटो चुनें") }
                        OutlinedTextField(value = name, onValueChange = { name = it; prefs.edit().putString("user_name", it).apply() }, label = { Text("आपका नाम (वैकल्पिक)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        Text("यह मॉडल आपकी दी हुई यादों से सीखने और निर्णय लेने का प्रदर्शन करता है। फोटो अभी केवल पहचान-चित्र है; यह अपने-आप रोबोट चेहरा नहीं बनाती।", color = Muted, style = MaterialTheme.typography.bodySmall)
                        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Color.DarkGray)
                        Text("यादें", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = memoryInput, onValueChange = { memoryInput = it }, label = { Text("अपने बारे में कोई बात लिखें") }, modifier = Modifier.fillMaxWidth())
                        Button(onClick = {
                            if (memoryInput.isNotBlank()) {
                                memories.add(0, memoryInput.trim())
                                prefs.edit().putString("memories", memories.joinToString("\n")).apply()
                                events.add(0, "नई याद रिकॉर्ड हुई: ${memoryInput.trim().take(45)}")
                                memoryInput = ""
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("याद सुरक्षित करें") }
                        if (memories.isNotEmpty()) {
                            Text("सिस्टम की यादें (${memories.size})", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                            memories.take(8).forEach { item -> Text("• $item", color = Muted, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) }
                        }
                        Button(onClick = {
                            val chosen = memories.firstOrNull()
                            events.add(0, if (chosen != null) "निर्णय मॉडल: उपलब्ध याद '${chosen.take(45)}' को ध्यान में रखकर अगला निर्णय चुनना होगा।" else "निर्णय मॉडल: अभी कोई याद नहीं; पहले जानकारी रिकॉर्ड करें।")
                        }, modifier = Modifier.fillMaxWidth()) { Text("यादों के आधार पर निर्णय का उदाहरण") }
                    }
                }
            }
            1 -> {
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("बॉक्स 2 • Natural Open System", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Refresh $tick • Connections बदलते रहते हैं", color = Muted)
                        Spacer(Modifier.height(8.dp))
                        StabilityRing(stability)
                        Text("Stability (मॉडल सूचक): ${(stability * 100).toInt()}%", fontWeight = FontWeight.Bold)
                        LinearProgressIndicator(progress = { stability }, modifier = Modifier.fillMaxWidth(), color = Red)
                        Text("आंतरिक स्थिति: ${(internalState * 100).toInt()}%")
                        Text("बाहरी वातावरण")
                        Slider(value = environment, onValueChange = { environment = it }, valueRange = 0f..1f)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { running = !running }) { Text(if (running) "रोकें" else "फिर चलाएँ") }
                            OutlinedButton(onClick = { tick++; events.add(0, "Manual Refresh $tick: एक नया बदलाव दर्ज हुआ।") }) { Text("एक Refresh") }
                        }
                        Text("Stability यहाँ ऐप के नियमों से निकला मॉडल सूचक है—चेतना का माप या प्रमाण नहीं।", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("हाल के बदलाव", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                events.take(15).forEach { Text("• $it", color = Muted, modifier = Modifier.padding(vertical = 3.dp)) }
            }
            else -> {
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("बॉक्स 3 • Refresh Theory सीखें", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("शैक्षिक स्लाइड अपने-आप बदलती रहेंगी", color = Muted)
                        Spacer(Modifier.height(14.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1822)), shape = RoundedCornerShape(18.dp)) {
                            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                                Text(lessons[activeLesson].first, color = Red, fontWeight = FontWeight.Black)
                                Spacer(Modifier.height(10.dp))
                                Text(lessons[activeLesson].second, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(18.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${activeLesson + 1} / ${lessons.size}", color = Muted)
                                    Spacer(Modifier.weight(1f))
                                    Button(onClick = { activeLesson = (activeLesson + 1) % lessons.size }) { Text("अगली स्लाइड") }
                                }
                            }
                        }
                        Text("नोट: यह टेक्स्ट-आधारित ऑटो-स्लाइड प्रोटोटाइप है। वीडियो क्लिप और असली विज्ञापन नेटवर्क अगले विकास चरण में जोड़े जा सकते हैं।", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("शोध-सिद्धांत: ‘चेतना Open System की Stability है’ हमारी परिकल्पना है। ऐप व्यवहार, याददाश्त और सिस्टम बदलावों को जाँचता है; यह अकेले चेतना सिद्ध नहीं करता।", color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun StabilityRing(stability: Float) {
    val transition = rememberInfiniteTransition(label = "ring")
    val pulse by transition.animateFloat(initialValue = .92f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "pulse")
    Canvas(Modifier.fillMaxWidth().height(240.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension * .34f * pulse
        drawCircle(Red.copy(alpha = .10f), radius * 1.35f, center)
        drawCircle(Red.copy(alpha = .18f), radius * 1.16f, center)
        drawCircle(Red.copy(alpha = .9f), radius, center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8.dp.toPx()))
        drawCircle(Cyan.copy(alpha = .85f), radius * .55f, center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
        repeat(18) { i ->
            val angle = (i * 2.0 * PI / 18.0) + (System.currentTimeMillis() % 4000L) / 4000.0 * 2.0 * PI
            val r = radius * (.35f + ((i * 17) % 60) / 100f)
            val point = Offset(center.x + cos(angle).toFloat() * r, center.y + sin(angle).toFloat() * r)
            drawCircle(if (i % 3 == 0) Color.White else Red, if (i % 3 == 0) 3.dp.toPx() else 2.dp.toPx(), point)
        }
    }
}
