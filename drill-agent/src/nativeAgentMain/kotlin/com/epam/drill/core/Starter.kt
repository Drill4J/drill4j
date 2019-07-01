package com.epam.drill.core

import com.epam.drill.api.enableJvmtiEventClassFileLoadHook
import com.epam.drill.api.enableJvmtiEventNativeMethodBind
import com.epam.drill.api.enableJvmtiEventVmDeath
import com.epam.drill.api.enableJvmtiEventVmInit
import com.epam.drill.common.AgentConfig
import com.epam.drill.jvmapi.printAllowedCapabilities
import com.epam.drill.logger.DLogger
import jvmapi.AddCapabilities
import jvmapi.AddToSystemClassLoaderSearch
import jvmapi.GetPotentialCapabilities
import jvmapi.JNIEnvVar
import jvmapi.JNI_OK
import jvmapi.JavaVMVar
import jvmapi.SetEventCallbacks
import jvmapi.SetNativeMethodPrefix
import jvmapi.agentSetup
import jvmapi.generateDefaultCallbacks
import jvmapi.gjavaVMGlob
import jvmapi.jint
import jvmapi.jvmtiEnvVar
import jvmapi.jvmtiEventCallbacks
import jvmapi.saveVmToGlobal
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.useContents
import kotlinx.cinterop.value
import platform.posix.getpid
import kotlin.native.concurrent.freeze


@Suppress("UNUSED_PARAMETER", "UNUSED")
@CName("Agent_OnLoad")
fun agentOnLoad(vmPointer: CPointer<JavaVMVar>, options: String, reservedPtr: Long): jint {
    try {
        initAgentGlobals(vmPointer)
        runAgent(options)
    } catch (ex: Throwable) {
        println("Can't load the agent. Ex: ${ex.message}")
    }
    return JNI_OK
}

@Jvmapi
private fun initAgentGlobals(vmPointer: CPointer<JavaVMVar>) {
    agentSetup(vmPointer.pointed.value)
    saveVmToGlobal(vmPointer)
}

private fun runAgent(options: String?) {
    options.asAgentParams().apply {
        val adminAddress = this.getValue("adminAddress")
        val agentId = this.getValue("agentId")
        val drillInstallationDir = this.getValue("drillInstallationDir")
        exec { this.drillInstallationDir = drillInstallationDir }
        exec { this.agentConfig = AgentConfig(agentId, adminAddress) }
        setUnhandledExceptionHook({ x: Throwable ->
            println("unhandled event $x")
        }.freeze())

        printAllowedCapabilities()
        AddCapabilities(GetPotentialCapabilities())
        AddToSystemClassLoaderSearch("$drillInstallationDir/drillRuntime.jar")
        SetNativeMethodPrefix("xxx_")
        callbackRegister()
        val logger = DLogger("StartLogger")
        logger.info { "The native agent was loaded" }
        logger.info { "Pid is: " + getpid() }
    }
}

fun String?.asAgentParams(): Map<String, String> {
    if (this.isNullOrEmpty()) return mutableMapOf()
    return try {
        this.split(",").associate {
            val split = it.split("=")
            split[0] to split[1]
        }
    } catch (parseException: Exception) {
        throw IllegalArgumentException("wrong agent parameters: $this")
    }
}

private fun callbackRegister() {
    generateDefaultCallbacks().useContents {
        SetEventCallbacks(this.ptr, sizeOf<jvmtiEventCallbacks>().toInt())
        null
    }
    SetEventCallbacks(gjavaVMGlob?.pointed?.callbackss?.ptr, sizeOf<jvmtiEventCallbacks>().toInt())
    gjavaVMGlob?.pointed?.callbackss?.VMDeath = staticCFunction(::vmDeathEvent)
    enableJvmtiEventVmDeath()
    enableJvmtiEventVmInit()
    enableJvmtiEventClassFileLoadHook()
    enableJvmtiEventNativeMethodBind()
}


@Suppress("UNUSED_PARAMETER")
fun vmDeathEvent(jvmtiEnv: CPointer<jvmtiEnvVar>?, jniEnv: CPointer<JNIEnvVar>?) {
    DLogger("vmDeathEvent").info { "vmDeathEvent" }
}


@Suppress("UNUSED_PARAMETER", "UNUSED")
@CName("Agent_OnUnload")
fun agentOnUnload(vmPointer: CPointer<JavaVMVar>) {
    DLogger("Agent_OnUnload").info { "Agent_OnUnload" }
}


val drillInstallationDir: String
    get() = exec { drillInstallationDir }