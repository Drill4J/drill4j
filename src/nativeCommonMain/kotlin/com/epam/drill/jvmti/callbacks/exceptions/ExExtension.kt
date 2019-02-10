package com.epam.drill.jvmti.callbacks.exceptions

import com.epam.drill.toKString
import com.epam.kjni.core.GlobState
import jvmapi.GetClassSignature
import jvmapi.jstring
import jvmapi.jthrowable
import kotlinx.cinterop.*

fun jthrowable.getMessage() = memScoped {
    val throwableClass = GlobState.jni.FindClass?.invoke(GlobState.env, "java/lang/Throwable".cstr.getPointer(this))
    val methodID = GlobState.jni.GetMethodID?.invoke(
        GlobState.env, throwableClass,
        "getMessage".cstr.getPointer(this),
        "()Ljava/lang/String;".cstr.getPointer(this)
    )
    val message: jstring? = GlobState.jni.CallObjectMethodV?.invoke(GlobState.env, this@getMessage, methodID, null)
    message?.toKString()
}


fun jthrowable.getType() = memScoped {
    val getObjectClass = GlobState.jni.GetObjectClass?.invoke(GlobState.env, this@getType)
    val name = this.alloc<CPointerVar<ByteVar>>()
    GetClassSignature(getObjectClass, name.ptr, null)
    name.value?.toKString()?.replace("/", ".")?.dropLast(1)?.drop(1) ?: "UnknownExType"

}