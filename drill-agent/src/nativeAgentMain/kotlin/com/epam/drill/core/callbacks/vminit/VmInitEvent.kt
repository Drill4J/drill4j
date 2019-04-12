@file:Suppress("unused")

package com.epam.drill.core.callbacks.vminit

import com.epam.drill.core.di
import com.epam.drill.core.ws.startWs
import com.epam.drill.logger.DLogger
import com.soywiz.klock.DateTime
import com.soywiz.klogger.Logger
import com.soywiz.korio.lang.Thread_sleep
import jvmapi.JNIEnvVar
import jvmapi.jthread
import jvmapi.jvmtiEnvVar
import kotlinx.cinterop.CPointer
import kotlinx.coroutines.runBlocking


val initLogger: Logger
    get() = DLogger("initLogger")

@ExperimentalUnsignedTypes
@Suppress("UNUSED_PARAMETER")
@CName("jvmtiEventVMInitEvent")
fun jvmtiEventVMInitEvent(env: CPointer<jvmtiEnvVar>?, jniEnv: CPointer<JNIEnvVar>?, thread: jthread?) = runBlocking {
    startWs()
    println("WM IS INIT")
    val timeoutSec = 60
    val startTime = DateTime.now()
    val pluginId = "coverage"
    while (di { pInstrumentedStorage }[pluginId] == null) {
        if (DateTime.now().minus(startTime).seconds > timeoutSec) {
            println("Can't loaded the $pluginId plugin in $timeoutSec seconds")
            break
        }
        Thread_sleep(1000)
    }
}

