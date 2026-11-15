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
    val accessible: Boolean = true,
    val refreshCount: Int = 0,
    val transferredFrom: List<Long> = emptyList()
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
    if (old.activity <= 0f) return old

    val nextStep = old.step + 1
    val exchangeEffect = old.exchange * 0.12f
    val refreshEffect = old.refresh * 0.10f
    val disturbanceEffect = old.disturbance * 0.12f
    val lossEffect = old.informationLoss * (1f - old.retention) * 0.10f

    // Memory records are internally refreshed only when the Refresh parameter is active.
    // With Refresh = 0, records remain unchanged during this simulation cycle.
    val newMemories = if (old.refresh > 0f) {
        old.memories.map { item ->
            val refreshGain = old.refresh * old.retention * 0.015f
            val strength = clamp(
                item.strength + refreshGain -
                    (old.informationLoss * 0.025f) -
                    (old.disturbance * 0.010f)
            )
            item.copy(
                strength = strength,
                age = item.age + 1,
                accessible = strength >= 0.20f,
                refreshCount = item.refreshCount + 1
            )
        }
    } else {
        old.memories
    }

    val newMemory = averageMemory(newMemories, old.memory)
    val newOrganization = clamp(
        old.organization + exchangeEffect + refreshEffect -
            disturbanceEffect - lossEffect
    )

    val newStability = clamp(
        old.stability +
            (old.refresh * 0.035f) +
            (old.exchange * 0.025f) +
            (newOrganization - old.organization) * 0.08f -
            (old.disturbance * 0.06f) -
            (old.informationLoss * 0.04f)
    )

    val newIdentity = clamp(
        old.identity + (newMemory * 0.025f) +
            (newStability * 0.015f) - (old.disturbance * 0.015f)
    )

    val newReality = clamp(
        old.reality + (old.exchange * 0.02f) +
            (old.refresh * 0.02f) + (newMemory * 0.015f)
    )

    return old.copy(
        step = nextStep,
        stability = newStability,
        memory = newMemory,
        organization = newOrganization,
        identity = newIdentity,
        reality = newReality,
        memories = newMemories,
        processContinuity = 1f,
        log = addLog(old, "चरण $nextStep: सिस्टम अपडेट हुआ।")
    )
}

private fun saveSystem(context: Context, key: String, state: SystemState) {
    val prefs = context.getSharedPreferences(
        "refresh_research_records", Context.MODE_PRIVATE
    )

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

        val logArray = JSONArray()
        state.log.forEach { logArray.put(it) }
        put("log", logArray)

        val memoryArray = JSONArray()
        state.memories.forEach { item ->
            memoryArray.put(JSONObject().apply {
                put("id", item.id)
                put("content", item.content)
                put("strength", item.strength.toDouble())
                put("age", item.age)
                put("accessible", item.accessible)
                put("refreshCount", item.refreshCount)
                val sourceArray = JSONArray()
                item.transferredFrom.forEach { sourceArray.put(it) }
                put("transferredFrom", sourceArray)
            })
        }
        put("memories", memoryArray)
    }

    prefs.edit().putString("system_$key", json.toString()).apply()
}

private fun parseSystem(jsonString: String?): SystemState? {
    if (jsonString.isNullOrBlank()) return null

    return try {
        val json = JSONObject(jsonString)
        val oldMemory = clamp(json.optDouble("memory", 0.20).toFloat())
        val memoryArray = json.optJSONArray("memories") ?: JSONArray()
        val memories = mutableListOf<MemoryItem>()

        for (i in 0 until memoryArray.length()) {
            val item = memoryArray.optJSONObject(i) ?: continue
            memories.add(
                MemoryItem(
                    id = item.optLong("id", System.currentTimeMillis() + i),
                    content = item.optString("content", "Saved information"),
                    strength = clamp(item.optDouble("strength", 0.5).toFloat()),
                    age = item.optInt("age", 0),
                    accessible = item.optBoolean("accessible", true),
                    refreshCount = item.optInt("refreshCount", 0),
                    transferredFrom = item.optJSONArray("transferredFrom")?.let { sources ->
                        buildList {
                            for (sourceIndex in 0 until sources.length()) {
                                add(sources.optLong(sourceIndex))
                            }
                        }
                    } ?: emptyList()
                )
            )
        }

        if (memories.isEmpty() && oldMemory > 0f) {
            memories.add(
                MemoryItem(
                    id = 1L,
                    content = "Previously stored information (legacy record)",
                    strength = oldMemory,
                    age = 0,
                    accessible = oldMemory >= 0.20f
                )
            )
        }

        val logArray = json.optJSONArray("log")
        val logs = mutableListOf<String>()
        if (logArray != null) {
            for (i in 0 until logArray.length()) logs.add(logArray.optString(i))
        }

        SystemState(
            step = json.optInt("step", 0),
            stability = clamp(json.optDouble("stability", 0.65).toFloat()),
            exchange = clamp(json.optDouble("exchange", 0.50).toFloat()),
            refresh = clamp(json.optDouble("refresh", 0.50).toFloat()),
            retention = clamp(json.optDouble("retention", 0.50).toFloat()),
            memory = averageMemory(memories, oldMemory),
            organization = clamp(json.optDouble("organization", 0.20).toFloat()),
            identity = clamp(json.optDouble("identity", 0.20).toFloat()),
            reality = clamp(json.optDouble("reality", 0.20).toFloat()),
            activity = clamp(json.optDouble("activity", 1.0).toFloat()),
            disturbance = clamp(json.optDouble("disturbance", 0.10).toFloat()),
            informationLoss = clamp(json.optDouble("informationLoss", 0.05).toFloat()),
            processContinuity = clamp(json.optDouble("processContinuity", 1.0).toFloat()),
            log = logs.ifEmpty { listOf("पुराना सिस्टम डेटा लोड हुआ।") },
            memories = memories
        )
    } catch (_: Exception) {
        null
    }
}

private fun loadSystem(context: Context, key: String): SystemState {
    val prefs = context.getSharedPreferences(
        "refresh_research_records", Context.MODE_PRIVATE
    )
    return parseSystem(prefs.getString("system_$key", null)) ?: initialSystem()
}

private fun saveExperiment(context: Context, record: ExperimentRecord) {
    val prefs = context.getSharedPreferences(
        "refresh_research_records", Context.MODE_PRIVATE
    )
    val oldArray = try {
        JSONArray(prefs.getString("experiment_history", "[]") ?: "[]")
    } catch (_: Exception) {
        JSONArray()
    }

    val newArray = JSONArray()
    newArray.put(JSONObject().apply {
        put("name", record.name)
        put("step", record.step)
        put("stability", record.stability.toDouble())
        put("memory", record.memory.toDouble())
        put("organization", record.organization.toDouble())
        put("identity", record.identity.toDouble())
        put("reality", record.reality.toDouble())
        put("inaccessibleRecords", record.inaccessibleRecords)
    })

    for (i in 0 until minOf(oldArray.length(), 99)) {
        val item = oldArray.optJSONObject(i) ?: continue
        newArray.put(item)
    }
    prefs.edit().putString("experiment_history", newArray.toString()).apply()
}

private fun loadExperiments(context: Context): List<ExperimentRecord> {
    val prefs = context.getSharedPreferences(
        "refresh_research_records", Context.MODE_PRIVATE
    )
    return try {
        val array = JSONArray(prefs.getString("experiment_history", "[]") ?: "[]")
        buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                add(
                    ExperimentRecord(
                        name = item.optString("name", "Experiment"),
                        step = item.optInt("step", 0),
                        stability = clamp(item.optDouble("stability", 0.0).toFloat()),
                        memory = clamp(item.optDouble("memory", 0.0).toFloat()),
                        organization = clamp(item.optDouble("organization", 0.0).toFloat()),
                        identity = clamp(item.optDouble("identity", 0.0).toFloat()),
                        reality = clamp(item.optDouble("reality", 0.0).toFloat()),
                        inaccessibleRecords = item.optInt("inaccessibleRecords", 0)
                    )
                )
            }
        }
    } catch (_: Exception) {
        emptyList()
    }
}

/* -------------------- APP NAVIGATION -------------------- */

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
                "open" -> OpenSystemLab(onBack = { selectedLab = "formation" })
                "continuity" -> ContinuityLab(onBack = { selectedLab = "home" })
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
        Text("Refresh System", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Refresh Theory पर आधारित एक experimental open-system model.",
            style = MaterialTheme.typography.bodyLarge
        )

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("इस ऐप का उद्देश्य", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "यह ऐप Open System, Information Exchange, Refresh Process, " +
                        "Stability, Memory और Identity जैसे विचारों को simulation " +
                        "के रूप में explore करने के लिए बनाया गया है।"
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().clickable { onOpenSystem() }
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "1. Open System Formation & Refresh Lab",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Atoms और Molecules से Pattern बनना, पुराने घटकों का बाहर जाना, " +
                        "नए घटकों का प्रवेश, Connections का Refresh और लंबे समय में Pattern Evolution."
                )
                Spacer(Modifier.height(10.dp))
                Button(onClick = onOpenSystem) { Text("Simulation शुरू करें") }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().clickable { onContinuity() }
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "2. Existence Continuity & Transfer Lab",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Original System और System Copy के बीच Memory, Identity, " +
                        "Development तथा Process Continuity की तुलना."
                )
                Spacer(Modifier.height(10.dp))
                Button(onClick = onContinuity) { Text("Continuity Lab खोलें") }
            }
        }
    }
}

/* -------------------- UNIFIED OPEN SYSTEM / REFRESH / EVOLUTION SIMULATION -------------------- */

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
        FormationAtom(id = index + 1, slot = index, microPhase = index * 0.37f)
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
    val atoms = old.atoms.ifEmpty { makeInitialFormation().atoms }.toMutableList()
    val boundary = old.boundaryParticles.mapNotNull { particle ->
        val p = particle.progress + 0.28f
        if (p >= 1f) null else particle.copy(progress = p)
    }.toMutableList()

    var totalIn = old.totalIn
    var totalOut = old.totalOut
    var bondChanges = old.bondChanges
    val refreshFraction = old.refreshPercent.coerceIn(0.01f, 0.50f)
    val exchangeCount = (FORMATION_SLOTS * refreshFraction).toInt().coerceAtLeast(1)

    // Once a pattern has started forming, only a fraction of particles cross the boundary
    // in each cycle. The rest still move and rearrange internally.
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

    // Every particle participates in internal refresh, including those that do not cross
    // the system boundary. This changes positions and bonds throughout the pattern.
    val movedAtoms = atoms.map { atom ->
        atom.copy(microPhase = atom.microPhase + old.internalMovement * 0.34f + 0.08f)
    }
    bondChanges += if (stage >= 2) (2 + (old.internalMovement * 5).toInt()) else 0

    val evolution = if (stage >= 4) {
        (old.evolution + 0.0028f * (0.35f + old.environment)).coerceAtMost(1f)
    } else 0f

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

    // Two nearby possible successor patterns. Environment biases the direction;
    // the system-rule tendency favors the T-like path in this illustrative model.
    val tPattern = (0 until FORMATION_SLOTS).map { index ->
        val t = index.toFloat() / (FORMATION_SLOTS - 1)
        val x = 0.12f + 0.76f * t
        val y = if (t < 0.5f) 0.36f + 0.30f * (t / 0.5f)
        else 0.66f - 0.30f * ((t - 0.5f) / 0.5f)
        x to y
    }
    val bPattern = (0 until FORMATION_SLOTS).map { index ->
        val t = index.toFloat() / (FORMATION_SLOTS - 1)
        val x = 0.12f + 0.76f * t
        val y = 0.50f + 0.20f * kotlin.math.sin(t * Math.PI * 2.0).toFloat()
        x to y
    }
    val environmentBias = environment.coerceIn(0f, 1f)
    val nextPattern = if (environmentBias < 0.5f) tPattern else bPattern
    val formProgress = when {
        step < 12 -> 0f
        step < 25 -> ((step - 12) / 13f).coerceIn(0f, 1f)
        else -> 1f
    }
    val evolveAmount = evolution.coerceIn(0f, 1f)

    return start.indices.map { i ->
        val scattered = start[i]
        val formed = w[i]
        val evolved = nextPattern[i]
        val x1 = scattered.first + (formed.first - scattered.first) * formProgress
        val y1 = scattered.second + (formed.second - scattered.second) * formProgress
        (x1 + (evolved.first - x1) * evolveAmount) to
            (y1 + (evolved.second - y1) * evolveAmount)
    }
}

@Composable
private fun OpenSystemFormationLab(
    onBack: () -> Unit,
    onAdvancedLab: () -> Unit
) {
    var simulation by remember { mutableStateOf(makeInitialFormation()) }
    var running by remember { mutableStateOf(true) }

    LaunchedEffect(running, simulation.speed) {
        while (running) {
            delay((650f / simulation.speed.coerceIn(0.4f, 3f)).toLong().coerceAtLeast(120L))
            simulation = nextFormationState(simulation)
        }
    }

    val stage = formationStage(simulation.step)
    val patternName = when {
        stage < 2 -> "अभी Pattern नहीं बना"
        stage < 4 -> "W-पैटर्न"
        simulation.environment < 0.5f -> "W से T की दिशा"
        else -> "W से B की दिशा"
    }
    val currentCount = simulation.atoms.size

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(onClick = onBack) { Text("← वापस") }
        Text("Open System: Refresh से Evolution तक", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("पूरी प्रक्रिया एक ही लगातार चलने वाले Simulation में देखें।", color = MaterialTheme.colorScheme.onSurfaceVariant)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101B29))
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("LIVE SIMULATION", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("चरण $stage / 4", color = Color(0xFF67E8F9))
                }
                FormationCanvas(
                    state = simulation,
                    modifier = Modifier.fillMaxWidth().height(280.dp)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    FormationLegend(Color(0xFF38BDF8), "नया प्रवेश")
                    FormationLegend(Color(0xFFF59E0B), "बाहर निकास")
                    FormationLegend(Color(0xFF4ADE80), "अंदर के कण")
                }
                Text(simulation.lastEvent, color = Color.White, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("प्रक्रिया की Timeline", style = MaterialTheme.typography.titleMedium)
                val stages = listOf(
                    "1. कणों की गति और Entry / Exit",
                    "2. कणों के जुड़ने से W-पैटर्न बनना",
                    "3. W-पैटर्न का लगातार Refresh होना",
                    "4. लंबे समय में Pattern Evolution"
                )
                stages.forEachIndexed { index, label ->
                    val active = stage == index + 1
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (active) "●" else "○", color = if (active) Color(0xFF22C55E) else MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(8.dp))
                        Text(label, color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("अभी सिस्टम में क्या हो रहा है?", style = MaterialTheme.typography.titleMedium)
                Text("वर्तमान Pattern: $patternName", fontWeight = FontWeight.SemiBold)
                Text("सिस्टम के अंदर कण: $currentCount")
                Text("कुल अंदर आए: ${simulation.totalIn}    |    कुल बाहर गए: ${simulation.totalOut}")
                Text("बदले / Refresh हुए Bonds: ${simulation.bondChanges}")
                Text("Pattern Evolution: ${(simulation.evolution * 100).toInt()}%")
                LinearProgressIndicator(progress = { simulation.evolution }, modifier = Modifier.fillMaxWidth())
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Simulation Controls", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { running = !running }, modifier = Modifier.weight(1f)) {
                        Text(if (running) "Pause" else "Play")
                    }
                    OutlinedButton(onClick = { simulation = nextFormationState(simulation) }, modifier = Modifier.weight(1f)) {
                        Text("एक Cycle")
                    }
                }
                Text("Boundary Exchange: ${(simulation.refreshPercent * 100).toInt()}%")
                Slider(
                    value = simulation.refreshPercent,
                    onValueChange = { simulation = simulation.copy(refreshPercent = it) },
                    valueRange = 0.01f..0.20f
                )
                Text("Internal Movement: ${(simulation.internalMovement * 100).toInt()}%")
                Slider(
                    value = simulation.internalMovement,
                    onValueChange = { simulation = simulation.copy(internalMovement = it) },
                    valueRange = 0.05f..1f
                )
                Text("Environment Influence: ${(simulation.environment * 100).toInt()}%")
                Slider(
                    value = simulation.environment,
                    onValueChange = { simulation = simulation.copy(environment = it) },
                    valueRange = 0f..1f
                )
                Text("Simulation Speed: ${String.format(Locale.getDefault(), "%.1f", simulation.speed)}x")
                Slider(
                    value = simulation.speed,
                    onValueChange = { simulation = simulation.copy(speed = it) },
                    valueRange = 0.4f..3f
                )
                OutlinedButton(
                    onClick = { running = false; simulation = makeInitialFormation() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("पूरी प्रक्रिया फिर से शुरू करें") }
                OutlinedButton(onClick = onAdvancedLab, modifier = Modifier.fillMaxWidth()) {
                    Text("Advanced Open System Lab खोलें")
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("आसान भाषा में समझें", style = MaterialTheme.typography.titleMedium)
                Text("शुरुआत में कण इधर-उधर गति करते हैं। कुछ अंदर आते हैं और कुछ बाहर जाते हैं।")
                Text("धीरे-धीरे कण जुड़ते हैं और W-पैटर्न बनता है।")
                Text("W-पैटर्न बनने के बाद भी पूरा Pattern Refresh होता रहता है। कुछ कण सीमा पार करते हैं, जबकि बाकी कण अंदर ही गति करते हैं और अपने संबंध बदलते हैं।")
                Text("इस Refresh के बावजूद कुछ समय तक W-पैटर्न पहचान में बना रहता है। लंबे समय में बदलाव जमा होकर नया मिलता-जुलता Pattern बना सकते हैं।")
            }
        }
    }
}

@Composable
private fun FormationLegend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(10.dp)) { drawCircle(color = color) }
        Spacer(Modifier.width(5.dp))
        Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun FormationCanvas(
    state: FormationState,
    modifier: Modifier = Modifier
) {
    Canvas(modifier.background(Color(0xFF0B1220), RoundedCornerShape(16.dp))) {
        val points = formationPoints(state.evolution, state.environment, state.step)
        val sx = size.width
        val sy = size.height
        val stage = formationStage(state.step)
        val centers = points.map { Offset(it.first * sx, it.second * sy) }

        // Boundary of the open system
        drawRoundRect(
            color = Color(0xFF64748B).copy(alpha = 0.55f),
            topLeft = Offset(sx * 0.07f, sy * 0.10f),
            size = androidx.compose.ui.geometry.Size(sx * 0.86f, sy * 0.80f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()),
            style = Stroke(width = 1.dp.toPx())
        )

        // Bonds: their appearance subtly changes continuously during internal refresh.
        if (stage >= 2) {
            for (i in 0 until centers.lastIndex) {
                val active = (state.step + i) % 6 != 0
                drawLine(
                    color = if (active) Color(0xFF38BDF8).copy(alpha = 0.78f) else Color(0xFF64748B).copy(alpha = 0.25f),
                    start = centers[i],
                    end = centers[i + 1],
                    strokeWidth = if (active) 2.2.dp.toPx() else 1.dp.toPx()
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

        // All particles move internally, not only the ones crossing the boundary.
        state.atoms.forEach { atom ->
            val target = centers[atom.slot]
            val phase = atom.microPhase + state.step * 0.12f
            val movement = state.internalMovement * (1.2f + (atom.slot % 4) * 0.35f) * dpScale(this)
            val x = target.x + kotlin.math.sin(phase.toDouble()).toFloat() * movement
            val y = target.y + kotlin.math.cos((phase * 1.3f).toDouble()).toFloat() * movement
            val point = Offset(x, y)
            drawCircle(Color(0xFF1E293B), 8.dp.toPx(), point)
            drawCircle(Color(0xFF4ADE80), 4.dp.toPx(), point)
        }

        // Entry / exit particles travel across the boundary at the same time.
        state.boundaryParticles.forEach { particle ->
            val target = centers[particle.slot]
            val p = particle.progress.coerceIn(0f, 1f)
            val edgeX = if (particle.entering) -sx * 0.04f else sx * 1.04f
            val start = if (particle.entering) Offset(edgeX, target.y) else target
            val end = if (particle.entering) target else Offset(edgeX, target.y)
            val position = Offset(
                start.x + (end.x - start.x) * p,
                start.y + (end.y - start.y) * p
            )
            val color = if (particle.entering) Color(0xFF38BDF8) else Color(0xFFF59E0B)
            drawCircle(color.copy(alpha = 0.20f), 10.dp.toPx(), position)
            drawCircle(color, 4.5.dp.toPx(), position)
        }

        // During the first stage, show scattered particles before the W structure forms.
        if (stage == 1) {
            for (i in 0 until 12) {
                val x = sx * (0.12f + ((i * 23) % 75) / 100f)
                val y = sy * (0.16f + ((i * 31) % 65) / 100f)
                drawCircle(Color(0xFF67E8F9).copy(alpha = 0.7f), 3.dp.toPx(), Offset(x, y))
            }
        }
    }
}

private fun dpScale(scope: androidx.compose.ui.graphics.drawscope.DrawScope): Float =
    scope.density * 1.0f
/* -------------------- EXISTING OPEN SYSTEM LAB -------------------- */

@Composable
private fun OpenSystemLab(onBack: () -> Unit) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(loadSystem(context, "open_system")) }
    var running by remember { mutableStateOf(false) }
    var information by remember { mutableStateOf("") }
    var retrievalCue by remember { mutableStateOf("") }
    var transferCue by remember { mutableStateOf("") }
    var experimentName by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("सिस्टम तैयार है।") }
    var experiments by remember { mutableStateOf(loadExperiments(context)) }

    LaunchedEffect(running) {
        while (running) {
            delay(1200)
            if (running && state.activity > 0f) {
                state = advanceOpenSystem(state)
                saveSystem(context, "open_system", state)
                status = "सिस्टम का नया refresh cycle पूरा हुआ।"
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(onClick = onBack) { Text("← वापस") }
        Text("Advanced Open System Lab", style = MaterialTheme.typography.headlineSmall)
        Text("यहाँ parameters बदलकर सिस्टम के व्यवहार को observe करें।")

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("System Status", style = MaterialTheme.typography.titleMedium)
                Text("Step: ${state.step}")
                Text("Activity: ${if (state.activity > 0f) "Active" else "Stopped"}")
                Text("Status: $status")
                Text("Stored Memory Records: ${state.memories.size}")
                Text("Inaccessible Records: ${inaccessibleCount(state)}")
                Spacer(Modifier.height(8.dp))
                ResultMetric("Stability", state.stability)
                ResultMetric("Memory", state.memory)
                ResultMetric("Organization", state.organization)
                ResultMetric("Identity Indicator", state.identity)
                ResultMetric("Internal Reality Indicator", state.reality)
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("System Parameters", style = MaterialTheme.typography.titleMedium)
                ParameterSlider("Information Exchange", state.exchange) {
                    state = state.copy(exchange = it); saveSystem(context, "open_system", state)
                }
                ParameterSlider("Refresh Rate", state.refresh) {
                    state = state.copy(refresh = it); saveSystem(context, "open_system", state)
                }
                ParameterSlider("Information Retention", state.retention) {
                    state = state.copy(retention = it); saveSystem(context, "open_system", state)
                }
                ParameterSlider("Disturbance", state.disturbance) {
                    state = state.copy(disturbance = it); saveSystem(context, "open_system", state)
                }
                ParameterSlider("Information Loss", state.informationLoss) {
                    state = state.copy(informationLoss = it); saveSystem(context, "open_system", state)
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("Information Exchange", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = information,
                    onValueChange = { information = it },
                    label = { Text("कोई जानकारी लिखें") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val text = information.trim()
                        if (text.isNotEmpty()) {
                            val newItem = MemoryItem(
                                id = System.currentTimeMillis(),
                                content = text,
                                strength = 1f,
                                age = 0,
                                accessible = true
                            )
                            val updatedMemories = state.memories + newItem
                            state = state.copy(
                                memories = updatedMemories,
                                memory = averageMemory(updatedMemories, state.memory),
                                exchange = clamp(state.exchange + 0.03f),
                                organization = clamp(state.organization + 0.02f),
                                log = addLog(state, "नई जानकारी सिस्टम में प्राप्त हुई।")
                            )
                            saveSystem(context, "open_system", state)
                            information = ""
                            status = "नई जानकारी Memory Record में सेव हुई।"
                        } else status = "पहले जानकारी लिखें।"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Receive Information") }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("Memory Retrieval", style = MaterialTheme.typography.titleMedium)
                Text(
                    "किसी शब्द या वाक्यांश से पहले से सेव जानकारी खोजें। यह एक सरल cue-based simulation है।",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = retrievalCue,
                    onValueChange = { retrievalCue = it },
                    label = { Text("Search cue") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val cue = retrievalCue.trim()
                        if (cue.isEmpty()) {
                            status = "जानकारी खोजने के लिए कोई शब्द लिखें।"
                        } else {
                            val matches = state.memories.filter {
                                it.content.contains(cue, ignoreCase = true)
                            }
                            if (matches.isNotEmpty()) {
                                val updated = state.memories.map { item ->
                                    if (matches.any { it.id == item.id }) {
                                        item.copy(strength = clamp(item.strength + 0.15f), accessible = true)
                                    } else item
                                }
                                state = state.copy(
                                    memories = updated,
                                    memory = averageMemory(updated, state.memory),
                                    log = addLog(state, "Cue-based retrieval: ${matches.size} record(s) मिले।")
                                )
                                status = "${matches.size} matching record(s) मिले।"
                            } else status = "इस cue से कोई matching record नहीं मिला।"
                            saveSystem(context, "open_system", state)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Retrieve Information") }

                state.memories.take(8).forEach { item ->
                    HorizontalDivider()
                    Text(item.content, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Strength: ${(item.strength * 100).toInt()}% • Age: ${item.age} cycles • " +
                            "Refreshes: ${item.refreshCount} • " +
                            if (item.accessible) "Accessible" else "Currently inaccessible",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("Information Transfer", style = MaterialTheme.typography.titleMedium)
                Text(
                    "यह प्रयोग चुने गए पुराने Memory Records की जानकारी को एक नए record में जोड़ता है। " +
                        "मूल records सुरक्षित रहते हैं; नया record उनके source IDs रखता है।",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = transferCue,
                    onValueChange = { transferCue = it },
                    label = { Text("Transfer के लिए search cue") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val cue = transferCue.trim()
                        if (cue.isEmpty()) {
                            status = "Transfer के लिए कोई cue लिखें।"
                        } else {
                            val sources = state.memories.filter {
                                it.accessible && it.content.contains(cue, ignoreCase = true)
                            }
                            if (sources.isEmpty()) {
                                status = "इस cue से कोई accessible source record नहीं मिला।"
                            } else {
                                val transferredText = sources
                                    .joinToString(" | ") { it.content }
                                val transferredRecord = MemoryItem(
                                    id = System.currentTimeMillis(),
                                    content = "Transferred information: $transferredText",
                                    strength = clamp(sources.map { it.strength }.average().toFloat()),
                                    age = 0,
                                    accessible = true,
                                    refreshCount = 0,
                                    transferredFrom = sources.map { it.id }
                                )
                                val updated = state.memories + transferredRecord
                                state = state.copy(
                                    memories = updated,
                                    memory = averageMemory(updated, state.memory),
                                    log = addLog(
                                        state,
                                        "Information transfer: ${sources.size} source record(s) से नया record बना।"
                                    )
                                )
                                saveSystem(context, "open_system", state)
                                status = "${sources.size} source record(s) से information transfer हुआ।"
                                transferCue = ""
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Transfer Information") }

                state.memories.filter { it.transferredFrom.isNotEmpty() }.take(5).forEach { item ->
                    HorizontalDivider()
                    Text(item.content, style = MaterialTheme.typography.bodySmall)
                    Text(
                        "New ID: ${item.id} • Sources: ${item.transferredFrom.joinToString()}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("Memory Updating & Forgetting", style = MaterialTheme.typography.titleMedium)
                Text("इस प्रयोग में memory records की strength घटाई जा सकती है। Record को मिटाने के बजाय उसकी accessibility कम की जाती है।")
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val updated = state.memories.map { item ->
                            val strength = clamp(item.strength - 0.25f)
                            item.copy(strength = strength, accessible = strength >= 0.20f)
                        }
                        state = state.copy(
                            memories = updated,
                            memory = averageMemory(updated, state.memory),
                            log = addLog(state, "Memory weakening प्रयोग किया गया।")
                        )
                        saveSystem(context, "open_system", state)
                        status = "Memory strength घटाई गई।"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Reduce Memory Strength") }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("Simulation Controls", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (state.activity > 0f) {
                                running = true
                                status = "Simulation चल रहा है।"
                            } else status = "Zero State के बाद Reset करें।"
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Start") }
                    Button(
                        onClick = { running = false; status = "Simulation pause किया गया।" },
                        modifier = Modifier.weight(1f)
                    ) { Text("Pause") }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        running = false
                        state = state.copy(
                            activity = 0f,
                            processContinuity = 0f,
                            log = addLog(state, "Zero State: simulation activity रोक दी गई।")
                        )
                        saveSystem(context, "open_system", state)
                        status = "Zero State: model activity बंद है।"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Enter Zero State") }
                OutlinedButton(
                    onClick = {
                        running = false
                        state = initialSystem()
                        saveSystem(context, "open_system", state)
                        status = "Simulation reset हो गया।"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Reset Current Simulation") }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("Save Experiment", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = experimentName,
                    onValueChange = { experimentName = it },
                    label = { Text("Experiment का नाम") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val name = experimentName.trim().ifEmpty { "Experiment ${experiments.size + 1}" }
                        val record = ExperimentRecord(
                            name = name,
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
                        status = "Experiment record सेव हो गया।"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save Current Observation") }

                if (experiments.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Saved Experiments", style = MaterialTheme.typography.titleSmall)
                    experiments.take(10).forEach { record ->
                        HorizontalDivider()
                        Text(record.name, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Step ${record.step} • Stability ${(record.stability * 100).toInt()}% • " +
                                "Memory ${(record.memory * 100).toInt()}% • Inaccessible: ${record.inaccessibleRecords}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (experiments.size >= 2) {
                        val first = experiments[1]
                        val second = experiments[0]
                        HorizontalDivider()
                        Text("Latest Two Experiments Comparison", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Stability difference: ${String.format(Locale.getDefault(), "%.1f", (second.stability - first.stability) * 100)} percentage points"
                        )
                        Text(
                            "Memory difference: ${String.format(Locale.getDefault(), "%.1f", (second.memory - first.memory) * 100)} percentage points"
                        )
                    }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("System Log", style = MaterialTheme.typography.titleMedium)
                state.log.take(15).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

/* -------------------- EXISTING CONTINUITY LAB -------------------- */

@Composable
private fun ContinuityLab(onBack: () -> Unit) {
    val context = LocalContext.current
    var original by remember { mutableStateOf(loadSystem(context, "continuity_original")) }
    var copy by remember { mutableStateOf(loadSystem(context, "continuity_copy")) }
    var hypothesis by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Continuity Lab तैयार है।") }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(onClick = onBack) { Text("← वापस") }
        Text("Existence Continuity & Transfer Lab", style = MaterialTheme.typography.headlineSmall)
        Text("Original System और उसकी Copy के बीच अलग-अलग विकास और source-process link की तुलना।")

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("Original System", style = MaterialTheme.typography.titleLarge)
                Text("Step: ${original.step}")
                ResultMetric("Stability", original.stability)
                ResultMetric("Memory", original.memory)
                ResultMetric("Organization", original.organization)
                ResultMetric("Identity Indicator", original.identity)
                Text("Source Process Link: 100%")
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        original = advanceOpenSystem(original).copy(processContinuity = 1f)
                        saveSystem(context, "continuity_original", original)
                        status = "Original System आगे बढ़ा।"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Update Original System") }
                OutlinedButton(
                    onClick = {
                        original = initialSystem()
                        saveSystem(context, "continuity_original", original)
                        status = "Original System reset हुआ।"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Reset Original") }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("System Copy", style = MaterialTheme.typography.titleLarge)
                Text("Step: ${copy.step}")
                ResultMetric("Stability", copy.stability)
                ResultMetric("Memory", copy.memory)
                ResultMetric("Organization", copy.organization)
                ResultMetric("Identity Indicator", copy.identity)
                Text("Source Process Link: ${(copy.processContinuity * 100).toInt()}%")
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        copy = advanceOpenSystem(copy).copy(processContinuity = 0f)
                        saveSystem(context, "continuity_copy", copy)
                        status = "Copy ने अपना अलग update cycle पूरा किया।"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Update Copy Independently") }
                OutlinedButton(
                    onClick = {
                        copy = initialSystem().copy(processContinuity = 0f)
                        saveSystem(context, "continuity_copy", copy)
                        status = "Copy reset हुई।"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Reset Copy") }
                OutlinedButton(
                    onClick = {
                        copy = original.copy(
                            processContinuity = 0f,
                            log = addLog(original, "Original System से नई Copy बनाई गई।")
                        )
                        saveSystem(context, "continuity_copy", copy)
                        status = "Original System की वर्तमान स्थिति से Copy बनाई गई।"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Create Copy From Original") }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("Comparison", style = MaterialTheme.typography.titleLarge)
                Text("Original और Copy की वर्तमान model values का अंतर:")
                ComparisonLine("Stability", original.stability, copy.stability)
                ComparisonLine("Memory", original.memory, copy.memory)
                ComparisonLine("Organization", original.organization, copy.organization)
                ComparisonLine("Identity Indicator", original.identity, copy.identity)
                ComparisonLine("Internal Reality", original.reality, copy.reality)
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("Transfer Hypothesis Notes", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = hypothesis,
                    onValueChange = { hypothesis = it },
                    label = { Text("अपनी research observation लिखें") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        context.getSharedPreferences(
                            "refresh_research_records", Context.MODE_PRIVATE
                        ).edit().putString("observation_transfer_hypothesis", hypothesis).apply()
                        status = "Research observation सेव हो गई।"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save Research Observation") }

                val savedNote = context.getSharedPreferences(
                    "refresh_research_records", Context.MODE_PRIVATE
                ).getString("observation_transfer_hypothesis", "") ?: ""
                if (savedNote.isNotBlank()) {
                    HorizontalDivider()
                    Text("Saved Observation", style = MaterialTheme.typography.titleSmall)
                    Text(savedNote)
                }
            }
        }
        Text("Status: $status", style = MaterialTheme.typography.bodyMedium)
    }
}

/* -------------------- SHARED UI HELPERS -------------------- */

@Composable
private fun ParameterSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Text("$label: ${(value * 100).toInt()}%")
        Slider(value = value, onValueChange = onValueChange, valueRange = 0f..1f)
    }
}

@Composable
private fun ResultMetric(label: String, value: Float) {
    Column(Modifier.fillMaxWidth()) {
        Text("$label: ${(value * 100).toInt()}%")
        LinearProgressIndicator(
            progress = { clamp(value) },
            modifier = Modifier.fillMaxWidth().height(7.dp)
        )
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun ComparisonLine(label: String, original: Float, copy: Float) {
    Column(Modifier.fillMaxWidth()) {
        Text("$label — Original: ${(original * 100).toInt()}% | Copy: ${(copy * 100).toInt()}%")
        Spacer(Modifier.height(4.dp))
    }
}
