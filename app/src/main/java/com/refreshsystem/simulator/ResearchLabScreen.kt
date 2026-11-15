
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
import androidx.compose.ui.geometry.CornerRadius
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
   REFRESH SYSTEM — RESEARCH INTERFACE
   ========================================================= */

private val AppBackground = Color(0xFF0B1020)
private val HeaderBackground = Color(0xFF0D1427)
private val TabBackground = Color(0xFF182238)
private val CardBackground = Color(0xFF182238)
private val CardInner = Color(0xFF222D47)

private val TextPrimary = Color(0xFFF4F6FF)
private val TextSecondary = Color(0xFFB3BED2)
private val DividerColor = Color(0xFF35415A)

private val RTGreen = Color(0xFF70D58A)
private val RTBlue = Color(0xFF55B8FF)
private val RTOrange = Color(0xFFFFB957)
private val RTViolet = Color(0xFFA995FF)
private val RTCyan = Color(0xFF4DD0E1)
private val RTPurple = Color(0xFFC18CFF)
private val RTRed = Color(0xFFFF6680)

private fun Float.fmt(digits: Int = 2): String =
    String.format(Locale.US, "%.${digits}f", this)

private fun Float.safe01(): Float =
    coerceIn(0f, 1f)

private val appColors = darkColorScheme(
    primary = RTViolet,
    secondary = RTGreen,
    tertiary = RTOrange,
    background = AppBackground,
    surface = CardBackground,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

/* =========================================================
   MAIN RESEARCH DATA
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
    val dynamicStability: Float = 0.50f,
    val memoryValue: Float = 0.20f,
    val recording: Float = 0.20f,
    val retrieval: Float = 0.10f,
    val informationInput: Float = 0f,
    val informationLost: Float = 0f,
    val continuity: Float = 1f,
    val totalRefresh: Int = 0,
    val history: List<MainPoint> = emptyList()
)

private fun initialMainState() = MainResearchState()

private fun experimentState(experiment: String): MainResearchState {
    return initialMainState().copy(
        refreshEnabled = experiment == "B" || experiment == "D",
        memoryEnabled = experiment == "C" || experiment == "D"
    )
}

/* =========================================================
   MAIN SIMULATION ENGINE
   ========================================================= */

private fun nextMainState(old: MainResearchState): MainResearchState {

    val refreshActivity =
        if (old.refreshEnabled) old.refreshRate else 0f

    val exchange = old.input - old.output

    val activityChange =
        exchange * 0.08f +
            refreshActivity * 0.06f -
            0.025f

    val activity =
        (old.systemActivity + activityChange).safe01()

    val refreshValue = refreshActivity * activity

    val newInformation =
        if (old.memoryEnabled) {
            old.input * old.recordingRate
        } else 0f

    val retainedMemory =
        if (old.memoryEnabled) {
            old.memoryValue * old.retentionRate
        } else 0f

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
            memory * old.retrievalRate *
                (0.5f + refreshValue * 0.5f)
            ).safe01()
    } else 0f

    val exchangeBalance =
        (1f - abs(old.input - old.output)).safe01()

    val activityContinuity =
        (1f - abs(activity - old.systemActivity)).safe01()

    val memoryContinuity = if (old.memoryEnabled) {
        (1f - abs(memory - old.memoryValue)).safe01()
    } else 0f

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

    val lost =
        (old.memoryValue - memory).coerceAtLeast(0f)

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

private data class MicroBond(val a: Int, val b: Int)

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
        particles.asSequence()
            .filter { it.id != p.id }
            .map { it to microDistance(p, it) }
            .filter { it.second < range }
            .sortedBy { it.second }
            .take(MICRO_NEIGHBORS)
            .forEach { (other, _) ->
                keys.add(
                    "${minOf(p.id, other.id)}-${maxOf(p.id, other.id)}"
                )
            }
    }

    return keys.mapNotNull { key ->
        val parts = key.split("-")
        val a = parts.getOrNull(0)?.toIntOrNull()
        val b = parts.getOrNull(1)?.toIntOrNull()

        if (a != null && b != null) {
            MicroBond(a, b)
        } else null
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
   MICROSTRUCTURE SIMULATION ENGINE
   ========================================================= */

private fun nextMicroState(old: MicroState): MicroState {

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

            val id = (particles.maxOfOrNull { it.id } ?: 0) + 1

            particles.add(
                MicroParticle(
                    id = id,
                    x = x,
                    y = y,
                    vx = (0.5f - x) * 0.004f,
                    vy = (0.5f - y) * 0.004f,
                    phase = random.nextFloat() * 6.28f
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

    val snapshots = if (nextCycle % 30 == 0) {
        (
            old.snapshots + MicroSnapshot(
                nextCycle,
                finalParticles.size,
                newBonds.size
            )
        ).takeLast(20)
    } else old.snapshots

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
   APP ENTRY POINT
   ========================================================= */

@Composable
fun ResearchHomeScreen() {

    var selectedLab by remember {
        mutableIntStateOf(0)
    }

    MaterialTheme(colorScheme = appColors) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
        ) {

            AppHeader()

            TabRow(
                selectedTabIndex = selectedLab,
                containerColor = TabBackground,
                contentColor = RTRed,

                // Custom indicator: tabIndicatorOffset की जरूरत नहीं
                indicator = { positions ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .offset(x = positions[selectedLab].left)
                                .width(positions[selectedLab].width)
                                .height(3.dp)
                                .background(RTRed)
                        )
                    }
                },

                divider = {
                    HorizontalDivider(color = DividerColor)
                }
            ) {

                Tab(
                    selected = selectedLab == 0,
                    onClick = { selectedLab = 0 },
                    text = {
                        Text(
                            "Main Research",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    selectedContentColor = RTRed,
                    unselectedContentColor = TextSecondary
                )

                Tab(
                    selected = selectedLab == 1,
                    onClick = { selectedLab = 1 },
                    text = {
                        Text(
                            "Microstructure",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    selectedContentColor = RTRed,
                    unselectedContentColor = TextSecondary
                )
            }

            when (selectedLab) {
                0 -> MainRefreshResearchLab()
                1 -> MicrostructureLab()
            }
        }
    }
}

@Composable
private fun AppHeader() {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderBackground)
            .padding(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        Text(
            "Refresh System",
            color = TextPrimary,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            "Research & Development",
            color = TextSecondary,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

/* =========================================================
   MAIN RESEARCH SCREEN
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
            .background(AppBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            "Dynamic Stability + Memory",
            color = TextPrimary,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            "Open System • Continuous Refresh • Chetna Research",
            color = TextSecondary,
            style = MaterialTheme.typography.titleMedium
        )

        SystemFlowCard()

        ExperimentCard(
            experiment = experiment,
            running = running,
            onExperimentChange = {
                experiment = it
                state = experimentState(it)
            },
            onRunningChange = {
                running = it
            },
            onReset = {
                state = experimentState(experiment)
            },
            onOneCycle = {
                state = nextMainState(state)
            }
        )

        MainControlsCard(
            state = state,
            onStateChange = { state = it }
        )

        MainResultsCard(state)

        ResearchGraphCard(state.history)

        MathematicalModelCard()

        ExperimentRecordCard(state, experiment)

        Spacer(Modifier.height(24.dp))
    }
}

/* =========================================================
   SYSTEM FLOW DIAGRAM
   ========================================================= */

@Composable
private fun SystemFlowCard() {

    ResearchCard {

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            SectionTitle("OPEN SYSTEM")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                FlowNode(
                    "INPUT",
                    "External entry",
                    RTBlue,
                    Modifier.weight(1f)
                )

                FlowNode(
                    "REFRESH",
                    "Continuous change",
                    RTGreen,
                    Modifier.weight(1f)
                )

                FlowNode(
                    "OUTPUT",
                    "External release",
                    RTOrange,
                    Modifier.weight(1f)
                )
            }

            FlowArrow()

            FlowNode(
                "DYNAMIC STABILITY",
                "Continuous system activity",
                RTViolet,
                Modifier.fillMaxWidth()
            )

            FlowArrow()

            FlowNode(
                "RECORDING & MEMORY",
                "Information recording and retention",
                RTPurple,
                Modifier.fillMaxWidth()
            )

            FlowArrow()

            FlowNode(
                "CHETNA RESEARCH",
                "Research and observation",
                RTOrange,
                Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun FlowNode(
    title: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {

    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.10f),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            color.copy(alpha = 0.42f)
        )
    ) {

        Column(
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 18.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {

            Text(
                title,
                color = color,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                subtitle,
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun FlowArrow() {

    Text(
        "↓",
        color = RTViolet,
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold
    )
}

/* =========================================================
   EXPERIMENT CARD
   ========================================================= */

@Composable
private fun ExperimentCard(
    experiment: String,
    running: Boolean,
    onExperimentChange: (String) -> Unit,
    onRunningChange: (Boolean) -> Unit,
    onReset: () -> Unit,
    onOneCycle: () -> Unit
) {

    ResearchCard {

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            SectionTitle("Controlled Experiments")

            ExperimentDescription("A", "Refresh OFF", "Memory OFF")
            ExperimentDescription("B", "Refresh ON", "Memory OFF")
            ExperimentDescription("C", "Refresh OFF", "Memory ON")
            ExperimentDescription("D", "Refresh ON", "Memory ON")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {

                listOf("A", "B", "C", "D").forEach { item ->

                    FilterChip(
                        selected = experiment == item,
                        onClick = { onExperimentChange(item) },
                        label = {
                            Text(
                                "Test $item",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RTViolet.copy(alpha = 0.22f),
                            selectedLabelColor = TextPrimary,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            Text(
                "Selected Experiment: $experiment",
                color = RTViolet,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Button(
                    onClick = { onRunningChange(!running) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RTViolet
                    )
                ) {
                    Text(if (running) "Pause" else "Resume")
                }

                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextPrimary
                    )
                ) {
                    Text("Reset")
                }
            }

            OutlinedButton(
                onClick = onOneCycle,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = RTGreen
                )
            ) {
                Text("Run One Cycle")
            }
        }
    }
}

@Composable
private fun ExperimentDescription(
    code: String,
    refresh: String,
    memory: String
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            code,
            color = RTViolet,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.width(28.dp)
        )

        Text(
            "$refresh  •  $memory",
            color = TextSecondary
        )
    }
}

/* =========================================================
   MAIN CONTROLS
   ========================================================= */

@Composable
private fun MainControlsCard(
    state: MainResearchState,
    onStateChange: (MainResearchState) -> Unit
) {

    ResearchCard {

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            SectionTitle("Open System Controls")

            ControlSlider(
                "External Input",
                state.input,
                RTBlue
            ) {
                onStateChange(state.copy(input = it))
            }

            ControlSlider(
                "External Output",
                state.output,
                RTOrange
            ) {
                onStateChange(state.copy(output = it))
            }

            ControlSlider(
                "Refresh Rate",
                state.refreshRate,
                RTGreen
            ) {
                onStateChange(state.copy(refreshRate = it))
            }

            HorizontalDivider(color = DividerColor)

            SectionTitle("Memory Controls")

            ControlSlider(
                "Recording Rate",
                state.recordingRate,
                RTPurple
            ) {
                onStateChange(state.copy(recordingRate = it))
            }

            ControlSlider(
                "Retention Rate",
                state.retentionRate,
                RTViolet
            ) {
                onStateChange(state.copy(retentionRate = it))
            }

            ControlSlider(
                "Retrieval Rate",
                state.retrievalRate,
                RTCyan
            ) {
                onStateChange(state.copy(retrievalRate = it))
            }
        }
    }
}

@Composable
private fun ControlSlider(
    label: String,
    value: Float,
    color: Color,
    onValueChange: (Float) -> Unit
) {

    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Text(
                label,
                color = TextSecondary
            )

            Text(
                "${(value * 100).toInt()}%",
                color = color,
                fontWeight = FontWeight.Bold
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = DividerColor
            )
        )
    }
}

/* =========================================================
   RESULTS
   ========================================================= */

@Composable
private fun MainResultsCard(state: MainResearchState) {

    ResearchCard {

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            SectionTitle("Mathematical Results")

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

            HorizontalDivider(color = DividerColor)

            ResultLine(
                "Information Received",
                state.informationInput.fmt(3)
            )

            ResultLine(
                "Information Lost",
                state.informationLost.fmt(3)
            )
        }
    }
}

/* =========================================================
   RESEARCH GRAPH
   ========================================================= */

@Composable
private fun ResearchGraphCard(history: List<MainPoint>) {

    ResearchCard {

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            SectionTitle("Research Graph")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GraphLegend(RTGreen, "Refresh")
                GraphLegend(RTViolet, "Stability")
                GraphLegend(RTPurple, "Memory")
            }

            ResearchGraph(
                history = history,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )
        }
    }
}

@Composable
private fun ResearchGraph(
    history: List<MainPoint>,
    modifier: Modifier = Modifier
) {

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardInner)
    ) {

        val left = 14.dp.toPx()
        val right = size.width - 14.dp.toPx()
        val top = 14.dp.toPx()
        val bottom = size.height - 14.dp.toPx()

        val graphWidth = right - left
        val graphHeight = bottom - top

        repeat(5) { i ->

            val y = top + graphHeight * i / 4f

            drawLine(
                color = DividerColor,
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

        drawSeries(history.map { it.refresh }, RTGreen)
        drawSeries(history.map { it.dynamicStability }, RTViolet)
        drawSeries(history.map { it.memory }, RTPurple)
    }
}

/* =========================================================
   MATHEMATICAL MODEL
   ========================================================= */

@Composable
private fun MathematicalModelCard() {

    ResearchCard {

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            SectionTitle("Mathematical Model")

            Text(
                "Open System",
                color = RTBlue,
                fontWeight = FontWeight.Bold
            )

            EquationText(
                "X(t+1) = F(X, Input, Output, Refresh)"
            )

            HorizontalDivider(color = DividerColor)

            Text(
                "Dynamic Stability",
                color = RTViolet,
                fontWeight = FontWeight.Bold
            )

            EquationText(
                "D(t) = f(Exchange, Activity, Refresh, Memory)"
            )

            HorizontalDivider(color = DividerColor)

            Text(
                "Memory",
                color = RTPurple,
                fontWeight = FontWeight.Bold
            )

            EquationText(
                "M(t+1) = G(M(t), Recording, Refresh)"
            )

            Text(
                "ये हमारे Research Model के शुरुआती समीकरण हैं। प्रयोगों के परिणामों के अनुसार इन्हें विकसित किया जाएगा।",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/* =========================================================
   EXPERIMENT RECORD
   ========================================================= */

@Composable
private fun ExperimentRecordCard(
    state: MainResearchState,
    experiment: String
) {

    ResearchCard {

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            SectionTitle("Experiment Record")

            ResultLine("Experiment", experiment)
            ResultLine("Cycles", state.cycle.toString())

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
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            "Microstructure Research Lab",
            color = TextPrimary,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            "Local Connections • Structural Refresh • Pattern Continuity",
            color = TextSecondary,
            style = MaterialTheme.typography.titleMedium
        )

        ResearchCard {

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                SectionTitle("Live Microstructure")

                MicroCanvas(
                    state = state,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    GraphLegend(RTGreen, "Particles")
                    GraphLegend(RTCyan, "Connections")
                    GraphLegend(RTBlue, "Input")
                    GraphLegend(RTOrange, "Output")
                }
            }
        }

        ResearchCard {

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                SectionTitle("Microstructure Status")

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
        }

        ResearchCard {

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                SectionTitle("Microstructure Controls")

                ControlSlider(
                    "Input",
                    state.inputRate,
                    RTBlue
                ) {
                    state = state.copy(inputRate = it)
                }

                ControlSlider(
                    "Output",
                    state.outputRate,
                    RTOrange
                ) {
                    state = state.copy(outputRate = it)
                }

                ControlSlider(
                    "Refresh",
                    state.refreshRate,
                    RTGreen
                ) {
                    state = state.copy(refreshRate = it)
                }

                ControlSlider(
                    "Attraction",
                    state.attraction,
                    RTViolet
                ) {
                    state = state.copy(attraction = it)
                }

                ControlSlider(
                    "Repulsion",
                    state.repulsion,
                    RTOrange
                ) {
                    state = state.copy(repulsion = it)
                }

                Text(
                    "Connection Range: ${state.connectionRange.fmt(2)}",
                    color = TextSecondary
                )

                Slider(
                    value = state.connectionRange,
                    onValueChange = {
                        state = state.copy(connectionRange = it)
                    },
                    valueRange = 0.05f..0.22f,
                    colors = SliderDefaults.colors(
                        thumbColor = RTCyan,
                        activeTrackColor = RTCyan,
                        inactiveTrackColor = DividerColor
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    Button(
                        onClick = { running = !running },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RTViolet
                        )
                    ) {
                        Text(if (running) "Pause" else "Resume")
                    }

                    OutlinedButton(
                        onClick = {
                            running = false
                            state = initialMicroState()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextPrimary
                        )
                    ) {
                        Text("Reset")
                    }
                }

                OutlinedButton(
                    onClick = {
                        state = nextMicroState(state)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = RTGreen
                    )
                ) {
                    Text("Run One Cycle")
                }
            }
        }

        ResearchCard {

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                SectionTitle("Pattern Snapshots")

                if (state.snapshots.isEmpty()) {

                    Text(
                        "Snapshots will appear after every 30 cycles.",
                        color = TextSecondary
                    )

                } else {

                    state.snapshots
                        .takeLast(8)
                        .reversed()
                        .forEach {
                            Text(
                                "Cycle ${it.cycle}  |  Particles ${it.particles}  |  Connections ${it.bonds}",
                                color = TextSecondary
                            )
                        }
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
private fun ResearchCard(
    content: @Composable ColumnScope.() -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
private fun SectionTitle(title: String) {

    Text(
        title,
        color = TextPrimary,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun EquationText(text: String) {

    Text(
        text,
        color = TextSecondary,
        style = MaterialTheme.typography.bodyLarge
    )
}

@Composable
private fun ResultLine(
    label: String,
    value: String
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            label,
            color = TextSecondary,
            modifier = Modifier.weight(1f)
        )

        Spacer(Modifier.width(12.dp))

        Text(
            value,
            color = TextPrimary,
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
            color = TextSecondary,
            style = MaterialTheme.typography.labelSmall
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
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF0D172A))
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
            cornerRadius = CornerRadius(15.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx())
        )

        val particleMap =
            state.particles.associateBy { it.id }

        state.bonds.forEach { bond ->

            val a = particleMap[bond.a]
            val b = particleMap[bond.b]

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
