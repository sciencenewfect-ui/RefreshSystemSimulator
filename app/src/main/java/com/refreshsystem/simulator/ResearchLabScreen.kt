package com.refreshsystem.simulator

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

private data class MemoryItem(
    val id: Long,
    val content: String,
    val strength: Float = 1f,
    val age: Int = 0,
    val accessible: Boolean = true
)

private data class SystemState(
    val step: Int = 0,
    val stability: Float = 0.65f,
    val exchange: Float = 0.50f,
    val refresh: Float = 0.50f,
    val retention: Float = 0.50f,
    val memory: Float = 0.20f,
    val organization: Float = 0.20f,
    val identity: Float = 0.20f,
    val reality: Float = 0.20f,
    val activity: Float = 1f,
    val disturbance: Float = 0.10f,
    val informationLoss: Float = 0.05f,
    val processContinuity: Float = 1f,
    val log: List<String> = emptyList(),
    val memories: List<MemoryItem> = emptyList()
)

private data class ExperimentRecord(
    val name: String,
    val step: Int,
    val stability: Float,
    val memory: Float,
    val organization: Float,
    val identity: Float,
    val reality: Float,
    val inaccessibleRecords: Int
)

private fun initialSystem(): SystemState = SystemState(
    log = listOf("सिस्टम तैयार है। प्रयोग शुरू किया जा सकता है।")
)

private fun clamp(value: Float): Float = value.coerceIn(0f, 1f)

private fun averageMemory(memories: List<MemoryItem>, fallback: Float): Float {
    if (memories.isEmpty()) return clamp(fallback)
    return clamp(memories.map { it.strength }.average().toFloat())
}

private fun inaccessibleCount(state: SystemState): Int =
    state.memories.count { !it.accessible }

private fun addLog(state: SystemState, message: String): List<String> =
    (listOf(message) + state.log).take(50)

private fun advanceOpenSystem(old: SystemState): SystemState {
    val nextStep = old.step + 1
    val exchange = clamp(old.exchange)
    val refresh = clamp(old.refresh)
    val disturbance = clamp(old.disturbance)
    val activity = clamp(old.activity)

    val incoming = exchange * refresh * activity
    val loss = (1f - refresh) * 0.16f + disturbance * 0.10f
    val organization = clamp(
        old.organization + incoming * 0.035f - disturbance * 0.025f
    )
    val stability = clamp(
        old.stability +
            (refresh * exchange * 0.035f) -
            (disturbance * 0.04f) -
            ((1f - activity) * 0.025f)
    )

    val updatedMemories = old.memories.map { memory ->
        val newStrength = clamp(
            memory.strength - loss * 0.025f + refresh * 0.008f
        )
        memory.copy(
            strength = newStrength,
            age = memory.age + 1,
            accessible = newStrength > 0.12f && memory.accessible
        )
    }

    val memoryLevel = averageMemory(updatedMemories, old.memory)
    val identity = clamp(
        old.identity + (stability * organization * 0.025f) -
            (disturbance * 0.015f)
    )
    val reality = clamp(
        old.reality + (identity * 0.02f) - (disturbance * 0.01f)
    )

    val newLog = if (nextStep % 5 == 0) {
        addLog(
            old,
            "चक्र $nextStep: पदार्थ का आदान-प्रदान हुआ और सिस्टम ने अपनी संरचना बनाए रखने की कोशिश की।"
        )
    } else {
        old.log
    }

    return old.copy(
        step = nextStep,
        stability = stability,
        memory = memoryLevel,
        organization = organization,
        identity = identity,
        reality = reality,
        processContinuity = clamp(
            old.processContinuity + refresh * 0.006f - disturbance * 0.004f
        ),
        log = newLog,
        memories = updatedMemories
    )
}

private fun saveSystem(context: Context, state: SystemState) {
    val prefs = context.getSharedPreferences("refresh_system", Context.MODE_PRIVATE)
    val memories = JSONArray()
    state.memories.forEach {
        memories.put(
            JSONObject().apply {
                put("id", it.id)
                put("content", it.content)
                put("strength", it.strength.toDouble())
                put("age", it.age)
                put("accessible", it.accessible)
            }
        )
    }

    val json = JSONObject().apply {
        put("step", state.step)
        put("stability", state.stability.toDouble())
        put("exchange", state.exchange.toDouble())
        put("refresh", state.refresh.toDouble())
        put("retention", state.retention.toDouble())
        put("memory", state.memory.toDouble())
        put("organization", state.organization.toDouble())
        put("identity", state.identity.toDouble())
        put("reality", state.reality.toDouble())
        put("activity", state.activity.toDouble())
        put("disturbance", state.disturbance.toDouble())
        put("informationLoss", state.informationLoss.toDouble())
        put("processContinuity", state.processContinuity.toDouble())
        put("memories", memories)
        put("log", JSONArray(state.log))
    }

    prefs.edit().putString("system_state", json.toString()).apply()
}

private fun loadSystem(context: Context): SystemState {
    val prefs = context.getSharedPreferences("refresh_system", Context.MODE_PRIVATE)
    val raw = prefs.getString("system_state", null) ?: return initialSystem()

    return try {
        val json = JSONObject(raw)
        val memoriesJson = json.optJSONArray("memories") ?: JSONArray()
        val memories = mutableListOf<MemoryItem>()

        for (i in 0 until memoriesJson.length()) {
            val item = memoriesJson.optJSONObject(i) ?: continue
            memories.add(
                MemoryItem(
                    id = item.optLong("id"),
                    content = item.optString("content"),
                    strength = item.optDouble("strength", 1.0).toFloat(),
                    age = item.optInt("age"),
                    accessible = item.optBoolean("accessible", true)
                )
            )
        }

        val logJson = json.optJSONArray("log") ?: JSONArray()
        val logs = mutableListOf<String>()
        for (i in 0 until logJson.length()) {
            logs.add(logJson.optString(i))
        }

        SystemState(
            step = json.optInt("step"),
            stability = json.optDouble("stability", 0.65).toFloat(),
            exchange = json.optDouble("exchange", 0.50).toFloat(),
            refresh = json.optDouble("refresh", 0.50).toFloat(),
            retention = json.optDouble("retention", 0.50).toFloat(),
            memory = json.optDouble("memory", 0.20).toFloat(),
            organization = json.optDouble("organization", 0.20).toFloat(),
            identity = json.optDouble("identity", 0.20).toFloat(),
            reality = json.optDouble("reality", 0.20).toFloat(),
            activity = json.optDouble("activity", 1.0).toFloat(),
            disturbance = json.optDouble("disturbance", 0.10).toFloat(),
            informationLoss = json.optDouble("informationLoss", 0.05).toFloat(),
            processContinuity = json.optDouble("processContinuity", 1.0).toFloat(),
            log = logs.ifEmpty { listOf("पुराना सिस्टम डेटा लोड हुआ।") },
            memories = memories
        )
    } catch (_: Exception) {
        initialSystem()
    }
}

private fun saveExperiment(context: Context, record: ExperimentRecord) {
    val prefs = context.getSharedPreferences("refresh_experiments", Context.MODE_PRIVATE)
    val array = JSONArray(prefs.getString("records", "[]"))
    array.put(
        JSONObject().apply {
            put("name", record.name)
            put("step", record.step)
            put("stability", record.stability.toDouble())
            put("memory", record.memory.toDouble())
            put("organization", record.organization.toDouble())
            put("identity", record.identity.toDouble())
            put("reality", record.reality.toDouble())
            put("inaccessibleRecords", record.inaccessibleRecords)
        }
    )
    prefs.edit().putString("records", array.toString()).apply()
}

private fun loadExperiments(context: Context): List<ExperimentRecord> {
    val prefs = context.getSharedPreferences("refresh_experiments", Context.MODE_PRIVATE)
    val array = JSONArray(prefs.getString("records", "[]"))
    val records = mutableListOf<ExperimentRecord>()

    for (i in 0 until array.length()) {
        val item = array.optJSONObject(i) ?: continue
        records.add(
            ExperimentRecord(
                name = item.optString("name"),
                step = item.optInt("step"),
                stability = item.optDouble("stability").toFloat(),
                memory = item.optDouble("memory").toFloat(),
                organization = item.optDouble("organization").toFloat(),
                identity = item.optDouble("identity").toFloat(),
                reality = item.optDouble("reality").toFloat(),
                inaccessibleRecords = item.optInt("inaccessibleRecords")
            )
        )
    }
    return records
}

private fun percent(value: Float): String =
    "${(clamp(value) * 100).toInt()}%"

private fun formatFloat(value: Float): String =
    String.format(Locale.US, "%.2f", value)

private val RefreshBlue = Color(0xFF42A5F5)
private val RefreshGreen = Color(0xFF66BB6A)
private val RefreshOrange = Color(0xFFFFB74D)
private val RefreshRed = Color(0xFFEF5350)
private val RefreshBackground = Color(0xFF101820)
private val RefreshCard = Color(0xFF1C2935)

@Composable
fun ResearchHomeScreen() {
    var page by remember { mutableStateOf("home") }

    when (page) {
        "formation" -> OpenSystemFormationLab(
            onBack = { page = "home" },
            onAdvancedLab = { page = "open" }
        )

        "open" -> OpenSystemLab(
            onBack = { page = "formation" }
        )

        "continuity" -> ContinuityLab(
            onBack = { page = "home" }
        )

        else -> MainResearchHome(
            onOpenSystem = { page = "formation" },
            onContinuity = { page = "continuity" }
        )
    }
}

@Composable
private fun MainResearchHome(
    onOpenSystem: () -> Unit,
    onContinuity: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RefreshBackground)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "REFRESH SYSTEM",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Open System और Stability को प्रयोग के माध्यम से समझें।",
            color = Color.LightGray
        )

        LabHomeCard(
            title = "Open System Formation & Refresh Lab",
            subtitle = "कण कैसे जुड़ते हैं, अलग होते हैं और संरचना को बनाए रखते हैं।",
            buttonText = "Simulation शुरू करें",
            color = RefreshGreen,
            onClick = onOpenSystem
        )

        LabHomeCard(
            title = "Existence Continuity & Transfer Lab",
            subtitle = "मेमोरी, पहचान और निरंतरता के मॉडल का प्रयोग।",
            buttonText = "Continuity Lab खोलें",
            color = RefreshBlue,
            onClick = onContinuity
        )
    }
}

@Composable
private fun LabHomeCard(
    title: String,
    subtitle: String,
    buttonText: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RefreshCard),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.LightGray)
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = color)
            ) {
                Text(buttonText, color = Color.Black)
            }
        }
    }
}

/* ---------------------------------------------------------
   FORMATION AND REFRESH SIMULATION
   --------------------------------------------------------- */

private data class FormationAtom(
    val id: Int,
    val x: Float,
    val y: Float,
    val entering: Boolean,
    val age: Int
)

private data class FormationState(
    val cycle: Int = 0,
    val incoming: Int = 0,
    val outgoing: Int = 0,
    val refreshRate: Float = 0.55f,
    val speed: Float = 1f,
    val evolution: Float = 0f,
    val isRunning: Boolean = false,
    val atoms: List<FormationAtom> = emptyList(),
    val lastEvent: String = "सिस्टम तैयार है।"
)

private val WPattern = listOf(
    Offset(0.10f, 0.38f),
    Offset(0.22f, 0.72f),
    Offset(0.36f, 0.42f),
    Offset(0.50f, 0.72f),
    Offset(0.64f, 0.42f),
    Offset(0.78f, 0.72f),
    Offset(0.90f, 0.38f)
)

private fun createFormationAtoms(): List<FormationAtom> {
    return WPattern.mapIndexed { index, point ->
        FormationAtom(
            id = index,
            x = point.x,
            y = point.y,
            entering = false,
            age = 0
        )
    }
}

private fun nextFormationCycle(state: FormationState): FormationState {
    val nextCycle = state.cycle + 1
    val atoms = state.atoms.ifEmpty { createFormationAtoms() }.toMutableList()

    val numberToReplace = when {
        state.refreshRate < 0.25f -> 1
        state.refreshRate < 0.55f -> 2
        state.refreshRate < 0.80f -> 3
        else -> 4
    }

    var outgoing = 0
    var incoming = 0

    repeat(numberToReplace.coerceAtMost(atoms.size)) { index ->
        val targetIndex = (nextCycle + index * 2) % atoms.size
        val old = atoms[targetIndex]
        atoms[targetIndex] = old.copy(
            id = old.id + nextCycle * 100,
            entering = true,
            age = 0
        )
        outgoing++
        incoming++
    }

    val updated = atoms.map {
        it.copy(age = it.age + 1, entering = false)
    }

    val evolution = clamp(
        state.evolution + (state.refreshRate * 0.0025f)
    )

    return state.copy(
        cycle = nextCycle,
        incoming = state.incoming + incoming,
        outgoing = state.outgoing + outgoing,
        evolution = evolution,
        atoms = updated,
        lastEvent = "चक्र $nextCycle: $incoming नए कण आए और $outgoing पुराने कण बाहर गए।"
    )
}

@Composable
private fun OpenSystemFormationLab(
    onBack: () -> Unit,
    onAdvancedLab: () -> Unit
) {
    var state by remember {
        mutableStateOf(
            FormationState(atoms = createFormationAtoms())
        )
    }

    LaunchedEffect(state.isRunning, state.speed, state.refreshRate) {
        while (state.isRunning) {
            delay((1000L / state.speed.coerceIn(0.25f, 4f)).toLong())
            state = nextFormationCycle(state)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RefreshBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← वापस", color = RefreshBlue)
            }
            Spacer(Modifier.weight(1f))
            Text(
                "FORMATION LAB",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            "Open System: बनना और बिखरना",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            "देखिए कि कण एक संरचना बनाते हैं। उसी समय कुछ पुराने कण बाहर जाते हैं और नए कण अंदर आते हैं।",
            color = Color.LightGray
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RefreshCard)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "जीवित संरचना का मॉडल",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                FormationCanvas(
                    atoms = state.atoms,
                    evolution = state.evolution,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                )

                Text(
                    "बीच की संरचना W-पैटर्न को दिखाती है। बाहर से आने वाले और बाहर जाने वाले कण लगातार बदलते रहते हैं।",
                    color = Color.LightGray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FormationMetric(
                title = "चक्र",
                value = state.cycle.toString(),
                color = RefreshBlue,
                modifier = Modifier.weight(1f)
            )
            FormationMetric(
                title = "अंदर आए",
                value = state.incoming.toString(),
                color = RefreshGreen,
                modifier = Modifier.weight(1f)
            )
            FormationMetric(
                title = "बाहर गए",
                value = state.outgoing.toString(),
                color = RefreshOrange,
                modifier = Modifier.weight(1f)
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RefreshCard)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Refresh Rate",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "एक चक्र में कितने कण बदलेंगे?",
                    color = Color.LightGray
                )
                Slider(
                    value = state.refreshRate,
                    onValueChange = {
                        state = state.copy(refreshRate = it)
                    },
                    valueRange = 0.05f..1f
                )
                Text(
                    "${(state.refreshRate * 100).toInt()}% Refresh",
                    color = RefreshGreen
                )

                Divider(color = Color.DarkGray)

                Text(
                    "Simulation Speed",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = state.speed,
                    onValueChange = {
                        state = state.copy(speed = it)
                    },
                    valueRange = 0.25f..4f
                )
                Text(
                    "${formatFloat(state.speed)}x",
                    color = RefreshBlue
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RefreshCard)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Pattern Evolution",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "समय के साथ छोटे-छोटे बदलाव जमा होते हैं। इस मॉडल में Evolution Slider उसी बदलाव को दिखाता है।",
                    color = Color.LightGray
                )
                LinearProgressIndicator(
                    progress = { state.evolution },
                    modifier = Modifier.fillMaxWidth(),
                    color = RefreshOrange
                )
                Text(
                    "${(state.evolution * 100).toInt()}% परिवर्तन",
                    color = RefreshOrange
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RefreshCard)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "अभी क्या हुआ?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    state.lastEvent,
                    color = Color.LightGray
                )
                Text(
                    "महत्वपूर्ण बात: इस मॉडल में संरचना का पैटर्न कुछ समय तक बना रहता है, जबकि उसके हिस्से बदलते रहते हैं।",
                    color = RefreshGreen
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    state = state.copy(isRunning = !state.isRunning)
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isRunning) RefreshOrange else RefreshGreen
                )
            ) {
                Text(
                    if (state.isRunning) "Pause" else "Start",
                    color = Color.Black
                )
            }

            OutlinedButton(
                onClick = {
                    state = nextFormationCycle(state)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("One Cycle", color = Color.White)
            }
        }

        OutlinedButton(
            onClick = {
                state = FormationState(atoms = createFormationAtoms())
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reset Simulation", color = RefreshRed)
        }

        Button(
            onClick = onAdvancedLab,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = RefreshBlue)
        ) {
            Text(
                "Advanced Open System Lab खोलें",
                color = Color.Black
            )
        }
    }
}

@Composable
private fun FormationCanvas(
    atoms: List<FormationAtom>,
    evolution: Float,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .background(
                Color(0xFF0C151D),
                RoundedCornerShape(14.dp)
            )
    ) {
        val width = size.width
        val height = size.height

        val points = WPattern.mapIndexed { index, point ->
            val curvedY = 0.48f + kotlin.math.sin(
                (index.toFloat() / (WPattern.size - 1)) * Math.PI
            ).toFloat() * 0.13f

            Offset(
                x = width * point.x,
                y = height * (
                    point.y * (1f - evolution) +
                        curvedY * evolution
                    )
            )
        }

        for (i in 0 until points.size - 1) {
            drawLine(
                color = Color(0xFF4D718B),
                start = points[i],
                end = points[i + 1],
                strokeWidth = 5f
            )
        }

        points.forEachIndexed { index, point ->
            drawCircle(
                color = Color(0xFF263D4F),
                radius = 18f,
                center = point
            )
            drawCircle(
                color = Color(0xFF64B5F6),
                radius = 9f,
                center = point
            )
            drawCircle(
                color = Color.White,
                radius = 3f,
                center = point
            )
        }

        atoms.forEachIndexed { index, atom ->
            val base = points[index % points.size]
            val offsetX = ((atom.id % 3) - 1) * 5f
            val offsetY = ((atom.id % 5) - 2) * 3f
            val position = Offset(
                base.x + offsetX,
                base.y + offsetY
            )

            drawCircle(
                color = if (atom.entering) RefreshGreen else Color(0xFF90CAF9),
                radius = 5f,
                center = position
            )
        }

        val leftDoor = Offset(width * 0.02f, height * 0.50f)
        val rightDoor = Offset(width * 0.98f, height * 0.50f)

        drawCircle(
            color = RefreshGreen,
            radius = 8f,
            center = leftDoor
        )
        drawCircle(
            color = RefreshOrange,
            radius = 8f,
            center = rightDoor
        )

        drawLine(
            color = RefreshGreen.copy(alpha = 0.6f),
            start = Offset(width * 0.02f, height * 0.50f),
            end = Offset(width * 0.10f, height * 0.38f),
            strokeWidth = 2f
        )
        drawLine(
            color = RefreshOrange.copy(alpha = 0.6f),
            start = Offset(width * 0.90f, height * 0.38f),
            end = Offset(width * 0.98f, height * 0.50f),
            strokeWidth = 2f
        )
    }
}

@Composable
private fun FormationMetric(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = RefreshCard)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = Color.LightGray)
            Text(
                value,
                color = color,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

/* ---------------------------------------------------------
   ADVANCED OPEN SYSTEM LAB
   --------------------------------------------------------- */

@Composable
private fun OpenSystemLab(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(loadSystem(context)) }
    var isRunning by remember { mutableStateOf(false) }
    var memoryInput by remember { mutableStateOf("") }
    var experimentName by remember { mutableStateOf("") }
    var experiments by remember { mutableStateOf(loadExperiments(context)) }

    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(1000)
            state = advanceOpenSystem(state)
            saveSystem(context, state)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RefreshBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← वापस", color = RefreshBlue)
        }

        Text(
            "Advanced Open System Lab",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            "यहाँ Exchange, Refresh, Disturbance और Memory के मान बदलकर सिस्टम के व्यवहार को देख सकते हैं।",
            color = Color.LightGray
        )

        MetricCard("Stability", state.stability)
        MetricCard("Organization", state.organization)
        MetricCard("Memory", state.memory)
        MetricCard("Identity", state.identity)
        MetricCard("Reality", state.reality)
        MetricCard("Process Continuity", state.processContinuity)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RefreshCard)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("System Controls", color = Color.White, fontWeight = FontWeight.Bold)

                ValueSlider(
                    title = "Material Exchange",
                    value = state.exchange,
                    onChange = { state = state.copy(exchange = it) }
                )
                ValueSlider(
                    title = "Refresh Rate",
                    value = state.refresh,
                    onChange = { state = state.copy(refresh = it) }
                )
                ValueSlider(
                    title = "Memory Retention",
                    value = state.retention,
                    onChange = { state = state.copy(retention = it) }
                )
                ValueSlider(
                    title = "Activity",
                    value = state.activity,
                    onChange = { state = state.copy(activity = it) }
                )
                ValueSlider(
                    title = "Disturbance",
                    value = state.disturbance,
                    onChange = { state = state.copy(disturbance = it) }
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RefreshCard)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Memory Input", color = Color.White, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = memoryInput,
                    onValueChange = { memoryInput = it },
                    label = { Text("कोई जानकारी लिखें") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = RefreshBlue,
                        unfocusedLabelColor = Color.LightGray
                    )
                )

                Button(
                    onClick = {
                        if (memoryInput.isNotBlank()) {
                            val item = MemoryItem(
                                id = System.currentTimeMillis(),
                                content = memoryInput.trim()
                            )
                            state = state.copy(
                                memories = state.memories + item,
                                memory = averageMemory(
                                    state.memories + item,
                                    state.memory
                                ),
                                log = addLog(state, "नई जानकारी मेमोरी में जोड़ी गई।")
                            )
                            memoryInput = ""
                            saveSystem(context, state)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Memory में जोड़ें")
                }

                Text(
                    "Stored Memories: ${state.memories.size}",
                    color = RefreshGreen
                )

                state.memories.takeLast(8).forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.content, color = Color.White)
                            Text(
                                "Strength: ${percent(item.strength)} | Age: ${item.age}",
                                color = Color.LightGray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            if (item.accessible) "Available" else "Weak",
                            color = if (item.accessible) RefreshGreen else RefreshOrange
                        )
                    }
                    Divider(color = Color.DarkGray)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { isRunning = !isRunning },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) RefreshOrange else RefreshGreen
                )
            ) {
                Text(if (isRunning) "Pause" else "Start", color = Color.Black)
            }

            OutlinedButton(
                onClick = {
                    state = advanceOpenSystem(state)
                    saveSystem(context, state)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("One Step", color = Color.White)
            }
        }

        OutlinedButton(
            onClick = {
                state = initialSystem()
                isRunning = false
                saveSystem(context, state)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reset System", color = RefreshRed)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RefreshCard)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Save Experiment", color = Color.White, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = experimentName,
                    onValueChange = { experimentName = it },
                    label = { Text("Experiment का नाम") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = RefreshBlue,
                        unfocusedLabelColor = Color.LightGray
                    )
                )

                Button(
                    onClick = {
                        val record = ExperimentRecord(
                            name = experimentName.ifBlank { "Experiment ${experiments.size + 1}" },
                            step = state.step,
                            stability = state.stability,
                            memory = state.memory,
                            organization = state.organization,
                            identity = state.identity,
                            reality = state.reality,
                            inaccessibleRecords = inaccessibleCount(state)
                        )
                        saveExperiment(context, record)
                        experiments = loadExperiments(context)
                        experimentName = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Current State")
                }

                experiments.takeLast(5).reversed().forEach { record ->
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(record.name, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(
                            "Step ${record.step} | Stability ${percent(record.stability)} | Memory ${percent(record.memory)}",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Divider(color = Color.DarkGray)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RefreshCard)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Experiment Log", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Total Steps: ${state.step}", color = RefreshBlue)

                state.log.take(12).forEach {
                    Text("• $it", color = Color.LightGray)
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RefreshCard)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, color = Color.White)
                Text(percent(value), color = RefreshGreen, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(
                progress = { clamp(value) },
                modifier = Modifier.fillMaxWidth(),
                color = RefreshGreen
            )
        }
    }
}

@Composable
private fun ValueSlider(
    title: String,
    value: Float,
    onChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, color = Color.White)
            Text(percent(value), color = RefreshBlue)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 0f..1f
        )
    }
}

/* ---------------------------------------------------------
   CONTINUITY AND TRANSFER LAB
   --------------------------------------------------------- */

@Composable
private fun ContinuityLab(
    onBack: () -> Unit
) {
    var originalMemory by remember { mutableFloatStateOf(0.80f) }
    var copyMemory by remember { mutableFloatStateOf(0.80f) }
    var originalIdentity by remember { mutableFloatStateOf(0.90f) }
    var copyIdentity by remember { mutableFloatStateOf(0.90f) }
    var originalContinuity by remember { mutableFloatStateOf(1f) }
    var copyContinuity by remember { mutableFloatStateOf(0f) }
    var transferred by remember { mutableStateOf(false) }

    val similarity = clamp(
        (originalMemory + copyMemory + originalIdentity + copyIdentity) / 4f
    )
    val originalScore = clamp(
        originalMemory * 0.35f +
            originalIdentity * 0.35f +
            originalContinuity * 0.30f
    )
    val copyScore = clamp(
        copyMemory * 0.35f +
            copyIdentity * 0.35f +
            copyContinuity * 0.30f
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RefreshBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← वापस", color = RefreshBlue)
        }

        Text(
            "Existence Continuity & Transfer Lab",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Original और Copy के बीच Memory, Identity और Continuity की तुलना करें।",
            color = Color.LightGray
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RefreshCard)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Original System", color = RefreshGreen, fontWeight = FontWeight.Bold)
                ValueSlider(
                    "Original Memory",
                    originalMemory
                ) { originalMemory = it }
                ValueSlider(
                    "Original Identity",
                    originalIdentity
                ) { originalIdentity = it }
                ValueSlider(
                    "Original Continuity",
                    originalContinuity
                ) { originalContinuity = it }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RefreshCard)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Copy / New System", color = RefreshBlue, fontWeight = FontWeight.Bold)
                ValueSlider(
                    "Copy Memory",
                    copyMemory
                ) { copyMemory = it }
                ValueSlider(
                    "Copy Identity",
                    copyIdentity
                ) { copyIdentity = it }
                ValueSlider(
                    "Copy Continuity",
                    copyContinuity
                ) { copyContinuity = it }
            }
        }

        MetricCard("Memory & Identity Similarity", similarity)
        MetricCard("Original Continuity Score", originalScore)
        MetricCard("Copy Continuity Score", copyScore)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RefreshCard)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Transfer Experiment", color = Color.White, fontWeight = FontWeight.Bold)

                Text(
                    if (transferred) {
                        "Transfer मॉडल चलाया गया। Original और Copy के मान अलग-अलग देखे जा सकते हैं।"
                    } else {
                        "Transfer शुरू करने पर Copy को Original की Memory और Identity के मान मिलेंगे। Continuity को अलग से नियंत्रित किया जा सकता है।"
                    },
                    color = Color.LightGray
                )

                Button(
                    onClick = {
                        copyMemory = originalMemory
                        copyIdentity = originalIdentity
                        copyContinuity = 0f
                        transferred = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Copy Memory & Identity")
                }

                OutlinedButton(
                    onClick = {
                        copyMemory = originalMemory
                        copyIdentity = originalIdentity
                        copyContinuity = originalContinuity
                        transferred = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Copy All Values Including Continuity")
                }

                OutlinedButton(
                    onClick = {
                        originalMemory = 0.80f
                        copyMemory = 0.80f
                        originalIdentity = 0.90f
                        copyIdentity = 0.90f
                        originalContinuity = 1f
                        copyContinuity = 0f
                        transferred = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset Experiment")
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RefreshCard)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Observation", color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    "Memory और Identity के मान समान हो सकते हैं, जबकि Continuity का मान अलग हो सकता है। इस मॉडल में इन्हें अलग-अलग मापदंडों के रूप में रखा गया है।",
                    color = Color.LightGray
                )
                Text(
                    "Original Score: ${percent(originalScore)}",
                    color = RefreshGreen
                )
                Text(
                    "Copy Score: ${percent(copyScore)}",
                    color = RefreshBlue
                )
            }
        }
    }
}
