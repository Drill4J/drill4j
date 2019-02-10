package com.epam.drill.jvmti.callbacks.classloading.instrumentation

import com.epam.drill.jvmti.callbacks.classloading.toByteArray
import jvmapi.Allocate
import jvmapi.jint
import jvmapi.jintVar
import kotlinx.cinterop.*
import org.objectweb.kn.asm.*

class IOUtilsAdapter(val cvs: ClassVisitor) : ClassVisitor(Opcodes.ASM5, cvs), Opcodes {

    override fun visitMethod(
        access: Int, name: String, desc: String,
        signature: String?, exceptions: Array<String?>?
    ): MethodVisitor? {
        val mv = cvs.visitMethod(access, name, desc, signature, exceptions)
        val l0 = Label()
        mv?.visitLabel(l0)
        mv?.visitLineNumber(12, l0)
        mv?.visitFieldInsn(
            Opcodes.GETSTATIC,
            "java/lang/System",
            "out",
            "Ljava/io/PrintStream;"
        )
        mv?.visitVarInsn(Opcodes.LLOAD, 3)
        mv?.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            "java/io/PrintStream",
            "println",
            "(J)V"
        )
        return mv
    }
}
@ExperimentalUnsignedTypes
fun modifyClass(
    classData: CPointer<UByteVar>,
    classDataLen: jint,
    newData: CPointer<CPointerVar<UByteVar>>?,
    newClassDataLen: CPointer<jintVar>?
) {
    val cr = ClassReader(classData.toByteArray(classDataLen))
    val cv = ClassWriter(0)
    cr.accept(IOUtilsAdapter(cv), 0)
    val toByteArray = cv.toByteArray()
    Allocate(toByteArray.size.toLong(), newData)
    toByteArray.forEachIndexed { x, y ->
        val pointed = newData!!.pointed
        val value = pointed.value!!
        value[x] = y.toUByte()
    }
    newClassDataLen!!.pointed.value = toByteArray.size
}
