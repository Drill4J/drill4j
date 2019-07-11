package com.epam.drill.plugins.coverage

import java.util.concurrent.*

/**
 * Provides boolean array for the probe.
 * Implementations must be kotlin singleton objects.
 */
typealias ProbeArrayProvider = (Long, String, Int) -> BooleanArray

interface SessionProbeArrayProvider : ProbeArrayProvider {
    fun start(sessionId: String, testType: String)
    fun stop(sessionId: String): List<ExecDatum>?
    fun cancel(sessionId: String)
}

interface InstrContext : () -> String? {
    operator fun get(key: String): String?
}

const val DRIlL_TEST_NAME = "drill-test-name"

class ExecDatum(
    val id: Long,
    val name: String,
    val probes: BooleanArray,
    val testType: String,
    val testName: String = ""
)

/**
 * A container for session runtime data and optionally runtime data of tests
 * TODO ad hoc implementation, rewrite to something more descent
 */
class ExecRuntime(
    val testType: String,
    val testName: String = ""
) : ProbeArrayProvider {

    val execData = ConcurrentHashMap<Long, ExecDatum>()

    val testRuntimes = ConcurrentHashMap<String, ExecRuntime>()

    fun with(testName: String?): ExecRuntime = when (testName) {
        null -> this
        else -> testRuntimes.getOrPut(testName) { ExecRuntime(testType, testName) }
    }

    override fun invoke(id: Long, name: String, probeCount: Int) = execData.getOrPut(id) {
        ExecDatum(
            id = id,
            name = name,
            probes = BooleanArray(probeCount),
            testType = testType,
            testName = testName
        )
    }.probes

    fun collect() = execData.values + testRuntimes.values.flatMap { it.execData.values }
}

/**
 * Simple probe array provider that employs ConcurrentHashMap for runtime data storage.
 * This class is intended to be an ancestor for a concrete probe array provider object.
 * The provider must be a Kotlin singleton object, otherwise the instrumented probe calls will fail.
 */
open class SimpleSessionProbeArrayProvider(private val instrContext: InstrContext) : SessionProbeArrayProvider {
    private val sessionRuntimes = ConcurrentHashMap<String, ExecRuntime>()

    override fun invoke(id: Long, name: String, probeCount: Int): BooleanArray {
        val sessionId = instrContext()
        val sessionRuntime = if (sessionId != null) sessionRuntimes[sessionId] else null
        return if (sessionRuntime != null) {
            val testName = instrContext[DRIlL_TEST_NAME]
            val runtime = sessionRuntime.with(testName)
            runtime(id, name, probeCount)
        } else BooleanArray(probeCount)
    }

    override fun start(sessionId: String, testType: String) {
        sessionRuntimes[sessionId] = ExecRuntime(testType)
    }

    override fun stop(sessionId: String) = sessionRuntimes.remove(sessionId)?.collect()

    override fun cancel(sessionId: String) {
        sessionRuntimes.remove(sessionId)
    }

}
