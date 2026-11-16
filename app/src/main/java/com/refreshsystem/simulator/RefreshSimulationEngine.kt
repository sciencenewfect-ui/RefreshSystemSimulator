
package com.refreshsystem.simulator

import kotlin.random.Random

/**
 * Refresh System Simulator
 *
 * Core simulation engine.
 * Pure Kotlin: no Android UI dependencies.
 *
 * IMPORTANT:
 * All numerical rates are provisional simulation assumptions.
 * They are adjustable parameters, not established scientific laws.
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
    val strength: Double = 0.70,
    val continuity: Double = 0.70,
    val activationProbability: Double = 0.0,
    val activationCount: Int = 0
)

data class SimulationConfig(
    // Global Dynamic Stability parameters
    val globalRefreshRate: Double = 0.04,
    val globalDecayRate: Double = 0.01,

    // Internal input has a weaker maintenance effect
    val internalMaintenanceRate: Double = 0.02,

    // Shared Memory Pattern parameters
    val sharedMemoryRefreshGain: Double = 0.015,
    val memoryDecayRate: Double = 0.008,

    val sharedContinuityGain: Double = 0.012,
    val continuityDecayRate: Double = 0.006,

    // Activation effect
    val activationReinforcement: Double = 0.025,

    // Memory selection parameters
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

        validatePatterns(patterns)

        val averageContinuity =
            patterns.map { it.continuity }.average()

        return SimulationState(
            randomSeed = seed,
            globalDynamicStability = 0.80,
            memoryDynamicStability = averageContinuity,
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

        validateState(state)
        validateConfig(config)

        require(input.currentContextKey.isNotBlank()) {
            "Current context key cannot be empty."
        }

        val condition = input.condition

        // 1. Read external and internal inputs separately.
        val externalInput = condition.externalInput
        val internalInput = condition.internalInput

        // 2. Calculate the Global Dynamic Stability change.
        //
        // External input provides the main refresh contribution.
        // Internal input provides weaker maintenance.
        // When external input is absent, gradual decay continues.

        val externalContribution =
            externalInput *
                config.globalRefreshRate *
                (1.0 - state.globalDynamicStability)

        val internalContribution =
            internalInput *
                config.internalMaintenanceRate *
                (1.0 - state.globalDynamicStability)

        val decay =
            if (externalInput <= 0.0) {
                config.globalDecayRate
            } else {
                0.0
            }

        val stabilityChange =
            if (condition.refreshEnabled) {
                externalContribution +
                    internalContribution -
                    decay
            } else {
                -config.globalDecayRate
            }

        val newGlobalStability = (
            state.globalDynamicStability + stabilityChange
        ).coerceIn(0.0, 1.0)

        // 3. Calculate the shared refresh level.
        //
        // Every memory pattern receives shared refresh exposure.
        // Internal input contributes less than external input.

        val sharedInputSupport = (
            externalInput +
                internalInput *
                config.internalMaintenanceRate /
                config.globalRefreshRate
        ).coerceIn(0.0, 1.0)

        val sharedRefresh =
            if (condition.refreshEnabled) {
                newGlobalStability * sharedInputSupport
            } else {
                0.0
            }

        // 4. Calculate memory activation probabilities.

        val probabilities = calculateProbabilities(
            patterns = state.patterns,
            currentContextKey = input.currentContextKey,
            config = config,
            random = Random(
                state.randomSeed.toLong() + state.step.toLong()
            )
        )

        // 5. Select one memory pattern probabilistically.

        val selectedId = selectPattern(
            patterns = state.patterns,
            probabilities = probabilities,
            random = Random(
                state.randomSeed.toLong() +
                    state.step.toLong() +
                    1000L
            )
        )

        // 6. Update ALL Memory Patterns.
        //
        // Shared refresh affects every pattern.
        // The selected pattern receives additional reinforcement.

        val updatedPatterns = state.patterns.map { pattern ->

            val selected = pattern.id == selectedId

            val activationGain =
                if (selected) {
                    config.activationReinforcement
                } else {
                    0.0
                }

            val strengthChange =
                config.sharedMemoryRefreshGain * sharedRefresh -
                    config.memoryDecayRate *
                    (1.0 - sharedRefresh) +
                    activationGain

            val continuityChange =
                config.sharedContinuityGain * sharedRefresh -
                    config.continuityDecayRate *
                    (1.0 - sharedRefresh)

            val newStrength = (
                pattern.strength + strengthChange
            ).coerceIn(0.0, 1.0)

            val newContinuity = (
                pattern.continuity + continuityChange
            ).coerceIn(0.0, 1.0)

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

        // 7. Estimate collective Memory Dynamic Stability.
        //
        // This is a provisional measurement:
        // average continuity of all memory patterns.

        val averageContinuity =
            updatedPatterns.map { it.continuity }.average()

        val newMemoryStability =
            averageContinuity.coerceIn(0.0, 1.0)

        // 8. Record experiment measurements.

        val selectedProbability =
            probabilities[selectedId] ?: 0.0

        val metrics = SimulationMetrics(
            step = state.step + 1,

            globalDynamicStability =
                newGlobalStability,

            memoryDynamicStability =
                newMemoryStability,

            averageMemoryStrength =
                updatedPatterns.map { it.strength }.average(),

            averagePatternContinuity =
                averageContinuity,

            selectedPatternId =
                selectedId,

            selectedPatternProbability =
                selectedProbability
        )

        // 9. Return the updated state.
        // Previous state remains unchanged.

        return state.copy(
            step = state.step + 1,

            globalDynamicStability =
                newGlobalStability,

            memoryDynamicStability =
                newMemoryStability,

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

            val totalWeight =
                randomComponent + contextComponent

            pattern.id to totalWeight.coerceAtLeast(0.000001)
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

            cumulative +=
                probabilities[pattern.id] ?: 0.0

            if (draw < cumulative) {
                return pattern.id
            }
        }

        return patterns.last().id
    }

    private fun validatePatterns(
        patterns: List<MemoryPattern>
    ) {
        require(patterns.isNotEmpty()) {
            "At least one Memory Pattern is required."
        }

        require(patterns.map { it.id }.distinct().size == patterns.size) {
            "Memory Pattern IDs must be unique."
        }

        patterns.forEach { pattern ->

            require(pattern.name.isNotBlank()) {
                "Memory Pattern name cannot be empty."
            }

            require(pattern.contextKey.isNotBlank()) {
                "Memory Pattern context cannot be empty."
            }

            require(pattern.strength in 0.0..1.0)
            require(pattern.continuity in 0.0..1.0)
            require(pattern.activationProbability in 0.0..1.0)
            require(pattern.activationCount >= 0)
        }
    }

    private fun validateState(state: SimulationState) {

        require(state.step >= 0) {
            "Simulation step cannot be negative."
        }

        require(state.globalDynamicStability in 0.0..1.0) {
            "Global Dynamic Stability must be between 0 and 1."
        }

        require(state.memoryDynamicStability in 0.0..1.0) {
            "Memory Dynamic Stability must be between 0 and 1."
        }

        validatePatterns(state.patterns)
    }

    private fun validateConfig(config: SimulationConfig) {

        require(config.globalRefreshRate in 0.0..1.0)
        require(config.globalDecayRate in 0.0..1.0)

        require(config.internalMaintenanceRate in 0.0..1.0)

        require(config.sharedMemoryRefreshGain in 0.0..1.0)
        require(config.memoryDecayRate in 0.0..1.0)

        require(config.sharedContinuityGain in 0.0..1.0)
        require(config.continuityDecayRate in 0.0..1.0)

        require(config.activationReinforcement in 0.0..1.0)

        require(config.randomSelectionWeight >= 0.0)
        require(config.contextSelectionWeight >= 0.0)
    }
}
