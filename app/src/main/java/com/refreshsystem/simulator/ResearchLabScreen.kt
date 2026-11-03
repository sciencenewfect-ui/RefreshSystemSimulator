
package com.refreshsystem.simulator

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

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
    val log: List<String> = emptyList()
)

private fun initialSystem() = SystemState()

@Composable
fun ResearchHomeScreen() {
    var selectedLab by remember { mutableStateOf(0) }

    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            when (selectedLab) {
                1 -> OpenSystemLab { selectedLab = 0 }
                2 -> ContinuityLab { selectedLab = 0 }
                else -> MainResearchHome(
                    onOpenSystem = { selectedLab = 1 },
                    onContinuity = { selectedLab = 2 }
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
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("REFRESH SYSTEM", style = MaterialTheme.typography.headlineMedium)
        Text("Open System Research Platform", style = MaterialTheme.typography.titleMedium)
        Text(
            "इस ऐप का उद्देश्य Open System के निर्माण, कार्यप्रणाली, " +
            "अस्तित्व और निरंतरता को Simulation के माध्यम से समझना है।"
        )

        Card(Modifier.fillMaxWidth().clickable { onOpenSystem() }) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("1. Open System Simulation Lab",
                    style = MaterialTheme.typography.titleLarge)
                Text("ओपन सिस्टम का निर्माण और उसका पूरा जीवन-चक्र")
                Text("System Formation • Information Exchange • Refresh Process")
                Text("Stability • Memory • I-Feeling • Reality • Zero State")
                Text("OPEN LAB →")
            }
        }

        Card(Modifier.fillMaxWidth().clickable { onContinuity() }) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("2. Existence Continuity & Transfer Lab",
                    style = MaterialTheme.typography.titleLarge)
                Text("अस्तित्व की निरंतरता और दूसरे सिस्टम में Transfer की परिकल्पना")
                Text("Memory Preservation • System Copy • Process Continuity")
                Text("Robot Transfer • Human-to-Human Transfer")
                Text("OPEN LAB →")
            }
        }

        HorizontalDivider()
        Text("Research Principle", style = MaterialTheme.typography.titleMedium)
        Text(
            "Modern Science, Refresh Theory Hypothesis और Simulation Results " +
            "को अलग-अलग समझें। किसी Simulation का परिणाम चेतना या वास्तविक " +
            "Transfer का प्रमाण नहीं माना जाएगा।"
        )
    }
}

@Composable
private fun OpenSystemLab(onBack: () -> Unit) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(loadSystem(context, "open_system")) }
    var running by remember { mutableStateOf(false) }
    var informationInput by remember { mutableStateOf("New information") }
    var disturbance by remember { mutableFloatStateOf(0.20f) }
    var observation by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }

    fun save() {
        saveSystem(context, "open_system", state)
    }

    fun updateSystem() {
        if (state.activity <= 0f) {
            status = "System activity is stopped. Reset or restart the system."
            return
        }

        val newStability = (
            state.stability +
            state.exchange * state.refresh * 0.12f -
            disturbance * 0.12f
        ).coerceIn(0f, 1f)

        val newMemory = (
            state.memory +
            state.retention * state.refresh * 0.10f
        ).coerceIn(0f, 1f)

        val newOrganization = (
            state.organization +
            state.stability * state.refresh * 0.06f
        ).coerceIn(0f, 1f)

        val newIdentity = (
            newMemory * 0.45f +
            newOrganization * 0.35f +
            newStability * 0.20f
        ).coerceIn(0f, 1f)

        val newReality = (
            state.reality +
            state.exchange * state.refresh * 0.08f
        ).coerceIn(0f, 1f)

        val next = state.copy(
            step = state.step + 1,
            stability = newStability,
            memory = newMemory,
            organization = newOrganization,
            identity = newIdentity,
            reality = newReality,
            log = (state.log + (
                "Step ${state.step + 1}: " +
                "Stability=${"%.2f".format(newStability)}, " +
                "Memory=${"%.2f".format(newMemory)}, " +
                "Identity indicator=${"%.2f".format(newIdentity)}"
            )).takeLast(50)
        )

        state = next
        save()
        status = "System updated."
    }

    LaunchedEffect(running) {
        while (running) {
            delay(1200)
            updateSystem()
        }
    }

    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TextButton(onClick = onBack) { Text("← Home") }
        Text("Open System Simulation Lab",
            style = MaterialTheme.typography.headlineSmall)

        Text("एक Open System बनाकर उसकी कार्यप्रणाली का निरीक्षण करें।")

        ResearchCard("Stage 1 — System Formation", "सिस्टम का निर्माण") {
            Text("System step: ${state.step}")
            Text("यह मॉडल एक शुरुआती Open System से शुरू होता है।")
            Text("System activity: ${if (state.activity > 0f) "Active" else "Stopped"}")
        }

        ResearchCard("Stage 2 — Information Exchange", "सूचना का आदान-प्रदान") {
            ParameterSlider("Information Exchange", state.exchange) {
                state = state.copy(exchange = it)
                save()
            }
            OutlinedTextField(
                value = informationInput,
                onValueChange = { informationInput = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("New information") }
            )
            Button(onClick = {
                state = state.copy(
                    memory = (state.memory + 0.05f * state.retention).coerceIn(0f, 1f),
                    log = (state.log + "Information received: $informationInput").takeLast(50)
                )
                save()
                status = "Information added to model."
            }) {
                Text("Receive Information")
            }
        }

        ResearchCard("Stage 3 — Refresh Process", "रिफ्रेश प्रक्रिया") {
            ParameterSlider("Refresh Rate", state.refresh) {
                state = state.copy(refresh = it)
                save()
            }
            Text("Refresh Process इस मॉडल में information update और integration को दर्शाता है।")
        }

        ResearchCard("Stage 4 — Dynamic Stability", "डायनामिक स्थिरता") {
            ParameterSlider("External Disturbance", disturbance) {
                disturbance = it
            }
            ResultMetric("Stability", state.stability)
            ResultMetric("Internal Organization", state.organization)
        }

        ResearchCard("Stage 5 — Memory and Retention", "स्मृति और जानकारी को बनाए रखना") {
            ParameterSlider("Retention Strength", state.retention) {
                state = state.copy(retention = it)
                save()
            }
            ResultMetric("Memory Indicator", state.memory)
            Button(onClick = {
                state = state.copy(
                    memory = (state.memory - 0.15f).coerceIn(0f, 1f),
                    log = (state.log + "Memory reduction experiment").takeLast(50)
                )
                save()
                status = "Memory indicator reduced."
            }) {
                Text("Remove Some Stored Information")
            }
        }

        ResearchCard("Stage 6 — I-Feeling and Identity", "मैं होने का अनुभव और पहचान") {
            ResultMetric("Identity Continuity Indicator", state.identity)
            Text(
                "यह गणितीय संकेतक है। यह वास्तविक I-Feeling या subjective experience " +
                "को मापता नहीं है।"
            )
        }

        ResearchCard("Stage 7 — Internal Reality", "आंतरिक वास्तविकता") {
            ResultMetric("Internal Reality Indicator", state.reality)
            Text(
                "यह indicator दिखाता है कि मॉडल में बाहरी सूचना के आधार पर " +
                "आंतरिक रिकॉर्ड कैसे बदल रहा है। यह वास्तविक अनुभव का माप नहीं है।"
            )
        }

        ResearchCard("Stage 8 — Zero State", "शून्य अवस्था") {
            Text("इस प्रयोग में System Activity रोककर मॉडल की स्थिति देखी जा सकती है।")
            Button(onClick = {
                running = false
                state = state.copy(
                    activity = 0f,
                    exchange = 0f,
                    refresh = 0f,
                    log = (state.log + "System activity stopped. Zero-state experiment.").takeLast(50)
                )
                save()
                status = "System activity stopped in this model."
            }) {
                Text("Stop System Activity")
            }
            Text(
                "यह केवल Simulation की activity रोकता है। इससे वास्तविक मृत्यु या " +
                "चेतना समाप्त होने के बारे में कोई वैज्ञानिक निष्कर्ष नहीं निकलता।"
            )
        }

        ResearchCard("Simulation Controls", "सिमुलेशन कंट्रोल") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    if (state.activity > 0f) running = true
                    else status = "Reset system before starting again."
                }) { Text("Start") }

                OutlinedButton(onClick = { running = false }) {
                    Text("Pause")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { updateSystem() }) { Text("Update System") }
                OutlinedButton(onClick = {
                    running = false
                    state = initialSystem()
                    disturbance = 0.20f
                    save()
                    status = "System reset."
                }) { Text("Reset") }
            }

            Button(onClick = {
                save()
                status = "Experiment saved on this device."
            }) { Text("Save Experiment") }

            Text(status)
        }

        ResearchCard("Observation and Results", "अवलोकन और परिणाम") {
            OutlinedTextField(
                value = observation,
                onValueChange = { observation = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Write your observation") },
                minLines = 3
            )

            Button(onClick = {
                saveObservation(context, "open_system", observation)
                status = "Observation saved."
            }) { Text("Save Observation") }

            Text("Current model results")
            ResultMetric("Stability", state.stability)
            ResultMetric("Memory", state.memory)
            ResultMetric("Organization", state.organization)
            ResultMetric("Identity indicator", state.identity)
            ResultMetric("Reality indicator", state.reality)

            state.log.asReversed().forEach {
                Text("• $it", style = MaterialTheme.typography.bodySmall)
            }
        }

        Text(
            "Important: इस simulation के mathematical rules demonstration के लिए चुने गए हैं। " +
            "इन्हें प्रकृति के प्रमाणित नियम या चेतना का प्रमाण न समझें।",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ContinuityLab(onBack: () -> Unit) {
    val context = LocalContext.current
    var original by remember { mutableStateOf(loadSystem(context, "continuity_original")) }
    var copy by remember { mutableStateOf<SystemState?>(loadOptionalSystem(context, "continuity_copy")) }
    var note by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TextButton(onClick = onBack) { Text("← Home") }
        Text("Existence Continuity & Transfer Lab",
            style = MaterialTheme.typography.headlineSmall)

        Text(
            "यहाँ हम information copy, system continuity और transfer की " +
            "अलग-अलग परिकल्पनाओं का प्रयोग करेंगे।"
        )

        ResearchCard("Experiment 1 — Original System", "मूल सिस्टम") {
            Text("Original system step: ${original.step}")
            ResultMetric("Stability", original.stability)
            ResultMetric("Memory", original.memory)
            ResultMetric("Identity indicator", original.identity)

            Button(onClick = {
                original = original.copy(
                    step = original.step + 1,
                    stability = (original.stability + 0.03f).coerceIn(0f, 1f),
                    memory = (original.memory + 0.02f).coerceIn(0f, 1f)
                )
                saveSystem(context, "continuity_original", original)
                status = "Original system advanced by one model step."
            }) {
                Text("Continue Original System")
            }
        }

        ResearchCard("Experiment 2 — Information Copy", "जानकारी की कॉपी") {
            Text("यह original system की वर्तमान जानकारी की अलग copy बनाएगा।")
            Button(onClick = {
                copy = original.copy(
                    log = original.log + "Independent copy created."
                )
                copy?.let { saveSystem(context, "continuity_copy", it) }
                status = "Separate information copy created."
            }) {
                Text("Create System Copy")
            }
            Text(
                "Copy बनना subjective experience या व्यक्ति की चेतना transfer " +
                "होने का प्रमाण नहीं है।"
            )
        }

        ResearchCard("Experiment 3 — Independent Development", "अलग-अलग विकास") {
            Text("Original और Copy के model indicators की तुलना करें।")

            Button(onClick = {
                original = original.copy(
                    step = original.step + 1,
                    memory = (original.memory + 0.04f).coerceIn(0f, 1f),
                    stability = (original.stability + 0.02f).coerceIn(0f, 1f)
                )
                saveSystem(context, "continuity_original", original)
                status = "Original system updated."
            }) {
                Text("Update Original Only")
            }

            Button(
                enabled = copy != null,
                onClick = {
                    copy = copy?.copy(
                        step = copy!!.step + 1,
                        memory = (copy!!.memory + 0.01f).coerceIn(0f, 1f),
                        stability = (copy!!.stability - 0.02f).coerceIn(0f, 1f)
                    )
                    copy?.let { saveSystem(context, "continuity_copy", it) }
                    status = "Copy system updated independently."
                }
            ) {
                Text("Update Copy Only")
            }

            ResultMetric("Original Memory", original.memory)
            ResultMetric("Copy Memory", copy?.memory ?: 0f)
            ResultMetric("Original Stability", original.stability)
            ResultMetric("Copy Stability", copy?.stability ?: 0f)
        }

        ResearchCard("Experiment 4 — Transfer Hypothesis", "ट्रांसफर की परिकल्पना") {
            Text(
                "मान लो कि किसी दूसरे Open System में information, memory और " +
                "organization की copy पहुँचाई जाती है। क्या इससे मूल व्यक्ति का " +
                "अनुभव भी जारी रहेगा? यह अभी खुला वैज्ञानिक प्रश्न है।"
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Your transfer hypothesis") },
                minLines = 3
            )
            Button(onClick = {
                saveObservation(context, "transfer_hypothesis", note)
                status = "Transfer hypothesis saved."
            }) {
                Text("Save Hypothesis")
            }
        }

        ResearchCard("Research Record", "शोध रिकॉर्ड") {
            Text(status)
            Button(onClick = {
                saveSystem(context, "continuity_original", original)
                copy?.let { saveSystem(context, "continuity_copy", it) }
                status = "Both available system records saved."
            }) {
                Text("Save Experiment")
            }
            Text(
                "यह model तुलना करता है। यह मानव से मानव या मानव से Robot " +
                "में चेतना transfer करने की तकनीक नहीं है।"
            )
        }
    }
}

@Composable
private fun ResearchCard(
    title: String,
    hindi: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(hindi, style = MaterialTheme.typography.labelMedium)
            content()
        }
    }
}

@Composable
private fun ParameterSlider(
    label: String,
    value: Float,
    onChange: (Float) -> Unit
) {
    Column {
        Text("$label: ${"%.2f".format(value)}")
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 0f..1f
        )
    }
}

@Composable
private fun ResultMetric(label: String, value: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text("$label: ${"%.2f".format(value)}")
        LinearProgressIndicator(
            progress = { value.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun researchPrefs(context: Context) =
    context.getSharedPreferences("refresh_research_records", Context.MODE_PRIVATE)

private fun saveSystem(context: Context, key: String, state: SystemState) {
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
        put("log", JSONArray(state.log))
    }

    researchPrefs(context).edit()
        .putString("system_$key", json.toString())
        .apply()
}

private fun loadOptionalSystem(context: Context, key: String): SystemState? {
    val raw = researchPrefs(context).getString("system_$key", null) ?: return null
    return parseSystem(raw)
}

private fun loadSystem(context: Context, key: String): SystemState {
    val raw = researchPrefs(context).getString("system_$key", null) ?: return initialSystem()
    return parseSystem(raw) ?: initialSystem()
}

private fun parseSystem(raw: String): SystemState? {
    return try {
        val json = JSONObject(raw)
        val array = json.optJSONArray("log") ?: JSONArray()

        SystemState(
            step = json.optInt("step", 0),
            stability = json.optDouble("stability", .65).toFloat(),
            exchange = json.optDouble("exchange", .5).toFloat(),
            refresh = json.optDouble("refresh", .5).toFloat(),
            retention = json.optDouble("retention", .5).toFloat(),
            memory = json.optDouble("memory", .2).toFloat(),
            organization = json.optDouble("organization", .2).toFloat(),
            identity = json.optDouble("identity", .2).toFloat(),
            reality = json.optDouble("reality", .2).toFloat(),
            activity = json.optDouble("activity", 1.0).toFloat(),
            log = (0 until array.length()).map { array.optString(it) }
        )
    } catch (_: Exception) {
        null
    }
}

private fun saveObservation(context: Context, key: String, text: String) {
    researchPrefs(context).edit()
        .putString("observation_$key", text)
        .apply()
}
