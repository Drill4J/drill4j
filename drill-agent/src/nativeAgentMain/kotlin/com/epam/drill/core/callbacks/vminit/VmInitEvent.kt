@file:Suppress("unused")

package com.epam.drill.core.callbacks.vminit

import com.epam.drill.core.exec
import com.epam.drill.core.ws.startWs
import com.epam.drill.logger.DLogger
import com.epam.drill.logger.Logger
import jvmapi.CallIntMethod
import jvmapi.FindClass
import jvmapi.GetMethodID
import jvmapi.GetStaticFieldID
import jvmapi.GetStaticObjectField
import jvmapi.JNIEnvVar
import jvmapi.jfieldID
import jvmapi.jmethodID
import jvmapi.jobject
import jvmapi.jthread
import jvmapi.jvmtiEnvVar
import kotlinx.cinterop.CPointer


val initLogger: Logger
    get() = DLogger("initLogger")

@Suppress("UNUSED_PARAMETER")
@CName("jvmtiEventVMInitEvent")
fun jvmtiEventVMInitEvent(env: CPointer<jvmtiEnvVar>?, jniEnv: CPointer<JNIEnvVar>?, thread: jthread?) {

    val initializerClass = FindClass("com/epam/drill/ws/Initializer")
    val selfMethodId: jfieldID? = GetStaticFieldID(initializerClass, "INSTANCE", "Lcom/epam/drill/ws/Initializer;")
    val initializer: jobject? = GetStaticObjectField(initializerClass, selfMethodId)
    val calculateBuild: jmethodID? = GetMethodID(initializerClass, "calculateBuild", "()I")
    val buildVersion = CallIntMethod(initializer, calculateBuild)
    exec { agentConfig.buildVersion = buildVersion.toString() }

    startWs()
}

