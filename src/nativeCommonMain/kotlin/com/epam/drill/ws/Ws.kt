package com.epam.drill.ws


import com.epam.drill.toKString
import jvmapi.JNIEnv
import jvmapi.jobject
import jvmapi.jstring
import kotlinx.coroutines.runBlocking

actual object Ws {


    @Suppress("UNUSED_PARAMETER", "unused")
    @CName("Java_com_epam_drill_ws_Ws_send")
    fun send(env: JNIEnv, thisObject: jobject, data: jstring?) = runBlocking {
        initRuntimeIfNeeded()
//        drillWs?.send(data?.toKString() ?: "")
//    println("privv???")
    }

}
