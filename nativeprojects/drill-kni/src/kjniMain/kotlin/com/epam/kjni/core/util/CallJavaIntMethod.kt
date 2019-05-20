package com.epam.kjni.core.util

import jvmapi.CallIntMethodA
import jvmapi.jclass
import jvmapi.jint
import jvmapi.jobject
import kotlinx.cinterop.free
import kotlinx.cinterop.nativeHeap

class CallJavaIntMethod(
    jO: jobject?,
    javaClass: jclass?,
    methodName: String,
    methodSignature: String
) : JavaMethod(jO, javaClass, methodName, methodSignature) {
    override fun invoke(vararg raw: X): jint {
        val arguments = toJObjectArray(raw)

        val value = CallIntMethodA(jO, getMethod(), arguments)
        nativeHeap.free(arguments)
        return value
    }


}