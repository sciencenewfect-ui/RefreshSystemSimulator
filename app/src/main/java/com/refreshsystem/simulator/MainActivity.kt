
package com.refreshsystem.simulator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.sqrt
import kotlin.random.Random

private data class Node(
    val id: Int,
    val x: Float,
    val y: Float,
    val energy: Float = 0.5f
)

private data class Link(
    val a: Int,
    val b: Int,
    val strength: Float = 0.5f
)

private data class MemoryRecord(
    val id: Int,
    val text: String,
    val tick: Int
)

private data class HistoryEvent(
    val tick: Int,
    val message: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                RefreshApp()
            }
        }
    }
}

@Composable
private fun RefreshApp() {
    var running by remember { mutableStateOf(true) }
    var tick by remember { mutableIntStateOf(0) }
    var nextId by remember { mutableIntStateOf(20) }
    var environment by remember { mutableFloatStateOf(0.5f) }
    var internalEnvironment by remember { mutableFloatStateOf(0.5f) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var memoryText by remember { mutableStateOf("") }
    var stability by remember { mutableFloatStateOf(1f) }
    var continuity by remember { mutableFloatStateOf(1f) }

    val nodes = remember {
        mutableStateListOf<Node>().apply {
            repeat(12) { i ->
                val angle = i * 0.5236f
                add(
                    Node(
                        i,
                        0.5f + 0.35f * kotlin.math.cos(angle),
                        0.5f + 0.35f * kotlin.math.sin(angle)
                    )
                )
            }
        }
    }

    val links = remember {
        mutableStateListOf<Link>().apply {
            for (i in 0 until 12) {
                add(Link(i, (i + 1) % 12))
                if (i % 2 == 0) add(Link(i, (i + 3) % 12))
            }
        }
    }

    val memories = remember { mutableStateListOf<MemoryRecord>() }
    val history = remember { mutableStateListOf<HistoryEvent>() }

    fun log(message: String) {
        history.add(0, HistoryEvent(tick, message))
        while (history.size > 100) history.removeAt(history.lastIndex)
    }

    fun refreshSystem() {
        tick++

        // पुराने रिकॉर्ड और वर्तमान आंतरिक स्थिति अगले बदलावों को प्रभावित करते हैं।
        val memoryInfluence =
            (memories.size.coerceAtMost(10) / 10f) * 0.15f

        internalEnvironment = (
            internalEnvironment * 0.65f +
                environment * 0.20f +
                memoryInfluence +
                Random.nextFloat() * 0.08f
            ).coerceIn(0f, 1f)

        // कनेक्शन टूट सकते हैं।
        if (links.isNotEmpty() && Random.nextFloat() < 0.65f) {
            val index = Random.nextInt(links.size)
            val removed = links.removeAt(index)
            log("कनेक्शन ${removed.a}–${removed.b} टूट गया")
        }

        // आंतरिक वातावरण और इतिहास नए कनेक्शन बनने की संभावना को प्रभावित करते हैं।
        val joinChance = (
            0.25f + internalEnvironment * 0.35f +
                memoryInfluence
            ).coerceIn(0.1f, 0.8f)

        if (nodes.size >= 2 && Random.nextFloat() < joinChance) {
            val a = nodes.random().id
            val possible = nodes.filter {
                it.id != a && links.none { link ->
                    (link.a == a && link.b == it.id) ||
                        (link.a == it.id && link.b == a)
                }
            }

            if (possible.isNotEmpty()) {
                val b = possible.random().id
                links.add(
                    Link(
                        a,
                        b,
                        (0.3f + internalEnvironment * 0.5f)
                            .coerceIn(0f, 1f)
                    )
                )
                log("नया कनेक्शन $a–$b बना")
            }
        }

        // धीरे-धीरे संरचना भी बदलती है।
        if (tick % 8 == 0 && nodes.size < 30) {
            val angle = Random.nextFloat() * 6.28318f
            val id = nextId++
            nodes.add(
                Node(
                    id,
                    (0.5f + 0.35f * kotlin.math.cos(angle))
                        .coerceIn(0.05f, 0.95f),
                    (0.5f + 0.35f * kotlin.math.sin(angle))
                        .coerceIn(0.05f, 0.95f)
                )
            )
            log("सिस्टम में नया हिस्सा $id जुड़ा")
        }

        if (tick % 15 == 0 && nodes.size > 8) {
            val removed = nodes.removeAt(Random.nextInt(nodes.size))
            links.removeAll { it.a == removed.id || it.b == removed.id }
            log("हिस्सा ${removed.id} सिस्टम से अलग हुआ")
        }

        // ये मॉडल-आधारित अनुमान हैं, वास्तविक चेतना का माप नहीं।
        val possibleLinks = (nodes.size * (nodes.size - 1) / 2)
            .coerceAtLeast(1)
        val connectivity = (links.size.toFloat() / possibleLinks)
            .coerceIn(0f, 1f)

        val organization = (
            0.55f * connectivity.coerceAtMost(1f) +
                0.45f * (1f - kotlin.math.abs(
                    internalEnvironment - 0.5f
                ))
            ).coerceIn(0f, 1f)

        stability = organization

        continuity = (
            0.5f + memories.size.coerceAtMost(10) * 0.035f +
                (1f - kotlin.math.abs(
                    internalEnvironment - environment
                )) * 0.15f
            ).coerceIn(0f, 1f)
    }

    LaunchedEffect(running) {
        while (running) {
            delay(900)
            refreshSystem()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            "Refresh System",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            "बदलते हुए भी सिस्टम का बना रहना",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("System", "Memory", "Experiments", "Theory")
                .forEachIndexed { index, title ->
                    FilterChip(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        label = { Text(title) }
                    )
                }
        }

        Spacer(Modifier.height(12.dp))

        when (selectedTab) {
            0 -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { running = !running }) {
                        Text(if (running) "Pause" else "Resume")
                    }
                    OutlinedButton(onClick = { refreshSystem() }) {
                        Text("Refresh Now")
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text("Refresh: $tick")
                Text("हिस्से: ${nodes.size}   कनेक्शन: ${links.size}")

                Text("Stability (मॉडल): ${(stability * 100).toInt()}%")
                LinearProgressIndicator(
                    progress = { stability },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Continuity (मॉडल): ${(continuity * 100).toInt()}%")
                LinearProgressIndicator(
                    progress = { continuity },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Text("आंतरिक वातावरण: ${(internalEnvironment * 100).toInt()}%")
                LinearProgressIndicator(
                    progress = { internalEnvironment },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("बाहरी वातावरण")
                Slider(
                    value = environment,
                    onValueChange = { environment = it },
                    valueRange = 0f..1f
                )

                NetworkView(nodes, links, Modifier.weight(1f))
            }

            1 -> {
                Text("इतिहास और याददाश्त")
                OutlinedTextField(
                    value = memoryText,
                    onValueChange = { memoryText = it },
                    label = { Text("जानकारी दर्ज करें") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (memoryText.isNotBlank()) {
                            memories.add(
                                MemoryRecord(
                                    memories.size + 1,
                                    memoryText.trim(),
                                    tick
                                )
                            )
                            log("नई जानकारी रिकॉर्ड हुई")
                            memoryText = ""
                        }
                    }
                ) {
                    Text("Record Memory")
                }

                LazyColumn {
                    items(memories) { memory ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(memory.text)
                                Text(
                                    "रिकॉर्ड #${memory.id} • Refresh ${memory.tick}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                TextButton(
                                    onClick = {
                                        memories.remove(memory)
                                        log("रिकॉर्ड #${memory.id} हटाया गया")
                                    }
                                ) {
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                Text("प्रयोग और इतिहास")
                Text("कनेक्शन और वातावरण के बदलाव का रिकॉर्ड")
                LazyColumn {
                    items(history) { event ->
                        ListItem(
                            headlineContent = { Text(event.message) },
                            supportingContent = {
                                Text("Refresh ${event.tick}")
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }

            3 -> {
                Text(
                    "Refresh Theory",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(8.dp))

                Text(
                    "मुख्य दावा: चेतना Open System की Stability है।"
                )
                Spacer(Modifier.height(8.dp))

                Text(
                    "Stability का अर्थ है कि सिस्टम लगातार बदलते हुए भी " +
                        "अपना व्यापक संगठन बनाए रख सकता है।"
                )
                Spacer(Modifier.height(8.dp))

                Text(
                    "कनेक्शन टूटते और बनते हैं। आंतरिक वातावरण बदलता है। " +
                        "पुराने रिकॉर्ड वर्तमान व्यवहार और आगे बनने वाले " +
                        "कनेक्शनों को प्रभावित कर सकते हैं।"
                )
                Spacer(Modifier.height(8.dp))

                Text(
                    "यह ऐप Refresh Theory पर आधारित एक प्रयोगात्मक " +
                        "सिमुलेशन है। Stability और Continuity के आँकड़े " +
                        "इस ऐप के नियमों से निकलते हैं; वे वास्तविक " +
                        "चेतना का वैज्ञानिक प्रमाण नहीं हैं।"
                )

                Spacer(Modifier.height(12.dp))
                Text("वर्तमान इतिहास: ${memories.size} रिकॉर्ड")
                Text("कनेक्शन: ${links.size}")
            }
        }
    }
}

@Composable
private fun NetworkView(
    nodes: List<Node>,
    links: List<Link>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            val byId = nodes.associateBy { it.id }

            links.forEach { link ->
                val a = byId[link.a]
                val b = byId[link.b]

                if (a != null && b != null) {
                    drawLine(
                        color = Color(0xFF5B8DEF).copy(
                            alpha = link.strength.coerceIn(0.2f, 1f)
                        ),
                        start = Offset(
                            a.x * size.width,
                            a.y * size.height
                        ),
                        end = Offset(
                            b.x * size.width,
                            b.y * size.height
                        ),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }

            nodes.forEach { node ->
                drawCircle(
                    color = Color(0xFF19A974),
                    radius = 6.dp.toPx(),
                    center = Offset(
                        node.x * size.width,
                        node.y * size.height
                    )
                )
            }
        }
    }
}
