@file:Suppress("unused", "UNUSED_PARAMETER")

package com.epam.drill.jvmti.callbacks.methodexit

import com.epam.drill.jvmti.logger.DLogger
import com.epam.drill.jvmti.util.getDeclaringClassName
import com.epam.drill.jvmti.util.getName
import jvmapi.*
import kotlinx.cinterop.memScoped

val logger
    get() = DLogger("jvmtiEventMethodExitEvent")

@kotlin.native.CName("jvmtiEventMethodExitEvent")
fun methodExitEvent(
    jvmtiEnv: jvmtiEnv?,
    jniEnv: JNIEnv?,
    thread: jthread?,
    method: jmethodID?,
    wasPoppedByException: jboolean,
    returnValue: jvalue
) = memScoped {
    val declaringClassName = method?.getDeclaringClassName()
    if (declaringClassName!!.contains("Applicationa") || declaringClassName == "Lorg/somevendor/petproject/test/A")
        logger.debug { declaringClassName + "." + method.getName() }
//    val bytecode_count_ptr = mutex<jintVar>()
//    val bytecodes_ptr = mutex<CPointerVar<UByteVar>>()
//    GetBytecodes(method, bytecode_count_ptr.ptr, bytecodes_ptr.ptr)
//    val value = bytecodes_ptr.value

}