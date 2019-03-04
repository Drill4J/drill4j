package com.epam.kjni.core.util

import jvmapi.CallFloatMethodA
import jvmapi.jclass
import jvmapi.jfloat
import jvmapi.jobject
import kotlinx.cinterop.free
import kotlinx.cinterop.nativeHeap

class CallJavaFloatMethod(
    jO: jobject?,
    javaClass: jclass?,
    methodName: String,
    methodSignature: String
) : JavaMethod(jO, javaClass, methodName, methodSignature) {
    override fun invoke(vararg raw: X): jfloat {
        val arguments = toJObjectArray(raw)

        val value = CallFloatMethodA(jO, getMethod(), arguments)
        nativeHeap.free(arguments)
        return value
    }


}