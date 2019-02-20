package com.epam.kjni.core

import jvmapi.*
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.CPointerVarOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value

import kotlin.native.concurrent.ThreadLocal

@kotlin.native.concurrent.ThreadLocal
object GlobState {
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

    val jni: JNI
        get() = env.pointed.pointed!!


}


@ThreadLocal
var ex: JNIEnvPointer? = null

typealias JNIEnvPointer = CPointer<JNIEnvVar>
typealias JNI = JNINativeInterface_
