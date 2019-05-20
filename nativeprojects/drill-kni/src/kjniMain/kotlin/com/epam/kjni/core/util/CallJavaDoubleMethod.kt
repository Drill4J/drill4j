package com.epam.kjni.core.util

import jvmapi.CallDoubleMethodA
import jvmapi.jclass
import jvmapi.jdouble
import jvmapi.jobject
import kotlinx.cinterop.free
import kotlinx.cinterop.nativeHeap

class CallJavaDoubleMethod(
    jO: jobject?,
    javaClass: jclass?,
    methodName: String,
    methodSignature: String
) : JavaMethod(jO, javaClass, methodName, methodSignature) {
    override fun invoke(vararg raw: X): jdouble {
        val arguments = toJObjectArray(raw)

        val value = CallDoubleMethodA(jO, getMethod(), arguments)
        nativeHeap.free(arguments)
        return value
    }


}