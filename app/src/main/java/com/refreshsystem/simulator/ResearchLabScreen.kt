
package com.refreshsystem.simulator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.*
import kotlin.random.Random

/* =========================================================
   REFRESH SYSTEM THEME
   ========================================================= */

private val RTGreen = Color(0xFF66BB6A)
private val RTBlue = Color(0xFF42A5F5)
private val RTOrange = Color(0xFFFFB74D)
private val RTViolet = Color(0xFF9C8CFF)
private val RTCyan = Color(0xFF38BDF8)
private val RTPurple = Color(0xFFBA8CFF)

private val RTBackground = Color(0xFF0B1020)
private val RTSurface = Color(0xFF171F32)
private val RTSurface2 = Color(0xFF202B40)
private val RTText = Color(0xFFF4F6FC)
private val RTSubText = Color(0xFFADB8CC)
private val RTAccent = Color(0xFFFF526B)
private val RTBorder = Color(0xFF34415A)

private fun Float.fmt(digits: Int = 2): String =
    String.format(Locale.US, "%.${digits}f", this)

private fun Float.safe01(): Float =
    coerceIn(0f, 1f)

/* =========================================================
   MAIN RESEARCH DATA MODEL
   ========================================================= */

private data class MainPoint(
    val cycle: Int,
    val refresh: Float,
    val dynamicStability: Float,
    val memory: Float,
    val recording: Float,
    val retrieval: Float
)

private data class MainResearchState(
    val cycle: Int = 0,
    val input: Float = 0.60f,
    val output: Float = 0.35f,
    val refreshRate: Float = 0.65f,
    val refreshEnabled: Boolean = true,
    val memoryEnabled: Boolean = true,
    val recordingRate: Float = 0.50f,
    val retentionRate: Float = 0.90f,
    val retrievalRate: Float = 0.60f,
    val systemActivity: Float = 1f,
    val dynamicStability: Float = 0f,
    val memoryValue: Float = 0f,
    val recording: Float = 0f,
    val retrieval: Float = 0f,
    val informationInput: Float = 0f,
    val informationLost: Float = 0f,
    val continuity: Float = 1f,
    val totalRefresh: Int = 0,
    val history: List<MainPoint> = emptyList()
)

private fun initialMainState(): MainResearchState {
    return MainResearchState(
        dynamicStability = 0.50f,
        memoryValue = 0.20f,
        recording = 0.20f,
        retrieval = 0.10f
    )
}

/* =========================================================
   MAIN MATHEMATICAL MODEL
   ========================================================= */

private fun nextMainState(
    old: MainResearchState
): MainResearchState {

    val dt = 1f

    val refreshActivity =
        if (old.refreshEnabled) old.refreshRate else 0f

    val exchange = old.input - old.output

    val activityChange =
        exchange * 0.08f +
            refreshActivity * 0.06f -
            0.025f

    val activity = (
        old.systemActivity + activityChange * dt
    ).safe01()

    val refreshValue = refreshActivity * activity

    val newInformation =
        if (old.memoryEnabled) {
            old.input * old.recordingRate
        } else {
            0f
        }

    val retainedMemory =
        if (old.memoryEnabled) {
            old.memoryValue * old.retentionRate
        } else {
            0f
        }

    val memory = (
        retainedMemory +
            newInformation * refreshValue * 0.12f
    ).safe01()

    val recording = (
        old.recording * 0.92f +
            newInformation * refreshValue * 0.08f
    ).safe01()

    val retrieval = if (old.memoryEnabled) {
        (
            memory *
                old.retrievalRate *
                (0.5f + refreshValue * 0.5f)
        ).safe01()
    } else {
        0f
    }

    val exchangeBalance = (
        1f - abs(old.input - old.output)
    ).safe01()

    val activityContinuity = (
        1f - abs(activity - old.systemActivity)
    ).safe01()

    val memoryContinuity = if (old.memoryEnabled) {
        (
            1f - abs(memory - old.memoryValue)
        ).safe01()
    } else {
        0f
    }

    val dynamicStability = (
        exchangeBalance * 0.25f +
            activityContinuity * 0.25f +
            refreshValue * 0.30f +
            memoryContinuity * 0.20f
    ).safe01()

    val continuity = (
        old.continuity * 0.70f +
            dynamicStability * 0.30f
    ).safe01()

    val lost = (
        old.memoryValue - memory
    ).coerceAtLeast(0f)

    val nextCycle = old.cycle + 1

    val point = MainPoint(
        cycle = nextCycle,
        refresh = refreshValue,
        dynamicStability = dynamicStability,
        memory = memory,
        recording = recording,
        retrieval = retrieval
    )

    return old.copy(
        cycle = nextCycle,
        systemActivity = activity,
        dynamicStability = dynamicStability,
        memoryValue = memory,
        recording = recording,
        retrieval = retrieval,
        informationInput = old.informationInput + newInformation,
        informationLost = old.informationLost + lost,
        continuity = continuity,
        totalRefresh = old.totalRefresh +
            if (refreshValue > 0f) 1 else 0,
        history = (old.history + point).takeLast(100)
    )
}

/* =========================================================
   MICROSTRUCTURE DATA
   ========================================================= */

private data class MicroParticle(
    val id: Int,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val phase: Float
)

private data class MicroBond(
    val a: Int,
    val b: Int
)

private data class MicroSnapshot(
    val cycle: Int,
    val particles: Int,
    val bonds: Int
)

private data class MicroState(
    val cycle: Int = 0,
    val particles: List<MicroParticle> = emptyList(),
    val bonds: List<MicroBond> = emptyList(),
    val inputRate: Float = 0.15f,
    val outputRate: Float = 0.08f,
    val refreshRate: Float = 0.45f,
    val attraction: Float = 0.40f,
    val repulsion: Float = 0.55f,
    val connectionRange: Float = 0.12f,
    val totalInput: Int = 0,
    val totalOutput: Int = 0,
    val formed: Int = 0,
    val broken: Int = 0,
    val continuity: Float = 1f,
    val drift: Float = 0f,
    val snapshots: List<MicroSnapshot> = emptyList()
)

private const val MICRO_MAX = 100
private const val MICRO_MIN = 20
private const val MICRO_NEIGHBORS = 4

private fun microDistance(
    a: MicroParticle,
    b: MicroParticle
): Float {
    return sqrt(
        (a.x - b.x).pow(2) +
            (a.y - b.y).pow(2)
    )
}

private fun microBondKey(b: MicroBond): String =
    "${minOf(b.a, b.b)}-${maxOf(b.a, b.b)}"

private fun calculateMicroBonds(
    particles: List<MicroParticle>,
    range: Float
): List<MicroBond> {

    val keys = mutableSetOf<String>()

    particles.forEach { p ->
        particles
            .asSequence()
            .filter { it.id != p.id }
            .map { it to microDistance(p, it) }
            .filter { it.second < range }
            .sortedBy { it.second }
            .take(MICRO_NEIGHBORS)
            .forEach { (other, _) ->
                val a = minOf(p.id, other.id)
                val b = maxOf(p.id, other.id)
                keys.add("$a-$b")
            }
    }

    return keys.mapNotNull { key ->
        val parts = key.split("-")
        val a = parts.getOrNull(0)?.toIntOrNull()
        val b = parts.getOrNull(1)?.toIntOrNull()

        if (a != null && b != null) MicroBond(a, b) else null
    }
}

private fun microAverage(
    particles: List<MicroParticle>
): Float {
    if (particles.isEmpty()) return 0f

    return particles.sumOf {
        (it.x + it.y).toDouble()
    }.toFloat() / (particles.size * 2f)
}

private fun initialMicroState(): MicroState {
    val random = Random(73)

    val particles = (0 until 65).map { i ->
        MicroParticle(
            id = i + 1,
            x = 0.10f + random.nextFloat() * 0.80f,
            y = 0.10f + random.nextFloat() * 0.80f,
            vx = (random.nextFloat() - 0.5f) * 0.004f,
            vy = (random.nextFloat() - 0.5f) * 0.004f,
            phase = random.nextFloat() * 6.28f
        )
    }

    return MicroState(
        particles = particles,
        bonds = calculateMicroBonds(particles, 0.12f)
    )
}

/* =========================================================
   MICROSTRUCTURE REFRESH ENGINE
   ========================================================= */

private fun nextMicroState(
    old: MicroState
): MicroState {

    val random = Random(old.cycle * 7919 + 31)
    val particles = old.particles.toMutableList()

    var totalInput = old.totalInput
    var totalOutput = old.totalOutput

    val inputCount = (
        old.inputRate * 3f +
            if (random.nextFloat() < old.inputRate) 1f else 0f
    ).toInt().coerceIn(0, 4)

    repeat(inputCount) {
        if (particles.size < MICRO_MAX) {
            val side = random.nextInt(4)

            val x = when (side) {
                0 -> 0.04f
                1 -> 0.96f
                else -> random.nextFloat()
            }

            val y = when (side) {
                2 -> 0.04f
                3 -> 0.96f
                else -> random.nextFloat()
            }

            val id = (
                particles.maxOfOrNull { it.id } ?: 0
            ) + 1

            particles.add(
                MicroParticle(
                    id,
                    x,
                    y,
                    (0.5f - x) * 0.004f,
                    (0.5f - y) * 0.004f,
                    random.nextFloat() * 6.28f
                )
            )

            totalInput++
        }
    }

    val moved = particles.map { p ->
        var fx = 0f
        var fy = 0f
        var neighbors = 0

        particles.forEach { other ->
            if (p.id != other.id) {
                val dx = other.x - p.x
                val dy = other.y - p.y
                val d = sqrt(dx * dx + dy * dy)

                if (d > 0.0001f &&
                    d < old.connectionRange * 1.8f
                ) {
                    val nx = dx / d
                    val ny = dy / d
                    val preferred = 0.075f

                    if (d < preferred) {
                        val push = (
                            (preferred - d) *
                                old.repulsion * 0.018f
                        ).coerceAtMost(0.002f)

                        fx -= nx * push
                        fy -= ny * push
                    } else {
                        val pull = (
                            (d - preferred) *
                                old.attraction * 0.004f
                        ).coerceAtMost(0.0015f)

                        fx += nx * pull
                        fy += ny * pull
                    }

                    neighbors++
                }
            }
        }

        if (neighbors > 0) {
            fx /= neighbors.toFloat()
            fy /= neighbors.toFloat()
        }

        val noise = old.refreshRate * 0.0008f

        val vx = (
            p.vx * 0.88f +
                fx +
                (random.nextFloat() - 0.5f) * noise
        ).coerceIn(-0.004f, 0.004f)

        val vy = (
            p.vy * 0.88f +
                fy +
                (random.nextFloat() - 0.5f) * noise
        ).coerceIn(-0.004f, 0.004f)

        p.copy(
            x = (p.x + vx).coerceIn(0.035f, 0.965f),
            y = (p.y + vy).coerceIn(0.035f, 0.965f),
            vx = vx,
            vy = vy,
            phase = p.phase + old.refreshRate * 0.003f
        )
    }

    val outputCount = (
        old.outputRate * 3f +
            if (random.nextFloat() < old.outputRate) 1f else 0f
    ).toInt().coerceIn(0, 4)

    val finalParticles = moved.toMutableList()

    repeat(outputCount) {
        if (finalParticles.size > MICRO_MIN) {
            finalParticles.removeAt(
                random.nextInt(finalParticles.size)
            )
            totalOutput++
        }
    }

    val newBonds = calculateMicroBonds(
        finalParticles,
        old.connectionRange
    )

    val oldKeys = old.bonds.map { microBondKey(it) }.toSet()
    val newKeys = newBonds.map { microBondKey(it) }.toSet()

    val formed = newKeys.subtract(oldKeys).size
    val broken = oldKeys.subtract(newKeys).size

    val common = oldKeys.intersect(newKeys).size
    val union = oldKeys.union(newKeys).size

    val similarity = if (union == 0) {
        1f
    } else {
        common.toFloat() / union
    }

    val drift = abs(
        microAverage(finalParticles) -
            microAverage(old.particles)
    )

    val continuity = (
        similarity * 0.70f +
            (1f - drift * 3f).safe01() * 0.30f
    ).safe01()

    val nextCycle = old.cycle + 1

    var snapshots = old.snapshots

    if (nextCycle % 30 == 0) {
        snapshots = (
            snapshots + MicroSnapshot(
                nextCycle,
                finalParticles.size,
                newBonds.size
            )
        ).takeLast(20)
    }

    return old.copy(
        cycle = nextCycle,
        particles = finalParticles,
        bonds = newBonds,
        totalInput = totalInput,
        totalOutput = totalOutput,
        formed = old.formed + formed,
        broken = old.broken + broken,
        continuity = continuity,
        drift = drift,
        snapshots = snapshots
    )
}

/* =========================================================
   MAIN SCREEN
   ========================================================= */

@Composable
fun ResearchHomeScreen() {

    var selectedLab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RTBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(RTBackground)
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Text(
                "Refresh System",
                color = RTText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(Modifier.height(5.dp))

            Text(
                "Research & Development",
                color = RTSubText,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        TabRow(
            selectedTabIndex = selectedLab,
            containerColor = RTSurface,
            contentColor = RTAccent
        ) {
            Tab(
                selected = selectedLab == 0,
                onClick = { selectedLab = 0 },
                text = {
                    Text(
                        "Main Research",
                        fontWeight = FontWeight.Bold
                    )
                }
            )

            Tab(
                selected = selectedLab == 1,
                onClick = { selectedLab = 1 },
                text = {
                    Text(
                        "Microstructure",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(RTBackground)
        ) {
            if (selectedLab == 0) {
                MainRefreshResearchLab()
            } else {
                MicrostructureLab()
            }
        }
    }
}

/* =========================================================
   REUSABLE RESEARCH CARD
   ========================================================= */

@Composable
private fun ResearchCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = RTSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                title,
                color = RTText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            content()
        }
    }
}

/* =========================================================
   MAIN REFRESH THEORY RESEARCH LAB
   ========================================================= */

@Composable
private fun MainRefreshResearchLab() {

    var state by remember {
        mutableStateOf(initialMainState())
    }

    var running by remember {
        mutableStateOf(true)
    }

    var experiment by remember {
        mutableStateOf("D")
    }

    LaunchedEffect(running) {
        while (running) {
            delay(700L)
            state = nextMainState(state)
        }
    }

    fun selectExperiment(item: String) {
        experiment = item

        val base = initialMainState()

        state = when (item) {
            "A" -> base.copy(
                refreshEnabled = false,
                memoryEnabled = false
            )

            "B" -> base.copy(
                refreshEnabled = true,
                memoryEnabled = false
            )

            "C" -> base.copy(
                refreshEnabled = false,
                memoryEnabled = true
            )

            else -> base.copy(
                refreshEnabled = true,
                memoryEnabled = true
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                "Dynamic Stability + Memory",
                color = RTText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                "Open System • Continuous Refresh • Chetna Research",
                color = RTSubText,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        /* OPEN SYSTEM FLOW */

        ResearchCard("OPEN SYSTEM") {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FlowBox(
                    "INPUT",
                    RTBlue,
                    Modifier.weight(1f)
                )

                FlowBox(
                    "REFRESH",
                    RTGreen,
                    Modifier.weight(1f)
                )

                FlowBox(
                    "OUTPUT",
                    RTOrange,
                    Modifier.weight(1f)
                )
            }

            ProcessArrow()

            ProcessBox(
                "DYNAMIC STABILITY",
                RTViolet,
                "Continuous system activity"
            )

            ProcessArrow()

            ProcessBox(
                "RECORDING & MEMORY",
                RTPurple,
                "Information recording and retention"
            )

            ProcessArrow()

            ProcessBox(
                "CHETNA RESEARCH",
                RTOrange,
                "Research into experience and awareness"
            )
        }

        /* CONTROLLED EXPERIMENTS */

        ResearchCard("CONTROLLED EXPERIMENTS") {

            Text(
                "Refresh और Memory के प्रभावों की तुलना करें।",
                color = RTSubText
            )

            val descriptions = listOf(
                "Refresh OFF • Memory OFF",
                "Refresh ON • Memory OFF",
                "Refresh OFF • Memory ON",
                "Refresh ON • Memory ON"
            )

            descriptions.forEachIndexed { index, description ->

                val key = listOf("A", "B", "C", "D")[index]
                val selected = experiment == key

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { selectExperiment(key) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) {
                        RTAccent.copy(alpha = 0.15f)
                    } else {
                        RTSurface2
                    },
                    border = if (selected) {
                        androidx.compose.foundation.BorderStroke(
                            1.dp,
                            RTAccent
                        )
                    } else {
                        androidx.compose.foundation.BorderStroke(
                            1.dp,
                            RTBorder
                        )
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            key,
                            color = if (selected) RTAccent else RTText,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleLarge
                        )

                        Spacer(Modifier.width(14.dp))

                        Text(
                            description,
                            color = RTText,
                            modifier = Modifier.weight(1f)
                        )

                        if (selected) {
                            Text(
                                "SELECTED",
                                color = RTAccent,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            Text(
                "Active Experiment: $experiment",
                color = RTAccent,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { running = !running },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RTAccent
                    )
                ) {
                    Text(if (running) "Pause" else "Resume")
                }

                OutlinedButton(
                    onClick = {
                        running = false
                        selectExperiment(experiment)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset")
                }
            }

            OutlinedButton(
                onClick = {
                    state = nextMainState(state)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Run One Cycle")
            }
        }

        /* SYSTEM CONTROLS */

        ResearchCard("OPEN SYSTEM CONTROLS") {

            ControlSlider(
                "External Input",
                state.input
            ) {
                state = state.copy(input = it)
            }

            ControlSlider(
                "External Output",
                state.output
            ) {
                state = state.copy(output = it)
            }

            ControlSlider(
                "Refresh Rate",
                state.refreshRate
            ) {
                state = state.copy(refreshRate = it)
            }

            HorizontalDivider(
                color = RTBorder
            )

            Text(
                "MEMORY CONTROLS",
                color = RTPurple,
                fontWeight = FontWeight.Bold
            )

            ControlSlider(
                "Recording Rate",
                state.recordingRate
            ) {
                state = state.copy(recordingRate = it)
            }

            ControlSlider(
                "Retention Rate",
                state.retentionRate
            ) {
                state = state.copy(retentionRate = it)
            }

            ControlSlider(
                "Retrieval Rate",
                state.retrievalRate
            ) {
                state = state.copy(retrievalRate = it)
            }
        }

        /* MATHEMATICAL RESULTS */

        ResearchCard("MATHEMATICAL RESULTS") {

            ResultLine("Simulation Cycle", state.cycle.toString())

            ResultLine(
                "System Activity",
                "${(state.systemActivity * 100).toInt()}%"
            )

            ResultLine(
                "Refresh Activity",
                "${(state.refreshRate * if (state.refreshEnabled) 100 else 0).toInt()}%"
            )

            ResultLine(
                "Dynamic Stability",
                "${(state.dynamicStability * 100).toInt()}%"
            )

            ResultLine(
                "Memory",
                "${(state.memoryValue * 100).toInt()}%"
            )

            ResultLine(
                "Recording",
                "${(state.recording * 100).toInt()}%"
            )

            ResultLine(
                "Retrieval",
                "${(state.retrieval * 100).toInt()}%"
            )

            ResultLine(
                "Continuity",
                "${(state.continuity * 100).toInt()}%"
            )

            ResultLine(
                "Total Refresh Cycles",
                state.totalRefresh.toString()
            )
        }

        /* RESEARCH GRAPH */

        ResearchCard("RESEARCH GRAPH") {

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GraphLegend(RTGreen, "Refresh")
                GraphLegend(RTViolet, "Stability")
                GraphLegend(RTPurple, "Memory")
            }

            ResearchGraph(
                history = state.history,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )
        }

        /* MATHEMATICAL MODEL */

        ResearchCard("MATHEMATICAL MODEL") {

            EquationLine(
                "Open System",
                "X(t+1) = F(X, Input, Output, Refresh)"
            )

            EquationLine(
                "Dynamic Stability",
                "D(t) = f(Exchange, Activity, Refresh, Memory)"
            )

            EquationLine(
                "Memory",
                "M(t+1) = G(M(t), Recording, Refresh)"
            )

            Text(
                "ये हमारे Research Model के शुरुआती समीकरण हैं। प्रयोगों के परिणामों के अनुसार इन्हें विकसित किया जाएगा।",
                color = RTSubText
            )
        }

        /* EXPERIMENT RECORD */

        ResearchCard("EXPERIMENT RECORD") {

            ResultLine("Experiment", experiment)

            ResultLine(
                "Cycles",
                state.cycle.toString()
            )

            ResultLine(
                "Refresh",
                if (state.refreshEnabled) "ON" else "OFF"
            )

            ResultLine(
                "Memory",
                if (state.memoryEnabled) "ON" else "OFF"
            )

            ResultLine(
                "Final Dynamic Stability",
                "${(state.dynamicStability * 100).toInt()}%"
            )

            ResultLine(
                "Final Memory",
                "${(state.memoryValue * 100).toInt()}%"
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

/* =========================================================
   MICROSTRUCTURE LAB
   ========================================================= */

@Composable
private fun MicrostructureLab() {

    var state by remember {
        mutableStateOf(initialMicroState())
    }

    var running by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(running) {
        while (running) {
            delay(650L)
            state = nextMicroState(state)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                "Microstructure Research Lab",
                color = RTText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                "Local Connections • Structural Refresh • Pattern Continuity",
                color = RTSubText
            )
        }

        ResearchCard("MICROSTRUCTURE SIMULATION") {

            MicroCanvas(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                GraphLegend(RTGreen, "Particles")
                GraphLegend(RTCyan, "Connections")
                GraphLegend(RTBlue, "Input")
                GraphLegend(RTOrange, "Output")
            }
        }

        ResearchCard("MICROSTRUCTURE STATUS") {

            ResultLine("Cycles", state.cycle.toString())
            ResultLine("Particles", state.particles.size.toString())
            ResultLine("Connections", state.bonds.size.toString())
            ResultLine("Connections Formed", state.formed.toString())
            ResultLine("Connections Broken", state.broken.toString())
            ResultLine("Input Total", state.totalInput.toString())
            ResultLine("Output Total", state.totalOutput.toString())

            ResultLine(
                "Continuity",
                "${(state.continuity * 100).toInt()}%"
            )

            ResultLine(
                "Pattern Drift",
                state.drift.fmt(4)
            )
        }

        ResearchCard("MICROSTRUCTURE CONTROLS") {

            ControlSlider(
                "Input",
                state.inputRate
            ) {
                state = state.copy(inputRate = it)
            }

            ControlSlider(
                "Output",
                state.outputRate
            ) {
                state = state.copy(outputRate = it)
            }

            ControlSlider(
                "Refresh",
                state.refreshRate
            ) {
                state = state.copy(refreshRate = it)
            }

            ControlSlider(
                "Attraction",
                state.attraction
            ) {
                state = state.copy(attraction = it)
            }

            ControlSlider(
                "Repulsion",
                state.repulsion
            ) {
                state = state.copy(repulsion = it)
            }

            Text(
                "Connection Range: ${state.connectionRange.fmt(2)}",
                color = RTText
            )

            Slider(
                value = state.connectionRange,
                onValueChange = {
                    state = state.copy(connectionRange = it)
                },
                valueRange = 0.05f..0.22f
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { running = !running },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (running) "Pause" else "Resume")
                }

                OutlinedButton(
                    onClick = {
                        running = false
                        state = initialMicroState()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset")
                }
            }

            OutlinedButton(
                onClick = {
                    state = nextMicroState(state)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Run One Cycle")
            }
        }

        ResearchCard("PATTERN SNAPSHOTS") {

            if (state.snapshots.isEmpty()) {
                Text(
                    "Snapshots will appear after 30 simulation cycles.",
                    color = RTSubText
                )
            } else {
                state.snapshots
                    .takeLast(8)
                    .reversed()
                    .forEach {
                        Text(
                            "Cycle ${it.cycle}  |  Particles ${it.particles}  |  Connections ${it.bonds}",
                            color = RTText
                        )
                    }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

/* =========================================================
   REUSABLE UI COMPONENTS
   ========================================================= */

@Composable
private fun FlowBox(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            color.copy(alpha = 0.35f)
        )
    ) {
        Box(
            modifier = Modifier.padding(
                horizontal = 4.dp,
                vertical = 16.dp
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                color = color,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun ProcessArrow() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "↓",
            color = RTViolet,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ProcessBox(
    title: String,
    color: Color,
    subtitle: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        color = color.copy(alpha = 0.13f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            color.copy(alpha = 0.40f)
        )
    ) {
        Column(
            Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                title,
                color = color,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                subtitle,
                color = RTSubText,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ControlSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = RTText)

            Text(
                "${(value * 100).toInt()}%",
                color = RTCyan,
                fontWeight = FontWeight.Bold
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange
        )
    }
}

@Composable
private fun EquationLine(
    title: String,
    equation: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            title,
            color = RTCyan,
            fontWeight = FontWeight.Bold
        )

        Surface(
            color = RTSurface2,
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                equation,
                color = RTText,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
private fun ResultLine(
    label: String,
    value: String
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = RTSubText,
            modifier = Modifier.weight(1f)
        )

        Text(
            value,
            color = RTText,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun GraphLegend(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(Modifier.size(9.dp)) {
            drawCircle(color)
        }

        Spacer(Modifier.width(5.dp))

        Text(
            label,
            color = RTSubText,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

/* =========================================================
   RESEARCH GRAPH
   ========================================================= */

@Composable
private fun ResearchGraph(
    history: List<MainPoint>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {

        val left = 12.dp.toPx()
        val right = size.width - 12.dp.toPx()
        val top = 12.dp.toPx()
        val bottom = size.height - 12.dp.toPx()

        val graphWidth = right - left
        val graphHeight = bottom - top

        repeat(5) { i ->
            val y = top + graphHeight * i / 4f

            drawLine(
                color = Color.Gray.copy(alpha = 0.25f),
                start = Offset(left, y),
                end = Offset(right, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        if (history.size < 2) return@Canvas

        fun drawSeries(
            values: List<Float>,
            color: Color
        ) {
            for (i in 0 until values.size - 1) {

                val x1 = left +
                    graphWidth * i /
                    (values.size - 1).toFloat()

                val x2 = left +
                    graphWidth * (i + 1) /
                    (values.size - 1).toFloat()

                val y1 = bottom -
                    values[i].safe01() * graphHeight

                val y2 = bottom -
                    values[i + 1].safe01() * graphHeight

                drawLine(
                    color = color,
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = 2.5.dp.toPx()
                )
            }
        }

        drawSeries(
            history.map { it.refresh },
            RTGreen
        )

        drawSeries(
            history.map { it.dynamicStability },
            RTViolet
        )

        drawSeries(
            history.map { it.memory },
            RTPurple
        )
    }
}

/* =========================================================
   MICROSTRUCTURE CANVAS
   ========================================================= */

@Composable
private fun MicroCanvas(
    state: MicroState,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0B1220))
    ) {

        val sx = size.width
        val sy = size.height

        val left = sx * 0.05f
        val right = sx * 0.95f
        val top = sy * 0.05f
        val bottom = sy * 0.95f

        drawRoundRect(
            color = RTViolet.copy(alpha = 0.75f),
            topLeft = Offset(left, top),
            size = Size(
                right - left,
                bottom - top
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                15.dp.toPx()
            ),
            style = Stroke(width = 1.5.dp.toPx())
        )

        val map = state.particles.associateBy { it.id }

        state.bonds.forEach { bond ->
            val a = map[bond.a]
            val b = map[bond.b]

            if (a != null && b != null) {
                drawLine(
                    color = RTCyan.copy(alpha = 0.48f),
                    start = Offset(a.x * sx, a.y * sy),
                    end = Offset(b.x * sx, b.y * sy),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        state.particles.forEach { p ->

            val point = Offset(
                p.x * sx,
                p.y * sy
            )

            val radius = (
                3.2f + sin(p.phase) * 0.35f
            ).coerceAtLeast(2.5f).dp.toPx()

            drawCircle(
                color = RTGreen.copy(alpha = 0.16f),
                radius = radius * 2f,
                center = point
            )

            drawCircle(
                color = RTGreen,
                radius = radius,
                center = point
            )
        }

        drawCircle(
            color = RTBlue,
            radius = 5.dp.toPx(),
            center = Offset(left, sy * 0.5f)
        )

        drawCircle(
            color = RTOrange,
            radius = 5.dp.toPx(),
            center = Offset(right, sy * 0.5f)
        )
    }
}
