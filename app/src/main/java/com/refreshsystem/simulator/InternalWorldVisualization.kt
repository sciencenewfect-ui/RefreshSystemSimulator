
package com.refreshsystem.simulator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.abs

private val WorldBg = Color(0xFF0D1729)
private val WorldPanel = Color(0xFF18263D)
private val WorldCyan = Color(0xFF52E5F2)
private val WorldGreen = Color(0xFF62E6A5)
private val WorldPurple = Color(0xFFB69CFF)
private val WorldOrange = Color(0xFFFFB86B)
private val WorldMuted = Color(0xFF9EABC2)
private val WorldLine = Color(0xFF34445D)

private val GroupColors = listOf(
    WorldPurple,
    WorldCyan,
    WorldGreen,
    WorldOrange
)

private data class WorldConnection(
    val first: Int,
    val second: Int,
    val strength: Float
)

@Composable
fun InternalWorldVisualization(
    snapshot: OpenSystemSnapshot,
    weights: List<List<Float>>
) {
    val pattern = snapshot.internalPattern
    val memory = snapshot.memoryState
    val count = minOf(pattern.size, memory.size, 16)

    val connections = buildList {
        for (i in 0 until minOf(count, weights.size)) {
            for (j in i + 1 until minOf(count, weights[i].size)) {
                add(
                    WorldConnection(
                        first = i,
                        second = j,
                        strength = weights[i][j]
                    )
                )
            }
        }
    }.sortedByDescending { abs(it.strength) }.take(8)

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "INTERNAL WORLD: ANDAR KYA HO RAHA HAI?",
            color = WorldCyan
        )

        Text(
            "Yahan 16 fixed ikaiyan hain. Har ikai ka Pattern " +
                "aur Memory badal sakta hai. Unke beech sambandh " +
                "seekhne ke saath mazboot ya kamzor hote hain.",
            color = WorldMuted
        )

        // Vartaman system ki sthiti
        SectionTitle("1. SYSTEM KI VARTAMAN STITHI")

        ValueRow("Cycle", snapshot.cycle.toString())
        ValueRow("External Information", percent(snapshot.informationLevel))
        ValueRow("Internal Refresh", if (snapshot.internalRefreshActive) "CHALU" else "BAND")
        ValueRow("Natural Decay", if (snapshot.naturalDecayActive) "CHALU" else "BAND")
        ValueRow("Refresh Count", snapshot.refreshCount.toString())

        // Chaar samooh aur unki 16 ikaiyan
        SectionTitle("2. ANDAR KI 16 IKAIYAN")

        Text(
            "Har box ek ikai hai. Neeli bar Pattern aur hari bar Memory " +
                "ki matra dikhati hai. Sankhya actual model value hai.",
            color = WorldMuted
        )

        for (group in 0 until 4) {
            val first = group * 4
            val last = minOf(first + 4, count)

            if (first < last) {
                GroupCard(
                    group = group,
                    pattern = pattern,
                    memory = memory,
                    first = first,
                    last = last
                )
            }
        }

        // Actual learned connections
        SectionTitle("3. IKAIYAN AAPAS MEIN KAISE JUDI HAIN?")

        Text(
            "Neeche model ke sabse mazboot 8 learned connections hain. " +
                "Positive value ka matlab positive weight aur negative " +
                "value ka matlab negative weight hai.",
            color = WorldMuted
        )

        if (connections.isEmpty() || connections.all { abs(it.strength) < 0.000001f }) {
            Text(
                "Abhi koi meaningful learned connection nahi bana hai. " +
                    "Simulation chalne par weights update honge.",
                color = WorldOrange
            )
        } else {
            connections.forEach { connection ->
                ConnectionRow(connection)
            }
        }

        // System measurements
        SectionTitle("4. SYSTEM MEIN KITNA BADLAV HO RAHA HAI?")

        Measurement(
            "Pattern mein badlav",
            snapshot.patternChange,
            WorldOrange,
            "%.4f".format(snapshot.patternChange)
        )

        Measurement(
            "Memory ki matra",
            snapshot.memoryRetention,
            WorldGreen,
            percent(snapshot.memoryRetention)
        )

        Measurement(
            "Pattern ki nirantarata",
            snapshot.identityContinuity,
            WorldPurple,
            percent(snapshot.identityContinuity)
        )

        Measurement(
            "Dynamic Stability",
            snapshot.dynamicStability,
            WorldCyan,
            percent(snapshot.dynamicStability)
        )

        Measurement(
            "Network ki taakat",
            snapshot.networkStrength,
            WorldGreen,
            percent(snapshot.networkStrength)
        )

        SectionTitle("5. IN NUMBERS KA MATLAB")

        MeaningRow(
            "Pattern",
            "Is samay ikai ke andar ka active pattern."
        )
        MeaningRow(
            "Memory",
            "Pichhle updates se bachi hui memory value."
        )
        MeaningRow(
            "Connection",
            "Do ikaiyon ke beech learned weight."
        )
        MeaningRow(
            "Refresh",
            "Pattern aur memory ko update karne ki prakriya."
        )
        MeaningRow(
            "Decay",
            "Pattern aur memory ki value ko dheere-dheere kam karna."
        )

        Text(
            "Dhyan dein: 16 ikaiyan abhi fixed hain. Is model mein " +
                "naye nodes ka banna ya purane nodes ka mitna implement " +
                "nahi hai. Connection weight bhi model ki mathematical " +
                "value hai, kisi physical distance ka maap nahi.",
            color = WorldMuted
        )
    }
}

@Composable
private fun GroupCard(
    group: Int,
    pattern: List<Float>,
    memory: List<Float>,
    first: Int,
    last: Int
) {
    val groupColor = GroupColors[group]

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = WorldPanel
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "SAMOOH ${group + 1}",
                color = groupColor
            )

            for (index in first until last) {
                NodeCard(
                    index = index,
                    pattern = pattern[index],
                    memory = memory[index],
                    color = groupColor
                )
            }
        }
    }
}

@Composable
private fun NodeCard(
    index: Int,
    pattern: Float,
    memory: Float,
    color: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                WorldBg,
                RoundedCornerShape(9.dp)
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(
            "IKAI ${index + 1}",
            color = color
        )

        ValueRow(
            "Pattern value",
            "%.4f".format(pattern)
        )

        LinearProgressIndicator(
            progress = abs(pattern).coerceIn(0f, 1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = WorldCyan,
            trackColor = WorldLine
        )

        ValueRow(
            "Memory value",
            "%.4f".format(memory)
        )

        LinearProgressIndicator(
            progress = abs(memory).coerceIn(0f, 1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = WorldGreen,
            trackColor = WorldLine
        )
    }
}

@Composable
private fun ConnectionRow(
    connection: WorldConnection
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                WorldPanel,
                RoundedCornerShape(9.dp)
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        ValueRow(
            "Ikai ${connection.first + 1}  ↔  Ikai ${connection.second + 1}",
            "%+.5f".format(connection.strength)
        )

        LinearProgressIndicator(
            progress = abs(connection.strength).coerceIn(0f, 1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = if (connection.strength >= 0f) WorldGreen else WorldOrange,
            trackColor = WorldLine
        )
    }
}

@Composable
private fun Measurement(
    label: String,
    value: Float,
    color: Color,
    display: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        ValueRow(label, display)

        LinearProgressIndicator(
            progress = value.coerceIn(0f, 1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp),
            color = color,
            trackColor = WorldLine
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        color = WorldCyan
    )
}

@Composable
private fun ValueRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = WorldMuted)
        Text(value, color = Color.White)
    }
}

@Composable
private fun MeaningRow(
    title: String,
    meaning: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(title, color = WorldGreen)
        Text(meaning, color = WorldMuted)
    }
}

private fun percent(value: Float): String {
    return "${(value.coerceIn(0f, 1f) * 100f).toInt()}%"
}
