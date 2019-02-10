package com.epam.kjni.core


import jvmapi.JNI_VERSION_1_6
import jvmapi.JavaVMVar
import jvmapi.saveVmToGlobal
import kotlinx.cinterop.CPointer


@Suppress("FunctionName", "UNUSED_PARAMETER", "unused")
@CName("JNI_OnLoad")
fun jniOnLoad(vm: CPointer<JavaVMVar>, reservedPtr: Long): Int {
    saveVmToGlobal(vm)
    return JNI_VERSION_1_6
}




