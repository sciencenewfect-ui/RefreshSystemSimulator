
package com.refreshsystem.simulator

import java.util.Locale

/* =========================================================
   REFRESH SYSTEM
   INTERNAL MEMORY & CONTINUOUS REFRESH ENGINE
   ========================================================= */

/*
 * यह Engine सिस्टम की आंतरिक जानकारी को रिकॉर्ड करता है।
 * यह Android Gallery या Photo Storage का उपयोग नहीं करता।
 *
 * यह एक प्रारंभिक computational research model है।
 */

/* -------------------------
   1. INTERNAL DATA STRUCTURE
   ------------------------- */

data class MemoryTrace(
    val id: Long,
    val content: String,
    val tokens: List<String>,
    val strength: Float = 1f,
    val createdAtCycle: Int,
    val lastUpdatedCycle: Int,
    val refreshCount: Int = 0,
    val sourceIds: List<Long> = emptyList()
)

data class PatternSnapshot(
    val cycle: Int,
    val traceIds: List<Long>,
    val tokenSet: Set<String>,
    val patternVersion: Int
)

data class InformationTransfer(
    val cycle: Int,
    val sourceTraceIds: List<Long>,
    val targetTraceId: Long,
    val transferredTokens: List<String>
)

data class RefreshEvent(
    val cycle: Int,
    val addedRecords: Int,
    val updatedRecords: Int,
    val transferredItems: Int,
    val totalRecords: Int
)

data class InternalMemoryState(
    val cycle: Int = 0,
    val records: List<MemoryTrace> = emptyList(),
    val patterns: List<PatternSnapshot> = emptyList(),
    val transfers: List<InformationTransfer> = emptyList(),
    val events: List<RefreshEvent> = emptyList(),
    val nextId: Long = 1L,
    val totalInputs: Int = 0,
    val totalRefreshes: Int = 0
)

/* -------------------------
   2. ENGINE CONFIGURATION
   ------------------------- */

data class RefreshEngineConfig(
    val retentionRate: Float = 0.96f,
    val refreshStrength: Float = 0.30f,
    val similarityThreshold: Float = 0.25f,
    val maxRecords: Int = 500
) {
    fun validated(): RefreshEngineConfig {
        return copy(
            retentionRate = retentionRate.coerceIn(0f, 1f),
            refreshStrength = refreshStrength.coerceIn(0f, 1f),
            similarityThreshold = similarityThreshold.coerceIn(0f, 1f),
            maxRecords = maxRecords.coerceIn(10, 5000)
        )
    }
}

/* -------------------------
   3. TEXT INFORMATION ENCODER
   ------------------------- */

object InternalMemoryEncoder {

    fun encode(input: String): List<String> {

        return input
            .lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .distinct()
    }

    fun similarity(
        first: List<String>,
        second: List<String>
    ): Float {

        val a = first.toSet()
        val b = second.toSet()

        if (a.isEmpty() || b.isEmpty()) return 0f

        val intersection = a.intersect(b).size
        val union = a.union(b).size

        if (union == 0) return 0f

        return intersection.toFloat() / union.toFloat()
    }
}

/* -------------------------
   4. INPUT RECORDING
   ------------------------- */

object InternalMemoryEngine {

    fun recordInput(
        state: InternalMemoryState,
        input: String
    ): InternalMemoryState {

        val content = input.trim()

        if (content.isBlank()) return state

        val tokens = InternalMemoryEncoder.encode(content)

        if (tokens.isEmpty()) return state

        val nextCycle = state.cycle + 1
        val newId = state.nextId

        val trace = MemoryTrace(
            id = newId,
            content = content,
            tokens = tokens,
            strength = 1f,
            createdAtCycle = nextCycle,
            lastUpdatedCycle = nextCycle
        )

        val updatedRecords =
            (state.records + trace).takeLast(5000)

        return state.copy(
            cycle = nextCycle,
            records = updatedRecords,
            nextId = newId + 1,
            totalInputs = state.totalInputs + 1
        )
    }

    /* -------------------------
       5. CONTINUOUS REFRESH
       ------------------------- */

    fun refresh(
        state: InternalMemoryState,
        config: RefreshEngineConfig = RefreshEngineConfig()
    ): InternalMemoryState {

        val settings = config.validated()
        val nextCycle = state.cycle + 1

        if (state.records.isEmpty()) {

            val event = RefreshEvent(
                cycle = nextCycle,
                addedRecords = 0,
                updatedRecords = 0,
                transferredItems = 0,
                totalRecords = 0
            )

            return state.copy(
                cycle = nextCycle,
                events = (state.events + event).takeLast(500)
            )
        }

        val oldRecords = state.records
        val updatedRecords = mutableListOf<MemoryTrace>()
        val transfers = mutableListOf<InformationTransfer>()

        var updatedCount = 0
        var transferredCount = 0

        /*
         * हर रिकॉर्ड को Refresh प्रक्रिया से गुजारा जाता है।
         * Retention और Refresh अलग-अलग नियंत्रित हैं।
         */

        oldRecords.forEach { old ->

            val newStrength = (
                old.strength * settings.retentionRate
            ).coerceIn(0f, 1f)

            val refreshed = old.copy(
                strength = newStrength,
                lastUpdatedCycle = nextCycle,
                refreshCount = old.refreshCount + 1
            )

            updatedRecords.add(refreshed)
            updatedCount++
        }

        /*
         * Pattern relationships की जाँच।
         * समान जानकारी वाले पुराने रिकॉर्ड से
         * नए रिकॉर्ड तक Information Transfer दर्ज होता है।
         */

        val latest = oldRecords.last()

        val relatedRecords = oldRecords
            .dropLast(1)
            .mapNotNull { previous ->

                val score = InternalMemoryEncoder.similarity(
                    previous.tokens,
                    latest.tokens
                )

                if (score >= settings.similarityThreshold) {
                    previous to score
                } else {
                    null
                }
            }

        if (relatedRecords.isNotEmpty()) {

            val transferredTokens = relatedRecords
                .flatMap { it.first.tokens }
                .distinct()
                .filter { it !in latest.tokens }

            if (transferredTokens.isNotEmpty()) {

                val transfer = InformationTransfer(
                    cycle = nextCycle,
                    sourceTraceIds = relatedRecords.map {
                        it.first.id
                    },
                    targetTraceId = latest.id,
                    transferredTokens = transferredTokens
                )

                transfers.add(transfer)
                transferredCount++
            }
        }

        val patternTokens = updatedRecords
            .flatMap { it.tokens }
            .toSet()

        val pattern = PatternSnapshot(
            cycle = nextCycle,
            traceIds = updatedRecords.map { it.id },
            tokenSet = patternTokens,
            patternVersion = state.patterns.size + 1
        )

        val event = RefreshEvent(
            cycle = nextCycle,
            addedRecords = 0,
            updatedRecords = updatedCount,
            transferredItems = transferredCount,
            totalRecords = updatedRecords.size
        )

        return state.copy(
            cycle = nextCycle,
            records = updatedRecords,
            patterns = (state.patterns + pattern).takeLast(200),
            transfers = (state.transfers + transfers).takeLast(1000),
            events = (state.events + event).takeLast(500),
            totalRefreshes = state.totalRefreshes + 1
        )
    }

    /* -------------------------
       6. RETRIEVAL
       ------------------------- */

    fun retrieve(
        state: InternalMemoryState,
        query: String,
        limit: Int = 10
    ): List<MemoryTrace> {

        val queryTokens = InternalMemoryEncoder.encode(query)

        if (queryTokens.isEmpty()) return emptyList()

        return state.records
            .map { record ->

                val score = InternalMemoryEncoder.similarity(
                    queryTokens,
                    record.tokens
                )

                record to score
            }
            .filter { it.second > 0f }
            .sortedByDescending { it.second }
            .take(limit.coerceIn(1, 100))
            .map { it.first }
    }

    /* -------------------------
       7. RESEARCH METRICS
       ------------------------- */

    fun getMetrics(
        state: InternalMemoryState
    ): InternalMemoryMetrics {

        val totalTokens = state.records
            .sumOf { it.tokens.size }

        val averageStrength =
            if (state.records.isEmpty()) {
                0f
            } else {
                state.records.sumOf {
                    it.strength.toDouble()
                }.toFloat() / state.records.size
            }

        val totalTransfers = state.transfers.size

        val uniqueTokens = state.records
            .flatMap { it.tokens }
            .toSet()
            .size

        return InternalMemoryMetrics(
            totalRecords = state.records.size,
            totalTokens = totalTokens,
            uniqueInformationUnits = uniqueTokens,
            averageRecordStrength = averageStrength,
            totalTransfers = totalTransfers,
            totalInputs = state.totalInputs,
            totalRefreshes = state.totalRefreshes
        )
    }
}

/* -------------------------
   8. METRICS DATA
   ------------------------- */

data class InternalMemoryMetrics(
    val totalRecords: Int,
    val totalTokens: Int,
    val uniqueInformationUnits: Int,
    val averageRecordStrength: Float,
    val totalTransfers: Int,
    val totalInputs: Int,
    val totalRefreshes: Int
)
