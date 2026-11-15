
package com.refreshsystem.simulator

import android.media.AudioManager
import android.media.ToneGenerator
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val Green = Color(0xFF66BB6A)
private val Orange = Color(0xFFFFB74D)
private val Blue = Color(0xFF42A5F5)
private val Violet = Color(0xFF9C8CFF)
private val BackgroundDark = Color(0xFF0B1220)
private val Cyan = Color(0xFF38BDF8)

private const val ATOM_COUNT = 36

private enum class BoundarySide {
    LEFT, RIGHT
}

private data class SystemAtom(
    val id: Int,
    val slot: Int,
    val phase: Float
)

private data class FlowParticle(
    val id: Int,
    val slot: Int,
    val progress: Float,
    val entering: Boolean,
    val side: BoundarySide
)

private data class InformationRecord(
    val id: Int,
    val strength: Float,
    val refreshCount: Int,
    val age: Int
)

private data class SystemState(
    val step: Int = 0,
    val elapsedMillis: Long = 0L,

    val atoms: List<SystemAtom> = emptyList(),
    val flowParticles: List<FlowParticle> = emptyList(),

    // External exchange and internal refresh
    val inputRate: Float = 0.12f,
    val outputRate: Float = 0.12f,
    val internalRefresh: Float = 0.65f,
    val speed: Float = 1f,

    // Information and memory
    val attention: Float = 0.65f,
    val repetition: Float = 0.50f,
    val memoryRetention: Float = 0.80f,
    val informationEntries: Int = 0,
    val recordingCount: Int = 0,
    val memoryRefreshCount: Int = 0,
    val records: List<InformationRecord> = emptyList(),

    // System continuity
    val totalIn: Int = 0,
    val totalOut: Int = 0,
    val internalChanges: Int = 0,
    val continuity: Float = 1f,

    val lastEvent: String =
        "Open System तैयार है। Refresh प्रक्रिया शुरू होने की प्रतीक्षा है।"
)

private fun clamp(value: Float): Float =
    value.coerceIn(0f, 1f)

private fun initialSystem(): SystemState {
    val atoms = (0 until ATOM_COUNT).map { index ->
        SystemAtom(
            id = index + 1,
            slot = index,
            phase = index * 0.41f
        )
    }

    return SystemState(atoms = atoms)
}

/* ---------------------------------------------------------
   DYNAMIC PATTERN
   Pattern remains recognizable but continuously changes.
   --------------------------------------------------------- */

private fun systemPattern(
    state: SystemState
): List<Pair<Float, Float>> {

    return (0 until ATOM_COUNT).map { index ->

        val angle = 2.0 * PI * index / ATOM_COUNT
        val baseRadius = 0.30f

        val phase = state.atoms
            .firstOrNull { it.slot == index }
            ?.phase ?: 0f

        val movement =
            state.internalRefresh * 0.025f

        val radius = baseRadius +
            sin(phase.toDouble()).toFloat() * movement

        val x = 0.50f +
            cos(angle).toFloat() * radius

        val y = 0.50f +
            sin(angle).toFloat() * radius

        x to y
    }
}

/* ---------------------------------------------------------
   SIMULATION ENGINE
   --------------------------------------------------------- */

private fun nextSystemState(
    old: SystemState,
    cycleDurationMs: Long
): SystemState {

    val nextStep = old.step + 1

    val atoms = old.atoms
        .ifEmpty { initialSystem().atoms }
        .toMutableList()

    val particles = old.flowParticles
        .mapNotNull { particle ->
            val next = particle.progress + 0.18f
            if (next >= 1f) null
            else particle.copy(progress = next)
        }
        .toMutableList()

    var totalIn = old.totalIn
    var totalOut = old.totalOut
    var informationEntries = old.informationEntries
    var recordingCount = old.recordingCount
    var memoryRefreshCount = old.memoryRefreshCount
    var internalChanges = old.internalChanges

    // -----------------------------------------------------
    // 1. Boundary Exchange
    // Information and particles enter the Open System.
    // Output and input are independent controls.
    // -----------------------------------------------------

    val exchangeSlots = listOf(
        0, 1, 2, 3, 4, 5,
        30, 31, 32, 33, 34, 35
    )

    val inputAccumulator =
        old.inputRate * 6f

    val outputAccumulator =
        old.outputRate * 6f

    val inputCount = inputAccumulator.toInt() +
        if ((nextStep % 10) < (inputAccumulator % 1f * 10f)) 1 else 0

    val outputCount = outputAccumulator.toInt() +
        if ((nextStep % 10) < (outputAccumulator % 1f * 10f)) 1 else 0

    repeat(inputCount.coerceIn(0, 6)) { n ->

        val slot = exchangeSlots[
            (nextStep + n) % exchangeSlots.size
        ]

        val newId = (atoms.maxOfOrNull { it.id } ?: 0) + 1
        val oldAtomIndex = atoms.indexOfFirst { it.slot == slot }

        if (oldAtomIndex >= 0) {
            atoms[oldAtomIndex] = SystemAtom(
                id = newId,
                slot = slot,
                phase = nextStep * 0.31f + n
            )
        }

        val side = if (slot < 6) {
            BoundarySide.LEFT
        } else {
            BoundarySide.RIGHT
        }

        particles.add(
            FlowParticle(
                id = newId,
                slot = slot,
                progress = 0f,
                entering = true,
                side = side
            )
        )

        totalIn++
    }

    repeat(outputCount.coerceIn(0, 6)) { n ->

        val slot = exchangeSlots[
            (nextStep * 3 + n) % exchangeSlots.size
        ]

        val atom = atoms.firstOrNull { it.slot == slot }

        if (atom != null) {

            val side = if (slot < 6) {
                BoundarySide.LEFT
            } else {
                BoundarySide.RIGHT
            }

            particles.add(
                FlowParticle(
                    id = atom.id,
                    slot = slot,
                    progress = 0f,
                    entering = false,
                    side = side
                )
            )

            totalOut++
        }
    }

    // -----------------------------------------------------
    // 2. Internal Refresh
    // Every internal atom changes its micro-state.
    // The overall pattern remains recognizable.
    // -----------------------------------------------------

    val movedAtoms = atoms.map { atom ->
        atom.copy(
            phase = atom.phase +
                0.08f +
                old.internalRefresh * 0.35f
        )
    }

    internalChanges += movedAtoms.size

    // -----------------------------------------------------
    // 3. Information Entry
    // Entry is part of the Refresh process itself.
    // -----------------------------------------------------

    val informationArrives =
        old.inputRate > 0f &&
        nextStep % 2 == 0

    var records = old.records

    if (informationArrives) {

        informationEntries++

        // Repetition and attention increase recording chance.
        val recordingProbability = clamp(
            0.10f +
                old.attention * 0.45f +
                old.repetition * 0.40f
        )

        val deterministicSignal =
            ((nextStep * 37) % 100) / 100f

        if (deterministicSignal < recordingProbability) {

            recordingCount++

            val newRecord = InformationRecord(
                id = recordingCount,
                strength = recordingProbability,
                refreshCount = 0,
                age = 0
            )

            records = (records + newRecord).takeLast(20)
        }
    }

    // -----------------------------------------------------
    // 4. Memory Refresh
    // Existing records are refreshed continuously.
    // Retention controls how strongly they remain.
    // -----------------------------------------------------

    records = records.map { record ->

        val refreshed = record.refreshCount + 1

        record.copy(
            strength = clamp(
                record.strength *
                    (0.995f + old.memoryRetention * 0.005f)
            ),
            refreshCount = refreshed,
            age = record.age + 1
        )
    }

    memoryRefreshCount += records.size

    // -----------------------------------------------------
    // 5. Operational Continuity
    // This is a simulation metric, not metaphysical
    // Stability or proof of consciousness.
    // -----------------------------------------------------

    val exchangeBalance =
        1f - kotlin.math.abs(old.inputRate - old.outputRate)

    val activity = (
        old.internalRefresh * 0.65f +
            exchangeBalance * 0.20f +
            old.memoryRetention * 0.15f
        ).coerceIn(0f, 1f)

    val continuity = (
        old.continuity * 0.90f +
            activity * 0.10f
        ).coerceIn(0f, 1f)

    val message = when {
        informationArrives && records.isNotEmpty() ->
            "नई सूचना सिस्टम में प्रवेश कर रही है। Refresh के दौरान कुछ सूचनाएँ Record हो रही हैं और पुरानी Memory भी Refresh हो रही है।"

        informationArrives ->
            "नई सूचना Open System में प्रवेश कर रही है। सूचना का प्रवेश स्वयं Refresh प्रक्रिया का हिस्सा है।"

        records.isNotEmpty() ->
            "सिस्टम का Pattern लगातार Refresh हो रहा है। पहले से Record की गई Memory भी Refresh हो रही है।"

        else ->
            "Open System की आंतरिक अवस्था बदल रही है। Pattern की निरंतरता बनी हुई है।"
    }

    return old.copy(
        step = nextStep,
        elapsedMillis = old.elapsedMillis + cycleDurationMs,
        atoms = movedAtoms,
        flowParticles = particles,
        totalIn = totalIn,
        totalOut = totalOut,
        informationEntries = informationEntries,
        recordingCount = recordingCount,
        memoryRefreshCount = memoryRefreshCount,
        records = records,
        internalChanges = internalChanges,
        continuity = continuity,
        lastEvent = message
    )
}

/* ---------------------------------------------------------
   MAIN SCREEN
   --------------------------------------------------------- */

@Composable
fun ResearchHomeScreen() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            OpenSystemDynamicStabilityLab()
        }
    }
}

/* ---------------------------------------------------------
   OPEN SYSTEM LAB
   --------------------------------------------------------- */

@Composable
private fun OpenSystemDynamicStabilityLab() {

    var simulation by remember {
        mutableStateOf(initialSystem())
    }

    var running by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(false) }

    val toneGenerator = remember {
        ToneGenerator(AudioManager.STREAM_MUSIC, 60)
    }

    DisposableEffect(Unit) {
        onDispose {
            toneGenerator.stopTone()
            toneGenerator.release()
        }
    }

    LaunchedEffect(running, simulation.speed) {

        while (running) {

            val cycleDelay = (
                650L / simulation.speed.coerceIn(0.4f, 3f)
            ).toLong().coerceAtLeast(80L)

            delay(cycleDelay)

            simulation = nextSystemState(
                simulation,
                cycleDelay
            )
        }
    }

    LaunchedEffect(running, soundEnabled, simulation.speed) {

        toneGenerator.stopTone()

        while (running && soundEnabled) {

            val interval = (
                500L / simulation.speed.coerceIn(0.4f, 3f)
            ).toLong().coerceAtLeast(100L)

            toneGenerator.startTone(
                ToneGenerator.TONE_PROP_BEEP2,
                50
            )

            delay(interval)
        }

        toneGenerator.stopTone()
    }

    val elapsedSeconds = simulation.elapsedMillis / 1000L
    val minutes = elapsedSeconds / 60L
    val seconds = elapsedSeconds % 60L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            "Refresh System",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Open System & Dynamic Stability Lab",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            "एक ऐसा Open System जिसमें Pattern लगातार Refresh होता है और बदलावों के बावजूद उसकी कार्यात्मक निरंतरता बनी रहती है।"
        )

        /* SYSTEM VISUALIZATION */

        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Text(
                    "जीवित Open System Pattern",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                SystemCanvas(
                    state = simulation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Legend(Blue, "सूचना / Entry")
                    Legend(Orange, "Output")
                    Legend(Green, "Internal Refresh")
                }

                Text(simulation.lastEvent)

                Text(
                    "Pattern: Continuous Refresh",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        /* DYNAMIC STABILITY */

        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    "Dynamic Stability",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "यहाँ Dynamic Stability का अर्थ है: सिस्टम के अंदर लगातार बदलाव होने के बावजूद उसकी कार्यात्मक निरंतरता।"
                )

                Text(
                    "Operational Continuity: ${(simulation.continuity * 100).toInt()}%",
                    fontWeight = FontWeight.Bold
                )

                LinearProgressIndicator(
                    progress = { simulation.continuity },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "Internal Changes: ${simulation.internalChanges}"
                )

                Text(
                    "यह एक प्रयोगात्मक गणनात्मक सूचकांक है, वास्तविकता की पूर्ण Stability का माप नहीं।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        /* INFORMATION AND MEMORY */

        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    "Information & Memory Lab",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text("सिस्टम में आई कुल सूचनाएँ: ${simulation.informationEntries}")
                Text("Record हुई सूचनाएँ: ${simulation.recordingCount}")
                Text("Memory Refresh Cycles: ${simulation.memoryRefreshCount}")
                Text("वर्तमान Memory Records: ${simulation.records.size}")

                HorizontalDivider()

                Text(
                    "हाल की Memory Records",
                    fontWeight = FontWeight.Bold
                )

                if (simulation.records.isEmpty()) {
                    Text("अभी कोई सूचना Record नहीं हुई है।")
                } else {
                    simulation.records.takeLast(5).reversed().forEach { record ->
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                "Record #${record.id}",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Strength: ${(record.strength * 100).toInt()}%  |  Refresh: ${record.refreshCount}"
                            )
                            LinearProgressIndicator(
                                progress = { record.strength },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        /* LIVE STATUS */

        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {

                Text(
                    "Live System Status",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text("Simulation Cycles: ${simulation.step}")
                Text("System Particles: ${simulation.atoms.size}")
                Text("Total Input: ${simulation.totalIn}")
                Text("Total Output: ${simulation.totalOut}")

                Text(
                    "Elapsed Time: %02d:%02d".format(minutes, seconds),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        /* CONTROLS */

        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Text(
                    "Simulation Controls",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                    Button(
                        onClick = { running = !running },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (running) "Pause" else "Resume")
                    }

                    OutlinedButton(
                        onClick = {
                            val cycleDelay = (
                                650L / simulation.speed.coerceIn(0.4f, 3f)
                            ).toLong().coerceAtLeast(80L)

                            simulation = nextSystemState(
                                simulation,
                                cycleDelay
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("एक Cycle")
                    }
                }

                Text("Simulation Speed: ${String.format(Locale.getDefault(), "%.1f", simulation.speed)}x")

                Slider(
                    value = simulation.speed,
                    onValueChange = {
                        simulation = simulation.copy(speed = it)
                    },
                    valueRange = 0.4f..3f
                )

                HorizontalDivider()

                Text(
                    "External Information / Input: ${(simulation.inputRate * 100).toInt()}%"
                )

                Slider(
                    value = simulation.inputRate,
                    onValueChange = {
                        simulation = simulation.copy(inputRate = it)
                    },
                    valueRange = 0f..1f
                )

                Text(
                    "External Output: ${(simulation.outputRate * 100).toInt()}%"
                )

                Slider(
                    value = simulation.outputRate,
                    onValueChange = {
                        simulation = simulation.copy(outputRate = it)
                    },
                    valueRange = 0f..1f
                )

                Text(
                    "Internal Refresh: ${(simulation.internalRefresh * 100).toInt()}%"
                )

                Slider(
                    value = simulation.internalRefresh,
                    onValueChange = {
                        simulation = simulation.copy(internalRefresh = it)
                    },
                    valueRange = 0f..1f
                )

                HorizontalDivider()

                Text(
                    "Attention: ${(simulation.attention * 100).toInt()}%"
                )

                Slider(
                    value = simulation.attention,
                    onValueChange = {
                        simulation = simulation.copy(attention = it)
                    },
                    valueRange = 0f..1f
                )

                Text(
                    "Repetition: ${(simulation.repetition * 100).toInt()}%"
                )

                Slider(
                    value = simulation.repetition,
                    onValueChange = {
                        simulation = simulation.copy(repetition = it)
                    },
                    valueRange = 0f..1f
                )

                Text(
                    "Memory Retention: ${(simulation.memoryRetention * 100).toInt()}%"
                )

                Slider(
                    value = simulation.memoryRetention,
                    onValueChange = {
                        simulation = simulation.copy(memoryRetention = it)
                    },
                    valueRange = 0f..1f
                )

                HorizontalDivider()

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Refresh Sound", fontWeight = FontWeight.Bold)
                        Text(
                            if (soundEnabled) "चालू" else "बंद",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = { soundEnabled = it }
                    )
                }

                OutlinedButton(
                    onClick = {
                        running = false
                        simulation = initialSystem()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset Simulation")
                }
            }
        }

        /* EXPERIMENT GUIDE */

        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    "प्रयोग कैसे करें?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "प्रयोग 1: Input और Output दोनों चालू रखें। देखें कि Pattern और Internal Refresh कैसे चलते हैं।"
                )

                Text(
                    "प्रयोग 2: Input को शून्य करें, लेकिन Output और Internal Refresh चालू रखें। देखें कि बाहरी सूचना बंद होने पर सिस्टम के अंदर क्या होता है।"
                )

                Text(
                    "प्रयोग 3: Attention और Repetition बढ़ाएँ। देखें कि सूचना के Record होने की संभावना कैसे बदलती है।"
                )

                Text(
                    "प्रयोग 4: Memory Retention बदलें और देखें कि पहले से Record हुई सूचनाओं की Strength कैसे बदलती है।"
                )

                Text(
                    "ध्यान दें: यह प्रारंभिक मॉडल है। इसमें वास्तविक जैविक चेतना, वास्तविक ऊर्जा संरक्षण या वास्तविक मानव स्मृति का अनुकरण सिद्ध नहीं किया गया है।"
                )
            }
        }
    }
}

/* ---------------------------------------------------------
   LEGEND
   --------------------------------------------------------- */

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(10.dp)) {
            drawCircle(color)
        }
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

/* ---------------------------------------------------------
   SYSTEM CANVAS
   --------------------------------------------------------- */

@Composable
private fun SystemCanvas(
    state: SystemState,
    modifier: Modifier = Modifier
) {

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(BackgroundDark)
    ) {

        val sx = size.width
        val sy = size.height

        val left = sx * 0.07f
        val right = sx * 0.93f
        val top = sy * 0.08f
        val bottom = sy * 0.92f

        val points = systemPattern(state)

        val centers = points.map {
            Offset(it.first * sx, it.second * sy)
        }

        // Open System boundary
        drawRoundRect(
            color = Violet.copy(alpha = 0.65f),
            topLeft = Offset(left, top),
            size = Size(right - left, bottom - top),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                18.dp.toPx()
            ),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Left and right boundary ports
        listOf(
            Offset(left, sy * 0.50f),
            Offset(right, sy * 0.50f)
        ).forEach { port ->
            drawCircle(
                Violet.copy(alpha = 0.30f),
                11.dp.toPx(),
                port
            )
            drawCircle(
                Violet,
                4.dp.toPx(),
                port
            )
        }

        // Continuous connections: recognizable system pattern
        for (i in centers.indices) {

            val next = (i + 1) % centers.size

            drawLine(
                color = Cyan.copy(alpha = 0.60f),
                start = centers[i],
                end = centers[next],
                strokeWidth = 1.8.dp.toPx()
            )

            if (i % 3 == 0) {
                val cross = (i + 3) % centers.size

                drawLine(
                    color = Violet.copy(alpha = 0.30f),
                    start = centers[i],
                    end = centers[cross],
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        // Internal particles
        state.atoms.forEach { atom ->

            val point = centers[atom.slot]

            val pulse = sin(atom.phase).toFloat()

            val radius = (4.0f + pulse * 0.8f).dp.toPx()

            drawCircle(
                color = Green.copy(alpha = 0.18f),
                radius = radius * 2f,
                center = point
            )

            drawCircle(
                color = Green,
                radius = radius,
                center = point
            )
        }

        // Boundary flow particles
        state.flowParticles.forEach { particle ->

            val target = centers[particle.slot]
            val progress = particle.progress.coerceIn(0f, 1f)

            val portX = if (particle.side == BoundarySide.LEFT) {
                left
            } else {
                right
            }

            val outsideX = if (particle.side == BoundarySide.LEFT) {
                left - 18.dp.toPx()
            } else {
                right + 18.dp.toPx()
            }

            val port = Offset(
                portX,
                target.y.coerceIn(top + 8.dp.toPx(), bottom - 8.dp.toPx())
            )

            val start: Offset
            val end: Offset

            if (particle.entering) {
                start = Offset(outsideX, port.y)
                end = target
            } else {
                start = target
                end = Offset(outsideX, port.y)
            }

            val position = Offset(
                start.x + (end.x - start.x) * progress,
                start.y + (end.y - start.y) * progress
            )

            val color = if (particle.entering) Blue else Orange

            drawLine(
                color = color.copy(alpha = 0.35f),
                start = port,
                end = target,
                strokeWidth = 1.2.dp.toPx()
            )

            drawCircle(
                color.copy(alpha = 0.20f),
                9.dp.toPx(),
                position
            )

            drawCircle(
                color,
                4.dp.toPx(),
                position
            )
        }
    }
}
