package com.epam.kjni.core.util

import jvmapi.CallShortMethodA
import jvmapi.jclass
import jvmapi.jobject
import jvmapi.jshort
import kotlinx.cinterop.free
import kotlinx.cinterop.nativeHeap

class CallJavaShortMethod(
    jO: jobject?,
    javaClass: jclass?,
    methodName: String,
    methodSignature: String
) : JavaMethod(jO, javaClass, methodName, methodSignature) {
    override fun invoke(vararg raw: X): jshort {
        val arguments = toJObjectArray(raw)

        val value = CallShortMethodA(jO, getMethod(), arguments)
        nativeHeap.free(arguments)
        return value
    }


}