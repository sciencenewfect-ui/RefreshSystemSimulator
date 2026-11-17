
package com.refreshsystem.simulator

import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.math.tanh

data class OpenSystemConfig(
    val patternSize: Int = 16,
    val patternRefreshRate: Float = 0.28f,
    val patternDecayRate: Float = 0.015f,
    val memoryRefreshRate: Float = 0.12f,
    val memoryDecayRate: Float = 0.015f,
    val inputWeight: Float = 0.48f,
    val memoryWeight: Float = 0.32f,
    val networkWeight: Float = 0.16f,
    val learningRate: Float = 0.008f,
    val weightDecay: Float = 0.0005f,

    // Finite energy and matter resources
    val initialEnergy: Float = 100f,
    val initialMatter: Float = 100f,
    val energyInflowPerCycle: Float = 1.2f,
    val matterInflowPerCycle: Float = 0.8f,
    val energyUsePerRefresh: Float = 0.80f,
    val matterUsePerRefresh: Float = 0.25f,
    val energyRecoveryRate: Float = 0.12f,
    val matterRecoveryRate: Float = 0.08f
)

data class OpenSystemSnapshot(
    val cycle: Int,
    val refreshCount: Long,
    val externalInputActive: Boolean,
    val internalRefreshActive: Boolean,
    val naturalDecayActive: Boolean,
    val internalPattern: List<Float>,
    val memoryState: List<Float>,
    val informationLevel: Float,
    val patternChange: Float,
    val memoryRetention: Float,
    val dynamicStability: Float,
    val identityContinuity: Float,
    val networkStrength: Float,
    val lastUpdateTime: Long,

    val energyReserve: Float = 100f,
    val matterReserve: Float = 100f,
    val resourceSupport: Float = 1f,
    val naturalDynamicStability: Float = 1f,
    val internalWorldStability: Float = 0f,
    val chetnaHypothesisIndex: Float = 0f
)

class OpenSystemEngine(
    private val seed: Int = 42,
    val config: OpenSystemConfig = OpenSystemConfig()
) {
    companion object {
        const val PATTERN_SIZE = 16
    }

    private val size = config.patternSize.coerceIn(2, 128)
    private val pattern = FloatArray(size)
    private val memory = FloatArray(size)
    private val weights = Array(size) { FloatArray(size) }

    private var cycle = 0
    private var refreshCount = 0L
    private var informationLevel = 0f
    private var patternChange = 0f
    private var memoryRetention = 0f
    private var dynamicStability = 1f
    private var identityContinuity = 1f
    private var networkStrength = 0f

    private var externalActive = false
    private var refreshActive = true
    private var decayActive = false
    private var lastUpdateTime = 0L

    private var energyReserve =
        config.initialEnergy.coerceIn(0f, 100f)

    private var matterReserve =
        config.initialMatter.coerceIn(0f, 100f)

    private var resourceSupport = 1f
    private var naturalDynamicStability = 1f
    private var internalWorldStability = 0f
    private var chetnaHypothesisIndex = 0f

    init {
        require(config.patternRefreshRate in 0f..1f)
        require(config.patternDecayRate in 0f..1f)
        require(config.memoryRefreshRate in 0f..1f)
        require(config.memoryDecayRate in 0f..1f)
        require(config.learningRate >= 0f)
        require(config.weightDecay in 0f..1f)
        require(config.initialEnergy > 0f)
        require(config.initialMatter > 0f)
        require(config.energyUsePerRefresh >= 0f)
        require(config.matterUsePerRefresh >= 0f)
        require(config.energyInflowPerCycle >= 0f)
        require(config.matterInflowPerCycle >= 0f)
        require(config.energyRecoveryRate in 0f..1f)
        require(config.matterRecoveryRate in 0f..1f)
    }

    @Synchronized
    fun step(
        externalFeatures: List<Float>?,
        internalRefreshEnabled: Boolean,
        naturalDecayEnabled: Boolean
    ): OpenSystemSnapshot {

        val previous = pattern.copyOf()

        externalActive = !externalFeatures.isNullOrEmpty()
        decayActive = naturalDecayEnabled

        val input = FloatArray(size)

        externalFeatures?.take(size)?.forEachIndexed { index, value ->
            input[index] = value.coerceIn(-1f, 1f)
        }

        informationLevel =
            input.map { abs(it) }.average().toFloat()

        // External input replenishes finite resource reserves.
        if (externalActive) {
            energyReserve = (
                energyReserve + config.energyInflowPerCycle
            ).coerceAtMost(config.initialEnergy)

            matterReserve = (
                matterReserve + config.matterInflowPerCycle
            ).coerceAtMost(config.initialMatter)
        }

        val energySupport =
            (energyReserve / config.initialEnergy).coerceIn(0f, 1f)

        val matterSupport =
            (matterReserve / config.initialMatter).coerceIn(0f, 1f)

        resourceSupport = minOf(energySupport, matterSupport)

        // Refresh requires both energy and matter.
        val canRefresh =
            internalRefreshEnabled &&
            energyReserve >= config.energyUsePerRefresh &&
            matterReserve >= config.matterUsePerRefresh &&
            resourceSupport > 0.001f

        refreshActive = canRefresh

        if (canRefresh) {
            val next = FloatArray(size)

            for (i in 0 until size) {
                var recurrent = 0f

                for (j in 0 until size) {
                    recurrent += weights[i][j] * pattern[j]
                }

                val signal =
                    config.inputWeight * input[i] +
                    config.memoryWeight * memory[i] +
                    config.networkWeight * recurrent +
                    0.04f * pattern[(i + 1) % size]

                val target = tanh(signal.toDouble()).toFloat()

                next[i] = (
                    pattern[i] +
                    config.patternRefreshRate *
                    resourceSupport *
                    (target - pattern[i])
                ).coerceIn(-1f, 1f)
            }

            for (i in 0 until size) {
                pattern[i] = next[i]

                memory[i] = (
                    memory[i] +
                    config.memoryRefreshRate *
                    resourceSupport *
                    (pattern[i] - memory[i])
                ).coerceIn(-1f, 1f)
            }

            updateWeights()

            // Partial recovery only; resources are not fully restored.
            energyReserve = (
                energyReserve -
                config.energyUsePerRefresh *
                (1f - config.energyRecoveryRate)
            ).coerceIn(0f, config.initialEnergy)

            matterReserve = (
                matterReserve -
                config.matterUsePerRefresh *
                (1f - config.matterRecoveryRate)
            ).coerceIn(0f, config.initialMatter)

            refreshCount++
        }

        // Natural decay is independent of resource consumption.
        if (naturalDecayEnabled) {
            for (i in 0 until size) {
                pattern[i] *= (1f - config.patternDecayRate)
                memory[i] *= (1f - config.memoryDecayRate)
            }
        }

        var change = 0f
        var memoryEnergy = 0f
        var patternEnergy = 0f
        var dot = 0f
        var previousEnergy = 0f

        for (i in 0 until size) {
            change += abs(pattern[i] - previous[i])
            memoryEnergy += memory[i] * memory[i]
            patternEnergy += pattern[i] * pattern[i]
            dot += previous[i] * pattern[i]
            previousEnergy += previous[i] * previous[i]
        }

        patternChange = change / size

        memoryRetention = sqrt(
            memoryEnergy / size
        ).coerceIn(0f, 1f)

        dynamicStability = (
            1f - (patternChange / 0.25f).coerceIn(0f, 1f)
        ).coerceIn(0f, 1f)

        identityContinuity =
            if (
                previousEnergy < 0.000001f ||
                patternEnergy < 0.000001f
            ) {
                if (
                    previousEnergy < 0.000001f &&
                    patternEnergy < 0.000001f
                ) 1f else 0f
            } else {
                (
                    (dot /
                        (sqrt(previousEnergy) * sqrt(patternEnergy)) + 1f) / 2f
                ).coerceIn(0f, 1f)
            }

        var links = 0f

        for (i in 0 until size) {
            for (j in 0 until size) {
                links += abs(weights[i][j])
            }
        }

        networkStrength = (
            links / (size * size)
        ).coerceIn(0f, 1f)

        // Recalculate resource support after consumption.
        val finalEnergySupport =
            (energyReserve / config.initialEnergy).coerceIn(0f, 1f)

        val finalMatterSupport =
            (matterReserve / config.initialMatter).coerceIn(0f, 1f)

        resourceSupport = minOf(
            finalEnergySupport,
            finalMatterSupport
        )

        // Natural open-system stability is separate from internal stability.
        naturalDynamicStability = (
            resourceSupport *
            (1f - abs(finalEnergySupport - finalMatterSupport))
        ).coerceIn(0f, 1f)

        val patternPresence = sqrt(
            patternEnergy / size
        ).coerceIn(0f, 1f)

        internalWorldStability = (
            0.35f * memoryRetention +
            0.35f * networkStrength +
            0.30f * identityContinuity
        ).coerceIn(0f, 1f) * patternPresence

        chetnaHypothesisIndex = (
            internalWorldStability *
            (
                0.5f * identityContinuity +
                0.5f * networkStrength
            )
        ).coerceIn(0f, 1f)

        cycle++
        lastUpdateTime = System.currentTimeMillis()

        return snapshot()
    }

    private fun updateWeights() {
        for (i in 0 until size) {
            for (j in 0 until size) {
                weights[i][j] = (
                    weights[i][j] +
                    config.learningRate * pattern[i] * pattern[j] -
                    config.weightDecay * weights[i][j]
                ).coerceIn(-1f, 1f)
            }
        }
    }

    @Synchronized
    fun snapshot(): OpenSystemSnapshot = OpenSystemSnapshot(
        cycle = cycle,
        refreshCount = refreshCount,
        externalInputActive = externalActive,
        internalRefreshActive = refreshActive,
        naturalDecayActive = decayActive,
        internalPattern = pattern.toList(),
        memoryState = memory.toList(),
        informationLevel = informationLevel,
        patternChange = patternChange,
        memoryRetention = memoryRetention,
        dynamicStability = dynamicStability,
        identityContinuity = identityContinuity,
        networkStrength = networkStrength,
        lastUpdateTime = lastUpdateTime,
        energyReserve = energyReserve,
        matterReserve = matterReserve,
        resourceSupport = resourceSupport,
        naturalDynamicStability = naturalDynamicStability,
        internalWorldStability = internalWorldStability,
        chetnaHypothesisIndex = chetnaHypothesisIndex
    )

    fun getSnapshot(): OpenSystemSnapshot = snapshot()

    @Synchronized
    fun reset() {
        pattern.fill(0f)
        memory.fill(0f)
        weights.forEach { it.fill(0f) }

        cycle = 0
        refreshCount = 0L
        informationLevel = 0f
        patternChange = 0f
        memoryRetention = 0f
        dynamicStability = 1f
        identityContinuity = 1f
        networkStrength = 0f

        externalActive = false
        refreshActive = true
        decayActive = false
        lastUpdateTime = 0L

        energyReserve = config.initialEnergy.coerceIn(0f, 100f)
        matterReserve = config.initialMatter.coerceIn(0f, 100f)

        resourceSupport = 1f
        naturalDynamicStability = 1f
        internalWorldStability = 0f
        chetnaHypothesisIndex = 0f
    }

    fun currentWeights(): List<List<Float>> =
        weights.map { it.toList() }
}
