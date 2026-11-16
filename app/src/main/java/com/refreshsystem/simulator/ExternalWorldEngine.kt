
package com.refreshsystem.simulator

import kotlin.random.Random

/**
 * EXTERNAL WORLD ENGINE
 *
 * Provisional Open System simulation.
 * External Input, Internal Refresh and Natural Decay
 * are treated as three independent processes.
 */

data class WorldEvent(
    val id: Int,
    val contextKey: String,
    val content: String
)

data class ExternalWorldState(
    val cycle: Int = 0,
    val externalInputEnabled: Boolean = true,
    val naturalDecayEnabled: Boolean = false,
    val observedEvents: Int = 0,
    val lastObservedEvent: WorldEvent? = null,
    val eventHistory: List<WorldEvent> = emptyList(),
    val memoryState: InternalMemoryState = InternalMemoryState(),
    val status: String = "External World ready."
)

object ExternalWorldEngine {

    private const val MAX_EVENT_HISTORY = 100

    fun defaultEvents(): List<WorldEvent> {
        return listOf(
            WorldEvent(1, "daily_life", "A person wakes up in the morning."),
            WorldEvent(2, "daily_life", "The person observes sunlight and the surroundings."),
            WorldEvent(3, "daily_life", "The person eats breakfast."),
            WorldEvent(4, "social", "The person meets another person."),
            WorldEvent(5, "social", "A conversation takes place."),
            WorldEvent(6, "environment", "Rain begins outside."),
            WorldEvent(7, "environment", "The temperature becomes cooler."),
            WorldEvent(8, "past_experience", "A familiar place is observed again."),
            WorldEvent(9, "daily_life", "The person travels to another location."),
            WorldEvent(10, "other", "An unexpected sound is heard.")
        )
    }

    fun createInitialState(): ExternalWorldState {
        return ExternalWorldState()
    }

    /**
     * One simulation cycle.
     *
     * 1. External observation
     * 2. Internal Refresh
     * 3. Natural Decay
     *
     * Each process can be independently enabled or disabled.
     */

    fun step(
        state: ExternalWorldState,
        externalInputEnabled: Boolean,
        internalRefreshEnabled: Boolean,
        seed: Int = 42,
        events: List<WorldEvent> = defaultEvents(),
        naturalDecayEnabled: Boolean = false
    ): ExternalWorldState {

        require(events.isNotEmpty()) {
            "External World must contain at least one event."
        }

        val nextCycle = state.cycle + 1

        var memory = state.memoryState
        var lastEvent = state.lastObservedEvent
        var observedCount = state.observedEvents
        var history = state.eventHistory

        // 1. External Observation
        if (externalInputEnabled) {

            val random = Random(seed + nextCycle)

            val event = events[
                random.nextInt(events.size)
            ]

            memory = InternalMemoryEngine.recordInput(
                state = memory,
                input = event.content
            )

            lastEvent = event
            observedCount++

            history = (
                history + event
            ).takeLast(MAX_EVENT_HISTORY)
        }

        // 2. Internal Refresh
        if (internalRefreshEnabled) {

            memory = InternalMemoryEngine.refresh(
                state = memory
            )
        }

        // 3. Natural Decay
        if (naturalDecayEnabled) {

            memory = InternalMemoryEngine.applyNaturalDecay(
                state = memory
            )
        }

        // 4. Simulation Status
        val status = when {
            externalInputEnabled &&
                internalRefreshEnabled &&
                naturalDecayEnabled ->
                "External input, internal refresh and natural decay are active."

            externalInputEnabled &&
                internalRefreshEnabled ->
                "External observation and internal refresh are active."

            externalInputEnabled &&
                naturalDecayEnabled ->
                "External input and natural decay are active."

            externalInputEnabled ->
                "External information is entering. Internal refresh and decay are OFF."

            internalRefreshEnabled &&
                naturalDecayEnabled ->
                "External input is OFF. Internal refresh and natural decay are active."

            internalRefreshEnabled ->
                "External input is OFF. Internal refresh continues."

            naturalDecayEnabled ->
                "External input is OFF. Natural memory decay is active."

            else ->
                "External input, internal refresh and natural decay are all OFF."
        }

        return state.copy(
            cycle = nextCycle,
            externalInputEnabled = externalInputEnabled,
            naturalDecayEnabled = naturalDecayEnabled,
            observedEvents = observedCount,
            lastObservedEvent = lastEvent,
            eventHistory = history,
            memoryState = memory,
            status = status
        )
    }

    /**
     * Retrieve stored information using a context cue.
     */

    fun retrieveMemory(
        state: ExternalWorldState,
        cue: String
    ): List<MemoryTrace> {

        return InternalMemoryEngine.retrieve(
            state = state.memoryState,
            query = cue
        )
    }

    /**
     * Get current memory measurements.
     */

    fun getMemoryMetrics(
        state: ExternalWorldState
    ): InternalMemoryMetrics {

        return InternalMemoryEngine.getMetrics(
            state.memoryState
        )
    }

    /**
     * Reset the complete experiment.
     */

    fun reset(): ExternalWorldState {
        return createInitialState()
    }
}
