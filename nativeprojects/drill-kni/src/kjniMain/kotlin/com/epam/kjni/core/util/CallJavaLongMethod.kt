package com.epam.kjni.core.util

import jvmapi.CallLongMethodA
import jvmapi.jclass
import jvmapi.jlong
import jvmapi.jobject
import kotlinx.cinterop.free
import kotlinx.cinterop.nativeHeap

class CallJavaLongMethod(
    jO: jobject?,
    javaClass: jclass?,
    methodName: String,
    methodSignature: String
) : JavaMethod(jO, javaClass, methodName, methodSignature) {
    override fun invoke(vararg raw: X): jlong {
        val arguments = toJObjectArray(raw)

        val value = CallLongMethodA(jO, getMethod(), arguments)
        nativeHeap.free(arguments)
        return value
    }


}