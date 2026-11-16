
package com.refreshsystem.simulator

import kotlin.random.Random

/**
 * Refresh System Simulator
 *
 * Core simulation engine.
 * Pure Kotlin: no Android UI dependencies.
 *
 * IMPORTANT:
 * The numerical update rules below are provisional simulation
 * assumptions. They are not established scientific laws.
 */

enum class ExperimentCondition(
    val externalInput: Double,
    val internalInput: Double,
    val refreshEnabled: Boolean,
    val label: String
) {
    A_BOTH_ON(
        externalInput = 1.0,
        internalInput = 1.0,
        refreshEnabled = true,
        label = "External ON + Internal ON"
    ),

    B_EXTERNAL_OFF(
        externalInput = 0.0,
        internalInput = 1.0,
        refreshEnabled = true,
        label = "External OFF + Internal ON"
    ),

    C_BOTH_OFF(
        externalInput = 0.0,
        internalInput = 0.0,
        refreshEnabled = false,
        label = "External OFF + Internal OFF"
    ),

    D_EXTERNAL_RESTORED(
        externalInput = 1.0,
        internalInput = 1.0,
        refreshEnabled = true,
        label = "External Input Restored"
    )
}

data class MemoryPattern(
    val id: Int,
    val name: String,
    val contextKey: String,

    // Provisional measurable properties: range 0.0 to 1.0
    val strength: Double = 0.70,
    val continuity: Double = 0.70,

    // Probability assigned during the latest selection cycle
    val activationProbability: Double = 0.0,

    val activationCount: Int = 0
)

data class SimulationConfig(
    // Provisional parameters; calibration requires experiments.
    val globalRefreshRate: Double = 0.04,
    val globalDecayRate: Double = 0.01,

    val sharedMemoryRefreshGain: Double = 0.015,
    val memoryDecayRate: Double = 0.008,

    val sharedContinuityGain: Double = 0.012,
    val continuityDecayRate: Double = 0.006,

    val activationReinforcement: Double = 0.025,

    // Gives unrelated memories a non-zero random selection chance.
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
    val selectedPatternProbability: Double
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

    fun createInitialState(
        seed: Int = 42,
        patterns: List<MemoryPattern> = defaultPatterns()
    ): SimulationState {
        require(patterns.isNotEmpty()) {
            "At least one Memory Pattern is required."
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

        validateConfig(config)

        val condition = input.condition

        // 1. Calculate input support.
        val inputSupport = maxOf(
            condition.externalInput,
            condition.internalInput
        )

        // 2. Update Global Dynamic Stability.
        val newGlobalStability =
            if (condition.refreshEnabled) {
                state.globalDynamicStability +
                    config.globalRefreshRate *
                    inputSupport *
                    (1.0 - state.globalDynamicStability)
            } else {
                state.globalDynamicStability -
                    config.globalDecayRate
            }.coerceIn(0.0, 1.0)

        // 3. Calculate Memory Pattern selection probabilities.
        val probabilities = calculateProbabilities(
            patterns = state.patterns,
            currentContextKey = input.currentContextKey,
            config = config,
            random = Random(state.randomSeed + state.step)
        )

        // 4. Select one pattern probabilistically.
        val selectedId = selectPattern(
            patterns = state.patterns,
            probabilities = probabilities,
            random = Random(state.randomSeed + state.step + 1000)
        )

        // 5. Update ALL Memory Patterns through shared refresh.
        val updatedPatterns = state.patterns.map { pattern ->

            val selected = pattern.id == selectedId

            val sharedRefresh =
                if (condition.refreshEnabled) {
                    newGlobalStability
                } else {
                    0.0
                }

            val strengthChange =
                config.sharedMemoryRefreshGain * sharedRefresh -
                    config.memoryDecayRate * (1.0 - sharedRefresh) +
                    if (selected) {
                        config.activationReinforcement
                    } else {
                        0.0
                    }

            val continuityChange =
                config.sharedContinuityGain * sharedRefresh -
                    config.continuityDecayRate * (1.0 - sharedRefresh)

            val newStrength =
                (pattern.strength + strengthChange)
                    .coerceIn(0.0, 1.0)

            val newContinuity =
                (pattern.continuity + continuityChange)
                    .coerceIn(0.0, 1.0)

            pattern.copy(
                strength = newStrength,
                continuity = newContinuity,
                activationProbability =
                    probabilities[pattern.id] ?: 0.0,
                activationCount =
                    pattern.activationCount +
                        if (selected) 1 else 0
            )
        }

        // 6. Estimate collective Memory Dynamic Stability.
        val averageContinuity =
            updatedPatterns.map { it.continuity }.average()

        val newMemoryStability = averageContinuity
            .coerceIn(0.0, 1.0)

        // 7. Record measurable results.
        val selectedProbability =
            probabilities[selectedId] ?: 0.0

        val metrics = SimulationMetrics(
            step = state.step + 1,
            globalDynamicStability = newGlobalStability,
            memoryDynamicStability = newMemoryStability,
            averageMemoryStrength =
                updatedPatterns.map { it.strength }.average(),
            averagePatternContinuity = averageContinuity,
            selectedPatternId = selectedId,
            selectedPatternProbability = selectedProbability
        )

        return state.copy(
            step = state.step + 1,
            globalDynamicStability = newGlobalStability,
            memoryDynamicStability = newMemoryStability,
            patterns = updatedPatterns,
            history = state.history + metrics
        )
    }

    private fun calculateProbabilities(
        patterns: List<MemoryPattern>,
        currentContextKey: String,
        config: SimulationConfig,
        random: Random
    ): Map<Int, Double> {

        // Randomness is reproducible through the supplied seed.
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

            pattern.id to
                (randomComponent + contextComponent)
                    .coerceAtLeast(0.000001)
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
