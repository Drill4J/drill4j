package com.epam.knasm.bytecode

import org.objectweb.kn.asm.ClassWriter
import org.objectweb.kn.asm.Label
import org.objectweb.kn.asm.MethodVisitor
import org.objectweb.kn.asm.MethodWriter

class SsdsaaSxxss(flags: Int) : ClassWriter(flags) {
    override fun visitMethod(
        access: Int,
        name: String,
        desc: String,
        signature: String?,
        exceptions: Array<String?>?
    ): MethodVisitor {
        println("name: $name")
        println("desc: $desc")
        println("signature: $signature")
        if (name == "readIntoNativeBuffer") {
            return w(this, access, name, desc, signature, exceptions, computeMaxs, computeFrames)
        } else {
            return super.visitMethod(access, name, desc, signature, exceptions)
        }
    }


}


class w(
    cw: ClassWriter,
    access: Int,
    name: String,
    descriptor: String,
    signature: String?,
    exceptions: Array<String?>?,
    computeMaxs: Boolean,
    computeFrames: Boolean
) : MethodWriter(cw, access, name, descriptor, signature, exceptions, computeMaxs, computeFrames) {

    override fun visitLocalVariable(
        name: String,
        desc: String,
        signature: String?,
        start: Label,
        end: Label,
        index: Int
    ) {
        println("name: $name")
        println("desc: $desc")
        println("signature: $signature")
        super.visitLocalVariable(name, desc, signature, start, end, index)
    }
}