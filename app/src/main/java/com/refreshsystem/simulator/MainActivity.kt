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
            "REFRESH PROCESS" to "A system updates over time. Its parts can change while its overall organization continues.",
            "OPEN SYSTEM" to "An open system exchanges energy, matter, or signals with its environment.",
            "STABILITY" to "In this model, stability describes how much organization and function remain during change.",
            "INFORMATION RETENTION" to "Recorded information can be retrieved later and may influence current decisions.",
            "IDENTITY & MEMORY" to "Memory can influence behavior; this alone does not prove subjective experience.",
            "RESEARCH QUESTION" to "Could stability and information retention help explain consciousness? This hypothesis needs testable experiments."
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
            val event = if (Random.nextBoolean()) "A connection weakened; the system can form a new link." else "A new connection formed between two components."
            events.add(0, "Refresh $tick: $event")
            while (events.size > 40) events.removeAt(events.lastIndex)
            val connectivity = .58f + Random.nextFloat() * .25f
            stability = (connectivity * .55f + (1f - kotlin.math.abs(internalState - .5f)) * .45f).coerceIn(.05f, 1f)
        }
    }

    Column(Modifier.fillMaxSize().background(Ink).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("REFRESH THEORY", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color.White)
        Text("A changing open system • experimental model", color = Muted)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("Robot", "Experiment", "Learn").forEachIndexed { i, label ->
                FilterChip(selected = tab == i, onClick = { tab = i }, label = { Text(label) })
            }
        }
        Spacer(Modifier.height(14.dp))
        when (tab) {
            0 -> {
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Box 1 • Digital Robot", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        RobotFigure(photoBitmap = photoBitmap, stability = stability, tick = tick)
                        Button(onClick = { photoPicker.launch(arrayOf("image/*")) }) { Text("Customize Robot Face") }
                        OutlinedTextField(value = name, onValueChange = { name = it; prefs.edit().putString("user_name", it).apply() }, label = { Text("Your name (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        Text("This prototype stores memories and demonstrates a simple memory-based response. A selected photo appears as the robot face; it does not generate a new robotic face.", color = Muted, style = MaterialTheme.typography.bodySmall)
                        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Color.DarkGray)
                        Text("Personal Memory", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = memoryInput, onValueChange = { memoryInput = it }, label = { Text("Add a memory or personal fact") }, modifier = Modifier.fillMaxWidth())
                        Button(onClick = {
                            if (memoryInput.isNotBlank()) {
                                memories.add(0, memoryInput.trim())
                                prefs.edit().putString("memories", memories.joinToString("\n")).apply()
                                events.add(0, "Memory recorded: ${memoryInput.trim().take(45)}")
                                memoryInput = ""
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("Save Memory") }
                        if (memories.isNotEmpty()) {
                            Text("Stored Memories (${memories.size})", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                            memories.take(8).forEach { item -> Text("• $item", color = Muted, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) }
                        }
                        Button(onClick = {
                            val chosen = memories.firstOrNull()
                            events.add(0, if (chosen != null) "Decision model: consider stored memory '${chosen.take(45)}' before choosing an action." else "Decision model: no stored memory yet. Add information first.")
                        }, modifier = Modifier.fillMaxWidth()) { Text("Run Memory-Based Decision Example") }
                    }
                }
            }
            1 -> {
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Box 2 • Natural Open System", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Refresh $tick • Connections keep changing", color = Muted)
                        Spacer(Modifier.height(8.dp))
                        StabilityRing(stability)
                        Text("Stability (model indicator): ${(stability * 100).toInt()}%", fontWeight = FontWeight.Bold)
                        LinearProgressIndicator(progress = { stability }, modifier = Modifier.fillMaxWidth(), color = Red)
                        Text("Internal state: ${(internalState * 100).toInt()}%")
                        Text("External environment")
                        Slider(value = environment, onValueChange = { environment = it }, valueRange = 0f..1f)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { running = !running }) { Text(if (running) "Pause" else "Resume") }
                            OutlinedButton(onClick = { tick++; events.add(0, "Manual Refresh $tick: a new change was recorded.") }) { Text("Refresh Once") }
                        }
                        Text("Stability is calculated from this app’s simulation rules. It is not a measure or proof of consciousness.", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Recent Changes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                events.take(15).forEach { Text("• $it", color = Muted, modifier = Modifier.padding(vertical = 3.dp)) }
            }
            else -> {
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Box 3 • Learn Refresh Theory", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Educational slides advance automatically", color = Muted)
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
                                    Button(onClick = { activeLesson = (activeLesson + 1) % lessons.size }) { Text("Next Slide") }
                                }
                            }
                        }
                        Text("Note: This is a text-based auto-slide prototype. Video clips and a real ad network can be added in a later development stage.", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("Research hypothesis: ‘Consciousness is the Stability of an Open System.’ This app explores behavior, memory, and system changes; it does not prove consciousness.", color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun RobotFigure(photoBitmap: android.graphics.Bitmap?, stability: Float, tick: Int) {
    val pulseTransition = rememberInfiniteTransition(label = "robot-pulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0.94f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "robot-pulse-value"
    )
    Box(Modifier.fillMaxWidth().height(330.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val unit = size.width / 260f
            fun pt(x: Float, y: Float) = Offset(cx + x * unit, y * unit)
            // Soft red stability halo
            drawCircle(Red.copy(alpha = .07f), 112f * unit * pulse, pt(0f, 112f))
            drawCircle(Red.copy(alpha = .24f), 100f * unit * pulse, pt(0f, 112f), style = androidx.compose.ui.graphics.drawscope.Stroke(2.5f * unit))
            // Neck and shoulders
            drawRoundRect(Color(0xFF718197), topLeft = pt(-19f, 153f), size = androidx.compose.ui.geometry.Size(38f*unit, 30f*unit), cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f*unit))
            drawRoundRect(Color(0xFF536176), topLeft = pt(-70f, 176f), size = androidx.compose.ui.geometry.Size(140f*unit, 108f*unit), cornerRadius = androidx.compose.ui.geometry.CornerRadius(28f*unit))
            drawRoundRect(Color(0xFF9AA8BA), topLeft = pt(-48f, 188f), size = androidx.compose.ui.geometry.Size(96f*unit, 78f*unit), cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f*unit))
            drawRoundRect(Red.copy(alpha = .8f), topLeft = pt(-28f, 204f), size = androidx.compose.ui.geometry.Size(56f*unit, 5f*unit), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f*unit))
            // Arms and hands
            drawRoundRect(Color(0xFF718197), topLeft = pt(-96f, 184f), size = androidx.compose.ui.geometry.Size(23f*unit, 83f*unit), cornerRadius = androidx.compose.ui.geometry.CornerRadius(11f*unit))
            drawRoundRect(Color(0xFF718197), topLeft = pt(73f, 184f), size = androidx.compose.ui.geometry.Size(23f*unit, 83f*unit), cornerRadius = androidx.compose.ui.geometry.CornerRadius(11f*unit))
            drawCircle(Cyan, 7f*unit, pt(-84f, 271f)); drawCircle(Cyan, 7f*unit, pt(84f, 271f))
            // Head casing
            drawRoundRect(Color(0xFF9AA8BA), topLeft = pt(-57f, 42f), size = androidx.compose.ui.geometry.Size(114f*unit, 112f*unit), cornerRadius = androidx.compose.ui.geometry.CornerRadius(28f*unit))
            drawRoundRect(Color(0xFF283346), topLeft = pt(-48f, 51f), size = androidx.compose.ui.geometry.Size(96f*unit, 94f*unit), cornerRadius = androidx.compose.ui.geometry.CornerRadius(23f*unit))
            // Default robot face details remain visible around the customizable face area
            drawRoundRect(Red.copy(alpha = .9f), topLeft = pt(-32f, 83f), size = androidx.compose.ui.geometry.Size(18f*unit, 6f*unit), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f*unit))
            drawRoundRect(Red.copy(alpha = .9f), topLeft = pt(14f, 83f), size = androidx.compose.ui.geometry.Size(18f*unit, 6f*unit), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f*unit))
            drawLine(Cyan.copy(alpha = .8f), pt(-18f, 119f), pt(18f, 119f), 2f*unit)
            // Live connections between body and environment
            repeat(12) { i ->
                val a = (i * 2.0 * PI / 12.0) + (tick % 10) * .10
                val x = cos(a).toFloat() * 105f * unit
                val y = 112f * unit + sin(a).toFloat() * 90f * unit
                val end = Offset(cx + x, y)
                drawLine(if (i % 3 == 0) Cyan else Red.copy(alpha = .72f), pt(0f, 120f), end, 1.3f*unit)
                drawCircle(if (i % 3 == 0) Cyan else Color.White, 3f*unit, end)
            }
        }
        if (photoBitmap != null) {
            Image(
                bitmap = photoBitmap.asImageBitmap(),
                contentDescription = "Custom robot face photo",
                modifier = Modifier.padding(bottom = 155.dp).size(width = 68.dp, height = 68.dp).clip(RoundedCornerShape(20.dp))
            )
        }
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
