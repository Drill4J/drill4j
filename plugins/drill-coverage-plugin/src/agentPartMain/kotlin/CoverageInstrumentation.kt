package com.epam.drill.plugins.coverage

import org.jacoco.core.instr.Instrumenter
import org.jacoco.core.internal.data.CRC64
import org.jacoco.core.internal.flow.ClassProbesAdapter
import org.jacoco.core.internal.flow.ClassProbesVisitor
import org.jacoco.core.internal.flow.MethodProbesVisitor
import org.jacoco.core.internal.instr.ClassInstrumenter
import org.jacoco.core.internal.instr.IProbeArrayStrategy
import org.jacoco.core.internal.instr.InstrSupport
import org.jacoco.core.runtime.IExecutionDataAccessorGenerator
import org.jacoco.core.runtime.RuntimeData
import org.objectweb.asm.*
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * Provides boolean array for the probe.
 * Implementations must be kotlin singleton objects.
 */
typealias ProbeArrayProvider = (Long, String, Int) -> BooleanArray

/**
 * Instrumenter type
 */
typealias DrillInstrumenter = (String, ByteArray) -> ByteArray

interface SessionProbeArrayProvider : ProbeArrayProvider {
    fun start(sessionId: String)
    fun stop(sessionId: String): Collection<RuntimeData>?
}

interface InstrContext : () -> String? {
    operator fun get(key: String): String?
}

/**
 * A container for session runtime data and optionally runtime data of tests
 * TODO ad hoc implementation, rewrite to something more descent
 */
class SessionRuntime {
    val sessionData = RuntimeData().apply { sessionId = "" }
    val testsData: ConcurrentMap<String, RuntimeData> = ConcurrentHashMap<String, RuntimeData>(1)

    fun get(testName: String?): RuntimeData =
        if (!testName.isNullOrBlank()) {
            testsData.getOrPut(testName) {
                RuntimeData().apply {
                    sessionId = testName
                }
            }
        } else sessionData
    
    fun collect() = listOf(sessionData) + testsData.values
}

/**
 * Simple probe array provider that employs ConcurrentHashMap for runtime data storage.
 * This class is intended to be an ancestor for a concrete probe array provider object.
 * The provider must be a Kotlin singleton object, otherwise the instrumented probe calls will fail.
 */
open class SimpleSessionProbeArrayProvider(private val instrContext: InstrContext) : SessionProbeArrayProvider {
    private val sessionRuntimes = ConcurrentHashMap<String, SessionRuntime>(1)

    override fun invoke(id: Long, name: String, probeCount: Int): BooleanArray {
        val sessionId = instrContext()
        val runtime = sessionId?.let {
            val testName = instrContext["DrillTestName"]
            sessionRuntimes[it]?.get(testName)
        }
        return runtime?.run {
            getExecutionData(id, name, probeCount).probes
        } ?: BooleanArray(probeCount)
    }

    override fun start(sessionId: String) {
        sessionRuntimes[sessionId] = SessionRuntime()
    }

    override fun stop(sessionId: String): Collection<RuntimeData>? = sessionRuntimes.remove(sessionId)?.collect()
}

/**
 * JaCoCo instrumenter
 */
fun instrumenter(probeArrayProvider: ProbeArrayProvider): DrillInstrumenter {
    return CustomInstrumenter(probeArrayProvider)
}

private class CustomInstrumenter(
    private val probeArrayProvider: ProbeArrayProvider
) : Instrumenter(EmptyExecutionDataAccessorGenerator), DrillInstrumenter {

    override fun invoke(className: String, classBody: ByteArray): ByteArray = instrument(classBody, className) 

    override fun instrument(buffer: ByteArray?, name: String?): ByteArray {
        try {
            return instrument(buffer!!)
        } catch (e: RuntimeException) {
            throw IOException("Error while instrumenting $name.", e)
        }

    }

    private fun instrument(source: ByteArray): ByteArray {
        val classId = CRC64.classId(source)
        val reader = InstrSupport.classReaderFor(source)
        val writer = object : ClassWriter(reader, 0) {
            override fun getCommonSuperClass(type1: String, type2: String): String = throw IllegalStateException()
        }
        val className = reader.className
        val version = InstrSupport.getVersionMajor(source)

        //count probes before transformation
        val counter = ProbeCounter()
        reader.accept(ClassProbesAdapter(counter, false), 0)

        val strategy =
            DrillProbeStrategy(probeArrayProvider, className, classId, InstrSupport.needsFrames(version), counter.count)

        val visitor = ClassProbesAdapter(
            ClassInstrumenter(strategy, writer),
            InstrSupport.needsFrames(version)
        )
        reader.accept(visitor, ClassReader.EXPAND_FRAMES)
        return writer.toByteArray()
    }
}

private object EmptyExecutionDataAccessorGenerator : IExecutionDataAccessorGenerator {
    override fun generateDataAccessor(classid: Long, classname: String?, probecount: Int, mv: MethodVisitor?): Int = 0
}

private class ProbeCounter : ClassProbesVisitor() {
    var count = 0
        private set(value) {
            field = value
        }

    override fun visitMethod(
        access: Int, name: String?, desc: String?, signature: String?, exceptions: Array<out String>?
    ): MethodProbesVisitor? {
        return null
    }

    override fun visitTotalProbeCount(count: Int) {
        this.count = count
    }

}


private class DrillProbeStrategy(
    private val probeArrayProvider: ProbeArrayProvider,
    private val className: String,
    private val classId: Long,
    private val withFrames: Boolean, //TODO frames?
    private val probeCount: Int
) : IProbeArrayStrategy {
    override fun storeInstance(mv: MethodVisitor?, clinit: Boolean, variable: Int): Int = mv!!.run {
        val drillClassName = probeArrayProvider.javaClass.name.replace('.', '/')
        visitFieldInsn(Opcodes.GETSTATIC, drillClassName, "INSTANCE", "L$drillClassName;")
        // Stack[0]: Lcom/epam/drill/jacoco/Stuff;

        visitLdcInsn(classId)
        visitLdcInsn(className)
        visitLdcInsn(probeCount)
        visitMethodInsn(
            Opcodes.INVOKEVIRTUAL, drillClassName, "invoke", "(JLjava/lang/String;I)[Z",
            false
        )
        visitVarInsn(Opcodes.ASTORE, variable)
        5 //stack size
    }

    override fun addMembers(cv: ClassVisitor?, probeCount: Int) {}
}