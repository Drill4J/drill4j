package com.epam.kjni.core.util

import jvmapi.CallVoidMethodA
import jvmapi.jclass
import jvmapi.jobject
import kotlinx.cinterop.free
import kotlinx.cinterop.nativeHeap

class CallJavaVoidMethod(
    jO: jobject?,
    javaClass: jclass?,
    methodName: String,
    methodSignature: String
) : JavaMethod(jO, javaClass, methodName, methodSignature) {
    override fun invoke(vararg raw: X) {
        val arguments = toJObjectArray(raw)

        val p3 = getMethod()
        CallVoidMethodA(jO, p3, arguments)
        nativeHeap.free(arguments)
    }


}