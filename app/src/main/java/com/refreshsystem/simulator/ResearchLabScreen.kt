
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

private val RTGreen = Color(0xFF66BB6A)
private val RTBlue = Color(0xFF42A5F5)
private val RTOrange = Color(0xFFFFB74D)
private val RTViolet = Color(0xFF9C8CFF)
private val RTCyan = Color(0xFF38BDF8)
private val RTDark = Color(0xFF0B1220)
private val RTPurple = Color(0xFFBA8CFF)

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

   These are proposed simulator equations.
   ========================================================= */

private fun nextMainState(
    old: MainResearchState
): MainResearchState {

    val dt = 1f

    val refreshActivity =
        if (old.refreshEnabled) {
            old.refreshRate
        } else {
            0f
        }

    // External exchange

    val exchange = old.input - old.output

    // System activity changes through exchange and refresh.

    val activityChange =
        exchange * 0.08f +
            refreshActivity * 0.06f -
            0.025f

    val activity = (
        old.systemActivity + activityChange * dt
        ).safe01()

    // Refresh activity

    val refreshValue =
        refreshActivity * activity

    // Memory recording

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

    // Information recording

    val recording = (
        old.recording * 0.92f +
            newInformation * refreshValue * 0.08f
        ).safe01()

    // Retrieval

    val retrieval = if (old.memoryEnabled) {
        (
            memory *
                old.retrievalRate *
                (0.5f + refreshValue * 0.5f)
            ).safe01()
    } else {
        0f
    }

    // Dynamic Stability

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

    // Continuity

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

        if (a != null && b != null) {
            MicroBond(a, b)
        } else {
            null
        }
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
   MAIN ENTRY POINT
   ========================================================= */

@Composable
fun ResearchHomeScreen() {

    var selectedLab by remember {
        mutableStateOf(0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        Column(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 12.dp
            )
        ) {

            Text(
                "Refresh System",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Research & Development",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        TabRow(selectedTabIndex = selectedLab) {

            Tab(
                selected = selectedLab == 0,
                onClick = { selectedLab = 0 },
                text = { Text("Main Research") }
            )

            Tab(
                selected = selectedLab == 1,
                onClick = { selectedLab = 1 },
                text = { Text("Microstructure") }
            )
        }

        when (selectedLab) {

            0 -> MainRefreshResearchLab()

            1 -> MicrostructureLab()
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            "Refresh Theory Research Lab",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Open System • Continuous Refresh • Dynamic Stability • Memory"
        )

        // SYSTEM FLOW

        Card(Modifier.fillMaxWidth()) {

            Column(
                Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    "OPEN SYSTEM",
                    fontWeight = FontWeight.Bold
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {

                    FlowBox("INPUT", RTBlue)
                    FlowBox("REFRESH", RTGreen)
                    FlowBox("OUTPUT", RTOrange)
                }

                IconArrow()

                FlowBox(
                    "DYNAMIC STABILITY",
                    RTViolet
                )

                IconArrow()

                FlowBox(
                    "RECORDING & MEMORY",
                    RTPurple
                )

                IconArrow()

                FlowBox(
                    "CHETNA RESEARCH",
                    RTOrange
                )
            }
        }

        // EXPERIMENT SELECTION

        Card(Modifier.fillMaxWidth()) {

            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    "Controlled Experiments",
                    fontWeight = FontWeight.Bold
                )

                Text("A: Refresh OFF, Memory OFF")
                Text("B: Refresh ON, Memory OFF")
                Text("C: Refresh OFF, Memory ON")
                Text("D: Refresh ON, Memory ON")

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {

                    listOf("A", "B", "C", "D").forEach { item ->

                        FilterChip(
                            selected = experiment == item,
                            onClick = {
                                experiment = item

                                state = initialMainState()

                                state = when (item) {
                                    "A" -> state.copy(
                                        refreshEnabled = false,
                                        memoryEnabled = false
                                    )

                                    "B" -> state.copy(
                                        refreshEnabled = true,
                                        memoryEnabled = false
                                    )

                                    "C" -> state.copy(
                                        refreshEnabled = false,
                                        memoryEnabled = true
                                    )

                                    else -> state.copy(
                                        refreshEnabled = true,
                                        memoryEnabled = true
                                    )
                                }
                            },
                            label = { Text(item) }
                        )
                    }
                }

                Text(
                    "Selected Experiment: $experiment",
                    fontWeight = FontWeight.Bold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Button(
                        onClick = {
                            running = !running
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (running) "Pause" else "Resume"
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            running = false
                            state = initialMainState()
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
        }

        // MAIN PARAMETERS

        Card(Modifier.fillMaxWidth()) {

            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    "Open System Controls",
                    fontWeight = FontWeight.Bold
                )

                Text("External Input: ${(state.input * 100).toInt()}%")

                Slider(
                    value = state.input,
                    onValueChange = {
                        state = state.copy(input = it)
                    }
                )

                Text("External Output: ${(state.output * 100).toInt()}%")

                Slider(
                    value = state.output,
                    onValueChange = {
                        state = state.copy(output = it)
                    }
                )

                Text("Refresh Rate: ${(state.refreshRate * 100).toInt()}%")

                Slider(
                    value = state.refreshRate,
                    onValueChange = {
                        state = state.copy(refreshRate = it)
                    }
                )

                HorizontalDivider()

                Text(
                    "Memory Controls",
                    fontWeight = FontWeight.Bold
                )

                Text("Recording Rate: ${(state.recordingRate * 100).toInt()}%")

                Slider(
                    value = state.recordingRate,
                    onValueChange = {
                        state = state.copy(recordingRate = it)
                    }
                )

                Text("Retention Rate: ${(state.retentionRate * 100).toInt()}%")

                Slider(
                    value = state.retentionRate,
                    onValueChange = {
                        state = state.copy(retentionRate = it)
                    }
                )

                Text("Retrieval Rate: ${(state.retrievalRate * 100).toInt()}%")

                Slider(
                    value = state.retrievalRate,
                    onValueChange = {
                        state = state.copy(retrievalRate = it)
                    }
                )
            }
        }

        // MATHEMATICAL RESULTS

        Card(Modifier.fillMaxWidth()) {

            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {

                Text(
                    "Mathematical Results",
                    fontWeight = FontWeight.Bold
                )

                ResultLine(
                    "Simulation Cycle",
                    state.cycle.toString()
                )

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
        }

        // GRAPH

        Card(Modifier.fillMaxWidth()) {

            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    "Research Graph",
                    fontWeight = FontWeight.Bold
                )

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
        }

        // PROPOSED EQUATIONS

        Card(Modifier.fillMaxWidth()) {

            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    "Mathematical Model",
                    fontWeight = FontWeight.Bold
                )

                Text("Open System:")

                Text(
                    "X(t+1) = F(X, Input, Output, Refresh)"
                )

                HorizontalDivider()

                Text("Dynamic Stability:")

                Text(
                    "D(t) = f(Exchange, Activity, Refresh, Memory)"
                )

                HorizontalDivider()

                Text("Memory:")

                Text(
                    "M(t+1) = G(M(t), Recording, Refresh)"
                )

                Text(
                    "ये हमारे Research Model के शुरुआती समीकरण हैं। प्रयोगों के परिणामों के अनुसार इन्हें विकसित किया जाएगा।"
                )
            }
        }

        // EXPERIMENT RECORD

        Card(Modifier.fillMaxWidth()) {

            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {

                Text(
                    "Experiment Record",
                    fontWeight = FontWeight.Bold
                )

                Text("Experiment: $experiment")
                Text("Cycles: ${state.cycle}")
                Text("Refresh: ${state.refreshEnabled}")
                Text("Memory: ${state.memoryEnabled}")

                Text(
                    "अंतिम Dynamic Stability: ${
                        (state.dynamicStability * 100).toInt()
                    }%"
                )

                Text(
                    "अंतिम Memory: ${
                        (state.memoryValue * 100).toInt()
                    }%"
                )
            }
        }
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            "Microstructure Research Lab",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Local Connections • Structural Refresh • Pattern Continuity"
        )

        Card(Modifier.fillMaxWidth()) {

            Column(
                Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

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
        }

        Card(Modifier.fillMaxWidth()) {

            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {

                Text(
                    "Microstructure Status",
                    fontWeight = FontWeight.Bold
                )

                ResultLine("Cycles", state.cycle.toString())
                ResultLine("Particles", state.particles.size.toString())
                ResultLine("Connections", state.bonds.size.toString())
                ResultLine("Connections Formed", state.formed.toString())
                ResultLine("Connections Broken", state.broken.toString())
                ResultLine("Input Total", state.totalInput.toString())
                ResultLine("Output Total", state.totalOutput.toString())
                ResultLine("Continuity", "${(state.continuity * 100).toInt()}%")
                ResultLine("Pattern Drift", state.drift.fmt(4))
            }
        }

        Card(Modifier.fillMaxWidth()) {

            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    "Microstructure Controls",
                    fontWeight = FontWeight.Bold
                )

                Text("Input: ${(state.inputRate * 100).toInt()}%")

                Slider(
                    value = state.inputRate,
                    onValueChange = {
                        state = state.copy(inputRate = it)
                    }
                )

                Text("Output: ${(state.outputRate * 100).toInt()}%")

                Slider(
                    value = state.outputRate,
                    onValueChange = {
                        state = state.copy(outputRate = it)
                    }
                )

                Text("Refresh: ${(state.refreshRate * 100).toInt()}%")

                Slider(
                    value = state.refreshRate,
                    onValueChange = {
                        state = state.copy(refreshRate = it)
                    }
                )

                Text("Attraction: ${(state.attraction * 100).toInt()}%")

                Slider(
                    value = state.attraction,
                    onValueChange = {
                        state = state.copy(attraction = it)
                    }
                )

                Text("Repulsion: ${(state.repulsion * 100).toInt()}%")

                Slider(
                    value = state.repulsion,
                    onValueChange = {
                        state = state.copy(repulsion = it)
                    }
                )

                Text("Connection Range: ${state.connectionRange.fmt(2)}")

                Slider(
                    value = state.connectionRange,
                    onValueChange = {
                        state = state.copy(connectionRange = it)
                    },
                    valueRange = 0.05f..0.22f
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Button(
                        onClick = {
                            running = !running
                        },
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
        }

        Card(Modifier.fillMaxWidth()) {

            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    "Pattern Snapshots",
                    fontWeight = FontWeight.Bold
                )

                state.snapshots.takeLast(8).reversed().forEach {

                    Text(
                        "Cycle ${it.cycle} | Particles ${it.particles} | Connections ${it.bonds}"
                    )
                }
            }
        }
    }
}

/* =========================================================
   REUSABLE UI COMPONENTS
   ========================================================= */

@Composable
private fun FlowBox(
    label: String,
    color: Color
) {

    Surface(
        color = color.copy(alpha = 0.13f),
        shape = RoundedCornerShape(10.dp)
    ) {

        Text(
            label,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 12.dp
            )
        )
    }
}

@Composable
private fun IconArrow() {

    Text(
        "↓",
        color = MaterialTheme.colorScheme.secondary,
        style = MaterialTheme.typography.headlineMedium
    )
}

@Composable
private fun ResultLine(
    label: String,
    value: String
) {

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Text(label)

        Text(
            value,
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

        Spacer(Modifier.width(4.dp))

        Text(
            label,
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
            .background(RTDark)
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
