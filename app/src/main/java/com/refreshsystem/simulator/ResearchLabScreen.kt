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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private val Green = Color(0xFF66BB6A)
private val Orange = Color(0xFFFFB74D)
private val Blue = Color(0xFF42A5F5)
private val Violet = Color(0xFF9C8CFF)
private val BackgroundDark = Color(0xFF0B1220)

private const val FORMATION_SLOTS = 36

private const val STAGE_ONE_MS = 60_000L
private const val STAGE_TWO_END_MS = 180_000L
private const val STAGE_THREE_END_MS = 360_000L

private enum class BoundarySide {
    LEFT, RIGHT, TOP, BOTTOM
}

private data class FormationAtom(
    val id: Int,
    val slot: Int,
    val microPhase: Float = 0f
)

private data class BoundaryParticle(
    val id: Int,
    val slot: Int,
    val progress: Float,
    val entering: Boolean,
    val side: BoundarySide
)

private data class FormationState(
    val step: Int = 0,
    val elapsedMillis: Long = 0L,
    val atoms: List<FormationAtom> = emptyList(),
    val boundaryParticles: List<BoundaryParticle> = emptyList(),
    val refreshPercent: Float = 0.05f,
    val internalMovement: Float = 0.55f,
    val environment: Float = 0.50f,
    val speed: Float = 1f,
    val totalIn: Int = 0,
    val totalOut: Int = 0,
    val bondChanges: Int = 0,
    val evolution: Float = 0f,
    val lastEvent: String = "कण गति कर रहे हैं। अभी कोई निश्चित पैटर्न नहीं है।"
)

private fun clamp(value: Float) = value.coerceIn(0f, 1f)

private fun formationStage(elapsedMillis: Long): Int = when {
    elapsedMillis < STAGE_ONE_MS -> 1
    elapsedMillis < STAGE_TWO_END_MS -> 2
    elapsedMillis < STAGE_THREE_END_MS -> 3
    else -> 4
}

private fun makeInitialFormation(): FormationState {
    val atoms = (0 until FORMATION_SLOTS).map { index ->
        FormationAtom(
            id = index + 1,
            slot = index,
            microPhase = index * 0.37f
        )
    }

    return FormationState(atoms = atoms)
}

/* ---------------------------------------------------------
   PATTERN POSITIONS
   --------------------------------------------------------- */

private fun scatteredPoints(): List<Pair<Float, Float>> {
    return (0 until FORMATION_SLOTS).map { index ->
        val x = 0.13f + ((index * 37) % 73) / 100f
        val y = 0.17f + ((index * 53) % 65) / 100f
        x to y
    }
}

private fun wPatternPoints(): List<Pair<Float, Float>> {
    val vertices = listOf(
        0.12f to 0.28f,
        0.24f to 0.72f,
        0.38f to 0.36f,
        0.50f to 0.72f,
        0.63f to 0.36f,
        0.76f to 0.72f,
        0.88f to 0.28f
    )

    return (0 until FORMATION_SLOTS).map { index ->
        val t = index.toFloat() / (FORMATION_SLOTS - 1)
        val scaled = t * (vertices.size - 1)
        val segment = scaled.toInt().coerceAtMost(vertices.size - 2)
        val local = scaled - segment

        val a = vertices[segment]
        val b = vertices[segment + 1]

        (a.first + (b.first - a.first) * local) to
            (a.second + (b.second - a.second) * local)
    }
}

private fun evolutionTarget(
    environment: Float
): List<Pair<Float, Float>> {

    return (0 until FORMATION_SLOTS).map { index ->
        val t = index.toFloat() / (FORMATION_SLOTS - 1)
        val x = 0.12f + 0.76f * t

        val y = if (environment < 0.5f) {
            if (t < 0.5f) {
                0.36f + 0.30f * (t / 0.5f)
            } else {
                0.66f - 0.30f * ((t - 0.5f) / 0.5f)
            }
        } else {
            0.50f + 0.20f * sin(t * Math.PI * 2.0).toFloat()
        }

        x to y
    }
}

/*
 * Stage 1: scattered particles.
 * Stage 2: particles gradually move toward W positions.
 * Stage 3: complete W remains visible.
 * Stage 4: W gradually changes toward another pattern.
 */
private fun formationPoints(
    state: FormationState
): List<Pair<Float, Float>> {

    val scattered = scatteredPoints()
    val w = wPatternPoints()
    val target = evolutionTarget(state.environment)

    val stage = formationStage(state.elapsedMillis)

    val buildProgress = when (stage) {
        1 -> 0f
        2 -> (
            (state.elapsedMillis - STAGE_ONE_MS).toFloat() /
                (STAGE_TWO_END_MS - STAGE_ONE_MS)
            ).coerceIn(0f, 1f)
        else -> 1f
    }

    val evolutionProgress = if (stage == 4) {
        state.evolution.coerceIn(0f, 1f)
    } else {
        0f
    }

    return (0 until FORMATION_SLOTS).map { index ->

        val baseX = scattered[index].first
        val baseY = scattered[index].second

        val wX = w[index].first
        val wY = w[index].second

        val evolvedX = target[index].first
        val evolvedY = target[index].second

        val formedX = baseX + (wX - baseX) * buildProgress
        val formedY = baseY + (wY - baseY) * buildProgress

        val finalX = formedX + (evolvedX - formedX) * evolutionProgress
        val finalY = formedY + (evolvedY - formedY) * evolutionProgress

        finalX to finalY
    }
}

/* ---------------------------------------------------------
   SIMULATION ENGINE
   --------------------------------------------------------- */

private fun nextFormationState(
    old: FormationState,
    cycleDurationMs: Long
): FormationState {

    val nextStep = old.step + 1

    val elapsed = old.elapsedMillis + cycleDurationMs
    val stage = formationStage(elapsed)

    val atoms = old.atoms
        .ifEmpty { makeInitialFormation().atoms }
        .toMutableList()

    val boundary = old.boundaryParticles.mapNotNull { particle ->
        val nextProgress = particle.progress + 0.22f
        if (nextProgress >= 1f) null
        else particle.copy(progress = nextProgress)
    }.toMutableList()

    var totalIn = old.totalIn
    var totalOut = old.totalOut
    var bondChanges = old.bondChanges

    val exchangeCount = (
        FORMATION_SLOTS *
            old.refreshPercent.coerceIn(0.01f, 0.20f)
        ).toInt().coerceAtLeast(1)

    val perSide = (exchangeCount / 4).coerceAtLeast(1)

    // चारों दिशाओं में Entry और Exit दोनों।
    if (nextStep % 2 == 0) {

        val sideSlots = listOf(
            BoundarySide.LEFT to listOf(0, 1, 2),
            BoundarySide.RIGHT to listOf(33, 34, 35),
            BoundarySide.TOP to listOf(5, 17, 29),
            BoundarySide.BOTTOM to listOf(11, 23, 35)
        )

        sideSlots.forEachIndexed { sideIndex, (side, slots) ->

            repeat(perSide.coerceAtMost(slots.size)) { n ->

                val slot = slots[
                    (nextStep / 2 + n + sideIndex) % slots.size
                ]

                val atomIndex = atoms.indexOfFirst {
                    it.slot == slot
                }

                if (atomIndex >= 0) {

                    val oldAtom = atoms[atomIndex]

                    boundary.add(
                        BoundaryParticle(
                            id = oldAtom.id,
                            slot = slot,
                            progress = 0f,
                            entering = false,
                            side = side
                        )
                    )

                    val newId =
                        (atoms.maxOfOrNull { it.id } ?: 0) + 1

                    atoms[atomIndex] = FormationAtom(
                        id = newId,
                        slot = slot,
                        microPhase = nextStep * 0.41f + n + sideIndex
                    )

                    boundary.add(
                        BoundaryParticle(
                            id = newId,
                            slot = slot,
                            progress = 0f,
                            entering = true,
                            side = side
                        )
                    )

                    totalOut++
                    totalIn++
                }
            }
        }
    }

    // पूरे सिस्टम के अंदर सभी कणों की आंतरिक गति।
    val movedAtoms = atoms.map { atom ->
        atom.copy(
            microPhase = atom.microPhase +
                old.internalMovement * 0.34f + 0.08f
        )
    }

    if (stage >= 2) {
        bondChanges += 2 + (old.internalMovement * 5).toInt()
    }

    // Evolution वास्तविक बीते समय के अनुसार बढ़ती है।
    val evolution = if (stage >= 4) {
        (
            old.evolution +
                (cycleDurationMs / 1000f) * 0.0015f *
                (0.35f + old.environment)
            ).coerceAtMost(1f)
    } else {
        0f
    }

    val message = when (stage) {
        1 ->
            "पहला चरण: कण स्वतंत्र रूप से गति कर रहे हैं। चारों दिशाओं में पदार्थ का आदान-प्रदान हो रहा है।"

        2 ->
            "दूसरा चरण: कण धीरे-धीरे अपनी नई स्थितियों की ओर बढ़ रहे हैं। उनके बीच नए संबंध बन रहे हैं और W-पैटर्न आकार ले रहा है।"

        3 ->
            "तीसरा चरण: W-पैटर्न बन चुका है। चारों दिशाओं से कुछ कण अंदर आ रहे हैं और कुछ बाहर जा रहे हैं। अंदर के कण भी लगातार अपने संबंध बदल रहे हैं।"

        else ->
            "चौथा चरण: पूरा सिस्टम लगातार Refresh हो रहा है। लंबे समय के बदलाव जमा होकर अगले मिलते-जुलते Pattern की दिशा को प्रभावित कर रहे हैं।"
    }

    return old.copy(
        step = nextStep,
        elapsedMillis = elapsed,
        atoms = movedAtoms,
        boundaryParticles = boundary,
        totalIn = totalIn,
        totalOut = totalOut,
        bondChanges = bondChanges,
        evolution = evolution,
        lastEvent = message
    )
}

/* ---------------------------------------------------------
   MAIN APP
   --------------------------------------------------------- */

@Composable
fun ResearchHomeScreen() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            OpenSystemFormationLab()
        }
    }
}

/* ---------------------------------------------------------
   FORMATION LAB SCREEN
   --------------------------------------------------------- */

@Composable
private fun OpenSystemFormationLab() {

    var simulation by remember {
        mutableStateOf(makeInitialFormation())
    }

    var running by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(true) }

    val toneGenerator = remember {
        ToneGenerator(AudioManager.STREAM_MUSIC, 65)
    }

    DisposableEffect(Unit) {
        onDispose {
            toneGenerator.stopTone()
            toneGenerator.release()
        }
    }

    // Simulation speed controls particle refresh.
    // Timeline advances by actual elapsed cycle time.
    LaunchedEffect(running, simulation.speed) {

        while (running) {

            val cycleDelay = (
                650L / simulation.speed.coerceIn(0.4f, 3f)
            ).toLong().coerceAtLeast(80L)

            delay(cycleDelay)

            simulation = nextFormationState(
                old = simulation,
                cycleDurationMs = cycleDelay
            )
        }
    }

    // Continuous vibration-like beep.
    // Higher simulation speed = faster beep repetition.
    LaunchedEffect(running, soundEnabled, simulation.speed) {

        toneGenerator.stopTone()

        while (running && soundEnabled) {

            val soundInterval = (
                260L / simulation.speed.coerceIn(0.4f, 3f)
            ).toLong().coerceAtLeast(65L)

            toneGenerator.startTone(
                ToneGenerator.TONE_PROP_BEEP2,
                55
            )

            delay(soundInterval)
        }

        toneGenerator.stopTone()
    }

    val stage = formationStage(simulation.elapsedMillis)

    val patternName = when (stage) {
        1 -> "अभी कोई निश्चित Pattern नहीं"
        2 -> "W Pattern बन रहा है"
        3 -> "W Pattern"
        else -> if (simulation.environment < 0.5f) {
            "T जैसा संभावित Pattern"
        } else {
            "B जैसा संभावित Pattern"
        }
    }

    val stageProgress = when (stage) {
        1 -> simulation.elapsedMillis.toFloat() / STAGE_ONE_MS
        2 -> (
            (simulation.elapsedMillis - STAGE_ONE_MS).toFloat() /
                (STAGE_TWO_END_MS - STAGE_ONE_MS)
            )
        3 -> (
            (simulation.elapsedMillis - STAGE_TWO_END_MS).toFloat() /
                (STAGE_THREE_END_MS - STAGE_TWO_END_MS)
            )
        else -> simulation.evolution
    }.coerceIn(0f, 1f)

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
            "Open System: Formation, Refresh और Evolution",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            "कणों के लगातार बदलने, संबंध बनने और सिस्टम के Pattern को बनाए रखने की प्रक्रिया।"
        )

        /* SIMULATION VIEW */

        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "जीवित संरचना का मॉडल",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "चरण $stage / 4",
                        color = Color(0xFF67E8F9),
                        fontWeight = FontWeight.Bold
                    )
                }

                FormationCanvas(
                    state = simulation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FormationLegend(Blue, "नया प्रवेश")
                    FormationLegend(Orange, "बाहर निकास")
                    FormationLegend(Green, "अंदर के कण")
                }

                Text(simulation.lastEvent)

                Text(
                    "वर्तमान Pattern: $patternName",
                    fontWeight = FontWeight.Bold
                )

                LinearProgressIndicator(
                    progress = { stageProgress },
                    modifier = Modifier.fillMaxWidth()
                )

                if (stage < 4) {
                    Text(
                        "इस चरण की प्रगति: ${(stageProgress * 100).toInt()}%"
                    )
                } else {
                    Text(
                        "Evolution प्रगति: ${(simulation.evolution * 100).toInt()}%"
                    )
                }
            }
        }

        /* TIMELINE */

        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Text(
                    "प्रक्रिया की Timeline",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                TimelineItem(
                    number = 1,
                    title = "कणों की गति और Entry / Exit",
                    duration = "1 मिनट",
                    active = stage == 1
                )

                TimelineItem(
                    number = 2,
                    title = "कणों के जुड़ने से W-पैटर्न बनना",
                    duration = "2 मिनट",
                    active = stage == 2
                )

                TimelineItem(
                    number = 3,
                    title = "W-पैटर्न का लगातार Refresh होना",
                    duration = "3 मिनट",
                    active = stage == 3
                )

                TimelineItem(
                    number = 4,
                    title = "लंबे समय में Pattern Evolution",
                    duration = "अनलिमिटेड",
                    active = stage == 4
                )
            }
        }

        /* LIVE SYSTEM STATUS */

        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    "अभी सिस्टम में क्या हो रहा है?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text("वर्तमान Pattern: $patternName")
                Text("कुल Simulation Cycles: ${simulation.step}")
                Text("सिस्टम के अंदर कण: ${simulation.atoms.size}")
                Text("कुल अंदर आए: ${simulation.totalIn}")
                Text("कुल बाहर गए: ${simulation.totalOut}")
                Text("बदले / Refresh हुए Bonds: ${simulation.bondChanges}")

                val elapsedSeconds = simulation.elapsedMillis / 1000L
                val minutes = elapsedSeconds / 60L
                val seconds = elapsedSeconds % 60L

                Text(
                    "कुल बीता समय: %02d:%02d".format(minutes, seconds),
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

                            simulation = nextFormationState(
                                simulation,
                                cycleDelay
                            )

                            if (soundEnabled) {
                                toneGenerator.startTone(
                                    ToneGenerator.TONE_PROP_BEEP2,
                                    55
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("एक Cycle")
                    }
                }

                Text(
                    "Simulation Speed: ${
                        String.format(
                            Locale.getDefault(),
                            "%.1f",
                            simulation.speed
                        )
                    }x",
                    fontWeight = FontWeight.Bold
                )

                Slider(
                    value = simulation.speed,
                    onValueChange = {
                        simulation = simulation.copy(speed = it)
                    },
                    valueRange = 0.4f..3f
                )

                Text(
                    "Boundary Exchange: ${(simulation.refreshPercent * 100).toInt()}%"
                )

                Slider(
                    value = simulation.refreshPercent,
                    onValueChange = {
                        simulation = simulation.copy(refreshPercent = it)
                    },
                    valueRange = 0.01f..0.20f
                )

                Text(
                    "Internal Movement: ${(simulation.internalMovement * 100).toInt()}%"
                )

                Slider(
                    value = simulation.internalMovement,
                    onValueChange = {
                        simulation = simulation.copy(internalMovement = it)
                    },
                    valueRange = 0.05f..1f
                )

                Text(
                    "Environment Influence: ${(simulation.environment * 100).toInt()}%"
                )

                Slider(
                    value = simulation.environment,
                    onValueChange = {
                        simulation = simulation.copy(environment = it)
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
                        Text(
                            "Refresh Vibration Sound",
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            if (soundEnabled) "बीप ध्वनि चालू है"
                            else "बीप ध्वनि बंद है",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = {
                            soundEnabled = it
                        }
                    )
                }

                OutlinedButton(
                    onClick = {
                        running = false
                        simulation = makeInitialFormation()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("पूरी प्रक्रिया फिर से शुरू करें")
                }
            }
        }

        /* EXPLANATION */

        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    "आसान भाषा में समझें",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "पहले चरण में कण स्वतंत्र रूप से गति करते हैं। चारों दिशाओं से पदार्थ का आदान-प्रदान होता है।"
                )

                Text(
                    "दूसरे चरण में कण धीरे-धीरे अपनी नई स्थितियों की ओर बढ़ते हैं। उनके बीच कनेक्शन एक-एक करके बनते हैं और W-पैटर्न धीरे-धीरे उभरता है।"
                )

                Text(
                    "तीसरे चरण में पूरा W-पैटर्न बना रहता है। कुछ कण बाहर जाते हैं, नए कण अंदर आते हैं और बाकी कण भी आंतरिक रूप से गति करके अपने संबंध बदलते रहते हैं।"
                )

                Text(
                    "चौथे चरण में लंबे समय के बदलाव जमा होकर अगले मिलते-जुलते Pattern की दिशा को प्रभावित करते हैं।"
                )
            }
        }
    }
}

/* ---------------------------------------------------------
   TIMELINE COMPONENT
   --------------------------------------------------------- */

@Composable
private fun TimelineItem(
    number: Int,
    title: String,
    duration: String,
    active: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            if (active) "●" else "○",
            color = if (active) Green
            else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.width(10.dp))

        Column(Modifier.weight(1f)) {
            Text(
                "$number. $title",
                color = if (active) Green
                else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                duration,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/* ---------------------------------------------------------
   LEGEND
   --------------------------------------------------------- */

@Composable
private fun FormationLegend(
    color: Color,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(10.dp)) {
            drawCircle(color = color)
        }

        Spacer(Modifier.width(5.dp))

        Text(
            label,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

/* ---------------------------------------------------------
   FORMATION CANVAS
   --------------------------------------------------------- */

@Composable
private fun FormationCanvas(
    state: FormationState,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(BackgroundDark)
    ) {

        val sx = size.width
        val sy = size.height
        val stage = formationStage(state.elapsedMillis)

        val points = formationPoints(state)

        val centers = points.map {
            Offset(it.first * sx, it.second * sy)
        }

        val left = sx * 0.07f
        val right = sx * 0.93f
        val top = sy * 0.10f
        val bottom = sy * 0.90f

        // System boundary.
        drawRoundRect(
            color = Violet.copy(alpha = 0.40f),
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(
                right - left,
                bottom - top
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                18.dp.toPx()
            ),
            style = Stroke(width = 1.4.dp.toPx())
        )

        // Four boundary ports.
        val ports = listOf(
            Offset(left, sy * 0.50f),
            Offset(right, sy * 0.50f),
            Offset(sx * 0.50f, top),
            Offset(sx * 0.50f, bottom)
        )

        ports.forEach { port ->
            drawCircle(
                Violet.copy(alpha = 0.25f),
                8.dp.toPx(),
                port
            )

            drawCircle(
                Violet.copy(alpha = 0.75f),
                3.dp.toPx(),
                port
            )
        }

        /*
         * W connections are built progressively.
         * Only the bonds whose construction progress
         * has been reached are drawn.
         */
        if (stage >= 2) {

            val buildProgress = if (stage == 2) {
                (
                    (state.elapsedMillis - STAGE_ONE_MS).toFloat() /
                        (STAGE_TWO_END_MS - STAGE_ONE_MS)
                    ).coerceIn(0f, 1f)
            } else {
                1f
            }

            val visibleBonds = (
                buildProgress * (centers.size - 1)
                ).toInt().coerceIn(0, centers.size - 1)

            for (i in 0 until visibleBonds) {

                drawLine(
                    color = Color(0xFF38BDF8).copy(alpha = 0.85f),
                    start = centers[i],
                    end = centers[i + 1],
                    strokeWidth = 2.2.dp.toPx()
                )
            }

            // Secondary connections appear gradually too.
            val secondaryCount = (
                buildProgress * (centers.size / 3)
                ).toInt()

            for (n in 0 until secondaryCount) {
                val i = n * 3
                val j = (i + 2).coerceAtMost(centers.lastIndex)

                if (j > i) {
                    drawLine(
                        color = Violet.copy(alpha = 0.42f),
                        start = centers[i],
                        end = centers[j],
                        strokeWidth = 1.2.dp.toPx()
                    )
                }
            }
        }

        // All internal particles move.
        state.atoms.forEach { atom ->

            val target = centers[atom.slot]
            val phase = atom.microPhase + state.step * 0.12f

            val movement = state.internalMovement *
                (1.2f + (atom.slot % 4) * 0.35f) *
                density

            val point = Offset(
                target.x + sin(phase) * movement,
                target.y + cos(phase * 1.3f) * movement
            )

            drawCircle(
                Color(0xFF1E293B),
                8.dp.toPx(),
                point
            )

            drawCircle(
                Green,
                4.dp.toPx(),
                point
            )
        }

        /*
         * Boundary entry/exit particles stay inside
         * the system boundary. No external W pattern.
         */
        state.boundaryParticles.forEach { particle ->

            val target = centers[particle.slot]
            val p = particle.progress.coerceIn(0f, 1f)

            val port = when (particle.side) {

                BoundarySide.LEFT -> Offset(
                    left + 7.dp.toPx(),
                    target.y.coerceIn(
                        top + 8.dp.toPx(),
                        bottom - 8.dp.toPx()
                    )
                )

                BoundarySide.RIGHT -> Offset(
                    right - 7.dp.toPx(),
                    target.y.coerceIn(
                        top + 8.dp.toPx(),
                        bottom - 8.dp.toPx()
                    )
                )

                BoundarySide.TOP -> Offset(
                    target.x.coerceIn(
                        left + 8.dp.toPx(),
                        right - 8.dp.toPx()
                    ),
                    top + 7.dp.toPx()
                )

                BoundarySide.BOTTOM -> Offset(
                    target.x.coerceIn(
                        left + 8.dp.toPx(),
                        right - 8.dp.toPx()
                    ),
                    bottom - 7.dp.toPx()
                )
            }

            val start = if (particle.entering) port else target
            val end = if (particle.entering) target else port

            val position = Offset(
                start.x + (end.x - start.x) * p,
                start.y + (end.y - start.y) * p
            )

            val color = if (particle.entering) Blue else Orange

            drawLine(
                color = color.copy(alpha = 0.25f),
                start = port,
                end = target,
                strokeWidth = 1.2.dp.toPx()
            )

            drawCircle(
                color.copy(alpha = 0.18f),
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
