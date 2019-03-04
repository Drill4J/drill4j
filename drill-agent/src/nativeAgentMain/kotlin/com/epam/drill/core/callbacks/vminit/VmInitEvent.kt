@file:Suppress("unused")

package com.epam.drill.core.callbacks.vminit

import com.epam.drill.core.ws.startWs
import com.epam.drill.logger.DLogger
import com.soywiz.klogger.Logger
import jvmapi.JNIEnvVar
import jvmapi.jthread
import jvmapi.jvmtiEnvVar
import kotlinx.cinterop.CPointer
import kotlinx.coroutines.runBlocking


val initLogger: Logger
    get() = DLogger("initLogger")

@Suppress("UNUSED_PARAMETER")
@CName("jvmtiEventVMInitEvent")
fun jvmtiEventVMInitEvent(env: CPointer<jvmtiEnvVar>?, jniEnv: CPointer<JNIEnvVar>?, thread: jthread?) = runBlocking {
    startWs()
}

