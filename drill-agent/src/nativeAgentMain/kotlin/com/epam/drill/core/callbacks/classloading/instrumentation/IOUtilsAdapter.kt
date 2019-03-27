package com.epam.drill.core.callbacks.classloading.instrumentation

import com.epam.drill.core.callbacks.classloading.toByteArray
import com.soywiz.korio.file.std.localVfs
import jvmapi.Allocate
import jvmapi.jint
import jvmapi.jintVar
import kotlinx.cinterop.*
import kotlinx.coroutines.runBlocking
import org.objectweb.kn.asm.*
import org.objectweb.kn.asm.Opcodes.Companion.ASTORE
import org.objectweb.kn.asm.commons.AdviceAdapter

class IOUtilsAdapter(val cvs: ClassVisitor) : ClassVisitor(Opcodes.ASM5, cvs), Opcodes {

    var name:String? = null

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String,
        interfaces: Array<String?>?
    ) {
        super.visit(version, access, name, signature, superName, interfaces)
       this.name = name
    }

    override fun visitMethod(
        access: Int, name: String, desc: String,
        signature: String?, exceptions: Array<String?>?
    ): MethodVisitor? {
        try {


            val mv = cvs.visitMethod(access, name, desc, signature, exceptions)
//            if(name == "showHtmlVetList"){
//                return mv
//            }


            val q = object : AdviceAdapter(Opcodes.ASM5, mv!!, access, name, desc) {
                override fun onMethodEnter() {

                    visitLdcInsn(this@IOUtilsAdapter.name+"#"+name);
                    val newLocal = newLocal(Type.getObjectType("java/lang/String"))
                    visitVarInsn(ASTORE, newLocal);
                    visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                    visitVarInsn(Opcodes.ALOAD, newLocal);
                    visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/Object;)V");
                }

                override fun visitTypeInsn(opcode: Int, type: String) {
                    println(type)
                    super.visitTypeInsn(opcode, type)
                }
            }

            return q
        } catch (ex: Throwable) {
            println(name)
            throw ex
        }

    }


    override fun visitSource(source: String, debug: String?) {
        println(source)
        println(debug)
        super.visitSource(source, debug)
    }
}

@ExperimentalUnsignedTypes
fun modifyClass(
    classData: CPointer<UByteVar>,
    classDataLen: jint,
    newData: CPointer<CPointerVar<UByteVar>>?,
    newClassDataLen: CPointer<jintVar>?,
    toKString: String
) {

    val b = classData.toByteArray(classDataLen)
    println(toKString)
    println(b.contentToString())
    val cr = ClassReader(b)
    val cv = ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)
//    cr.accept(IOUtilsAdapter(cv), ClassReader.EXPAND_FRAMES or ClassWriter.COMPUTE_FRAMES)
    cr.accept(cv, ClassReader.EXPAND_FRAMES or ClassWriter.COMPUTE_FRAMES)
//    cr.accept(cv, 0)
    val toByteArray = cv.toByteArray()
    println(toByteArray.contentToString())
    runBlocking {
        localVfs("C:\\Users\\Igor_Kuzminykh\\xxDrill4J\\temp\\${toKString.replace("/","_")}.class").write(toByteArray)
    }
    Allocate(toByteArray.size.toLong(), newData)
    toByteArray.forEachIndexed { x, y ->
        val pointed = newData!!.pointed
        val value: CPointer<UByteVarOf<UByte>> = pointed.value!!
        value[x] = y.toUByte()
    }
    newClassDataLen!!.pointed.value = toByteArray.size
    println("_________________________________________________________________________________")
}
