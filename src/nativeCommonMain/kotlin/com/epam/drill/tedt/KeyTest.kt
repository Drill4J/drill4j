package com.epam.drill.tedt


import jvmapi.JNIEnvVar
import jvmapi.jobject
import kotlinx.cinterop.CPointer
import kotlinx.coroutines.runBlocking

@Suppress("unused", "UNUSED_PARAMETER")
@kotlin.native.CName("Java_com_epam_kjni_testdata_DrillNativeManager_returnInteger")
fun returnInteger(env: CPointer<JNIEnvVar>, obj: jobject): jobject = runBlocking {
    initRuntimeIfNeeded()


    java.lang.Integer(129).javaObject
}