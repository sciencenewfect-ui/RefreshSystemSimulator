
package com.refreshsystem.simulator

import android.content.Context
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val WorldCard = Color(0xFF1A2030)
private val WorldCyan = Color(0xFF6BE7F2)
private val WorldMuted = Color(0xFFB7C0D3)
private val WorldGreen = Color(0xFF9AE6B4)

@Composable
fun RobotWorldScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("refresh_zero_world", Context.MODE_PRIVATE)
    }

    var command by remember { mutableStateOf("") }
    var response by remember {
        mutableStateOf("Robot शांत अवस्था में है। कोई command देकर इसे सक्रिय करें।")
    }
    var refreshCount by remember {
        mutableIntStateOf(prefs.getInt("refresh_count", 0))
    }
    var stability by remember {
        mutableFloatStateOf(prefs.getFloat("world_stability", 1f))
    }
    var outsideActive by remember {
        mutableStateOf(prefs.getBoolean("outside_active", false))
    }
    var cameraEnabled by remember { mutableStateOf(false) }

    val worldItems = remember {
        mutableStateListOf<String>().apply {
            addAll(
                (prefs.getString("world_items", "") ?: "")
                    .split("\n").filter { it.isNotBlank() }
            )
        }
    }
    val records = remember {
        mutableStateListOf<String>().apply {
            addAll(
                (prefs.getString("world_records", "") ?: "")
                    .split("\n").filter { it.isNotBlank() }
            )
        }
    }
    val experienceFilm = remember {
        mutableStateListOf<String>().apply {
            addAll(
                (prefs.getString("world_film", "") ?: "")
                    .split("\n").filter { it.isNotBlank() }
            )
        }
    }
    val refreshLog = remember {
        mutableStateListOf<String>().apply {
            addAll(
                (prefs.getString("world_log", "") ?: "")
                    .split("\n").filter { it.isNotBlank() }
            )
        }
    }

    fun saveAll() {
        prefs.edit()
            .putString("world_items", worldItems.joinToString("\n"))
            .putString("world_records", records.take(100).joinToString("\n"))
            .putString("world_film", experienceFilm.take(100).joinToString("\n"))
            .putString("world_log", refreshLog.take(30).joinToString("\n"))
            .putInt("refresh_count", refreshCount)
            .putFloat("world_stability", stability)
            .putBoolean("outside_active", outsideActive)
            .apply()
    }

    fun refresh(reason: String) {
        refreshCount++

        // Stability और information records अलग-अलग metrics हैं।
        // Stability यहां केवल simulation score है।
        val targetStability =
            if (worldItems.isEmpty() && records.isEmpty()) 1f else 0.92f

        stability = (
            stability * 0.94f + targetStability * 0.06f
        ).coerceIn(0.05f, 1f)

        refreshLog.add(0, "Refresh $refreshCount • $reason")
        while (refreshLog.size > 30) {
            refreshLog.removeAt(refreshLog.lastIndex)
        }
        saveAll()
    }

    fun recordInformation(text: String, source: String = "Command") {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return

        records.add(0, cleanText)
        while (records.size > 100) records.removeAt(records.lastIndex)

        experienceFilm.add(
            0,
            "रिकॉर्ड ${records.size} • $source: $cleanText"
        )
        while (experienceFilm.size > 100) {
            experienceFilm.removeAt(experienceFilm.lastIndex)
        }

        refresh("$source से जानकारी रिकॉर्ड हुई")
        response = "जानकारी रिकॉर्ड हो गई। Records: ${records.size} • अनुभव-फिल्म: ${experienceFilm.size}"
        saveAll()
    }

    fun activateOutsideWorld() {
        outsideActive = true
        if (worldItems.none { it == "बाहरी दुनिया सक्रिय" }) {
            worldItems.add(0, "बाहरी दुनिया सक्रिय")
        }
    }

    fun processCommand(rawCommand: String) {
        val text = rawCommand.trim()
        if (text.isBlank()) {
            response = "पहले कोई command लिखें।"
            return
        }

        val lower = text.lowercase()

        when {
            lower == "मदद" || lower == "help" || lower == "commands" -> {
                response = """
                    Commands:
                    • सक्रिय करो
                    • बाहर जाओ
                    • बनाओ: एक पेड़
                    • याद रखो: मुझे नीला रंग पसंद है
                    • हटाओ: एक पेड़
                    • दुनिया दिखाओ
                    • रिकॉर्ड दिखाओ
                    • अनुभव दिखाओ
                """.trimIndent()
            }

            lower.contains("बाहर जाओ") ||
                lower.contains("बाहरी दुनिया") ||
                lower.contains("activate world") ||
                lower.contains("दुनिया सक्रिय") -> {
                activateOutsideWorld()
                recordInformation("User command: $text", "Robot")
                response = "Robot सक्रिय है और बाहर की दुनिया खुल गई। अब आप दुनिया में चीज़ें जोड़ सकते हैं।"
                saveAll()
            }

            lower.contains("सक्रिय") ||
                lower.contains("जागो") ||
                lower.contains("activate") ||
                lower.contains("चलो") -> {
                activateOutsideWorld()
                recordInformation("Robot activation command: $text", "Robot")
                response = "Robot और उसकी बाहरी दुनिया सक्रिय हो गए हैं।"
                saveAll()
            }

            lower.startsWith("बनाओ:") ||
                lower.startsWith("create:") -> {
                val item = text.substringAfter(":").trim()

                if (item.isBlank()) {
                    response = "बताइए कि क्या बनाना है। उदाहरण: बनाओ: एक पेड़"
                } else {
                    activateOutsideWorld()

                    if (worldItems.none { it.equals(item, ignoreCase = true) }) {
                        worldItems.add(item)
                    }

                    recordInformation("दुनिया में जोड़ा गया: $item", "Command")
                    response = "बाहरी दुनिया सक्रिय हुई और उसमें '$item' जोड़ दिया गया।"
                    saveAll()
                }
            }

            lower.startsWith("याद रखो:") ||
                lower.startsWith("remember:") -> {
                val memory = text.substringAfter(":").trim()
                if (memory.isBlank()) {
                    response = "याद रखने के लिए जानकारी लिखें।"
                } else {
                    recordInformation(memory, "User memory")
                }
            }

            lower.startsWith("हटाओ:") ||
                lower.startsWith("remove:") -> {
                val target = text.substringAfter(":").trim()
                val removed = worldItems.removeAll {
                    it.equals(target, ignoreCase = true)
                }

                if (removed) {
                    recordInformation("दुनिया से हटाया गया: $target", "Command")
                    response = "'$target' हटा दिया गया और बदलाव रिकॉर्ड हुआ।"
                } else {
                    response = "'$target' नहीं मिला। पहले 'दुनिया दिखाओ' लिखें।"
                }
                saveAll()
            }

            lower.contains("दुनिया दिखाओ") ||
                lower.contains("show world") -> {
                response = if (worldItems.isEmpty()) {
                    "बाहरी दुनिया में अभी कोई अलग तत्व नहीं है। 'बाहर जाओ' या 'बनाओ: एक पेड़' लिखें।"
                } else {
                    "बाहरी दुनिया के तत्व:\n" +
                        worldItems.mapIndexed { i, item -> "${i + 1}. $item" }
                            .joinToString("\n")
                }
            }

            lower.contains("रिकॉर्ड दिखाओ") ||
                lower.contains("यादें दिखाओ") ||
                lower.contains("show records") -> {
                response = if (records.isEmpty()) {
                    "अभी कोई information record नहीं है।"
                } else {
                    "रिकॉर्ड की गई जानकारी:\n" +
                        records.take(12).mapIndexed { i, item -> "${i + 1}. $item" }
                            .joinToString("\n")
                }
            }

            lower.contains("अनुभव दिखाओ") ||
                lower.contains("फिल्म दिखाओ") ||
                lower.contains("show experience") -> {
                response = if (experienceFilm.isEmpty()) {
                    "अभी अनुभव-फिल्म में कोई प्रविष्टि नहीं है।"
                } else {
                    "अनुभव-फिल्म (रिकॉर्ड की simulation):\n" +
                        experienceFilm.take(10).joinToString("\n")
                }
            }

            else -> {
                // अन्य command को भी सूचना की तरह रिकॉर्ड करें।
                activateOutsideWorld()
                recordInformation("User ने कहा: $text", "Command")
                response = "Command रिकॉर्ड हुई और Robot की बाहरी दुनिया सक्रिय हुई। वस्तु जोड़ने के लिए 'बनाओ: ...' लिखें।"
                saveAll()
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = WorldCard),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "REFRESH SYSTEM • ROBOT WORLD",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Text(
                "Robot कमरे में शांत बैठा है। Command मिलने पर Robot और बाहर की दुनिया सक्रिय होते हैं।",
                color = WorldCyan
            )

            RobotRoomScene(
                outsideActive = outsideActive,
                recordCount = records.size
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(Modifier.weight(1f)) {
                    Metric("Stability", "${(stability * 100).toInt()}%")
                }
                Box(Modifier.weight(1f)) {
                    Metric("Refresh", refreshCount.toString())
                }
                Box(Modifier.weight(1f)) {
                    Metric("Records", records.size.toString())
                }
            }

            LinearProgressIndicator(
                progress = { stability },
                modifier = Modifier.fillMaxWidth(),
                color = WorldCyan
            )

            Text(
                if (outsideActive) "बाहरी दुनिया: सक्रिय" else "बाहरी दुनिया: अभी प्रतीक्षा में",
                color = if (outsideActive) WorldGreen else WorldMuted,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = {
                    if (outsideActive) {
                        outsideActive = false
                        refresh("बाहरी दुनिया pause हुई")
                        response = "बाहरी दुनिया pause है। रिकॉर्ड सुरक्षित हैं।"
                    } else {
                        activateOutsideWorld()
                        recordInformation("बाहरी दुनिया Activate World बटन से सक्रिय हुई", "System")
                    }
                    saveAll()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (outsideActive) "Pause Outside World" else "Activate Outside World")
            }

            Text(
                if (outsideActive) {
                    "बाहरी दृश्य: खुला मैदान, रास्ता और आसमान। आप command से इसमें पेड़ जैसी चीज़ें जोड़ सकते हैं।"
                } else {
                    "बाहरी दुनिया अभी निष्क्रिय है। 'बाहर जाओ' लिखें या Activate बटन दबाएँ।"
                },
                color = WorldMuted
            )

            OutlinedTextField(
                value = command,
                onValueChange = { command = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Robot को command दें") },
                placeholder = { Text("जैसे: बाहर जाओ / बनाओ: एक पेड़") },
                minLines = 2
            )

            Button(
                onClick = {
                    processCommand(command)
                    command = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Command चलाएँ → Refresh")
            }

            Text("कमांड के उदाहरण", fontWeight = FontWeight.Bold, color = Color.White)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AssistChip(
                    onClick = { processCommand("बाहर जाओ") },
                    label = { Text("बाहर जाओ") }
                )
                AssistChip(
                    onClick = { processCommand("बनाओ: एक पेड़") },
                    label = { Text("पेड़ बनाओ") }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AssistChip(
                    onClick = { processCommand("याद रखो: मुझे नीला रंग पसंद है") },
                    label = { Text("याद रखो") }
                )
                AssistChip(
                    onClick = { processCommand("अनुभव दिखाओ") },
                    label = { Text("अनुभव दिखाओ") }
                )
            }

            HorizontalDivider(color = Color.DarkGray)

            Text("Live camera → Information records", fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                "कैमरे से उपलब्ध face/scene labels रिकॉर्ड किए जा सकते हैं। यह camera analysis है, वास्तविक अनुभूति का प्रमाण नहीं।",
                color = WorldMuted,
                style = MaterialTheme.typography.bodySmall
            )

            CameraPanel(
                enabled = cameraEnabled,
                onEnabledChange = { cameraEnabled = it },
                onObservation = { observation, faces ->
                    if (
                        cameraEnabled &&
                        observation.isNotBlank() &&
                        !observation.startsWith("Camera could not start")
                    ) {
                        val entry = "Camera observation: $observation; faces=$faces"
                        if (records.firstOrNull() != entry) {
                            recordInformation(entry, "Camera")
                        }
                    } else if (observation.startsWith("Camera could not start")) {
                        response = observation
                    }
                }
            )

            Text("सिस्टम का जवाब", fontWeight = FontWeight.Bold, color = Color.White)
            Text(response, color = WorldCyan)

            HorizontalDivider(color = Color.DarkGray)

            Text("बाहरी दुनिया के तत्व (${worldItems.size})", fontWeight = FontWeight.Bold, color = Color.White)
            if (worldItems.isEmpty()) {
                Text("अभी कोई दुनिया-तत्व नहीं है।", color = WorldMuted)
            } else {
                worldItems.take(20).forEach {
                    Text("• $it", color = WorldMuted)
                }
            }

            Text("Information Records (${records.size})", fontWeight = FontWeight.Bold, color = Color.White)
            if (records.isEmpty()) {
                Text(
                    "शून्य रिकॉर्ड। Stability और Records अलग-अलग माप हैं।",
                    color = WorldMuted
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 150.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(records.take(10)) { item ->
                        Text("• $item", color = WorldMuted)
                    }
                }
            }

            Text("अनुभव-फिल्म (${experienceFilm.size})", fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                "यह records से बनी क्रमवार simulation है, असली subjective experience का प्रमाण नहीं।",
                color = WorldMuted,
                style = MaterialTheme.typography.bodySmall
            )

            if (experienceFilm.isEmpty()) {
                Text("अभी कोई प्रविष्टि नहीं है। Command या camera observation के बाद रिकॉर्ड जुड़ेंगे।", color = WorldMuted)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(experienceFilm.take(10)) { item ->
                        Text("▶ $item", color = WorldCyan)
                    }
                }
            }

            Text("हाल के Refresh", fontWeight = FontWeight.Bold, color = Color.White)
            if (refreshLog.isEmpty()) {
                Text("पहली command का इंतज़ार है।", color = WorldMuted)
            } else {
                refreshLog.take(5).forEach {
                    Text("• $it", color = WorldMuted, style = MaterialTheme.typography.bodySmall)
                }
            }

            Text(
                "वैज्ञानिक नोट: Stability ऐप का simulation score है। यह चेतना को मापता या सिद्ध नहीं करता।",
                color = WorldMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Surface(
        color = Color(0xFF252D40),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(label, color = WorldMuted, style = MaterialTheme.typography.labelSmall)
            Text(value, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RobotRoomScene(
    outsideActive: Boolean,
    recordCount: Int
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .background(Color(0xFF121A2A), RoundedCornerShape(18.dp))
    ) {
        val w = size.width
        val h = size.height

        // कमरे की दीवार और फर्श
        drawRect(Color(0xFF202A3C), size = Size(w, h * 0.76f))
        drawRect(
            Color(0xFF34384A),
            topLeft = Offset(0f, h * 0.76f),
            size = Size(w, h * 0.24f)
        )
        drawLine(
            Color(0xFF59667D),
            Offset(0f, h * 0.76f),
            Offset(w, h * 0.76f),
            strokeWidth = 3f
        )

        // खिड़की और बाहर का दृश्य
        val winX = w * 0.07f
        val winY = h * 0.12f
        val winW = w * 0.34f
        val winH = h * 0.34f

        drawRoundRect(
            color = if (outsideActive) Color(0xFF254E66) else Color(0xFF101827),
            topLeft = Offset(winX, winY),
            size = Size(winW, winH),
            cornerRadius = CornerRadius(10f)
        )

        if (outsideActive) {
            drawCircle(
                color = Color(0xFFFFD27D),
                radius = 12f,
                center = Offset(winX + winW * 0.75f, winY + winH * 0.27f)
            )
            drawRect(
                Color(0xFF2D765F),
                topLeft = Offset(winX, winY + winH * 0.72f),
                size = Size(winW, winH * 0.28f)
            )
            drawLine(
                Color(0xFF8FD7F2),
                Offset(winX + winW * 0.5f, winY),
                Offset(winX + winW * 0.5f, winY + winH),
                2f
            )
            drawLine(
                Color(0xFF8FD7F2),
                Offset(winX, winY + winH * 0.5f),
                Offset(winX + winW, winY + winH * 0.5f),
                2f
            )
        } else {
            drawLine(
                Color(0xFF48566B),
                Offset(winX, winY),
                Offset(winX + winW, winY + winH),
                2f
            )
            drawLine(
                Color(0xFF48566B),
                Offset(winX + winW, winY),
                Offset(winX, winY + winH),
                2f
            )
        }

        // कुर्सी
        val cx = w * 0.70f
        drawRoundRect(
            color = Color(0xFF705044),
            topLeft = Offset(cx - 47f, h * 0.49f),
            size = Size(94f, 62f),
            cornerRadius = CornerRadius(12f)
        )
        drawRect(
            Color(0xFF705044),
            topLeft = Offset(cx - 40f, h * 0.72f),
            size = Size(9f, h * 0.17f)
        )
        drawRect(
            Color(0xFF705044),
            topLeft = Offset(cx + 31f, h * 0.72f),
            size = Size(9f, h * 0.17f)
        )

        // रोबोट का सिर, धड़, हाथ और बैठे हुए पैर
        drawRoundRect(
            color = Color(0xFFB6C7D7),
            topLeft = Offset(cx - 29f, h * 0.20f),
            size = Size(58f, 52f),
            cornerRadius = CornerRadius(14f)
        )
        drawRoundRect(
            color = if (outsideActive) WorldCyan else Color(0xFF536A7B),
            topLeft = Offset(cx - 19f, h * 0.28f),
            size = Size(38f, 9f),
            cornerRadius = CornerRadius(5f)
        )
        drawRoundRect(
            color = Color(0xFF7D95AA),
            topLeft = Offset(cx - 36f, h * 0.41f),
            size = Size(72f, 62f),
            cornerRadius = CornerRadius(14f)
        )
        drawLine(
            Color(0xFFB6C7D7),
            Offset(cx - 34f, h * 0.45f),
            Offset(cx - 54f, h * 0.59f),
            strokeWidth = 12f
        )
        drawLine(
            Color(0xFFB6C7D7),
            Offset(cx + 34f, h * 0.45f),
            Offset(cx + 54f, h * 0.59f),
            strokeWidth = 12f
        )
        drawLine(
            WorldCyan,
            Offset(cx - 20f, h * 0.65f),
            Offset(cx + 10f, h * 0.69f),
            strokeWidth = 14f
        )
        drawLine(
            WorldCyan,
            Offset(cx + 10f, h * 0.69f),
            Offset(cx + 40f, h * 0.65f),
            strokeWidth = 14f
        )

        // सिर के ऊपर activity light
        drawCircle(
            color = if (outsideActive) WorldGreen else Color(0xFF687386),
            radius = 5f,
            center = Offset(cx, h * 0.17f)
        )

        // रिकॉर्ड की संख्या बढ़ने पर कमरे की छोटी indicator lights
        val lightCount = recordCount.coerceAtMost(5)
        repeat(lightCount) { index ->
            drawCircle(
                color = WorldCyan,
                radius = 3.5f,
                center = Offset(w * 0.08f + index * 13f, h * 0.91f)
            )
        }
    }
}
