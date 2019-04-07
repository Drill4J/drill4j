package com.epam.drill.core

import com.epam.drill.api.enableJvmtiEventClassFileLoadHook
import com.epam.drill.api.enableJvmtiEventNativeMethodBind
import com.epam.drill.api.enableJvmtiEventVmInit
import com.epam.drill.core.callbacks.classloading.classLoadEvent
import com.epam.drill.jvmapi.printAllowedCapabilities
import com.epam.drill.logger.DLogger
import com.soywiz.klogger.Logger
import drillInternal.config
import drillInternal.createClassLoadingQueue
import drillInternal.createQueue
import jvmapi.*
import kotlinx.cinterop.*
import kotlinx.coroutines.runBlocking
import platform.posix.getpid
import storage.loggers

@ExperimentalUnsignedTypes
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
        val drillInstallationDir = this.getValue("drillInstallationDir")
        initCGlobals(CConfig(drillInstallationDir))
        initLoggers()
        parseConfigs()
        createQueue()

        printAllowedCapabilities()
        AddCapabilities(GetPotentialCapabilities())
        AddToSystemClassLoaderSearch("$drillInstallationDir/drillRuntime.jar")
        SetNativeMethodPrefix("xxx_")
        callbackRegister()
        val logger = DLogger("StartLogger")
        logger.info { agentInfo.adminUrl }
        logger.info { "The native agent was loaded" }
        logger.info { "Pid is: " + getpid() }
    }
}


fun initCGlobals(cConfig: CConfig) {
    config.di = StableRef.create(DI()).asCPointer()
    config.drillInstallationDir = cConfig.drillInstallationDir.cstr.getPointer(Arena())
}

private fun printStartWarning() {
    println("________________________________________________________________")
    println(
        "NATIVE WARNING: Please fill the config folder in agent args.\n" +
                "Drill is OFF.\n"
    )
    println("________________________________________________________________")
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

 fun initLoggers() {
    loggers.logs = StableRef.create(mutableMapOf<String, Logger>()).asCPointer()
}

@ExperimentalUnsignedTypes
private fun callbackRegister() {
    generateDefaultCallbacks().useContents {
        SetEventCallbacks(this.ptr, sizeOf<jvmtiEventCallbacks>().toInt())
        null
    }
    gjavaVMGlob?.pointed?.callbackss?.ClassFileLoadHook = staticCFunction(::classLoadEvent)
    SetEventCallbacks(gjavaVMGlob?.pointed?.callbackss?.ptr, sizeOf<jvmtiEventCallbacks>().toInt())
    enableJvmtiEventVmInit()
    enableJvmtiEventClassFileLoadHook()
    enableJvmtiEventNativeMethodBind()
}

data class CConfig(val drillInstallationDir: String)