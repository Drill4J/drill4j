package com.epam.drill.plugins.coverage

import org.jacoco.core.analysis.Analyzer
import org.jacoco.core.analysis.CoverageBuilder
import org.jacoco.core.data.ExecutionData
import org.jacoco.core.data.ExecutionDataStore
import org.jacoco.core.internal.data.CRC64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InstrumentationTests {

    companion object {
        const val sessionId = "xxx"

        val instrContextStub: InstrContext = object : InstrContext {
            override fun get(key: String): String? = when (key) {
                DRILL_TEST_TYPE -> "MANUAL"
                DRIlL_TEST_NAME -> "test"
                else -> null
            }

            override fun invoke(): String? = sessionId

        }
    }


    object TestProbeArrayProvider : SimpleSessionProbeArrayProvider(instrContextStub)


    val instrument = instrumenter(TestProbeArrayProvider)

    val memoryClassLoader = MemoryClassLoader()

    val targetClass = TestTarget::class.java

    val originalBytes = targetClass.readBytes()

    val originalClassId = CRC64.classId(originalBytes)

    @Test
    fun `instrumented class should be larger the the original`() {
        val instrumented = instrument(targetClass.name, originalClassId, originalBytes)
        assertTrue { instrumented.count() > originalBytes.count() }
    }

    @Test
    fun `should provide coverage for run with the instrumented class`() {
        addInstrumentedClass()
        val instrumentedClass = memoryClassLoader.loadClass(targetClass.name)
        TestProbeArrayProvider.start(sessionId)
        val runnable = instrumentedClass.newInstance() as Runnable
        runnable.run()
        val runtimeData = TestProbeArrayProvider.stop(sessionId)
        val executionData = ExecutionDataStore()
        runtimeData?.forEach { executionData.put(ExecutionData(it.id, it.name, it.probes)) }
        val coverageBuilder = CoverageBuilder()
        val analyzer = Analyzer(executionData, coverageBuilder)
        analyzer.analyzeClass(originalBytes, targetClass.name)
        val coverage = coverageBuilder.getBundle("all")
        val counter = coverage.instructionCounter
        assertEquals(27, counter.coveredCount)
        assertEquals(2, counter.missedCount)
    }

    @Test
    fun `should transform any of stringified TestType values to TestType`() {
        val autoFromString = TestType["AUTO"]
        assertEquals(TestType.AUTO, autoFromString)
        val manualFromString = TestType["MANUAL"]
        assertEquals(TestType.MANUAL, manualFromString)
        val performanceFromString = TestType["PERFORMANCE"]
        assertEquals(TestType.PERFORMANCE, performanceFromString)
        val undefinedFromString = TestType["UNDEFINED"]
        assertEquals(TestType.UNDEFINED, undefinedFromString)
    }

    @Test
    fun `should transform any unexpected string to undefined test type`() {
        val nullTestType = TestType[null]
        assertEquals(TestType.UNDEFINED, nullTestType)
        val trashTestType = TestType["asdf"]
        assertEquals(TestType.UNDEFINED, trashTestType)
    }

    @Test
    fun `should associate execution data with test name and type gathered from request headers`() {
        addInstrumentedClass()
        val instrumentedClass = memoryClassLoader.loadClass(targetClass.name)
        TestProbeArrayProvider.start(sessionId)
        val runnable = instrumentedClass.newInstance() as Runnable
        runnable.run()
        val runtimeData = TestProbeArrayProvider.stop(sessionId)
        val transformedForAdminPart = runtimeData!!.map { datum ->
            ExDataTemp(
                id = datum.id,
                className = datum.name,
                probes = datum.probes.toList(),
                testName = datum.testName,
                testType = TestType[datum.testType]
            )
        }
        transformedForAdminPart.forEach {
            assertEquals(TestType.MANUAL, it.testType)
            assertEquals("test", it.testName)
        }
    }

    private fun addInstrumentedClass() {
        val name = targetClass.name
        val instrumented = instrument(name, originalClassId, originalBytes)
        memoryClassLoader.addDefinition(name, instrumented)
    }
}

fun Class<*>.readBytes() = this.getResourceAsStream("/${this.name.replace('.', '/')}.class").readBytes()

class MemoryClassLoader : ClassLoader() {
    private val definitions = mutableMapOf<String, ByteArray?>()

    fun addDefinition(name: String, bytes: ByteArray) {
        definitions[name] = bytes
    }

    override fun loadClass(name: String?, resolve: Boolean): Class<*> {
        val bytes = definitions[name]
        return if (bytes != null) {
            defineClass(name, bytes, 0, bytes.size)
        } else {
            super.loadClass(name, resolve)
        }
    }
}