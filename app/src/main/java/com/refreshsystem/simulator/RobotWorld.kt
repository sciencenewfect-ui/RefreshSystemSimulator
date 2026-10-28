
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

private val WorldCard = Color(0xFF1A2030)
private val WorldCyan = Color(0xFF6BE7F2)
private val WorldMuted = Color(0xFFB7C0D3)

@Composable
fun RobotWorldScreen() {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("refresh_zero_world", Context.MODE_PRIVATE)
    }

    var command by remember { mutableStateOf("") }
    var response by remember {
        mutableStateOf("शून्य अवस्था: अभी Robot की दुनिया में कोई तत्व नहीं है।")
    }
    var refreshCount by remember {
        mutableIntStateOf(prefs.getInt("refresh_count", 0))
    }
    var stability by remember {
        mutableFloatStateOf(prefs.getFloat("world_stability", 1f))
    }

    val worldItems = remember {
        mutableStateListOf<String>().apply {
            addAll(
                (prefs.getString("world_items", "") ?: "")
                    .split("\n")
                    .filter { it.isNotBlank() }
            )
        }
    }

    val memories = remember {
        mutableStateListOf<String>().apply {
            addAll(
                (prefs.getString("world_memories", "") ?: "")
                    .split("\n")
                    .filter { it.isNotBlank() }
            )
        }
    }

    val log = remember {
        mutableStateListOf<String>().apply {
            addAll(
                (prefs.getString("world_log", "") ?: "")
                    .split("\n")
                    .filter { it.isNotBlank() }
            )
        }
    }

    fun saveAll() {
        prefs.edit()
            .putString("world_items", worldItems.joinToString("\n"))
            .putString("world_memories", memories.joinToString("\n"))
            .putString("world_log", log.take(30).joinToString("\n"))
            .putInt("refresh_count", refreshCount)
            .putFloat("world_stability", stability)
            .apply()
    }

    fun refresh(reason: String) {
        refreshCount++

        stability = (
            stability * 0.88f +
                0.12f * if (worldItems.isEmpty()) 0.35f else 0.78f
            ).coerceIn(0.05f, 1f)

        log.add(0, "Refresh $refreshCount • $reason")
        while (log.size > 30) log.removeAt(log.lastIndex)
        saveAll()
    }

    fun processCommand(raw: String) {
        val text = raw.trim()

        if (text.isBlank()) {
            response = "पहले कोई command लिखो।"
            return
        }

        when {
            text.startsWith("बनाओ:") -> {
                val item = text.removePrefix("बनाओ:").trim()

                if (item.isBlank()) {
                    response = "बताओ कि क्या बनाना है।"
                } else if (worldItems.any { it.equals(item, true) }) {
                    response = "यह तत्व पहले से मौजूद है: $item"
                } else {
                    worldItems.add(item)
                    refresh("नया तत्व बना: $item")
                    response = "Refresh पूरा हुआ। दुनिया में नया तत्व जुड़ा: $item"
                }
            }

            text.startsWith("याद रखो:") -> {
                val memory = text.removePrefix("याद रखो:").trim()

                if (memory.isBlank()) {
                    response = "याद रखने के लिए जानकारी लिखो।"
                } else {
                    memories.add(0, memory)
                    refresh("नई जानकारी रिकॉर्ड हुई")
                    response = "जानकारी रिकॉर्ड हो गई और सिस्टम Refresh हुआ।"
                }
            }

            text == "मेरी दुनिया दिखाओ" -> {
                response = if (worldItems.isEmpty()) {
                    "दुनिया अभी शून्य अवस्था में है। 'बनाओ: ...' लिखकर शुरू करो।"
                } else {
                    "अब तक बनी दुनिया:\n" +
                        worldItems.mapIndexed { i, item -> "${i + 1}. $item" }
                            .joinToString("\n")
                }
            }

            text == "मेरी यादें दिखाओ" -> {
                response = if (memories.isEmpty()) {
                    "अभी कोई जानकारी रिकॉर्ड नहीं है।"
                } else {
                    "रिकॉर्ड की गई जानकारी:\n" +
                        memories.mapIndexed { i, item -> "${i + 1}. $item" }
                            .joinToString("\n")
                }
            }

            text.startsWith("हटाओ:") -> {
                val target = text.removePrefix("हटाओ:").trim()

                if (worldItems.remove(target)) {
                    refresh("तत्व हटाया: $target")
                    response = "Refresh के बाद यह तत्व हटा दिया गया: $target"
                } else {
                    response = "यह तत्व दुनिया में नहीं मिला: $target"
                }
            }

            else -> {
                response = "यह command समझ नहीं आई। नीचे दिए गए उदाहरण आज़माओ।"
            }
        }

        saveAll()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = WorldCard),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "ROBOT • ZERO STATE",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Text(
                "Robot की दुनिया पहले से तैयार नहीं है। " +
                    "Command मिलने पर Refresh होगा और दुनिया धीरे-धीरे बनेगी।",
                color = WorldCyan
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = Color(0xFF252D40),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Refresh: $refreshCount",
                        modifier = Modifier.padding(10.dp),
                        color = Color.White
                    )
                }

                Surface(
                    color = Color(0xFF252D40),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Stability: ${(stability * 100).toInt()}%",
                        modifier = Modifier.padding(10.dp),
                        color = Color.White
                    )
                }
            }

            LinearProgressIndicator(
                progress = { stability },
                modifier = Modifier.fillMaxWidth(),
                color = WorldCyan
            )

            OutlinedTextField(
                value = command,
                onValueChange = { command = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Robot को command दो") },
                placeholder = { Text("जैसे: बनाओ: एक नीला कमरा") },
                minLines = 2
            )

            Button(
                onClick = {
                    processCommand(command)
                    command = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Command → Refresh")
            }

            Text("Command के उदाहरण", fontWeight = FontWeight.Bold)
            Text("• बनाओ: एक नीला कमरा", color = WorldMuted)
            Text("• बनाओ: कमरे में एक खिड़की", color = WorldMuted)
            Text("• बनाओ: एक पेड़", color = WorldMuted)
            Text("• याद रखो: मुझे नीला रंग पसंद है", color = WorldMuted)
            Text("• मेरी दुनिया दिखाओ", color = WorldMuted)
            Text("• मेरी यादें दिखाओ", color = WorldMuted)
            Text("• हटाओ: एक पेड़", color = WorldMuted)

            HorizontalDivider(color = Color.DarkGray)

            Text("सिस्टम का जवाब", fontWeight = FontWeight.Bold)
            Text(response, color = WorldCyan)

            Text(
                "दुनिया के तत्व: ${worldItems.size}",
                fontWeight = FontWeight.Bold
            )

            if (worldItems.isEmpty()) {
                Text(
                    "शून्य अवस्था — अभी कोई दुनिया-तत्व नहीं बना है।",
                    color = WorldMuted
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(worldItems) { item ->
                        Surface(
                            color = Color(0xFF252D40),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                "◈ $item",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Text(
                "रिकॉर्ड की गई जानकारी: ${memories.size}",
                fontWeight = FontWeight.Bold
            )

            Text("हाल के Refresh", fontWeight = FontWeight.Bold)

            if (log.isEmpty()) {
                Text("पहली command का इंतज़ार है।", color = WorldMuted)
            } else {
                log.take(6).forEach {
                    Text("• $it", color = WorldMuted)
                }
            }

            Text(
                "यह Refresh Theory पर आधारित एक प्रयोगात्मक simulation है। " +
                    "Stability का आँकड़ा ऐप के नियमों से बनता है; " +
                    "यह वास्तविक चेतना या अनुभूति का प्रमाण नहीं है।",
                color = WorldMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
