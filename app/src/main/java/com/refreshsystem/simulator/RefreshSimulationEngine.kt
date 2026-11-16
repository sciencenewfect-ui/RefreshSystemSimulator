
package com.refreshsystem.simulator

import kotlin.random.Random

/**
 * Refresh System Simulator
 *
 * Pure Kotlin simulation engine.
 *
 * All numerical values are provisional assumptions.
 * They must be tested and calibrated through experiments.
 */

enum class ExperimentCondition(
    val externalInput: Double,
    val internalInput: Double,
    val refreshEnabled: Boolean,
    val label: String
) {
    A_BOTH_ON(
        1.0, 1.0, true,
        "External ON + Internal ON"
    ),

    B_EXTERNAL_OFF(
        0.0, 1.0, true,
        "External OFF + Internal ON"
    ),

    C_BOTH_OFF(
        0.0, 0.0, false,
        "External OFF + Internal OFF"
    ),

    D_EXTERNAL_RESTORED(
        1.0, 1.0, true,
        "External Input Restored"
    )
}

data class MemoryPattern(
    val id: Int,
    val name: String,
    val contextKey: String,
    val strength: Double = 0.70,
    val continuity: Double = 0.70,
    val activationProbability: Double = 0.0,
    val activationCount: Int = 0
)

data class SimulationConfig(
    // Relative contribution of external and internal inputs.
    val externalInputWeight: Double = 0.75,
    val internalInputWeight: Double = 0.25,

    // Global Dynamic Stability parameters.
    val globalRefreshRate: Double = 0.04,
    val globalDecayRate: Double = 0.01,

    // Memory parameters.
    val sharedMemoryRefreshGain: Double = 0.015,
    val memoryDecayRate: Double = 0.008,

    val sharedContinuityGain: Double = 0.012,
    val continuityDecayRate: Double = 0.006,

    // Activation parameters.
    val activationReinforcement: Double = 0.025,
    val randomSelectionWeight: Double = 0.10,
    val contextSelectionWeight: Double = 0.90
)

data class SimulationInput(
    val condition: ExperimentCondition,
    val currentContextKey: String
)

data class SimulationMetrics(
    val step: Int,
    val globalDynamicStability: Double,
    val memoryDynamicStability: Double,
    val averageMemoryStrength: Double,
    val averagePatternContinuity: Double,
    val selectedPatternId: Int?,
    val selectedPatternProbability: Double,
    val externalInput: Double,
    val internalInput: Double,
    val effectiveInputSupport: Double
)

data class SimulationState(
    val step: Int = 0,
    val randomSeed: Int = 42,
    val globalDynamicStability: Double = 0.80,
    val memoryDynamicStability: Double = 0.70,
    val patterns: List<MemoryPattern>,
    val history: List<SimulationMetrics> = emptyList()
)

object RefreshSimulationEngine {

    private const val MAX_HISTORY = 500

    fun createInitialState(
        seed: Int = 42,
        patterns: List<MemoryPattern> = defaultPatterns()
    ): SimulationState {

        require(patterns.isNotEmpty()) {
            "At least one Memory Pattern is required."
        }

        require(patterns.map { it.id }.distinct().size == patterns.size) {
            "Memory Pattern IDs must be unique."
        }

        return SimulationState(
            randomSeed = seed,
            patterns = patterns
        )
    }

    fun defaultPatterns(): List<MemoryPattern> {
        return listOf(
            MemoryPattern(
                id = 1,
                name = "Yesterday's Events",
                contextKey = "daily_life"
            ),
            MemoryPattern(
                id = 2,
                name = "Food and Activities",
                contextKey = "daily_life"
            ),
            MemoryPattern(
                id = 3,
                name = "Old Experience",
                contextKey = "past_experience"
            ),
            MemoryPattern(
                id = 4,
                name = "Places and People",
                contextKey = "social"
            ),
            MemoryPattern(
                id = 5,
                name = "Unrelated Memory",
                contextKey = "other"
            )
        )
    }

    fun step(
        state: SimulationState,
        input: SimulationInput,
        config: SimulationConfig = SimulationConfig()
    ): SimulationState {

        require(state.patterns.isNotEmpty()) {
            "Memory Pattern list cannot be empty."
        }

        require(
            state.patterns.map { it.id }.distinct().size ==
                state.patterns.size
        ) {
            "Memory Pattern IDs must be unique."
        }

        validateConfig(config)

        val condition = input.condition

        // 1. Calculate the combined input support.
        // External and internal inputs belong to the same Refresh process.
        val effectiveInputSupport = (
            condition.externalInput * config.externalInputWeight +
                condition.internalInput * config.internalInputWeight
            ).coerceIn(0.0, 1.0)

        // 2. Update Global Dynamic Stability.
        // Internal-only support is weaker in this provisional model.
        val stabilityChange = if (condition.refreshEnabled) {
            config.globalRefreshRate *
                effectiveInputSupport *
                (1.0 - state.globalDynamicStability) -
                config.globalDecayRate *
                (1.0 - effectiveInputSupport)
        } else {
            -config.globalDecayRate
        }

        val newGlobalStability = (
            state.globalDynamicStability + stabilityChange
            ).coerceIn(0.0, 1.0)

        // 3. Calculate activation probabilities.
        val probabilities = calculateProbabilities(
            patterns = state.patterns,
            currentContextKey = input.currentContextKey,
            config = config,
            random = Random(state.randomSeed + state.step)
        )

        // 4. Select one Memory Pattern probabilistically.
        val selectedId = selectPattern(
            patterns = state.patterns,
            probabilities = probabilities,
            random = Random(state.randomSeed + state.step + 1000)
        )

        // 5. All patterns participate in shared Refresh.
        // Selection adds an additional activation effect to one pattern.
        val sharedRefresh = if (condition.refreshEnabled) {
            effectiveInputSupport * newGlobalStability
        } else {
            0.0
        }

        val updatedPatterns = state.patterns.map { pattern ->

            val selected = pattern.id == selectedId

            val activationGain = if (selected) {
                config.activationReinforcement
            } else {
                0.0
            }

            val strengthChange =
                config.sharedMemoryRefreshGain * sharedRefresh -
                    config.memoryDecayRate * (1.0 - sharedRefresh) +
                    activationGain

            val continuityChange =
                config.sharedContinuityGain * sharedRefresh -
                    config.continuityDecayRate * (1.0 - sharedRefresh)

            pattern.copy(
                strength = (
                    pattern.strength + strengthChange
                    ).coerceIn(0.0, 1.0),

                continuity = (
                    pattern.continuity + continuityChange
                    ).coerceIn(0.0, 1.0),

                activationProbability =
                    probabilities[pattern.id] ?: 0.0,

                activationCount =
                    pattern.activationCount +
                        if (selected) 1 else 0
            )
        }

        // 6. Provisional collective Memory Dynamic Stability measure.
        val averageContinuity =
            updatedPatterns.map { it.continuity }.average()

        val newMemoryStability =
            averageContinuity.coerceIn(0.0, 1.0)

        val selectedProbability =
            probabilities[selectedId] ?: 0.0

        // 7. Save experiment measurements.
        val metrics = SimulationMetrics(
            step = state.step + 1,
            globalDynamicStability = newGlobalStability,
            memoryDynamicStability = newMemoryStability,
            averageMemoryStrength =
                updatedPatterns.map { it.strength }.average(),
            averagePatternContinuity = averageContinuity,
            selectedPatternId = selectedId,
            selectedPatternProbability = selectedProbability,
            externalInput = condition.externalInput,
            internalInput = condition.internalInput,
            effectiveInputSupport = effectiveInputSupport
        )

        return state.copy(
            step = state.step + 1,
            globalDynamicStability = newGlobalStability,
            memoryDynamicStability = newMemoryStability,
            patterns = updatedPatterns,
            history = (state.history + metrics).takeLast(MAX_HISTORY)
        )
    }

    private fun calculateProbabilities(
        patterns: List<MemoryPattern>,
        currentContextKey: String,
        config: SimulationConfig,
        random: Random
    ): Map<Int, Double> {

        val weights = patterns.associate { pattern ->

            val contextMatch =
                if (pattern.contextKey == currentContextKey) {
                    1.0
                } else {
                    0.0
                }

            val randomComponent =
                random.nextDouble() *
                    config.randomSelectionWeight

            val contextComponent =
                contextMatch *
                    config.contextSelectionWeight *
                    pattern.strength

            pattern.id to (
                randomComponent + contextComponent
                ).coerceAtLeast(0.000001)
        }

        val totalWeight = weights.values.sum()

        return weights.mapValues { (_, weight) ->
            weight / totalWeight
        }
    }

    private fun selectPattern(
        patterns: List<MemoryPattern>,
        probabilities: Map<Int, Double>,
        random: Random
    ): Int {

        val draw = random.nextDouble()
        var cumulative = 0.0

        for (pattern in patterns) {
            cumulative += probabilities[pattern.id] ?: 0.0

            if (draw <= cumulative) {
                return pattern.id
            }
        }

        return patterns.last().id
    }

    private fun validateConfig(config: SimulationConfig) {

        require(config.externalInputWeight in 0.0..1.0)
        require(config.internalInputWeight in 0.0..1.0)

        require(
            config.externalInputWeight +
                config.internalInputWeight > 0.0
        ) {
            "At least one input weight must be positive."
        }

        require(config.globalRefreshRate in 0.0..1.0)
        require(config.globalDecayRate in 0.0..1.0)

        require(config.sharedMemoryRefreshGain in 0.0..1.0)
        require(config.memoryDecayRate in 0.0..1.0)

        require(config.sharedContinuityGain in 0.0..1.0)
        require(config.continuityDecayRate in 0.0..1.0)

        require(config.activationReinforcement in 0.0..1.0)
        require(config.randomSelectionWeight >= 0.0)
        require(config.contextSelectionWeight >= 0.0)
    }
}
