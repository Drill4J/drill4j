@file:Suppress("unused")

package com.epam.drill.core.callbacks.vminit

import com.epam.drill.core.exec
import com.epam.drill.core.ws.startWs
import com.epam.drill.logger.DLogger
import com.epam.drill.logger.Logger
import com.soywiz.klock.DateTime
import com.soywiz.korio.lang.Thread_sleep
import jvmapi.JNIEnvVar
import jvmapi.JNI_GetDefaultJavaVMInitArgs
import jvmapi.jmm_interface
import jvmapi.jthread
import jvmapi.jvmtiEnvVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.invoke
import kotlinx.cinterop.pointed


val initLogger: Logger
    get() = DLogger("initLogger")

@ExperimentalUnsignedTypes
@Suppress("UNUSED_PARAMETER")
@CName("jvmtiEventVMInitEvent")
fun jvmtiEventVMInitEvent(env: CPointer<jvmtiEnvVar>?, jniEnv: CPointer<JNIEnvVar>?, thread: jthread?) {
    startWs()

//    val getInputArguments = jmm_interface?.pointed?.GetInputArguments?.invoke(jniEnv)
//println(getInputArguments)

//    println("WM IS INIT")
//    val timeoutSec = 60
//    val startTime = DateTime.now()
//    val pluginId = "coverage"
//
//
//    while (exec { needSync });
//        while (exec { pInstrumentedStorage[pluginId] } == null) {
//        if (DateTime.now().minus(startTime).seconds > timeoutSec) {
//            println("Can't loaded the $pluginId plugin in $timeoutSec seconds")
//            break
//        }
//        Thread_sleep(1000)
//    }
}

