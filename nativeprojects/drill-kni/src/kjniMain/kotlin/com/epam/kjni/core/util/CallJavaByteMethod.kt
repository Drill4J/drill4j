package com.epam.kjni.core.util

import jvmapi.CallByteMethodA
import jvmapi.jbyte
import jvmapi.jclass
import jvmapi.jobject
import kotlinx.cinterop.free
import kotlinx.cinterop.nativeHeap

class CallJavaByteMethod(
    jO: jobject?,
    javaClass: jclass?,
    methodName: String,
    methodSignature: String
) : JavaMethod(jO, javaClass, methodName, methodSignature) {
    override fun invoke(vararg raw: X): jbyte {
        val arguments = toJObjectArray(raw)
        val value = CallByteMethodA(jO, getMethod(), arguments)
        nativeHeap.free(arguments)
        return value
    }


}