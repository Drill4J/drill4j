@file:Suppress("unused")

package com.epam.drill.core.callbacks.vminit

import com.epam.drill.core.*
import com.epam.drill.core.ws.*
import jvmapi.*
import kotlinx.cinterop.*

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

