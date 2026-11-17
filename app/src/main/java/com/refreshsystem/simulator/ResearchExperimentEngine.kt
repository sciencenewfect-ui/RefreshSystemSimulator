
package com.refreshsystem.simulator

import kotlin.math.sqrt

data class ResearchCondition(
    val id: Int,
    val name: String,
    val external: Boolean,
    val refresh: Boolean,
    val decay: Boolean
)

data class ResearchResult(
    val condition: ResearchCondition,
    val seed: Int,
    val finalStability: Float,
    val finalMemory: Float,
    val finalContinuity: Float,
    val finalNetwork: Float,
    val meanStability: Float,
    val meanMemory: Float,
    val trainingEndMemory: Float,
    val memoryCurve: List<Float>,
    val finalMemoryFidelity: Float,
    val meanMemoryFidelity: Float,
    val memoryFidelityCurve: List<Float>,

    val finalEnergy: Float = 0f,
    val finalMatter: Float = 0f,
    val finalResourceSupport: Float = 0f,
    val finalNaturalStability: Float = 0f,
    val finalInternalWorldStability: Float = 0f,
    val finalChetnaIndex: Float = 0f,
    val meanEnergy: Float = 0f,
    val meanMatter: Float = 0f,
    val meanNaturalStability: Float = 0f,
    val meanInternalWorldStability: Float = 0f,
    val meanChetnaIndex: Float = 0f
)

data class ResearchCycle(
    val seed: Int,
    val conditionId: Int,
    val cycle: Int,
    val external: Boolean,
    val refresh: Boolean,
    val decay: Boolean,
    val stability: Float,
    val memory: Float,
    val continuity: Float,
    val network: Float,
    val patternChange: Float,
    val phase: String = "TEST",
    val memoryFidelity: Float = -1f,

    val energy: Float = 0f,
    val matter: Float = 0f,
    val resourceSupport: Float = 0f,
    val naturalStability: Float = 0f,
    val internalWorldStability: Float = 0f,
    val chetnaIndex: Float = 0f
)

object ResearchExperimentEngine {

    const val TRAINING_CYCLES = 40
    const val TEST_CYCLES = 500
    const val DEFAULT_REPEATS = 5

    val conditions = listOf(
        ResearchCondition(1, "External ON / Refresh ON / Decay ON", true, true, true),
        ResearchCondition(2, "External ON / Refresh ON / Decay OFF", true, true, false),
        ResearchCondition(3, "External ON / Refresh OFF / Decay ON", true, false, true),
        ResearchCondition(4, "External ON / Refresh OFF / Decay OFF", true, false, false),
        ResearchCondition(5, "External OFF / Refresh ON / Decay ON", false, true, true),
        ResearchCondition(6, "External OFF / Refresh ON / Decay OFF", false, true, false),
        ResearchCondition(7, "External OFF / Refresh OFF / Decay ON", false, false, true),
        ResearchCondition(8, "External OFF / Refresh OFF / Decay OFF", false, false, false)
    )

    private fun inputFor(cycle: Int, seed: Int): List<Float> =
        List(OpenSystemEngine.PATTERN_SIZE) { i ->
            val phase = cycle * 0.17 + i * 0.31 + seed * 0.013
            kotlin.math.sin(phase).toFloat().coerceIn(-1f, 1f)
        }

    private fun calculateMemoryFidelity(
        baseline: List<Float>,
        current: List<Float>
    ): Float {
        val length = minOf(baseline.size, current.size)
        if (length == 0) return 0f

        var dot = 0f
        var baselineEnergy = 0f
        var currentEnergy = 0f

        for (i in 0 until length) {
            dot += baseline[i] * current[i]
            baselineEnergy += baseline[i] * baseline[i]
            currentEnergy += current[i] * current[i]
        }

        if (baselineEnergy < 0.000001f || currentEnergy < 0.000001f) {
            return 0f
        }

        return (
            dot / (sqrt(baselineEnergy) * sqrt(currentEnergy))
        ).coerceIn(-1f, 1f)
    }

    fun run(
        baseSeed: Int = 42,
        repeats: Int = DEFAULT_REPEATS
    ): Pair<List<ResearchResult>, List<ResearchCycle>> {

        require(repeats in 1..100)

        val results = mutableListOf<ResearchResult>()
        val records = mutableListOf<ResearchCycle>()

        conditions.forEach { condition ->
            repeat(repeats) { repeatIndex ->

                val seed = baseSeed + repeatIndex
                val engine = OpenSystemEngine(seed)
                var current = engine.getSnapshot()

                // PHASE 1: IDENTICAL TRAINING FOR ALL CONDITIONS.
                repeat(TRAINING_CYCLES) { index ->

                    current = engine.step(
                        externalFeatures = inputFor(index, seed),
                        internalRefreshEnabled = true,
                        naturalDecayEnabled = false
                    )

                    records.add(
                        ResearchCycle(
                            seed = seed,
                            conditionId = condition.id,
                            cycle = index + 1,
                            external = true,
                            refresh = true,
                            decay = false,
                            stability = current.dynamicStability,
                            memory = current.memoryRetention,
                            continuity = current.identityContinuity,
                            network = current.networkStrength,
                            patternChange = current.patternChange,
                            phase = "TRAINING",
                            energy = current.energyReserve,
                            matter = current.matterReserve,
                            resourceSupport = current.resourceSupport,
                            naturalStability = current.naturalDynamicStability,
                            internalWorldStability = current.internalWorldStability,
                            chetnaIndex = current.chetnaHypothesisIndex
                        )
                    )
                }

                val trainingMemoryPattern = current.memoryState.toList()
                val trainingEndMemory = current.memoryRetention

                // PHASE 2: CONDITION-SPECIFIC TEST.
                val stabilityValues = mutableListOf<Float>()
                val memoryValues = mutableListOf<Float>()
                val memoryCurve = mutableListOf<Float>()
                val fidelityCurve = mutableListOf<Float>()

                val energyValues = mutableListOf<Float>()
                val matterValues = mutableListOf<Float>()
                val naturalStabilityValues = mutableListOf<Float>()
                val internalWorldValues = mutableListOf<Float>()
                val chetnaValues = mutableListOf<Float>()

                repeat(TEST_CYCLES) { index ->

                    val testCycle = TRAINING_CYCLES + index

                    val features = if (condition.external) {
                        inputFor(testCycle, seed)
                    } else {
                        null
                    }

                    current = engine.step(
                        externalFeatures = features,
                        internalRefreshEnabled = condition.refresh,
                        naturalDecayEnabled = condition.decay
                    )

                    val fidelity = calculateMemoryFidelity(
                        trainingMemoryPattern,
                        current.memoryState
                    )

                    stabilityValues.add(current.dynamicStability)
                    memoryValues.add(current.memoryRetention)
                    memoryCurve.add(current.memoryRetention)
                    fidelityCurve.add(fidelity)

                    energyValues.add(current.energyReserve)
                    matterValues.add(current.matterReserve)
                    naturalStabilityValues.add(current.naturalDynamicStability)
                    internalWorldValues.add(current.internalWorldStability)
                    chetnaValues.add(current.chetnaHypothesisIndex)

                    records.add(
                        ResearchCycle(
                            seed = seed,
                            conditionId = condition.id,
                            cycle = testCycle + 1,
                            external = condition.external,
                            refresh = condition.refresh,
                            decay = condition.decay,
                            stability = current.dynamicStability,
                            memory = current.memoryRetention,
                            continuity = current.identityContinuity,
                            network = current.networkStrength,
                            patternChange = current.patternChange,
                            phase = "TEST",
                            memoryFidelity = fidelity,
                            energy = current.energyReserve,
                            matter = current.matterReserve,
                            resourceSupport = current.resourceSupport,
                            naturalStability = current.naturalDynamicStability,
                            internalWorldStability = current.internalWorldStability,
                            chetnaIndex = current.chetnaHypothesisIndex
                        )
                    )
                }

                results.add(
                    ResearchResult(
                        condition = condition,
                        seed = seed,
                        finalStability = current.dynamicStability,
                        finalMemory = current.memoryRetention,
                        finalContinuity = current.identityContinuity,
                        finalNetwork = current.networkStrength,
                        meanStability = stabilityValues.average().toFloat(),
                        meanMemory = memoryValues.average().toFloat(),
                        trainingEndMemory = trainingEndMemory,
                        memoryCurve = memoryCurve.toList(),
                        finalMemoryFidelity = fidelityCurve.lastOrNull() ?: 0f,
                        meanMemoryFidelity = fidelityCurve.average().toFloat(),
                        memoryFidelityCurve = fidelityCurve.toList(),

                        finalEnergy = current.energyReserve,
                        finalMatter = current.matterReserve,
                        finalResourceSupport = current.resourceSupport,
                        finalNaturalStability = current.naturalDynamicStability,
                        finalInternalWorldStability = current.internalWorldStability,
                        finalChetnaIndex = current.chetnaHypothesisIndex,

                        meanEnergy = energyValues.average().toFloat(),
                        meanMatter = matterValues.average().toFloat(),
                        meanNaturalStability = naturalStabilityValues.average().toFloat(),
                        meanInternalWorldStability = internalWorldValues.average().toFloat(),
                        meanChetnaIndex = chetnaValues.average().toFloat()
                    )
                )
            }
        }

        return results to records
    }

    fun csv(
        records: List<ResearchCycle>,
        results: List<ResearchResult>
    ): String {

        val out = StringBuilder()

        out.appendLine(
            "DATA_TYPE,PHASE,SEED,CONDITION_ID,CYCLE," +
                "EXTERNAL,REFRESH,DECAY,STABILITY,MEMORY," +
                "CONTINUITY,NETWORK,PATTERN_CHANGE,MEMORY_FIDELITY," +
                "ENERGY,MATTER,RESOURCE_SUPPORT,NATURAL_STABILITY," +
                "INTERNAL_WORLD_STABILITY,CHETNA_INDEX"
        )

        records.forEach {
            out.appendLine(
                "CYCLE,${it.phase},${it.seed},${it.conditionId},${it.cycle}," +
                    "${it.external},${it.refresh},${it.decay}," +
                    "${it.stability},${it.memory},${it.continuity}," +
                    "${it.network},${it.patternChange},${it.memoryFidelity}," +
                    "${it.energy},${it.matter},${it.resourceSupport}," +
                    "${it.naturalStability},${it.internalWorldStability}," +
                    "${it.chetnaIndex}"
            )
        }

        out.appendLine()

        out.appendLine(
            "RESULT_TYPE,SEED,CONDITION_ID,CONDITION," +
                "FINAL_STABILITY,FINAL_MEMORY,FINAL_CONTINUITY," +
                "FINAL_NETWORK,MEAN_STABILITY,MEAN_MEMORY," +
                "TRAINING_END_MEMORY,MEMORY_RETENTION_RATIO," +
                "FINAL_MEMORY_FIDELITY,MEAN_MEMORY_FIDELITY," +
                "FINAL_ENERGY,FINAL_MATTER,FINAL_RESOURCE_SUPPORT," +
                "FINAL_NATURAL_STABILITY,FINAL_INTERNAL_WORLD_STABILITY," +
                "FINAL_CHETNA_INDEX,MEAN_ENERGY,MEAN_MATTER," +
                "MEAN_NATURAL_STABILITY,MEAN_INTERNAL_WORLD_STABILITY," +
                "MEAN_CHETNA_INDEX"
        )

        results.forEach {
            val safeName = it.condition.name.replace(",", ";")

            val ratio = if (it.trainingEndMemory > 0.000001f) {
                it.finalMemory / it.trainingEndMemory
            } else {
                null
            }

            out.appendLine(
                "RESULT,${it.seed},${it.condition.id},$safeName," +
                    "${it.finalStability},${it.finalMemory}," +
                    "${it.finalContinuity},${it.finalNetwork}," +
                    "${it.meanStability},${it.meanMemory}," +
                    "${it.trainingEndMemory},${ratio ?: "N/A"}," +
                    "${it.finalMemoryFidelity},${it.meanMemoryFidelity}," +
                    "${it.finalEnergy},${it.finalMatter}," +
                    "${it.finalResourceSupport},${it.finalNaturalStability}," +
                    "${it.finalInternalWorldStability},${it.finalChetnaIndex}," +
                    "${it.meanEnergy},${it.meanMatter}," +
                    "${it.meanNaturalStability}," +
                    "${it.meanInternalWorldStability},${it.meanChetnaIndex}"
            )
        }

        return out.toString()
    }
}
