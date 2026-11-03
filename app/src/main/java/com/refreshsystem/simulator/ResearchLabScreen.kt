
package com.refreshsystem.simulator

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

private fun inaccessibleCount(state: SystemState): Int {
    return state.memories.count { !it.accessible }
}

private fun addLog(state: SystemState, message: String): List<String> {
    return (listOf(message) + state.log).take(50)
}

private fun advanceOpenSystem(old: SystemState): SystemState {
    if (old.activity <= 0f) return old

    val nextStep = old.step + 1
    val exchangeEffect = old.exchange * 0.12f
    val refreshEffect = old.refresh * 0.10f
    val disturbanceEffect = old.disturbance * 0.12f
    val lossEffect = old.informationLoss * (1f - old.retention) * 0.10f

    val newMemories = old.memories.map { item ->
        val newStrength = clamp(
            item.strength +
                (old.retention * 0.015f) -
                (old.informationLoss * 0.025f) -
                (old.disturbance * 0.010f)
        )
        item.copy(
            strength = newStrength,
            age = item.age + 1,
            accessible = newStrength >= 0.20f
        )
    }

    val newMemory = averageMemory(newMemories, old.memory)
    val newOrganization = clamp(
        old.organization +
            exchangeEffect +
            refreshEffect -
            disturbanceEffect -
            lossEffect
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
        old.identity +
            (newMemory * 0.025f) +
            (newStability * 0.015f) -
            (old.disturbance * 0.015f)
    )

    val newReality = clamp(
        old.reality +
            (old.exchange * 0.02f) +
            (old.refresh * 0.02f) +
            (newMemory * 0.015f)
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
        "refresh_research_records",
        Context.MODE_PRIVATE
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
            memoryArray.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("content", item.content)
                    put("strength", item.strength.toDouble())
                    put("age", item.age)
                    put("accessible", item.accessible)
                }
            )
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
                    accessible = item.optBoolean("accessible", true)
                )
            )
        }

        // पुराने वर्जन में केवल memory की संख्या सेव होती थी।
        // उस पुराने डेटा को नए Memory Record में बदला जा रहा है।
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
            for (i in 0 until logArray.length()) {
                logs.add(logArray.optString(i))
            }
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
        "refresh_research_records",
        Context.MODE_PRIVATE
    )
    return parseSystem(prefs.getString("system_$key", null)) ?: initialSystem()
}

private fun saveExperiment(context: Context, record: ExperimentRecord) {
    val prefs = context.getSharedPreferences(
        "refresh_research_records",
        Context.MODE_PRIVATE
    )

    val oldArray = try {
        JSONArray(prefs.getString("experiment_history", "[]") ?: "[]")
    } catch (_: Exception) {
        JSONArray()
    }

    val newArray = JSONArray()
    newArray.put(
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

    for (i in 0 until minOf(oldArray.length(), 99)) {
        val item = oldArray.optJSONObject(i) ?: continue
        newArray.put(item)
    }

    prefs.edit().putString("experiment_history", newArray.toString()).apply()
}

private fun loadExperiments(context: Context): List<ExperimentRecord> {
    val prefs = context.getSharedPreferences(
        "refresh_research_records",
        Context.MODE_PRIVATE
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

@Composable
fun ResearchHomeScreen() {
    var selectedLab by remember { mutableStateOf("home") }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (selectedLab) {
                "open" -> OpenSystemLab(onBack = { selectedLab = "home" })
                "continuity" -> ContinuityLab(onBack = { selectedLab = "home" })
                else -> MainResearchHome(
                    onOpenSystem = { selectedLab = "open" },
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

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("इस ऐप का उद्देश्य", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "यह ऐप Open System, Information Exchange, Refresh Process, " +
                        "Stability, Memory और Identity जैसे विचारों को एक simulation " +
                        "के रूप में जांचने के लिए बनाया गया है।"
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "महत्वपूर्ण: Refresh Theory एक परिकल्पना है। यह simulation " +
                        "चेतना या चेतना के transfer को वैज्ञानिक रूप से सिद्ध नहीं करता।",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenSystem() }
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("1. Open System Simulation Lab", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    "System Formation, Information Exchange, Refresh Process, " +
                        "Dynamic Stability, Memory Formation, Memory Updating, " +
                        "Identity और Internal Reality का simulation."
                )
                Spacer(Modifier.height(10.dp))
                Button(onClick = onOpenSystem) {
                    Text("Open System Lab खोलें")
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
                    "2. Existence Continuity & Transfer Lab",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Original System और System Copy के बीच Memory, Identity, " +
                        "Development तथा Process Continuity की तुलना."
                )
                Spacer(Modifier.height(10.dp))
                Button(onClick = onContinuity) {
                    Text("Continuity Lab खोलें")
                }
            }
        }
    }
}

@Composable
private fun OpenSystemLab(onBack: () -> Unit) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(loadSystem(context, "open_system")) }
    var running by remember { mutableStateOf(false) }
    var information by remember { mutableStateOf("") }
    var retrievalCue by remember { mutableStateOf("") }
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
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(onClick = onBack) { Text("← वापस") }
        Text("Open System Simulation Lab", style = MaterialTheme.typography.headlineSmall)
        Text(
            "यहाँ parameters बदलकर सिस्टम के व्यवहार को observe करें।",
            style = MaterialTheme.typography.bodyMedium
        )

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
                    state = state.copy(exchange = it)
                    saveSystem(context, "open_system", state)
                }
                ParameterSlider("Refresh Rate", state.refresh) {
                    state = state.copy(refresh = it)
                    saveSystem(context, "open_system", state)
                }
                ParameterSlider("Information Retention", state.retention) {
                    state = state.copy(retention = it)
                    saveSystem(context, "open_system", state)
                }
                ParameterSlider("Disturbance", state.disturbance) {
                    state = state.copy(disturbance = it)
                    saveSystem(context, "open_system", state)
                }
                ParameterSlider("Information Loss", state.informationLoss) {
                    state = state.copy(informationLoss = it)
                    saveSystem(context, "open_system", state)
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
                        } else {
                            status = "पहले जानकारी लिखें।"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Receive Information")
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("Memory Retrieval", style = MaterialTheme.typography.titleMedium)
                Text(
                    "किसी शब्द या वाक्यांश से पहले से सेव जानकारी खोजें। " +
                        "यह एक सरल cue-based simulation है।",
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
                                        item.copy(
                                            strength = clamp(item.strength + 0.15f),
                                            accessible = true
                                        )
                                    } else item
                                }
                                state = state.copy(
                                    memories = updated,
                                    memory = averageMemory(updated, state.memory),
                                    log = addLog(
                                        state,
                                        "Cue-based retrieval: ${matches.size} record(s) मिले।"
                                    )
                                )
                                status = "${matches.size} matching record(s) मिले।"
                            } else {
                                status = "इस cue से कोई matching record नहीं मिला।"
                            }
                            saveSystem(context, "open_system", state)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Retrieve Information")
                }

                state.memories.take(8).forEach { item ->
                    Divider()
                    Text(
                        item.content,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Strength: ${(item.strength * 100).toInt()}% • " +
                            "Age: ${item.age} cycles • " +
                            if (item.accessible) "Accessible" else "Currently inaccessible",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("Memory Updating & Forgetting", style = MaterialTheme.typography.titleMedium)
                Text(
                    "इस प्रयोग में memory records की strength घटाई जा सकती है। " +
                        "Record को मिटाने के बजाय उसकी accessibility कम की जाती है।"
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val updated = state.memories.map { item ->
                            val strength = clamp(item.strength - 0.25f)
                            item.copy(
                                strength = strength,
                                accessible = strength >= 0.20f
                            )
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
                ) {
                    Text("Reduce Memory Strength")
                }
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
                            } else {
                                status = "Zero State के बाद Reset करें।"
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Start") }

                    Button(
                        onClick = {
                            running = false
                            status = "Simulation pause किया गया।"
                        },
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
                ) {
                    Text("Enter Zero State")
                }
                OutlinedButton(
                    onClick = {
                        running = false
                        state = initialSystem()
                        saveSystem(context, "open_system", state)
                        status = "Simulation reset हो गया।"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset Current Simulation")
                }
                Text(
                    "Zero State यहाँ केवल simulation activity बंद होने का नाम है। " +
                        "यह वास्तविक जीवन, मृत्यु या चेतना के बारे में कोई प्रमाण नहीं है।",
                    style = MaterialTheme.typography.bodySmall
                )
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
                        val name = experimentName.trim().ifEmpty {
                            "Experiment ${experiments.size + 1}"
                        }
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
                ) {
                    Text("Save Current Observation")
                }

                if (experiments.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Saved Experiments", style = MaterialTheme.typography.titleSmall)
                    experiments.take(10).forEach { record ->
                        Divider()
                        Text(record.name, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Step ${record.step} • Stability ${(record.stability * 100).toInt()}% • " +
                                "Memory ${(record.memory * 100).toInt()}% • " +
                                "Inaccessible: ${record.inaccessibleRecords}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (experiments.size >= 2) {
                        val first = experiments[1]
                        val second = experiments[0]
                        Divider()
                        Text("Latest Two Experiments Comparison", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Stability difference: ${
                                String.format(
                                    Locale.getDefault(),
                                    "%.1f",
                                    (second.stability - first.stability) * 100
                                )
                            } percentage points"
                        )
                        Text(
                            "Memory difference: ${
                                String.format(
                                    Locale.getDefault(),
                                    "%.1f",
                                    (second.memory - first.memory) * 100
                                )
                            } percentage points"
                        )
                    }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("System Log", style = MaterialTheme.typography.titleMedium)
                state.log.take(15).forEach {
                    Text("• $it", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Text(
            "वैज्ञानिक सावधानी: इस मॉडल के परिणाम केवल चुने गए mathematical rules के अनुसार simulation outputs हैं। इन्हें वास्तविक चेतना, subjective experience या consciousness transfer का प्रमाण न मानें।",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ContinuityLab(onBack: () -> Unit) {
    val context = LocalContext.current

    var original by remember {
        mutableStateOf(loadSystem(context, "continuity_original"))
    }
    var copy by remember {
        mutableStateOf(loadSystem(context, "continuity_copy"))
    }
    var hypothesis by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Continuity Lab तैयार है।") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(onClick = onBack) { Text("← वापस") }
        Text(
            "Existence Continuity & Transfer Lab",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            "यहाँ Original System और उसकी Copy के बीच अलग-अलग विकास और source-process link की तुलना की जाती है।"
        )

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
                        original = advanceOpenSystem(original).copy(
                            processContinuity = 1f
                        )
                        saveSystem(context, "continuity_original", original)
                        status = "Original System आगे बढ़ा।"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Update Original System")
                }
                OutlinedButton(
                    onClick = {
                        original = initialSystem()
                        saveSystem(context, "continuity_original", original)
                        status = "Original System reset हुआ।"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset Original")
                }
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
                Text(
                    "Source Process Link: ${
                        (copy.processContinuity * 100).toInt()
                    }%"
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        copy = advanceOpenSystem(copy).copy(
                            processContinuity = 0f
                        )
                        saveSystem(context, "continuity_copy", copy)
                        status = "Copy ने अपना अलग update cycle पूरा किया।"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Update Copy Independently")
                }
                OutlinedButton(
                    onClick = {
                        copy = initialSystem().copy(processContinuity = 0f)
                        saveSystem(context, "continuity_copy", copy)
                        status = "Copy reset हुई।"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset Copy")
                }
                OutlinedButton(
                    onClick = {
                        copy = original.copy(
                            processContinuity = 0f,
                            log = addLog(
                                original,
                                "Original System से नई Copy बनाई गई।"
                            )
                        )
                        saveSystem(context, "continuity_copy", copy)
                        status = "Original System की वर्तमान स्थिति से Copy बनाई गई।"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create Copy From Original")
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("Comparison", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Original और Copy की वर्तमान model values का अंतर:"
                )
                ComparisonLine("Stability", original.stability, copy.stability)
                ComparisonLine("Memory", original.memory, copy.memory)
                ComparisonLine("Organization", original.organization, copy.organization)
                ComparisonLine("Identity Indicator", original.identity, copy.identity)
                ComparisonLine("Internal Reality", original.reality, copy.reality)
                Divider()
                Text(
                    "ध्यान दें: Copy में data समान हो सकता है, लेकिन उसका source process link अलग रखा गया है। यह मॉडल का नियम है; इससे वास्तविक व्यक्ति की चेतना transfer होने या न होने का निष्कर्ष नहीं निकलता।",
                    style = MaterialTheme.typography.bodySmall
                )
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
                            "refresh_research_records",
                            Context.MODE_PRIVATE
                        ).edit()
                            .putString("observation_transfer_hypothesis", hypothesis)
                            .apply()
                        status = "Research observation सेव हो गई।"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Research Observation")
                }
                val savedNote = context.getSharedPreferences(
                    "refresh_research_records",
                    Context.MODE_PRIVATE
                ).getString("observation_transfer_hypothesis", "") ?: ""
                if (savedNote.isNotBlank()) {
                    Divider()
                    Text("Saved Observation", style = MaterialTheme.typography.titleSmall)
                    Text(savedNote)
                }
            }
        }

        Text("Status: $status", style = MaterialTheme.typography.bodyMedium)
        Text(
            "यह Lab एक conceptual simulation है। Memory, identity और process continuity के model indicators को subjective consciousness का direct measurement नहीं माना जा सकता।",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ParameterSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Text("$label: ${(value * 100).toInt()}%")
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f
        )
    }
}

@Composable
private fun ResultMetric(label: String, value: Float) {
    Column(Modifier.fillMaxWidth()) {
        Text("$label: ${(value * 100).toInt()}%")
        LinearProgressIndicator(
            progress = { clamp(value) },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
        )
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun ComparisonLine(
    label: String,
    original: Float,
    copy: Float
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            "$label — Original: ${(original * 100).toInt()}% | " +
                "Copy: ${(copy * 100).toInt()}%"
        )
        Spacer(Modifier.height(4.dp))
    }
}
