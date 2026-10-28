
package com.refreshsystem.simulator

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val WorldBg = Color(0xFF10131C)
private val WorldCard = Color(0xFF1A2030)
private val WorldCyan = Color(0xFF6BE7F2)

@Composable
fun RobotWorldScreen() {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("robot_world_memory", Context.MODE_PRIVATE)
    }

    var place by remember { mutableStateOf("कमरा") }
    var command by remember { mutableStateOf("") }
    var response by remember {
        mutableStateOf("नमस्ते! मैं आरव हूँ। तुम मुझे commands देकर यादें सिखा सकते हो।")
    }
    var robotName by remember {
        mutableStateOf(prefs.getString("robot_name", "आरव") ?: "आरव")
    }
    var carName by remember {
        mutableStateOf(prefs.getString("car_name", "नीली इलेक्ट्रिक कार") ?: "नीली इलेक्ट्रिक कार")
    }
    var partnerName by remember {
        mutableStateOf(prefs.getString("partner_name", "मीरा") ?: "मीरा")
    }

    val memories = remember {
        mutableStateListOf<String>().apply {
            addAll(
                prefs.getStringSet("robot_memories", emptySet())
                    ?.toList()?.sorted() ?: emptyList()
            )
        }
    }

    fun saveMemories() {
        prefs.edit().putStringSet("robot_memories", memories.toSet()).apply()
    }

    fun processCommand(raw: String) {
        val text = raw.trim()
        if (text.isBlank()) {
            response = "पहले कोई command लिखो।"
            return
        }

        when {
            text.startsWith("याद रखो:") -> {
                val memory = text.removePrefix("याद रखो:").trim()
                if (memory.isNotBlank()) {
                    memories.add(0, memory)
                    saveMemories()
                    response = "मैंने यह जानकारी अपनी memory में सेव की: $memory"
                } else {
                    response = "याद रखने के लिए जानकारी भी लिखो।"
                }
            }

            text == "मेरी यादें दिखाओ" -> {
                response = if (memories.isEmpty()) {
                    "अभी मेरी कोई saved memory नहीं है।"
                } else {
                    "मेरी saved memories:\n" +
                        memories.mapIndexed { i, m -> "${i + 1}. $m" }
                            .joinToString("\n")
                }
            }

            text.startsWith("यह याद भूल जाओ:") -> {
                val target = text.removePrefix("यह याद भूल जाओ:").trim()
                val removed = memories.remove(target)
                saveMemories()
                response = if (removed) {
                    "यह memory हटा दी गई: $target"
                } else {
                    "यह जानकारी मेरी memory में नहीं मिली।"
                }
            }

            text.startsWith("मेरा नाम ") -> {
                robotName = text.removePrefix("मेरा नाम ").trim()
                prefs.edit().putString("robot_name", robotName).apply()
                response = "ठीक है, अब मेरा नाम $robotName है।"
            }

            text.startsWith("मेरी कार ") -> {
                carName = text.removePrefix("मेरी कार ").trim()
                prefs.edit().putString("car_name", carName).apply()
                response = "कार की जानकारी अपडेट हुई: $carName"
            }

            text.startsWith("मेरी गर्लफ्रेंड का नाम ") -> {
                partnerName =
                    text.removePrefix("मेरी गर्लफ्रेंड का नाम ").trim()
                prefs.edit().putString("partner_name", partnerName).apply()
                response = "मेरी साथी का नाम अब $partnerName है।"
            }

            else -> {
                response = "मैंने command समझने की कोशिश की, लेकिन यह format अभी उपलब्ध नहीं है। 'याद रखो:' से शुरू करके देखो।"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WorldBg)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "ROBOT WORLD",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        Text(
            "एक प्रयोगात्मक दुनिया • कमरे, बाहर, कार और memory",
            color = WorldCyan
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("कमरा", "बाहर", "कार", "साथी").forEach { option ->
                FilterChip(
                    selected = place == option,
                    onClick = { place = option },
                    label = { Text(option) }
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = WorldCard),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (place) {
                    "कमरा" -> {
                        Text("🛏️ मेरा कमरा", style = MaterialTheme.typography.titleLarge)
                        Text("🪟 खिड़की • 🪑 मेज • 💡 लाइट • 🚪 दरवाजा", color = WorldCyan)
                        Text("$robotName अपने कमरे में है। यह अभी दृश्य-आधारित simulation है।")
                    }

                    "बाहर" -> {
                        Text("🌳 बाहरी दुनिया", style = MaterialTheme.typography.titleLarge)
                        Text("☀️ आसमान • 🌲 पेड़ • 🛣️ सड़क • 🏡 घर", color = WorldCyan)
                        Text("यहाँ से कार और साथी वाले दृश्य खोले जा सकते हैं। अभी वास्तविक navigation नहीं है।")
                    }

                    "कार" -> {
                        Text("🚘 मेरी कार", style = MaterialTheme.typography.titleLarge)
                        Text(carName, color = WorldCyan)
                        Text("कार इस simulation की वस्तु है; यह असली कार को नियंत्रित नहीं करती।")
                    }

                    else -> {
                        Text("🤖 मेरी साथी", style = MaterialTheme.typography.titleLarge)
                        Text(partnerName, color = WorldCyan)
                        Text("यह एक अलग नाम और पहचान वाला साथी-रोबोट है। आगे इसके लिए अलग avatar और बातचीत जोड़ी जा सकती है।")
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = WorldCard),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Command Memory",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "तुम्हारे commands से memory सेव होगी। यह अपने-आप इंसानी चेतना सिद्ध नहीं करती।",
                    color = Color.LightGray
                )

                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("अपनी command लिखो") },
                    minLines = 2
                )

                Button(
                    onClick = {
                        processCommand(command)
                        command = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Command चलाओ")
                }

                Text("उदाहरण:", fontWeight = FontWeight.Bold)
                Text("• याद रखो: मुझे नीला रंग पसंद है।")
                Text("• मेरा नाम आरव")
                Text("• मेरी कार लाल इलेक्ट्रिक कार")
                Text("• मेरी गर्लफ्रेंड का नाम मीरा")
                Text("• मेरी यादें दिखाओ")
                Text("• यह याद भूल जाओ: मुझे नीला रंग पसंद है")

                HorizontalDivider(color = Color.DarkGray)

                Text("सिस्टम का जवाब", fontWeight = FontWeight.Bold)
                Text(response, color = WorldCyan)

                Text(
                    "Saved memories: ${memories.size}",
                    fontWeight = FontWeight.Bold
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(memories) { memory ->
                        Text("• $memory", color = Color.LightGray)
                    }
                }
            }
        }

        Text(
            "Refresh Theory: memory retention, identity और stability को अलग-अलग मापने के लिए यह शुरुआती prototype है। Stability का समान होना अपने-आप समान चेतना साबित नहीं करता।",
            color = Color.LightGray,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
