
package com.refreshsystem.simulator

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
    val meanMemory: Float
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
    val phase: String = "TEST"
)

object ResearchExperimentEngine {

    const val TRAINING_CYCLES = 40
    const val TEST_CYCLES = 80
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
            val phase = (cycle * 0.17 + i * 0.31 + seed * 0.013)
            kotlin.math.sin(phase).toFloat().coerceIn(-1f, 1f)
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
                            phase = "TRAINING"
                        )
                    )
                }

                // PHASE 2: CONDITION-SPECIFIC TEST.
                val stabilityValues = mutableListOf<Float>()
                val memoryValues = mutableListOf<Float>()

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

                    stabilityValues.add(current.dynamicStability)
                    memoryValues.add(current.memoryRetention)

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
                            phase = "TEST"
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
                        meanMemory = memoryValues.average().toFloat()
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
                "CONTINUITY,NETWORK,PATTERN_CHANGE"
        )

        records.forEach {
            out.appendLine(
                "CYCLE,${it.phase},${it.seed},${it.conditionId},${it.cycle}," +
                    "${it.external},${it.refresh},${it.decay}," +
                    "${it.stability},${it.memory},${it.continuity}," +
                    "${it.network},${it.patternChange}"
            )
        }

        out.appendLine()

        out.appendLine(
            "RESULT_TYPE,SEED,CONDITION_ID,CONDITION," +
                "FINAL_STABILITY,FINAL_MEMORY,FINAL_CONTINUITY," +
                "FINAL_NETWORK,MEAN_STABILITY,MEAN_MEMORY"
        )

        results.forEach {
            val safeName = it.condition.name.replace(",", ";")

            out.appendLine(
                "RESULT,${it.seed},${it.condition.id},$safeName," +
                    "${it.finalStability},${it.finalMemory}," +
                    "${it.finalContinuity},${it.finalNetwork}," +
                    "${it.meanStability},${it.meanMemory}"
            )
        }

        return out.toString()
    }
}
