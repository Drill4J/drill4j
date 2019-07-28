package com.epam.drill.plugin.exception.datatypes

import com.epam.drill.jvmapi.gen.*
import kotlinx.cinterop.*

class RefType(val env: CPointer<JNIEnvVar>?) : JType() {

    override fun retrieveValue(thread: jthread?, depth: Int, currentEntry: jvmtiLocalVariableEntry): Pair<String, Any>? {
//        val valuePtr = nativeHeap.mutex<jobjectVar>()
//        jvmtiInterface?.GetLocalObject!!(jmvtiEnvVar, thread, depth, currentEntry.slot, valuePtr.ptr)
//        val value = valuePtr.value
//        val getClassSignature = jvmtiInterface?.GetClassSignature
//        val getObjectClass = env?.pointed?.value?.pointed!!.GetObjectClass
//        return if (value != null) {
//            val objectClass = getObjectClass!!(env, value)
//            val xx = nativeHeap.mutex<CPointerVar<ByteVar>>()
//            getClassSignature!!(jmvtiEnvVar!!, objectClass!!, xx.ptr, null)
//            val className = Utils.getPrettyClassName(xx)
//            Pair(className!!, value)
//        } else {
//            null
////            Pair("InnerClass", env.pointed.value?.pointed!!.NewStringUTF!!(env, "not defined".cstr.getPointer(Arena()))!!)
//            //todo?? filer these?
//        }
        return Pair("InnerClass","x")

    }


}