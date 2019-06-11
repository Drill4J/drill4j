package com.epam.drill.plugins.coverage

import org.jacoco.core.internal.flow.ClassProbesAdapter
import org.jacoco.core.internal.flow.ClassProbesVisitor
import org.jacoco.core.internal.flow.MethodProbesVisitor
import org.jacoco.core.internal.instr.ClassInstrumenter
import org.jacoco.core.internal.instr.IProbeArrayStrategy
import org.jacoco.core.internal.instr.InstrSupport
import org.objectweb.asm.*
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Provides boolean array for the probe.
 * Implementations must be kotlin singleton objects.
 */
typealias ProbeArrayProvider = (Long, String, Int) -> BooleanArray

/**
 * Instrumenter type
 */
typealias DrillInstrumenter = (String, Long, ByteArray) -> ByteArray

class ExecDatum(
    val id: Long,
    val name: String,
    val probes: BooleanArray,
    val testName: String? = null
)

interface SessionProbeArrayProvider : ProbeArrayProvider {
    fun start(sessionId: String)
    fun stop(sessionId: String): List<ExecDatum>?
    fun cancel(sessionId: String)
}

interface InstrContext : () -> String? {
    operator fun get(key: String): String?
}

/**
 * A container for session runtime data and optionally runtime data of tests
 * TODO ad hoc implementation, rewrite to something more descent
 */
class ExecRuntime(val testName: String? = null) : ProbeArrayProvider {

    val execData = ConcurrentHashMap<Long, ExecDatum>()

    val testRuntimes = ConcurrentHashMap<String, ExecRuntime>()

    operator fun get(testName: String): ExecRuntime =
        testRuntimes.getOrPut(testName) { ExecRuntime(testName) }

    override fun invoke(id: Long, name: String, probeCount: Int) = execData.getOrPut(id) {
        ExecDatum(
            id = id,
            name = name,
            probes = BooleanArray(probeCount),
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
            val testName = instrContext["drill-test-name"]
            val runtime = if (testName != null) sessionRuntime[testName] else sessionRuntime
            runtime(id, name, probeCount)
        } else BooleanArray(probeCount)
    }

    override fun start(sessionId: String) {
        sessionRuntimes[sessionId] = ExecRuntime()
    }

    override fun stop(sessionId: String) = sessionRuntimes.remove(sessionId)?.collect()

    override fun cancel(sessionId: String) {
        sessionRuntimes.remove(sessionId)
    }

}

/**
 * JaCoCo instrumenter
 */
fun instrumenter(probeArrayProvider: ProbeArrayProvider): DrillInstrumenter {
    return CustomInstrumenter(probeArrayProvider)
}

private class CustomInstrumenter(
    private val probeArrayProvider: ProbeArrayProvider
) : DrillInstrumenter {

    override fun invoke(className: String, classId: Long, classBody: ByteArray) =
        try {
            instrument(className, classId, classBody)
        } catch (e: RuntimeException) {
            throw IOException("Error while instrumenting $className classId=$classId.", e)
        }

    fun instrument(className: String, classId: Long, classBody: ByteArray): ByteArray {
        val version = InstrSupport.getVersionMajor(classBody)

        //count probes before transformation
        val counter = ProbeCounter()
        val reader = InstrSupport.classReaderFor(classBody)
        reader.accept(ClassProbesAdapter(counter, false), 0)

        val strategy = DrillProbeStrategy(
            probeArrayProvider,
            className,
            classId,
            InstrSupport.needsFrames(version),
            counter.count
        )
        val writer = object : ClassWriter(reader, 0) {
            override fun getCommonSuperClass(type1: String, type2: String): String = throw IllegalStateException()
        }
        val visitor = ClassProbesAdapter(
            ClassInstrumenter(strategy, writer),
            InstrSupport.needsFrames(version)
        )
        reader.accept(visitor, ClassReader.EXPAND_FRAMES)
        return writer.toByteArray()
    }
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