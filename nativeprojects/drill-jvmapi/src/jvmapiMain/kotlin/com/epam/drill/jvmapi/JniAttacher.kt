package com.epam.drill.jvmapi

import jvmapi.*
import kotlinx.cinterop.*
import kotlin.native.concurrent.ThreadLocal

@CName("currentEnvs")
fun currentEnvs(): JNIEnvPointer {
    return env
}

@kotlin.native.concurrent.ThreadLocal
val env: JNIEnvPointer
    get() {
        return if (ex != null) {
            ex!!
        } else {
            memScoped {
                val vms = gjavaVMGlob?.pointed?.jvm!!
                val vmFns = vms.pointed.value!!.pointed
                val jvmtiEnvPtr = alloc<CPointerVar<JNIEnvVar>>()
                vmFns.AttachCurrentThread!!(vms, jvmtiEnvPtr.ptr.reinterpret(), null)
                val value: CPointer<CPointerVarOf<JNIEnv>>? = jvmtiEnvPtr.value
                ex = value
                JNI_VERSION_1_6
                value!!
            }
        }
    }

@kotlin.native.concurrent.ThreadLocal
val jni: JNI
    get() = env.pointed.pointed!!


@ThreadLocal
var ex: JNIEnvPointer? = null

typealias JNIEnvPointer = CPointer<JNIEnvVar>
typealias JNI = JNINativeInterface_