package com.epam.kjni.core.util

import jvmapi.CallBooleanMethodA
import jvmapi.jclass
import jvmapi.jobject
import kotlinx.cinterop.free
import kotlinx.cinterop.nativeHeap

class CallJavaBooleanMethod(
    jO: jobject?,
    javaClass: jclass?,
    methodName: String,
    methodSignature: String
) : JavaMethod(jO, javaClass, methodName, methodSignature) {
    @ExperimentalUnsignedTypes
    override fun invoke(vararg raw: X): Boolean {
        val arguments = toJObjectArray(raw)

        val value =CallBooleanMethodA(jO, getMethod(), arguments)
        nativeHeap.free(arguments)
        return value != 0.toUByte()
    }


}