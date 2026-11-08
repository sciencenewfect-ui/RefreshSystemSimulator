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

private fun saveSystem(context: Context, key: String, state: SystemState) {
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

    prefs.edit().putString(key, json.toString()).apply()
}

private fun parseSystem(jsonString: String?): SystemState? {
    if (jsonString == null) return null

    return try {
        val json = JSONObject(jsonString)
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
            log = logs.ifEmpty { listOf("सिस्टम डेटा लोड हुआ।") },
            memories = memories
        )
    } catch (_: Exception) {
        null
    }
}

private fun loadSystem(context: Context, key: String): SystemState {
    val prefs = context.getSharedPreferences("refresh_system", Context.MODE_PRIVATE)
    return parseSystem(prefs.getString(key, null)) ?: initialSystem()
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

/* ---------------------------------------------------------
   MAIN NAVIGATION
   --------------------------------------------------------- */

@Composable
fun ResearchHomeScreen() {
    var selectedLab by remember { mutableStateOf("home") }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (selectedLab) {
                "formation" -> OpenSystemFormationLab(
                    onBack = { selectedLab = "home" },
                    onAdvancedLab = { selectedLab = "open" }
                )

                "open" -> OpenSystemLab(
                    onBack = { selectedLab = "formation" }
                )

                "continuity" -> ContinuityLab(
                    onBack = { selectedLab = "home" }
                )

                else -> MainResearchHome(
                    onOpenSystem = { selectedLab = "formation" },
                    onContinuity = { selectedLab = "continuity" }
                )
            }
        }
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
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Refresh System",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            "Refresh Theory पर आधारित Research Labs",
            style = MaterialTheme.typography.bodyLarge
        )

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Open System Formation & Refresh Lab",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Atom और Molecule से Pattern बनना, अंदर-बाहर का आदान-प्रदान, " +
                        "Internal Refresh और लंबे समय में Pattern Evolution."
                )

                Spacer(Modifier.height(10.dp))

                Button(onClick = onOpenSystem) {
                    Text("पूरी Simulation शुरू करें")
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onContinuity() }
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Existence Continuity & Transfer Lab",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Original System और Copy के बीच Memory, Identity और Continuity की तुलना।"
                )

                Spacer(Modifier.height(10.dp))

                Button(onClick = onContinuity) {
                    Text("Continuity Lab खोलें")
                }
            }
        }
    }
}

/* ---------------------------------------------------------
   UNIFIED OPEN SYSTEM / REFRESH / EVOLUTION SIMULATION
   --------------------------------------------------------- */

private data class FormationAtom(
    val id: Int,
    val slot: Int,
    val microPhase: Float = 0f
)

private data class BoundaryParticle(
    val id: Int,
    val slot: Int,
    val progress: Float,
    val entering: Boolean
)

private data class FormationState(
    val step: Int = 0,
    val atoms: List<FormationAtom> = emptyList(),
    val boundaryParticles: List<BoundaryParticle> = emptyList(),
    val refreshPercent: Float = 0.05f,
    val internalMovement: Float = 0.55f,
    val environment: Float = 0.50f,
    val speed: Float = 1f,
    val totalIn: Int = 0,
    val totalOut: Int = 0,
    val bondChanges: Int = 0,
    val evolution: Float = 0f,
    val isRunning: Boolean = true,
    val lastEvent: String = "कण गति कर रहे हैं। अभी कोई निश्चित पैटर्न नहीं है।"
)

private const val FORMATION_SLOTS = 36

private fun makeInitialFormation(): FormationState {
    val atoms = (0 until FORMATION_SLOTS).map { index ->
        FormationAtom(
            id = index + 1,
            slot = index,
            microPhase = index * 0.37f
        )
    }

    return FormationState(atoms = atoms)
}

private fun formationStage(step: Int): Int = when {
    step < 12 -> 1
    step < 25 -> 2
    step < 85 -> 3
    else -> 4
}

private fun nextFormationState(old: FormationState): FormationState {
    val nextStep = old.step + 1
    val stage = formationStage(nextStep)

    val atoms = old.atoms
        .ifEmpty { makeInitialFormation().atoms }
        .toMutableList()

    val boundary = old.boundaryParticles.mapNotNull { particle ->
        val progress = particle.progress + 0.28f
        if (progress >= 1f) null
        else particle.copy(progress = progress)
    }.toMutableList()

    var totalIn = old.totalIn
    var totalOut = old.totalOut
    var bondChanges = old.bondChanges

    val refreshFraction = old.refreshPercent.coerceIn(0.01f, 0.50f)
    val exchangeCount = (FORMATION_SLOTS * refreshFraction)
        .toInt()
        .coerceAtLeast(1)

    // Boundary Exchange: केवल कुछ कण सीमा पार करते हैं।
    if (stage >= 2 && nextStep % 2 == 0) {
        repeat(exchangeCount.coerceAtMost(FORMATION_SLOTS)) { n ->
            val slot = (nextStep * 3 + n * 7) % FORMATION_SLOTS
            val oldAtomIndex = atoms.indexOfFirst { it.slot == slot }

            if (oldAtomIndex >= 0) {
                val oldAtom = atoms[oldAtomIndex]

                boundary.add(
                    BoundaryParticle(
                        id = oldAtom.id,
                        slot = slot,
                        progress = 0f,
                        entering = false
                    )
                )

                val newId = (atoms.maxOfOrNull { it.id } ?: 0) + 1

                atoms[oldAtomIndex] = FormationAtom(
                    id = newId,
                    slot = slot,
                    microPhase = nextStep * 0.41f + n
                )

                boundary.add(
                    BoundaryParticle(
                        id = newId,
                        slot = slot,
                        progress = 0f,
                        entering = true
                    )
                )

                totalOut++
                totalIn++
            }
        }
    }

    // Internal Refresh: सभी कणों की स्थिति और संबंध बदलते रहते हैं।
    val movedAtoms = atoms.map { atom ->
        atom.copy(
            microPhase = atom.microPhase +
                old.internalMovement * 0.34f + 0.08f
        )
    }

    bondChanges += if (stage >= 2) {
        2 + (old.internalMovement * 5).toInt()
    } else {
        0
    }

    val evolution = if (stage >= 4) {
        (old.evolution + 0.0028f * (0.35f + old.environment))
            .coerceAtMost(1f)
    } else {
        0f
    }

    val message = when (stage) {
        1 -> "अभी कण इधर-उधर गति कर रहे हैं। कुछ अंदर आ रहे हैं और कुछ बाहर जा रहे हैं। अभी कोई निश्चित Pattern नहीं बना है।"

        2 -> "कण आपस में जुड़ रहे हैं। पुराने संबंध टूट रहे हैं और नए संबंध बन रहे हैं। W-पैटर्न उभरना शुरू हो रहा है।"

        3 -> "अब W-पैटर्न बन चुका है। कुछ कण अंदर-बाहर हो रहे हैं और बाकी कण भी अंदर ही अंदर गति करके अपने संबंध बदल रहे हैं। पूरा Pattern Refresh हो रहा है, फिर भी W जैसा दिखाई दे रहा है।"

        else -> "लंबे समय के बदलाव जमा हो रहे हैं। वातावरण और सिस्टम के नियम अगले मिलते-जुलते Pattern की दिशा को प्रभावित कर रहे हैं।"
    }

    return old.copy(
        step = nextStep,
        atoms = movedAtoms,
        boundaryParticles = boundary,
        totalIn = totalIn,
        totalOut = totalOut,
        bondChanges = bondChanges,
        evolution = evolution,
        lastEvent = message
    )
}

private fun formationPoints(
    evolution: Float,
    environment: Float,
    step: Int
): List<Pair<Float, Float>> {

    val start = (0 until FORMATION_SLOTS).map { index ->
        val t = index.toFloat() / (FORMATION_SLOTS - 1)
        val x = 0.12f + 0.76f * t
        val y = 0.20f + ((index * 37) % 61) / 100f
        x to y
    }

    val wVertices = listOf(
        0.12f to 0.28f,
        0.24f to 0.72f,
        0.38f to 0.36f,
        0.50f to 0.72f,
        0.63f to 0.36f,
        0.76f to 0.72f,
        0.88f to 0.28f
    )

    val w = (0 until FORMATION_SLOTS).map { index ->
        val t = index.toFloat() / (FORMATION_SLOTS - 1)
        val scaled = t * (wVertices.size - 1)
        val segment = scaled.toInt().coerceAtMost(wVertices.size - 2)
        val local = scaled - segment

        val a = wVertices[segment]
        val b = wVertices[segment + 1]

        (a.first + (b.first - a.first) * local) to
            (a.second + (b.second - a.second) * local)
    }

    val tPattern = (0 until FORMATION_SLOTS).map { index ->
        val t = index.toFloat() / (FORMATION_SLOTS - 1)
        val x = 0.12f + 0.76f * t

        val y = if (t < 0.5f) {
            0.36f + 0.30f * (t / 0.5f)
        } else {
            0.66f - 0.30f * ((t - 0.5f) / 0.5f)
        }

        x to y
    }

    val bPattern = (0 until FORMATION_SLOTS).map { index ->
        val t = index.toFloat() / (FORMATION_SLOTS - 1)
        val x = 0.12f + 0.76f * t
        val y = 0.50f +
            0.20f * kotlin.math.sin(t * Math.PI * 2.0).toFloat()

        x to y
    }

    val environmentBias = environment.coerceIn(0f, 1f)
    val nextPattern = if (environmentBias < 0.5f) {
        tPattern
    } else {
        bPattern
    }

    val formProgress = when {
        step < 12 -> 0f
        step < 25 -> ((step - 12) / 13f).coerceIn(0f, 1f)
        step < 85 -> 0f
        else -> evolution.coerceIn(0f, 1f)
    }

    return (0 until FORMATION_SLOTS).map { index ->
        val base = when {
            step < 12 -> start[index]
            else -> w[index]
        }

        val target = nextPattern[index]

        val progress = if (step >= 85) formProgress else 0f

        base.first + (target.first - base.first) * progress to
            base.second + (target.second - base.second) * progress
    }
}

/* ---------------------------------------------------------
   MAIN SIMULATION SCREEN
   --------------------------------------------------------- */

@Composable
private fun OpenSystemFormationLab(
    onBack: () -> Unit,
    onAdvancedLab: () -> Unit
) {
    var simulation by remember {
        mutableStateOf(makeInitialFormation())
    }

    var running by remember { mutableStateOf(true) }

    LaunchedEffect(running, simulation.speed) {
        while (running) {
            delay((650L / simulation.speed.coerceIn(0.4f, 3f)).toLong())
            simulation = nextFormationState(simulation)
        }
    }

    val stage = formationStage(simulation.step)

    val patternName = when {
        stage < 2 -> "अभी कोई निश्चित Pattern नहीं"
        stage < 4 -> "W Pattern"
        simulation.environment < 0.5f -> "T जैसा संभावित Pattern"
        else -> "B जैसा संभावित Pattern"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← वापस")
            }

            Spacer(Modifier.weight(1f))

            Text(
                "OPEN SYSTEM LAB",
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            "Open System: Refresh से Evolution तक",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            "इस Simulation में देखिए कि कणों का लगातार बदलना कैसे एक Pattern को बनाए रख सकता है और लंबे समय में उसका स्वरूप कैसे बदल सकता है।"
        )

        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Live Molecular Structure",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        "चरण $stage / 4",
                        color = Color(0xFF67E8F9)
                    )
                }

                FormationCanvas(
                    state = simulation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FormationLegend(Color(0xFF38BDF8), "नया प्रवेश")
                    FormationLegend(Color(0xFFF59E0B), "बाहर निकास")
                    FormationLegend(Color(0xFF4ADE80), "अंदर के कण")
                }

                Text(
                    simulation.lastEvent,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "प्रक्रिया की Timeline",
                    style = MaterialTheme.typography.titleMedium
                )

                val stages = listOf(
                    "1. कणों की गति और Entry / Exit",
                    "2. कणों के जुड़ने से W-पैटर्न बनना",
                    "3. W-पैटर्न का लगातार Refresh होना",
                    "4. लंबे समय में Pattern Evolution"
                )

                stages.forEachIndexed { index, label ->
                    val active = stage == index + 1

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (active) "●" else "○",
                            color = if (active) {
                                Color(0xFF22C55E)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )

                        Spacer(Modifier.width(8.dp))

                        Text(
                            label,
                            color = if (active) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "अभी सिस्टम में क्या हो रहा है?",
                    style = MaterialTheme.typography.titleMedium
                )

                Text("वर्तमान Pattern: $patternName")
                Text("सिस्टम के अंदर कण: ${simulation.atoms.size}")
                Text("कुल अंदर आए: ${simulation.totalIn}")
                Text("कुल बाहर गए: ${simulation.totalOut}")
                Text("बदले / Refresh हुए Bonds: ${simulation.bondChanges}")
                Text("Pattern Evolution: ${(simulation.evolution * 100).toInt()}%")

                LinearProgressIndicator(
                    progress = { simulation.evolution },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Simulation Controls",
                    style = MaterialTheme.typography.titleMedium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { running = !running },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (running) "Pause" else "Play")
                    }

                    OutlinedButton(
                        onClick = {
                            simulation = nextFormationState(simulation)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("एक Cycle")
                    }
                }

                Text(
                    "Boundary Exchange: ${(simulation.refreshPercent * 100).toInt()}%"
                )

                Slider(
                    value = simulation.refreshPercent,
                    onValueChange = {
                        simulation = simulation.copy(refreshPercent = it)
                    },
                    valueRange = 0.01f..0.20f
                )

                Text(
                    "Internal Movement: ${(simulation.internalMovement * 100).toInt()}%"
                )

                Slider(
                    value = simulation.internalMovement,
                    onValueChange = {
                        simulation = simulation.copy(internalMovement = it)
                    },
                    valueRange = 0.05f..1f
                )

                Text(
                    "Environment Influence: ${(simulation.environment * 100).toInt()}%"
                )

                Slider(
                    value = simulation.environment,
                    onValueChange = {
                        simulation = simulation.copy(environment = it)
                    },
                    valueRange = 0f..1f
                )

                Text(
                    "Simulation Speed: ${
                        String.format(
                            Locale.getDefault(),
                            "%.1f",
                            simulation.speed
                        )
                    }x"
                )

                Slider(
                    value = simulation.speed,
                    onValueChange = {
                        simulation = simulation.copy(speed = it)
                    },
                    valueRange = 0.4f..3f
                )

                OutlinedButton(
                    onClick = {
                        running = false
                        simulation = makeInitialFormation()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("पूरी प्रक्रिया फिर से शुरू करें")
                }

                OutlinedButton(
                    onClick = onAdvancedLab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Advanced Open System Lab खोलें")
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "आसान भाषा में समझें",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    "शुरुआत में कण इधर-उधर गति करते हैं। कुछ अंदर आते हैं और कुछ बाहर जाते हैं।"
                )

                Text(
                    "धीरे-धीरे कण जुड़ते हैं और W-पैटर्न बनता है।"
                )

                Text(
                    "W-पैटर्न बनने के बाद भी पूरा Pattern Refresh होता रहता है। कुछ कण सीमा पार करते हैं, जबकि बाकी कण अंदर ही गति करते हैं और अपने संबंध बदलते हैं।"
                )

                Text(
                    "इस Refresh के बावजूद कुछ समय तक W-पैटर्न पहचान में बना रहता है। लंबे समय में बदलाव जमा होकर नया मिलता-जुलता Pattern बना सकते हैं।"
                )
            }
        }
    }
}

@Composable
private fun FormationLegend(
    color: Color,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(10.dp)) {
            drawCircle(color = color)
        }

        Spacer(Modifier.width(5.dp))

        Text(
            label,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun FormationCanvas(
    state: FormationState,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier.background(
            Color(0xFF0B1220),
            RoundedCornerShape(16.dp)
        )
    ) {
        val points = formationPoints(
            state.evolution,
            state.environment,
            state.step
        )

        val sx = size.width
        val sy = size.height
        val stage = formationStage(state.step)

        val centers = points.map {
            Offset(it.first * sx, it.second * sy)
        }

        // Open System की सीमा
        drawRoundRect(
            color = Color(0xFF64748B).copy(alpha = 0.55f),
            topLeft = Offset(sx * 0.07f, sy * 0.10f),
            size = androidx.compose.ui.geometry.Size(
                sx * 0.86f,
                sy * 0.80f
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                18.dp.toPx()
            ),
            style = Stroke(width = 1.dp.toPx())
        )

        // Bonds लगातार बदलते रहते हैं।
        if (stage >= 2) {
            for (i in 0 until centers.lastIndex) {
                val active = (state.step + i) % 6 != 0

                drawLine(
                    color = if (active) {
                        Color(0xFF38BDF8).copy(alpha = 0.78f)
                    } else {
                        Color(0xFF64748B).copy(alpha = 0.25f)
                    },
                    start = centers[i],
                    end = centers[i + 1],
                    strokeWidth = if (active) {
                        2.2.dp.toPx()
                    } else {
                        1.dp.toPx()
                    }
                )
            }

            for (i in 0 until centers.size - 3 step 3) {
                val j = i + 2

                if ((state.step + i) % 4 != 0) {
                    drawLine(
                        color = Color(0xFF818CF8).copy(alpha = 0.48f),
                        start = centers[i],
                        end = centers[j],
                        strokeWidth = 1.2.dp.toPx()
                    )
                }
            }
        }

        // सभी अंदरूनी कणों की गति।
        state.atoms.forEach { atom ->
            val target = centers[atom.slot]
            val phase = atom.microPhase + state.step * 0.12f

            val movement = state.internalMovement *
                (1.2f + (atom.slot % 4) * 0.35f) *
                dpScale(this)

            val x = target.x +
                kotlin.math.sin(phase.toDouble()).toFloat() * movement

            val y = target.y +
                kotlin.math.cos((phase * 1.3f).toDouble()).toFloat() * movement

            val point = Offset(x, y)

            drawCircle(
                Color(0xFF1E293B),
                8.dp.toPx(),
                point
            )

            drawCircle(
                Color(0xFF4ADE80),
                4.dp.toPx(),
                point
            )
        }

        // Entry और Exit एक साथ दिखते हैं।
        state.boundaryParticles.forEach { particle ->
            val target = centers[particle.slot]
            val p = particle.progress.coerceIn(0f, 1f)

            val edgeX = if (particle.entering) {
                -sx * 0.04f
            } else {
                sx * 1.04f
            }

            val start = if (particle.entering) {
                Offset(edgeX, target.y)
            } else {
                target
            }

            val end = if (particle.entering) {
                target
            } else {
                Offset(edgeX, target.y)
            }

            val position = Offset(
                start.x + (end.x - start.x) * p,
                start.y + (end.y - start.y) * p
            )

            val color = if (particle.entering) {
                Color(0xFF38BDF8)
            } else {
                Color(0xFFF59E0B)
            }

            drawCircle(
                color.copy(alpha = 0.20f),
                10.dp.toPx(),
                position
            )

            drawCircle(
                color,
                4.5.dp.toPx(),
                position
            )
        }

        // शुरुआत में बिखरे हुए कण।
        if (stage == 1) {
            for (i in 0 until 12) {
                val x = sx * (0.12f + ((i * 23) % 75) / 100f)
                val y = sy * (0.16f + ((i * 31) % 65) / 100f)

                drawCircle(
                    Color(0xFF67E8F9).copy(alpha = 0.7f),
                    3.dp.toPx(),
                    Offset(x, y)
                )
            }
        }
    }
}

private fun dpScale(
    scope: androidx.compose.ui.graphics.drawscope.DrawScope
): Float = scope.density * 1.0f

/* ---------------------------------------------------------
   ADVANCED OPEN SYSTEM LAB
   --------------------------------------------------------- */

@Composable
private fun OpenSystemLab(onBack: () -> Unit) {
    val context = LocalContext.current

    var state by remember {
        mutableStateOf(loadSystem(context, "system_state"))
    }

    var isRunning by remember { mutableStateOf(false) }
    var memoryInput by remember { mutableStateOf("") }
    var experimentName by remember { mutableStateOf("") }

    var experiments by remember {
        mutableStateOf(loadExperiments(context))
    }

    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(1000)
            state = advanceOpenSystem(state)
            saveSystem(context, "system_state", state)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← वापस")
        }

        Text(
            "Advanced Open System Lab",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Exchange, Refresh, Disturbance और Memory के मान बदलकर सिस्टम के व्यवहार को देखें।"
        )

        ResultMetric("Stability", state.stability)
        ResultMetric("Organization", state.organization)
        ResultMetric("Memory", state.memory)
        ResultMetric("Identity", state.identity)
        ResultMetric("Reality", state.reality)
        ResultMetric("Process Continuity", state.processContinuity)

        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "System Controls",
                    style = MaterialTheme.typography.titleMedium
                )

                ParameterSlider(
                    "Material Exchange",
                    state.exchange
                ) {
                    state = state.copy(exchange = it)
                }

                ParameterSlider(
                    "Refresh Rate",
                    state.refresh
                ) {
                    state = state.copy(refresh = it)
                }

                ParameterSlider(
                    "Memory Retention",
                    state.retention
                ) {
                    state = state.copy(retention = it)
                }

                ParameterSlider(
                    "Activity",
                    state.activity
                ) {
                    state = state.copy(activity = it)
                }

                ParameterSlider(
                    "Disturbance",
                    state.disturbance
                ) {
                    state = state.copy(disturbance = it)
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Memory Input",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = memoryInput,
                    onValueChange = { memoryInput = it },
                    label = { Text("कोई जानकारी लिखें") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (memoryInput.isNotBlank()) {
                            val item = MemoryItem(
                                id = System.currentTimeMillis(),
                                content = memoryInput.trim()
                            )

                            val updated = state.memories + item

                            state = state.copy(
                                memories = updated,
                                memory = averageMemory(updated, state.memory),
                                log = addLog(
                                    state,
                                    "नई जानकारी मेमोरी में जोड़ी गई।"
                                )
                            )

                            memoryInput = ""
                            saveSystem(context, "system_state", state)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Memory में जोड़ें")
                }

                Text("Stored Memories: ${state.memories.size}")

                state.memories.takeLast(8).forEach { item ->
                    Column {
                        Text(item.content)
                        Text(
                            "Strength: ${percent(item.strength)} | Age: ${item.age}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            if (item.accessible) "Available" else "Weak",
                            color = if (item.accessible) Green else Orange
                        )
                    }

                    HorizontalDivider()
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { isRunning = !isRunning },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isRunning) "Pause" else "Start")
            }

            OutlinedButton(
                onClick = {
                    state = advanceOpenSystem(state)
                    saveSystem(context, "system_state", state)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("One Step")
            }
        }

        OutlinedButton(
            onClick = {
                state = initialSystem()
                isRunning = false
                saveSystem(context, "system_state", state)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reset System")
        }

        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Save Experiment",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = experimentName,
                    onValueChange = { experimentName = it },
                    label = { Text("Experiment का नाम") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val record = ExperimentRecord(
                            name = experimentName.ifBlank {
                                "Experiment ${experiments.size + 1}"
                            },
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
                    Column {
                        Text(
                            record.name,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "Step ${record.step} | Stability ${percent(record.stability)} | Memory ${percent(record.memory)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    HorizontalDivider()
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Experiment Log",
                    style = MaterialTheme.typography.titleMedium
                )

                Text("Total Steps: ${state.step}")

                state.log.take(12).forEach {
                    Text("• $it")
                }
            }
        }
    }
}

/* ---------------------------------------------------------
   CONTINUITY AND TRANSFER LAB
   --------------------------------------------------------- */

@Composable
private fun ContinuityLab(onBack: () -> Unit) {
    var originalMemory by remember { mutableFloatStateOf(0.80f) }
    var copyMemory by remember { mutableFloatStateOf(0.80f) }

    var originalIdentity by remember { mutableFloatStateOf(0.90f) }
    var copyIdentity by remember { mutableFloatStateOf(0.90f) }

    var originalContinuity by remember { mutableFloatStateOf(1f) }
    var copyContinuity by remember { mutableFloatStateOf(0f) }

    var transferred by remember { mutableStateOf(false) }

    val similarity = clamp(
        (
            originalMemory +
                copyMemory +
                originalIdentity +
                copyIdentity
            ) / 4f
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
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← वापस")
        }

        Text(
            "Existence Continuity & Transfer Lab",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Original और Copy के बीच Memory, Identity और Continuity की तुलना करें।"
        )

        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Original System",
                    color = Green,
                    fontWeight = FontWeight.Bold
                )

                ParameterSlider(
                    "Original Memory",
                    originalMemory
                ) {
                    originalMemory = it
                }

                ParameterSlider(
                    "Original Identity",
                    originalIdentity
                ) {
                    originalIdentity = it
                }

                ParameterSlider(
                    "Original Continuity",
                    originalContinuity
                ) {
                    originalContinuity = it
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Copy / New System",
                    color = Blue,
                    fontWeight = FontWeight.Bold
                )

                ParameterSlider(
                    "Copy Memory",
                    copyMemory
                ) {
                    copyMemory = it
                }

                ParameterSlider(
                    "Copy Identity",
                    copyIdentity
                ) {
                    copyIdentity = it
                }

                ParameterSlider(
                    "Copy Continuity",
                    copyContinuity
                ) {
                    copyContinuity = it
                }
            }
        }

        ResultMetric("Memory & Identity Similarity", similarity)
        ResultMetric("Original Continuity Score", originalScore)
        ResultMetric("Copy Continuity Score", copyScore)

        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Transfer Experiment",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    if (transferred) {
                        "Transfer मॉडल चलाया गया। Original और Copy के मान अलग-अलग देखे जा सकते हैं।"
                    } else {
                        "Transfer शुरू करने पर Copy को Original की Memory और Identity के मान मिलेंगे। Continuity को अलग से नियंत्रित किया जा सकता है।"
                    }
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

        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Observation",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    "Memory और Identity के मान समान हो सकते हैं, जबकि Continuity का मान अलग हो सकता है। इस मॉडल में इन्हें अलग-अलग मापदंडों के रूप में रखा गया है।"
                )

                Text("Original Score: ${percent(originalScore)}")
                Text("Copy Score: ${percent(copyScore)}")
            }
        }
    }
}

/* ---------------------------------------------------------
   SHARED UI COMPONENTS
   --------------------------------------------------------- */

@Composable
private fun ParameterSlider(
    title: String,
    value: Float,
    onChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title)
            Text(percent(value), color = Blue)
        }

        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 0f..1f
        )
    }
}

@Composable
private fun ResultMetric(
    label: String,
    value: Float
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label)
                Text(
                    percent(value),
                    fontWeight = FontWeight.Bold
                )
            }

            LinearProgressIndicator(
                progress = { clamp(value) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ComparisonLine(
    label: String,
    original: Float,
    copy: Float
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, fontWeight = FontWeight.SemiBold)

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Original: ${percent(original)}")
            Text("Copy: ${percent(copy)}")
        }
    }
}
