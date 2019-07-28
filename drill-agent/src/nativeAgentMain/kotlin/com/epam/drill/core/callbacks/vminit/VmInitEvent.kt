@file:Suppress("unused")

package com.epam.drill.core.callbacks.vminit

import com.epam.drill.core.agent.*
import com.epam.drill.core.ws.*
import com.epam.drill.jvmapi.gen.*
import kotlinx.cinterop.*
import kotlin.native.concurrent.*

@Suppress("UNUSED_PARAMETER")
@CName("jvmtiEventVMInitEvent")
fun jvmtiEventVMInitEvent(env: CPointer<jvmtiEnvVar>?, jniEnv: CPointer<JNIEnvVar>?, thread: jthread?) {
    wsThread.execute(TransferMode.UNSAFE, {}) {
        calculateBuildVersion()
        startWs()
    }
}

