package com.epam.kjni.core.util

import com.epam.kjni.core.GlobState
import jvmapi.jclass
import jvmapi.jobject
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.free
import kotlinx.cinterop.invoke
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.pointed
import kotlinx.cinterop.value

class CallJavaVoidMethod(
    jO: jobject?,
    javaClass: jclass?,
    methodName: CPointer<ByteVar>,
    methodSignature: CPointer<ByteVar>
) : JavaMethod(jO, javaClass, methodName, methodSignature) {
    override fun invoke(vararg raw: X) {
        val arguments = toJObjectArray(raw)
        val jniEnv = GlobState.env?.pointed?.value?.pointed
        val p3 = getMethod()
        jniEnv!!.CallVoidMethodA!!(GlobState.env, jO, p3, arguments)
        nativeHeap.free(arguments)
    }


}