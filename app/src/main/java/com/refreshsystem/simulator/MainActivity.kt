
package com.refreshsystem.simulator

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.cos
import kotlin.math.sin

private val Bg = Color(0xFF08111F)
private val Panel = Color(0xFF111F32)
private val Cyan = Color(0xFF43D9E8)
private val Green = Color(0xFF63E6A5)
private val Muted = Color(0xFFA8B8CC)
private val White = Color(0xFFF3F7FC)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Bg
                ) {
                    RefreshSystemApp(applicationContext)
                }
            }
        }
    }
}

private data class MemoryRecord(
    val text: String,
    val time: Long,
    val score: Int
)

@Composable
private fun RefreshSystemApp(context: Context) {
    val prefs = remember {
        context.getSharedPreferences("refresh_system", Context.MODE_PRIVATE)
    }

    var memoryText by remember { mutableStateOf("") }
    var records by remember {
        mutableStateOf(loadRecords(prefs))
    }
    var refreshCount by remember {
        mutableIntStateOf(prefs.getInt("refresh_count", 0))
    }
    var stability by remember {
        mutableIntStateOf(prefs.getInt("stability", 100))
    }
    var testResult by remember {
        mutableStateOf("Stability test अभी शुरू नहीं हुआ है।")
    }

    val transition = rememberInfiniteTransition(label = "system")
    val movement by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 6000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "nodeMovement"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                "REFRESH SYSTEM",
                color = Cyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Text(
                "Open System प्रयोगशाला",
                color = White,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Refresh Theory पर आधारित प्रयोगात्मक मॉडल",
                color = Muted,
                fontSize = 14.sp
            )
        }

        NetworkCanvas(
            movement = movement,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = "STABILITY",
                value = "$stability%",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "REFRESHES",
                value = refreshCount.toString(),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "MEMORIES",
                value = records.size.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        SectionTitle("01 — Refresh Process")

        Text(
            "Refresh करने पर सिस्टम का refresh counter बढ़ेगा। " +
                "यह एक सिमुलेशन है, वास्तविक चेतना का प्रमाण नहीं।",
            color = Muted,
            fontSize = 14.sp
        )

        Button(
            onClick = {
                refreshCount++
                stability = (stability - 1).coerceAtLeast(0)

                prefs.edit()
                    .putInt("refresh_count", refreshCount)
                    .putInt("stability", stability)
                    .apply()

                testResult = "Refresh पूरा हुआ। Stability अब $stability% है।"
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Cyan,
                contentColor = Bg
            )
        ) {
            Text("RUN REFRESH", fontWeight = FontWeight.Bold)
        }

        SectionTitle("02 — Information Retention")

        Text(
            "यहाँ कोई विचार, अनुभव या जानकारी लिखकर रिकॉर्ड सेव करो।",
            color = Muted,
            fontSize = 14.sp
        )

        BasicTextField(
            value = memoryText,
            onValueChange = { memoryText = it },
            modifier = Modifier
                .fillMaxWidth()
                .background(Panel, RoundedCornerShape(12.dp))
                .padding(14.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                color = White,
                fontSize = 15.sp
            ),
            minLines = 3,
            decorationBox = { innerTextField ->
                Box {
                    if (memoryText.isEmpty()) {
                        Text(
                            "अपनी जानकारी यहाँ लिखो...",
                            color = Muted
                        )
                    }
                    innerTextField()
                }
            }
        )

        Button(
            onClick = {
                val cleanText = memoryText.trim()

                if (cleanText.isNotEmpty()) {
                    val newRecord = MemoryRecord(
                        text = cleanText,
                        time = System.currentTimeMillis(),
                        score = stability
                    )

                    records = listOf(newRecord) + records
                    saveRecords(prefs, records)
                    memoryText = ""
                    testResult = "जानकारी सफलतापूर्वक सेव हुई।"
                } else {
                    testResult = "पहले कोई जानकारी लिखो।"
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Green,
                contentColor = Bg
            )
        ) {
            Text("SAVE MEMORY RECORD", fontWeight = FontWeight.Bold)
        }

        SectionTitle("03 — Stability Test Lab")

        Text(
            "इस सरल परीक्षण में लगातार refresh करने पर Stability Score घटता है। " +
                "यह केवल चुने गए मॉडल का नियम है, प्रकृति का प्रमाणित नियम नहीं।",
            color = Muted,
            fontSize = 14.sp
        )

        LinearProgressIndicator(
            progress = { stability / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = if (stability > 50) Green else Color(0xFFFFB454),
            trackColor = Panel,
            strokeCap = StrokeCap.Round
        )

        Text(
            "वर्तमान Stability: $stability%",
            color = White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        OutlinedButton(
            onClick = {
                refreshCount = 0
                stability = 100
                testResult = "टेस्ट रीसेट हुआ। Stability 100% पर है।"

                prefs.edit()
                    .putInt("refresh_count", refreshCount)
                    .putInt("stability", stability)
                    .apply()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("RESET TEST", color = Cyan)
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Panel),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = testResult,
                modifier = Modifier.padding(14.dp),
                color = White,
                fontSize = 14.sp
            )
        }

        SectionTitle("04 — Saved Memory Records")

        if (records.isEmpty()) {
            Text(
                "अभी कोई रिकॉर्ड सेव नहीं है।",
                color = Muted
            )
        } else {
            records.forEachIndexed { index, record ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Panel
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Record ${index + 1}",
                            color = Cyan,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            record.text,
                            color = White,
                            fontSize = 15.sp
                        )

                        Text(
                            "Saved: ${java.text.SimpleDateFormat(
                                "dd MMM yyyy, HH:mm",
                                java.util.Locale.getDefault()
                            ).format(java.util.Date(record.time))}",
                            color = Muted,
                            fontSize = 12.sp
                        )

                        Text(
                            "उस समय Stability: ${record.score}%",
                            color = Green,
                            fontSize = 12.sp
                        )

                        OutlinedButton(
                            onClick = {
                                records = records.filterIndexed { i, _ ->
                                    i != index
                                }
                                saveRecords(prefs, records)
                                testResult = "रिकॉर्ड हटाया गया।"
                            }
                        ) {
                            Text("DELETE RECORD", color = Color(0xFFFF8A8A))
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    records = emptyList()
                    saveRecords(prefs, records)
                    testResult = "सभी Memory Records हटाए गए।"
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("DELETE ALL RECORDS", color = Color(0xFFFF8A8A))
            }
        }

        Text(
            "Refresh Theory Lab • Experimental Simulation",
            color = Muted,
            fontSize = 12.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .wrapContentWidth(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(
                title,
                color = Muted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                value,
                color = Cyan,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        color = White,
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun NetworkCanvas(
    movement: Float,
    modifier: Modifier = Modifier
) {
    val points = remember {
        listOf(
            0.15f to 0.30f,
            0.32f to 0.18f,
            0.50f to 0.34f,
            0.72f to 0.20f,
            0.86f to 0.43f,
            0.23f to 0.65f,
            0.46f to 0.76f,
            0.68f to 0.63f,
            0.83f to 0.80f
        )
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(16.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            val w = size.width
            val h = size.height

            val centers = points.mapIndexed { index, point ->
                val angle = movement + index * 0.75f
                Offset(
                    x = (point.first * w) +
                        cos(angle) * 9f,
                    y = (point.second * h) +
                        sin(angle) * 9f
                )
            }

            for (i in centers.indices) {
                for (j in i + 1 until centers.size) {
                    val dx = centers[i].x - centers[j].x
                    val dy = centers[i].y - centers[j].y
                    val distance = kotlin.math.sqrt(dx * dx + dy * dy)

                    if (distance < w * 0.32f) {
                        drawLine(
                            color = Cyan.copy(alpha = 0.35f),
                            start = centers[i],
                            end = centers[j],
                            strokeWidth = 1.5f
                        )
                    }
                }
            }

            centers.forEachIndexed { index, center ->
                drawCircle(
                    color = if (index % 3 == 0) Green else Cyan,
                    radius = if (index == 4) 9f else 6f,
                    center = center
                )
                drawCircle(
                    color = Cyan.copy(alpha = 0.12f),
                    radius = 15f,
                    center = center
                )
            }
        }
    }
}

private fun loadRecords(
    prefs: android.content.SharedPreferences
): List<MemoryRecord> {
    return try {
        val json = JSONArray(prefs.getString("records", "[]") ?: "[]")
        val result = mutableListOf<MemoryRecord>()

        for (i in 0 until json.length()) {
            val item = json.getJSONObject(i)
            result.add(
                MemoryRecord(
                    text = item.optString("text"),
                    time = item.optLong("time"),
                    score = item.optInt("score", 100)
                )
            )
        }

        result
    } catch (_: Exception) {
        emptyList()
    }
}

private fun saveRecords(
    prefs: android.content.SharedPreferences,
    records: List<MemoryRecord>
) {
    val json = JSONArray()

    records.forEach { record ->
        json.put(
            JSONObject()
                .put("text", record.text)
                .put("time", record.time)
                .put("score", record.score)
        )
    }

    prefs.edit()
        .putString("records", json.toString())
        .apply()
}
