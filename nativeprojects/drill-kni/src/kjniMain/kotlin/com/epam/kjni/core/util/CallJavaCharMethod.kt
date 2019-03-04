package com.epam.kjni.core.util

import jvmapi.CallCharMethodA
import jvmapi.jclass
import jvmapi.jobject
import kotlinx.cinterop.free
import kotlinx.cinterop.nativeHeap

class CallJavaCharMethod(
    jO: jobject?,
    javaClass: jclass?,
    methodName: String,
    methodSignature: String
) : JavaMethod(jO, javaClass, methodName, methodSignature) {
    @ExperimentalUnsignedTypes
    override fun invoke(vararg raw: X): Char {
        val arguments = toJObjectArray(raw)

        val value = CallCharMethodA(jO, getMethod(), arguments)
        nativeHeap.free(arguments)
        return value.toShort().toChar()
    }


}