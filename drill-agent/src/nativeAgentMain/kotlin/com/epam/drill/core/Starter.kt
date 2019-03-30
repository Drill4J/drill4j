package com.epam.drill.core


import com.epam.drill.api.enableJvmtiEventClassLoad
import com.epam.drill.api.enableJvmtiEventNativeMethodBind
import com.epam.drill.api.enableJvmtiEventVmStart
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
fun agentOnLoad(vmPointer: CPointer<JavaVMVar>, options: CPointer<ByteVar>?, reservedPtr: Long): jint = runBlocking {
    try {
        agentSetup(vmPointer.pointed.value)
        saveVmToGlobal(vmPointer)

        val args = options?.toKString()?.split(",")?.associate {
            val split = it.split("=")
            split[0] to split[1]
        }!!
        config.di = StableRef.create(DI()).asCPointer()
        if (args["drillInstallationDir"] == null) {
            println("________________________________________________________________")
            println(
                "NATIVE WARNING: Please fill the config folder in agent args.\n" +
                        "Drill is OFF.\n"
            )
            println("________________________________________________________________")
            return@runBlocking JNI_OK
        }
        config.drillInstallationDir = args.getValue("drillInstallationDir").cstr.getPointer(Arena())
        intiLoggers()
        parseConfigs()
        val logger = DLogger("StartLogger")
        logger.info { agentInfo.adminUrl }
        logger.info { "The native agent was loaded" }
        logger.info { "Pid is: " + getpid() }
        printAllowedCapabilities()
        createQueue()
        createClassLoadingQueue()
        AddCapabilities(GetPotentialCapabilities())
        AddToSystemClassLoaderSearch("${args["drillInstallationDir"]}/drillRuntime.jar")
        SetNativeMethodPrefix("xxx_")
        callbackRegister()
        SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, null)
        SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, null)

    } catch (ex: Throwable) {
        ex.printStackTrace()
    }
    JNI_OK

}


private fun intiLoggers() {
    val sr = StableRef.create(mutableMapOf<String, Logger>())
    loggers.logs = sr.asCPointer()
}

private fun callbackRegister() {
    generateDefaultCallbacks().useContents {
        SetEventCallbacks(this.ptr, sizeOf<jvmtiEventCallbacks>().toInt())
        null
    }
//    gjavaVMGlob?.pointed?.callbackss?.VMInit = initVmEvent
//    gjavaVMGlob?.pointed?.callbackss?.NativeMethodBind = staticCFunction { x1, x2, x3, x4, x5, x6
//        ->
//        initRuntimeIfNeeded()
//
//    }
//    gjavaVMGlob?.pointed?.callbackss?.MethodEntry = staticCFunction(::x)
    gjavaVMGlob?.pointed?.callbackss?.ClassFileLoadHook = staticCFunction(::classLoadEvent)
    gjavaVMGlob?.pointed?.callbackss?.VMStart = staticCFunction(::x)
    SetEventCallbacks(gjavaVMGlob?.pointed?.callbackss?.ptr, sizeOf<jvmtiEventCallbacks>().toInt())
//    enableJvmtiEventNativeMethodBind()
    enableJvmtiEventClassLoad()
    enableJvmtiEventVmStart()
    enableJvmtiEventNativeMethodBind()
}

@Suppress("UNUSED_PARAMETER", "UNUSED")
@CName("Agent_OnUnload")
fun agentOnUnload(vmPtr: Long) {

}


fun x(
    x1: kotlinx.cinterop.CPointer<jvmapi.jvmtiEnvVar>?,
    x2: kotlinx.cinterop.CPointer<jvmapi.JNIEnvVar>?
) {
    println("VM IS START")
}