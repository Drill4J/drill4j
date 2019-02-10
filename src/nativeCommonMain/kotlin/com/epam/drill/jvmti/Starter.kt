package com.epam.drill.jvmti

import com.epam.drill.jvmti.logger.DLogger
import com.epam.drill.jvmti.util.enableJvmtiEventNativeMethodBind
import com.epam.drill.jvmti.util.printAllowedCapabilities
import com.epam.drill.jvmti.ws.startWs
import com.soywiz.klogger.Logger
import jvmapi.*
import kotlinx.cinterop.*
import kotlinx.coroutines.runBlocking
import platform.posix.getpid


@ExperimentalUnsignedTypes
@Suppress("UNUSED_PARAMETER", "UNUSED")
@CName("Agent_OnLoad")
fun agentOnLoad(vmPointer: CPointer<JavaVMVar>, options: CPointer<ByteVar>?, reservedPtr: Long): jint = runBlocking {
    try {
        agentSetup(vmPointer.pointed.value)
        saveVmToGlobal(vmPointer)
        intiLoggers()
        val args = options?.toKString()?.split(",")?.associate {
            val split = it.split("=")
            split[0] to split[1]
        }!!
        AgentInfo.parseConfigs(args["configsFolder"])
        val logger = DLogger("StartLogger")
        logger.info { drillAdminUrl }
        logger.info { "The native agent was loaded" }
        logger.info { "Pid is: " + getpid() }
        printAllowedCapabilities()
        createQueue()
        startWs()

        AddCapabilities(GetPotentialCapabilities())

        AddToSystemClassLoaderSearch("${args["distrFolder"]}/drillRuntime.jar")
        AddToSystemClassLoaderSearch("${args["distrFolder"]}/../composite/spring-petclinic-kotlin/stuff/.drill/except-ions/agent-part.jar")
        SetNativeMethodPrefix("xxx_")
        callbackRegister()
        SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, null)
        SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, null)


        enableJvmtiEventNativeMethodBind()

    } catch (ex: Throwable) {
        ex.printStackTrace()
    }
    JNI_OK

}


private fun intiLoggers() {
    val sr = StableRef.create(mutableMapOf<String, Logger>())
    config.loggers = sr.asCPointer()


}

private fun callbackRegister() {
    generateDefaultCallbacks().useContents {
        SetEventCallbacks(this.ptr, sizeOf<jvmtiEventCallbacks>().toInt())
        null
    }
}

@Suppress("UNUSED_PARAMETER", "UNUSED")
@CName("Agent_OnUnload")
fun agentOnUnload(vmPtr: Long) {
//todo deallocate all and dispose the stableRefs
}
