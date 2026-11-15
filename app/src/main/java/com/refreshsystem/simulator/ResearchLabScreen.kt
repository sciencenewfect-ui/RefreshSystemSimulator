
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

private val Green = Color(0xFF66BB6A)
private val Blue = Color(0xFF42A5F5)
private val Orange = Color(0xFFFFB74D)
private val Violet = Color(0xFF9C8CFF)
private val Cyan = Color(0xFF38BDF8)
private val Dark = Color(0xFF0B1220)

private const val MAX_PARTICLES = 100
private const val MIN_PARTICLES = 20

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

private data class PatternSnapshot(
    val step: Int,
    val bonds: Int,
    val averagePosition: Float
)

private data class ResearchState(
    val step: Int = 0,
    val elapsed: Long = 0L,
    val particles: List<MicroParticle> = emptyList(),

    val inputRate: Float = 0.15f,
    val outputRate: Float = 0.08f,
    val refreshRate: Float = 0.65f,

    val attraction: Float = 0.55f,
    val repulsion: Float = 0.40f,
    val environment: Float = 0.50f,
    val connectionRange: Float = 0.16f,

    val speed: Float = 1f,
    val running: Boolean = true,

    val totalIn: Int = 0,
    val totalOut: Int = 0,
    val connectionsFormed: Int = 0,
    val connectionsBroken: Int = 0,

    val bonds: List<MicroBond> = emptyList(),
    val snapshots: List<PatternSnapshot> = emptyList(),

    val continuity: Float = 1f,
    val patternDrift: Float = 0f,

    val lastEvent: String =
        "Open System तैयार है। स्थानीय नियमों से Pattern उभरना शुरू होगा।"
)

private fun clamp(v: Float) = v.coerceIn(0f, 1f)

private fun distance(
    a: MicroParticle,
    b: MicroParticle
): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return sqrt(dx * dx + dy * dy)
}

private fun initialResearchState(): ResearchState {

    val random = Random(73)

    val particles = (0 until 65).map { index ->
        MicroParticle(
            id = index + 1,
            x = 0.12f + random.nextFloat() * 0.76f,
            y = 0.12f + random.nextFloat() * 0.76f,
            vx = (random.nextFloat() - 0.5f) * 0.01f,
            vy = (random.nextFloat() - 0.5f) * 0.01f,
            phase = random.nextFloat() * 6.28f
        )
    }

    return ResearchState(particles = particles)
}

/* ---------------------------------------------------------
   CONNECTION GENERATION
   --------------------------------------------------------- */

private fun calculateBonds(
    particles: List<MicroParticle>,
    range: Float
): List<MicroBond> {

    val result = mutableListOf<MicroBond>()

    for (i in particles.indices) {
        for (j in i + 1 until particles.size) {

            if (distance(particles[i], particles[j]) < range) {
                result.add(
                    MicroBond(
                        particles[i].id,
                        particles[j].id
                    )
                )
            }
        }
    }

    return result
}

/* ---------------------------------------------------------
   PATTERN METRICS
   --------------------------------------------------------- */

private fun averagePosition(
    particles: List<MicroParticle>
): Float {

    if (particles.isEmpty()) return 0f

    return particles.sumOf {
        (it.x + it.y).toDouble()
    }.toFloat() / (particles.size * 2f)
}

private fun bondSimilarity(
    oldBonds: List<MicroBond>,
    newBonds: List<MicroBond>
): Float {

    if (oldBonds.isEmpty() && newBonds.isEmpty()) return 1f
    if (oldBonds.isEmpty() || newBonds.isEmpty()) return 0f

    val oldSet = oldBonds.map {
        minOf(it.a, it.b).toString() + "-" +
            maxOf(it.a, it.b).toString()
    }.toSet()

    val newSet = newBonds.map {
        minOf(it.a, it.b).toString() + "-" +
            maxOf(it.a, it.b).toString()
    }.toSet()

    val common = oldSet.intersect(newSet).size
    val union = oldSet.union(newSet).size

    return if (union == 0) 1f
    else common.toFloat() / union
}

/* ---------------------------------------------------------
   MICROSTRUCTURE REFRESH ENGINE
   --------------------------------------------------------- */

private fun nextResearchState(
    old: ResearchState,
    cycleMs: Long
): ResearchState {

    val random = Random(old.step * 7919 + 31)

    val particles = old.particles.toMutableList()

    val oldBonds = old.bonds
    var totalIn = old.totalIn
    var totalOut = old.totalOut

    // -----------------------------------------------------
    // 1. EXTERNAL INPUT
    // -----------------------------------------------------

    val inputAmount = (
        old.inputRate * 4f +
            if (random.nextFloat() < old.inputRate) 1f else 0f
        ).toInt().coerceIn(0, 5)

    repeat(inputAmount) {

        if (particles.size < MAX_PARTICLES) {

            val side = random.nextInt(4)

            val x = when (side) {
                0 -> 0.03f
                1 -> 0.97f
                else -> random.nextFloat()
            }

            val y = when (side) {
                2 -> 0.03f
                3 -> 0.97f
                else -> random.nextFloat()
            }

            val id = (particles.maxOfOrNull { it.id } ?: 0) + 1

            particles.add(
                MicroParticle(
                    id = id,
                    x = x,
                    y = y,
                    vx = (0.5f - x) * 0.02f,
                    vy = (0.5f - y) * 0.02f,
                    phase = random.nextFloat() * 6.28f
                )
            )

            totalIn++
        }
    }

    // -----------------------------------------------------
    // 2. LOCAL INTERACTIONS
    // Particles respond to nearby particles.
    // -----------------------------------------------------

    val moved = particles.map { particle ->

        var forceX = 0f
        var forceY = 0f
        var neighborCount = 0

        particles.forEach { other ->

            if (particle.id != other.id) {

                val dx = other.x - particle.x
                val dy = other.y - particle.y
                val d = sqrt(dx * dx + dy * dy)

                if (d > 0.0001f && d < old.connectionRange * 1.8f) {

                    neighborCount++

                    val nx = dx / d
                    val ny = dy / d

                    val preferredDistance =
                        0.055f + old.environment * 0.025f

                    if (d < preferredDistance) {

                        val push =
                            (preferredDistance - d) *
                                old.repulsion * 0.65f

                        forceX -= nx * push
                        forceY -= ny * push

                    } else {

                        val pull =
                            (d - preferredDistance) *
                                old.attraction * 0.20f

                        forceX += nx * pull
                        forceY += ny * pull
                    }
                }
            }
        }

        // Environmental influence
        val environmentForce =
            (old.environment - 0.5f) * 0.0015f

        forceX += environmentForce

        // Internal micro refresh
        val phaseChange =
            old.refreshRate * 0.003f

        val vx = (
            particle.vx * 0.90f +
                forceX * 0.12f +
                (random.nextFloat() - 0.5f) *
                old.refreshRate * 0.0015f
            ).coerceIn(-0.012f, 0.012f)

        val vy = (
            particle.vy * 0.90f +
                forceY * 0.12f +
                (random.nextFloat() - 0.5f) *
                old.refreshRate * 0.0015f
            ).coerceIn(-0.012f, 0.012f)

        val newX = (particle.x + vx).coerceIn(0.04f, 0.96f)
        val newY = (particle.y + vy).coerceIn(0.04f, 0.96f)

        particle.copy(
            x = newX,
            y = newY,
            vx = vx,
            vy = vy,
            phase = particle.phase + phaseChange
        )
    }

    // -----------------------------------------------------
    // 3. EXTERNAL OUTPUT
    // -----------------------------------------------------

    val outputAmount = (
        old.outputRate * 4f +
            if (random.nextFloat() < old.outputRate) 1f else 0f
        ).toInt().coerceIn(0, 5)

    val finalParticles = moved.toMutableList()

    repeat(outputAmount) {

        if (finalParticles.size > MIN_PARTICLES) {

            val index = random.nextInt(finalParticles.size)

            finalParticles.removeAt(index)
            totalOut++
        }
    }

    // -----------------------------------------------------
    // 4. NEW CONNECTIONS AND BROKEN CONNECTIONS
    // -----------------------------------------------------

    val newBonds = calculateBonds(
        finalParticles,
        old.connectionRange
    )

    val oldBondKeys = oldBonds.map {
        "${minOf(it.a, it.b)}-${maxOf(it.a, it.b)}"
    }.toSet()

    val newBondKeys = newBonds.map {
        "${minOf(it.a, it.b)}-${maxOf(it.a, it.b)}"
    }.toSet()

    val formed = newBondKeys.subtract(oldBondKeys).size
    val broken = oldBondKeys.subtract(newBondKeys).size

    // -----------------------------------------------------
    // 5. DYNAMIC STABILITY AND PATTERN DRIFT
    // -----------------------------------------------------

    val similarity = bondSimilarity(oldBonds, newBonds)

    val oldCenter = averagePosition(old.particles)
    val newCenter = averagePosition(finalParticles)

    val drift = abs(newCenter - oldCenter)

    val continuity = clamp(
        similarity * 0.65f +
            (1f - drift * 3f) * 0.35f
    )

    // -----------------------------------------------------
    // 6. SNAPSHOT
    // -----------------------------------------------------

    var snapshots = old.snapshots

    if ((old.step + 1) % 30 == 0) {

        snapshots = (
            snapshots + PatternSnapshot(
                step = old.step + 1,
                bonds = newBonds.size,
                averagePosition = newCenter
            )
        ).takeLast(20)
    }

    val message = when {

        formed > broken ->
            "नई संरचना बन रही है। स्थानीय कणों के बीच नए Connections बन रहे हैं।"

        broken > formed ->
            "पुराने Connections टूट रहे हैं। Microstructure अपनी व्यवस्था बदल रहा है।"

        formed > 0 || broken > 0 ->
            "Connections लगातार बन और टूट रहे हैं। Pattern Refresh हो रहा है।"

        else ->
            "कणों की आंतरिक अवस्था बदल रही है। वर्तमान संरचना की निरंतरता बनी हुई है।"
    }

    return old.copy(
        step = old.step + 1,
        elapsed = old.elapsed + cycleMs,
        particles = finalParticles,
        totalIn = totalIn,
        totalOut = totalOut,
        bonds = newBonds,
        connectionsFormed = old.connectionsFormed + formed,
        connectionsBroken = old.connectionsBroken + broken,
        continuity = continuity,
        patternDrift = drift,
        snapshots = snapshots,
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
            MicrostructureResearchLab()
        }
    }
}

/* ---------------------------------------------------------
   RESEARCH LAB
   --------------------------------------------------------- */

@Composable
private fun MicrostructureResearchLab() {

    var state by remember {
        mutableStateOf(initialResearchState())
    }

    var running by remember { mutableStateOf(true) }

    LaunchedEffect(running, state.speed) {

        while (running) {

            val cycle = (
                650L / state.speed.coerceIn(0.4f, 3f)
                ).toLong().coerceAtLeast(80L)

            delay(cycle)

            state = nextResearchState(state, cycle)
        }
    }

    val minutes = state.elapsed / 60000L
    val seconds = (state.elapsed / 1000L) % 60L

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
            "Microstructure & Dynamic Stability Lab",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            "स्थानीय कणों और उनके Connections में लगातार बदलाव से Pattern का उभरना, Refresh होना और समय के साथ विकसित होना।"
        )

        // -------------------------------------------------
        // SIMULATION
        // -------------------------------------------------

        Card(Modifier.fillMaxWidth()) {

            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Text(
                    "Living Microstructure",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                MicrostructureCanvas(
                    state = state,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendDot(Green, "कण")
                    LegendDot(Cyan, "Connection")
                    LegendDot(Blue, "Input")
                    LegendDot(Orange, "Output")
                }

                Text(state.lastEvent)

                Text(
                    "Pattern: Self-Organizing Dynamic Structure",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // -------------------------------------------------
        // DYNAMIC STABILITY
        // -------------------------------------------------

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
                    "कण और Connections बदल रहे हैं। यह सूचकांक पुराने और नए Connections की समानता तथा औसत स्थिति में बदलाव पर आधारित है।"
                )

                Text(
                    "Structural Continuity: ${(state.continuity * 100).toInt()}%",
                    fontWeight = FontWeight.Bold
                )

                LinearProgressIndicator(
                    progress = { state.continuity },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "Pattern Drift: ${String.format(Locale.US, "%.4f", state.patternDrift)}"
                )

                Text(
                    "यह गणनात्मक Proxy है, पूर्ण Stability या चेतना का माप नहीं।",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // -------------------------------------------------
        // LIVE STRUCTURE
        // -------------------------------------------------

        Card(Modifier.fillMaxWidth()) {

            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {

                Text(
                    "Microstructure Status",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text("Simulation Cycles: ${state.step}")
                Text("Total Particles: ${state.particles.size}")
                Text("Active Connections: ${state.bonds.size}")
                Text("Connections Formed: ${state.connectionsFormed}")
                Text("Connections Broken: ${state.connectionsBroken}")
                Text("Total Input: ${state.totalIn}")
                Text("Total Output: ${state.totalOut}")

                Text(
                    "Elapsed Time: %02d:%02d".format(minutes, seconds),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // -------------------------------------------------
        // SNAPSHOTS AND EVOLUTION
        // -------------------------------------------------

        Card(Modifier.fillMaxWidth()) {

            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    "Long-Term Pattern Evolution",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "हर 30 Cycles के बाद Pattern Snapshot लिया जाता है। इससे समय के साथ संरचना में हुए बदलावों की तुलना की जा सकती है।"
                )

                Text("Recorded Snapshots: ${state.snapshots.size}")

                state.snapshots.takeLast(5).reversed().forEach { snapshot ->

                    Text(
                        "Cycle ${snapshot.step}  |  Connections: ${snapshot.bonds}  |  Position: ${
                            String.format(Locale.US, "%.3f", snapshot.averagePosition)
                        }"
                    )
                }
            }
        }

        // -------------------------------------------------
        // CONTROLS
        // -------------------------------------------------

        Card(Modifier.fillMaxWidth()) {

            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {

                Text(
                    "Research Controls",
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
                            val cycle = (
                                650L / state.speed.coerceIn(0.4f, 3f)
                                ).toLong().coerceAtLeast(80L)

                            state = nextResearchState(state, cycle)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("एक Cycle")
                    }
                }

                Text("Simulation Speed: ${state.speed.format1()}x")

                Slider(
                    value = state.speed,
                    onValueChange = {
                        state = state.copy(speed = it)
                    },
                    valueRange = 0.4f..3f
                )

                HorizontalDivider()

                Text("External Input: ${(state.inputRate * 100).toInt()}%")

                Slider(
                    value = state.inputRate,
                    onValueChange = {
                        state = state.copy(inputRate = it)
                    },
                    valueRange = 0f..1f
                )

                Text("External Output: ${(state.outputRate * 100).toInt()}%")

                Slider(
                    value = state.outputRate,
                    onValueChange = {
                        state = state.copy(outputRate = it)
                    },
                    valueRange = 0f..1f
                )

                Text("Internal Refresh: ${(state.refreshRate * 100).toInt()}%")

                Slider(
                    value = state.refreshRate,
                    onValueChange = {
                        state = state.copy(refreshRate = it)
                    },
                    valueRange = 0f..1f
                )

                HorizontalDivider()

                Text("Attraction: ${(state.attraction * 100).toInt()}%")

                Slider(
                    value = state.attraction,
                    onValueChange = {
                        state = state.copy(attraction = it)
                    },
                    valueRange = 0f..1f
                )

                Text("Repulsion: ${(state.repulsion * 100).toInt()}%")

                Slider(
                    value = state.repulsion,
                    onValueChange = {
                        state = state.copy(repulsion = it)
                    },
                    valueRange = 0f..1f
                )

                Text("Connection Range: ${state.connectionRange.format1()}")

                Slider(
                    value = state.connectionRange,
                    onValueChange = {
                        state = state.copy(connectionRange = it)
                    },
                    valueRange = 0.05f..0.30f
                )

                HorizontalDivider()

                Text("Environment: ${(state.environment * 100).toInt()}%")

                Slider(
                    value = state.environment,
                    onValueChange = {
                        state = state.copy(environment = it)
                    },
                    valueRange = 0f..1f
                )

                OutlinedButton(
                    onClick = {
                        running = false
                        state = initialResearchState()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset Research Simulation")
                }
            }
        }

        // -------------------------------------------------
        // EXPERIMENT GUIDE
        // -------------------------------------------------

        Card(Modifier.fillMaxWidth()) {

            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    "प्रयोग करने का तरीका",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Experiment A: Attraction और Repulsion को स्थिर रखकर Refresh Rate बदलो। देखो कि Microstructure कैसे बदलता है।"
                )

                Text(
                    "Experiment B: Input और Output बदलकर देखो कि कणों की संख्या और Connections कैसे प्रभावित होते हैं।"
                )

                Text(
                    "Experiment C: Environment बदलो। देखो कि स्थानीय नियमों के प्रभाव से संरचना में क्या बदलाव आता है।"
                )

                Text(
                    "Experiment D: Connection Range बदलकर देखो कि नई संरचनाएँ कैसे उभरती हैं।"
                )

                Text(
                    "Experiment E: लंबे समय तक चलाकर Snapshots की तुलना करो। क्या Pattern अपनी पहचान बनाए रखता है या धीरे-धीरे बदलता है?"
                )

                Text(
                    "वैज्ञानिक सीमा: यह स्थानीय attraction, repulsion, random fluctuation और boundary exchange पर आधारित एक सरलीकृत मॉडल है। यह वास्तविक जैविक Microstructure की पूर्ण नकल नहीं है।"
                )
            }
        }
    }
}

/* ---------------------------------------------------------
   SMALL UI HELPERS
   --------------------------------------------------------- */

private fun Float.format1(): String =
    String.format(Locale.US, "%.1f", this)

@Composable
private fun LegendDot(
    color: Color,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
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

/* ---------------------------------------------------------
   MICROSTRUCTURE CANVAS
   --------------------------------------------------------- */

@Composable
private fun MicrostructureCanvas(
    state: ResearchState,
    modifier: Modifier = Modifier
) {

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Dark)
    ) {

        val sx = size.width
        val sy = size.height

        val left = sx * 0.05f
        val right = sx * 0.95f
        val top = sy * 0.05f
        val bottom = sy * 0.95f

        // Open System boundary
        drawRoundRect(
            color = Violet.copy(alpha = 0.70f),
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(
                right - left,
                bottom - top
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                15.dp.toPx()
            ),
            style = Stroke(width = 1.5.dp.toPx())
        )

        val particleMap =
            state.particles.associateBy { it.id }

        // Draw current connections
        state.bonds.forEach { bond ->

            val a = particleMap[bond.a]
            val b = particleMap[bond.b]

            if (a != null && b != null) {

                drawLine(
                    color = Cyan.copy(alpha = 0.42f),
                    start = Offset(a.x * sx, a.y * sy),
                    end = Offset(b.x * sx, b.y * sy),
                    strokeWidth = 1.2.dp.toPx()
                )
            }
        }

        // Draw particles
        state.particles.forEach { particle ->

            val point = Offset(
                particle.x * sx,
                particle.y * sy
            )

            val pulse = sin(particle.phase).toFloat()

            val radius = (
                3.3f + pulse * 0.6f
                ).coerceAtLeast(2.5f).dp.toPx()

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

        // Input / output visual indicators
        drawCircle(
            Blue.copy(alpha = 0.85f),
            5.dp.toPx(),
            Offset(left, sy * 0.50f)
        )

        drawCircle(
            Orange.copy(alpha = 0.85f),
            5.dp.toPx(),
            Offset(right, sy * 0.50f)
        )
    }
}
