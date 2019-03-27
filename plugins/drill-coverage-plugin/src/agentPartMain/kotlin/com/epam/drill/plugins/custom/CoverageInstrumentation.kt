package com.epam.drill.plugins.custom

import org.jacoco.core.instr.Instrumenter
import org.jacoco.core.internal.data.CRC64
import org.jacoco.core.internal.flow.ClassProbesAdapter
import org.jacoco.core.internal.flow.ClassProbesVisitor
import org.jacoco.core.internal.flow.MethodProbesVisitor
import org.jacoco.core.internal.instr.ClassInstrumenter
import org.jacoco.core.internal.instr.IProbeArrayStrategy
import org.jacoco.core.internal.instr.InstrSupport
import org.jacoco.core.runtime.IExecutionDataAccessorGenerator
import org.objectweb.asm.*
import java.io.IOException

/**
 * Provides boolean array for the probe.
 * Implementations must be kotlin singleton objects.
 */
typealias ProbeArrayProvider = (Long, String, Int) -> BooleanArray

/**
 * Instrumenter type
 */
typealias DrillInstrumenter = (String, ByteArray) -> ByteArray

/**
 * JaCoCo instrumenter
 */
fun instrumenter(probeArrayProvider: ProbeArrayProvider): DrillInstrumenter = CustomInstrumenter(probeArrayProvider)

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