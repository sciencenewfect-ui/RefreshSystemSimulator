package com.refreshsystem.simulator

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

data class OpenSystemSnapshot(
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
    val lastUpdateTime: Long
)

class OpenSystemEngine {

    companion object {
        const val PATTERN_SIZE = 16
    }

    private val lock = Any()
    private val pattern = FloatArray(PATTERN_SIZE)
    private val memory = FloatArray(PATTERN_SIZE)
    private val weights = Array(PATTERN_SIZE) { FloatArray(PATTERN_SIZE) }

    private var refreshCount = 0L
    private var informationLevel = 0f
    private var patternChange = 0f
    private var memoryRetention = 0f
    private var dynamicStability = 1f
    private var identityContinuity = 1f
    private var networkStrength = 0f
    private var lastUpdateTime = 0L
    private var externalActive = false
    private var refreshActive = true
    private var decayActive = false

    fun step(
        externalFeatures: List<Float>?,
        internalRefreshEnabled: Boolean,
        naturalDecayEnabled: Boolean
    ): OpenSystemSnapshot = synchronized(lock) {

        val previous = pattern.copyOf()
        val previousMemory = memory.copyOf()

        externalActive = !externalFeatures.isNullOrEmpty()
        refreshActive = internalRefreshEnabled
        decayActive = naturalDecayEnabled

        val input = FloatArray(PATTERN_SIZE)
        if (externalFeatures != null) {
            for (i in 0 until minOf(externalFeatures.size, PATTERN_SIZE)) {
                input[i] = externalFeatures[i].coerceIn(0f, 1f)
            }
        }

        informationLevel = input.average().toFloat()

        if (internalRefreshEnabled) {
            val next = FloatArray(PATTERN_SIZE)

            for (i in 0 until PATTERN_SIZE) {
                var recurrent = 0f
                for (j in 0 until PATTERN_SIZE) {
                    recurrent += weights[i][j] * pattern[j]
                }

                val signal =
                    0.48f * input[i] +
                    0.32f * memory[i] +
                    0.16f * recurrent +
                    0.04f * pattern[(i + 1) % PATTERN_SIZE]

                val target = tanh(signal.toDouble()).toFloat()
                next[i] = (0.72f * pattern[i] + 0.28f * target)
                    .coerceIn(-1f, 1f)
            }

            for (i in 0 until PATTERN_SIZE) {
                pattern[i] = next[i]
                memory[i] = (
                    0.985f * memory[i] +
                    0.12f * (pattern[i] - memory[i])
                ).coerceIn(-1f, 1f)
            }

            updateWeights()
            refreshCount++
        }

        if (naturalDecayEnabled) {
            for (i in 0 until PATTERN_SIZE) {
                memory[i] *= 0.985f
                pattern[i] *= 0.997f
            }
        }

        var change = 0f
        var memoryEnergy = 0f
        var activity = 0f
        var links = 0f

        for (i in 0 until PATTERN_SIZE) {
            change += abs(pattern[i] - previous[i])
            memoryEnergy += memory[i] * memory[i]
            activity += abs(pattern[i])

            for (j in 0 until PATTERN_SIZE) {
                links += abs(weights[i][j])
            }
        }

        patternChange = change / PATTERN_SIZE
        memoryRetention = sqrt(memoryEnergy / PATTERN_SIZE)
            .coerceIn(0f, 1f)

        val averageActivity = activity / PATTERN_SIZE
        val changePenalty = (patternChange / 0.25f).coerceIn(0f, 1f)
        dynamicStability = (
            1f - 0.7f * changePenalty -
            0.3f * abs(0.25f - averageActivity).coerceIn(0f, 1f)
        ).coerceIn(0f, 1f)

        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in 0 until PATTERN_SIZE) {
            dot += previous[i] * pattern[i]
            normA += previous[i] * previous[i]
            normB += pattern[i] * pattern[i]
        }

        identityContinuity = if (normA < 0.000001f || normB < 0.000001f) {
            1f
        } else {
            ((dot / (sqrt(normA) * sqrt(normB)) + 1f) / 2f)
                .coerceIn(0f, 1f)
        }

        networkStrength = (links / (PATTERN_SIZE * PATTERN_SIZE))
            .coerceIn(0f, 1f)

        lastUpdateTime = System.currentTimeMillis()
        createSnapshot()
    }

    private fun updateWeights() {
        for (i in 0 until PATTERN_SIZE) {
            for (j in 0 until PATTERN_SIZE) {
                weights[i][j] = (
                    weights[i][j] +
                    0.008f * pattern[i] * pattern[j] -
                    0.0005f * weights[i][j]
                ).coerceIn(-1f, 1f)
            }
        }
    }

    fun getSnapshot(): OpenSystemSnapshot = synchronized(lock) {
        createSnapshot()
    }

    private fun createSnapshot() = OpenSystemSnapshot(
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
        lastUpdateTime = lastUpdateTime
    )

    fun reset() = synchronized(lock) {
        pattern.fill(0f)
        memory.fill(0f)
        weights.forEach { it.fill(0f) }
        refreshCount = 0L
        informationLevel = 0f
        patternChange = 0f
        memoryRetention = 0f
        dynamicStability = 1f
        identityContinuity = 1f
        networkStrength = 0f
        lastUpdateTime = 0L
        externalActive = false
        refreshActive = true
        decayActive = false
    }
}
