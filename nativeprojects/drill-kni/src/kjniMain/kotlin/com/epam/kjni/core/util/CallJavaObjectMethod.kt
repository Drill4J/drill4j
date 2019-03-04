package com.epam.kjni.core.util

import jvmapi.CallObjectMethodA
import jvmapi.jclass
import jvmapi.jobject
import kotlinx.cinterop.free
import kotlinx.cinterop.nativeHeap

class CallJavaObjectMethod(
    jO: jobject?,
    javaClass: jclass?,
    methodName: String,
    methodSignature: String
) : JavaMethod(jO, javaClass, methodName, methodSignature) {
    override fun invoke(vararg raw: X): jobject {
        val arguments = toJObjectArray(raw)

        val value = CallObjectMethodA(jO, getMethod(), arguments)
        nativeHeap.free(arguments)

        //fixme can be null reference
        return value!!
    }


}