package com.epam.drill.plugins.coverage

import org.jacoco.core.analysis.Analyzer
import org.jacoco.core.analysis.CoverageBuilder
import org.jacoco.core.data.ExecutionDataStore
import org.jacoco.core.data.SessionInfoStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


const val sessionId = "xxx"

object TestProbeArrayProvider : SimpleSessionProbeArrayProvider({ sessionId })

class InstrumentationTests {

    val instrument = instrumenter(TestProbeArrayProvider)

    val memoryClassLoader = MemoryClassLoader()
    
    val targetClass = TestTarget::class.java
    
    val originalBytes = targetClass.readBytes()

    @Test
    fun `instrumented class should be larger the the original`() {
        val instrumented = instrument(targetClass.name, originalBytes)
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
        runtimeData?.collect(executionData, SessionInfoStore(), false)
        val coverageBuilder = CoverageBuilder()
        val analyzer = Analyzer(executionData, coverageBuilder)
        analyzer.analyzeClass(originalBytes, targetClass.name)
        val coverage = coverageBuilder.getBundle("all")
        val counter = coverage.instructionCounter
        assertEquals(27, counter.coveredCount)
        assertEquals(2, counter.missedCount)
    }

    private fun addInstrumentedClass() {
        val name = targetClass.name
        val instrumented = instrument(name, originalBytes)
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


