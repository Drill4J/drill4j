package com.epam.drill.plugins.coverage

import org.hamcrest.*
import org.jacoco.core.analysis.*
import org.jacoco.core.data.*
import org.jacoco.core.internal.data.*
import org.junit.*
import org.junit.rules.*
import kotlin.test.*
import kotlin.test.Test

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

    @get:Rule
    val collector = ErrorCollector()

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
        val autoString: TestTypeString = "AUTO"
        collector.checkThat(autoString.toTestType(), CoreMatchers.equalTo(TestType.AUTO))
        val manualString: TestTypeString = "MANUAL"
        collector.checkThat(manualString.toTestType(), CoreMatchers.equalTo(TestType.MANUAL))
        val performanceString: TestTypeString = "PERFORMANCE"
        collector.checkThat(performanceString.toTestType(), CoreMatchers.equalTo(TestType.PERFORMANCE))
        val undefinedString: TestTypeString = "UNDEFINED"
        collector.checkThat(undefinedString.toTestType(), CoreMatchers.equalTo(TestType.UNDEFINED))
    }

    @Test
    fun `should transform any unexpected string to undefined test type`() {
        val nullTypeString: TestTypeString = null
        collector.checkThat(nullTypeString.toTestType(), CoreMatchers.equalTo(TestType.UNDEFINED))
        val unexpectedTypeString: TestTypeString = "asdf"
        collector.checkThat(unexpectedTypeString.toTestType(), CoreMatchers.equalTo(TestType.UNDEFINED))
    }

    @Test
    fun `should associate execution data with test name and type gathered from request headers`() {
        addInstrumentedClass()
        val instrumentedClass = memoryClassLoader.loadClass(targetClass.name)
        TestProbeArrayProvider.start(sessionId)
        val runnable = instrumentedClass.newInstance() as Runnable
        runnable.run()
        val runtimeData = TestProbeArrayProvider.stop(sessionId)
        runtimeData?.forEach {
            collector.checkThat("MANUAL", CoreMatchers.equalTo(it.testType))
            collector.checkThat("test", CoreMatchers.equalTo(it.testName))
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