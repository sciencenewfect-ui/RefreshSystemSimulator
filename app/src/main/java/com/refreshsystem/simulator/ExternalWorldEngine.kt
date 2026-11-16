
package com.refreshsystem.simulator

import kotlin.random.Random

/**
 * EXTERNAL WORLD ENGINE
 *
 * Creates a repeatable simulated environment.
 * The Open System can observe events when external input is ON.
 *
 * This is a provisional research model, not a model of the
 * full complexity of the real human environment.
 */

data class WorldEvent(
    val id: Int,
    val contextKey: String,
    val content: String
)

data class ExternalWorldState(
    val cycle: Int = 0,
    val externalInputEnabled: Boolean = true,
    val observedEvents: Int = 0,
    val lastObservedEvent: WorldEvent? = null,
    val eventHistory: List<WorldEvent> = emptyList(),
    val memoryState: InternalMemoryState = InternalMemoryState(),
    val status: String = "External World ready."
)

object ExternalWorldEngine {

    private const val MAX_EVENT_HISTORY = 100

    /**
     * Simulated events from the environment.
     * Related events share context keys.
     */
    fun defaultEvents(): List<WorldEvent> {
        return listOf(
            WorldEvent(
                1,
                "daily_life",
                "A person wakes up in the morning."
            ),
            WorldEvent(
                2,
                "daily_life",
                "The person observes sunlight and the surroundings."
            ),
            WorldEvent(
                3,
                "daily_life",
                "The person eats breakfast."
            ),
            WorldEvent(
                4,
                "social",
                "The person meets another person."
            ),
            WorldEvent(
                5,
                "social",
                "A conversation takes place."
            ),
            WorldEvent(
                6,
                "environment",
                "Rain begins outside."
            ),
            WorldEvent(
                7,
                "environment",
                "The temperature becomes cooler."
            ),
            WorldEvent(
                8,
                "past_experience",
                "A familiar place is observed again."
            ),
            WorldEvent(
                9,
                "daily_life",
                "The person travels to another location."
            ),
            WorldEvent(
                10,
                "other",
                "An unexpected sound is heard."
            )
        )
    }

    fun createInitialState(): ExternalWorldState {
        return ExternalWorldState()
    }

    /**
     * One simulation cycle.
     *
     * External ON:
     *   A world event enters the system as information.
     *
     * External OFF:
     *   No new external event enters the system.
     *
     * Internal Refresh ON:
     *   Existing internal records pass through the
     *   provisional memory refresh process.
     *
     * Internal Refresh OFF:
     *   No memory refresh is performed.
     */
    fun step(
        state: ExternalWorldState,
        externalInputEnabled: Boolean,
        internalRefreshEnabled: Boolean,
        seed: Int = 42,
        events: List<WorldEvent> = defaultEvents()
    ): ExternalWorldState {

        require(events.isNotEmpty()) {
            "External World must contain at least one event."
        }

        val nextCycle = state.cycle + 1

        var memory = state.memoryState
        var lastEvent = state.lastObservedEvent
        var observedCount = state.observedEvents
        var history = state.eventHistory

        // 1. External observation
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

        // 2. Internal memory refresh
        if (internalRefreshEnabled) {

            memory = InternalMemoryEngine.refresh(
                state = memory
            )
        }

        // 3. Status
        val status = when {
            externalInputEnabled && internalRefreshEnabled ->
                "External observation and internal refresh are active."

            externalInputEnabled && !internalRefreshEnabled ->
                "External information is entering. Internal refresh is OFF."

            !externalInputEnabled && internalRefreshEnabled ->
                "External input is OFF. Internal refresh continues."

            else ->
                "External input and internal refresh are both OFF."
        }

        return state.copy(
            cycle = nextCycle,
            externalInputEnabled = externalInputEnabled,
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
     * Reset the complete external-world experiment.
     */
    fun reset(): ExternalWorldState {
        return createInitialState()
    }
}
