
package com.refreshsystem.simulator

import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.math.tanh

data class OpenSystemSnapshot(
    val refreshCount: Long,
    val visualInputActive: Boolean,
    val audioInputActive: Boolean,
    val internalPattern: List<Float>,
    val memoryState: List<Float>,
    val selfModel: List<Float>,
    val informationLevel: Float,
    val patternChange: Float,
    val memoryRetention: Float,
    val dynamicStability: Float,
    val identityContinuity: Float,
    val homeostasis: Float,
    val lastUpdateTime: Long
)

class OpenSystemEngine {

    companion object {
        const val VERSION = "1.0"
        const val PATTERN_SIZE = 16

        private const val MEMORY_RETENTION = 0.96f
        private const val MEMORY_LEARNING_RATE = 0.12f
        private const val NETWORK_LEARNING_RATE = 0.008f
        private const val WEIGHT_DECAY = 0.002f
        private const val REFRESH_RATE = 0.35f
        private const val TARGET_ACTIVITY = 0.35f
    }

    private val lock = Any()

    private val visualInput = FloatArray(PATTERN_SIZE)
    private val audioInput = FloatArray(PATTERN_SIZE)
    private val pattern = FloatArray(PATTERN_SIZE)
    private val memory = FloatArray(PATTERN_SIZE)
    private val selfModel = FloatArray(PATTERN_SIZE)

    private val weights =
        Array(PATTERN_SIZE) { FloatArray(PATTERN_SIZE) }

    private var visualActive = false
    private var audioActive = false
    private var refreshCount = 0L
    private var lastUpdateTime = 0L

    private var informationLevel = 0f
    private var patternChange = 0f
    private var memoryRetention = 0f
    private var dynamicStability = 1f
    private var identityContinuity = 1f
    private var homeostasis = 0.5f
    private var hasPreviousSelfModel = false

    fun submitVisualFeatures(features: List<Float>) {
        synchronized(lock) {
            copyFeatures(features, visualInput)
            visualActive = features.isNotEmpty()
        }
    }

    fun submitAudioFeatures(features: List<Float>) {
        synchronized(lock) {
            copyFeatures(features, audioInput)
            audioActive = features.isNotEmpty()
        }
    }

    fun clearVisualInput() {
        synchronized(lock) {
            visualInput.fill(0f)
            visualActive = false
        }
    }

    fun clearAudioInput() {
        synchronized(lock) {
            audioInput.fill(0f)
            audioActive = false
        }
    }

    fun refresh(): OpenSystemSnapshot {
        synchronized(lock) {
            val previousPattern = pattern.copyOf()
            val previousSelfModel = selfModel.copyOf()
            val externalInput = FloatArray(PATTERN_SIZE)

            val activeChannels =
                (if (visualActive) 1 else 0) +
                (if (audioActive) 1 else 0)

            for (i in 0 until PATTERN_SIZE) {
                externalInput[i] = when (activeChannels) {
                    0 -> 0f
                    1 -> (visualInput[i] + audioInput[i])
                        .coerceIn(0f, 1f)
                    else -> ((visualInput[i] + audioInput[i]) / 2f)
                        .coerceIn(0f, 1f)
                }
            }

            informationLevel = externalInput.average()
                .toFloat()
                .coerceIn(0f, 1f)

            val nextPattern = FloatArray(PATTERN_SIZE)

            for (i in 0 until PATTERN_SIZE) {
                var recurrentSignal = 0f

                for (j in 0 until PATTERN_SIZE) {
                    recurrentSignal += weights[i][j] * pattern[j]
                }

                val combinedSignal =
                    0.50f * externalInput[i] +
                    0.30f * memory[i] +
                    0.15f * recurrentSignal +
                    0.05f * selfModel[i] +
                    0.10f * (homeostasis - 0.5f)

                val target = tanh(combinedSignal.toDouble()).toFloat()

                nextPattern[i] = (
                    (1f - REFRESH_RATE) * pattern[i] +
                    REFRESH_RATE * target
                ).coerceIn(-1f, 1f)
            }

            for (i in 0 until PATTERN_SIZE) {
                pattern[i] = nextPattern[i]
            }

            var totalChange = 0f
            for (i in 0 until PATTERN_SIZE) {
                totalChange += abs(pattern[i] - previousPattern[i])
            }

            patternChange =
                (totalChange / PATTERN_SIZE).coerceIn(0f, 2f)

            for (i in 0 until PATTERN_SIZE) {
                memory[i] = (
                    MEMORY_RETENTION * memory[i] +
                    MEMORY_LEARNING_RATE * (pattern[i] - memory[i])
                ).coerceIn(-1f, 1f)
            }

            updateNetworkWeights()

            var activity = 0f
            for (value in pattern) {
                activity += abs(value)
            }
            activity /= PATTERN_SIZE

            homeostasis = (
                homeostasis +
                0.03f * (TARGET_ACTIVITY - activity)
            ).coerceIn(0f, 1f)

            for (i in 0 until PATTERN_SIZE) {
                selfModel[i] = (
                    0.90f * selfModel[i] +
                    0.10f * pattern[i]
                ).coerceIn(-1f, 1f)
            }

            var memoryMagnitude = 0f
            for (value in memory) {
                memoryMagnitude += value * value
            }

            memoryRetention =
                sqrt(memoryMagnitude / PATTERN_SIZE).coerceIn(0f, 1f)

            val changePenalty = (patternChange / 0.5f).coerceIn(0f, 1f)
            val homeostasisPenalty =
                abs(TARGET_ACTIVITY - activity).coerceIn(0f, 1f)

            dynamicStability = (
                1f - 0.65f * changePenalty -
                0.35f * homeostasisPenalty
            ).coerceIn(0f, 1f)

            identityContinuity = if (hasPreviousSelfModel) {
                cosineSimilarity(previousSelfModel, selfModel)
            } else {
                1f
            }

            hasPreviousSelfModel = true
            refreshCount++
            lastUpdateTime = System.currentTimeMillis()

            return createSnapshot()
        }
    }

    private fun updateNetworkWeights() {
        for (i in 0 until PATTERN_SIZE) {
            for (j in 0 until PATTERN_SIZE) {
                val learningChange = NETWORK_LEARNING_RATE * (
                    pattern[i] * pattern[j] -
                    WEIGHT_DECAY * weights[i][j]
                )

                weights[i][j] = (
                    weights[i][j] + learningChange
                ).coerceIn(-1f, 1f)
            }
        }
    }

    private fun cosineSimilarity(
        first: FloatArray,
        second: FloatArray
    ): Float {
        var dot = 0f
        var firstNorm = 0f
        var secondNorm = 0f

        for (i in first.indices) {
            dot += first[i] * second[i]
            firstNorm += first[i] * first[i]
            secondNorm += second[i] * second[i]
        }

        if (firstNorm < 0.000001f || secondNorm < 0.000001f) {
            return 1f
        }

        val similarity =
            dot / (sqrt(firstNorm) * sqrt(secondNorm))

        return ((similarity + 1f) / 2f).coerceIn(0f, 1f)
    }

    private fun copyFeatures(
        source: List<Float>,
        destination: FloatArray
    ) {
        destination.fill(0f)
        val count = minOf(source.size, PATTERN_SIZE)

        for (i in 0 until count) {
            destination[i] = source[i].coerceIn(0f, 1f)
        }
    }

    fun getSnapshot(): OpenSystemSnapshot {
        synchronized(lock) {
            return createSnapshot()
        }
    }

    private fun createSnapshot() = OpenSystemSnapshot(
        refreshCount = refreshCount,
        visualInputActive = visualActive,
        audioInputActive = audioActive,
        internalPattern = pattern.toList(),
        memoryState = memory.toList(),
        selfModel = selfModel.toList(),
        informationLevel = informationLevel,
        patternChange = patternChange,
        memoryRetention = memoryRetention,
        dynamicStability = dynamicStability,
        identityContinuity = identityContinuity,
        homeostasis = homeostasis,
        lastUpdateTime = lastUpdateTime
    )

    fun reset() {
        synchronized(lock) {
            visualInput.fill(0f)
            audioInput.fill(0f)
            pattern.fill(0f)
            memory.fill(0f)
            selfModel.fill(0f)

            for (row in weights) {
                row.fill(0f)
            }

            visualActive = false
            audioActive = false
            refreshCount = 0L
            lastUpdateTime = 0L
            informationLevel = 0f
            patternChange = 0f
            memoryRetention = 0f
            dynamicStability = 1f
            identityContinuity = 1f
            homeostasis = 0.5f
            hasPreviousSelfModel = false
        }
    }
}
