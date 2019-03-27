package com.epam.drill.core.plugin.loader

import com.epam.drill.*
import com.epam.drill.common.PluginBean
import com.epam.drill.core.agentInfo
import com.epam.drill.core.plugin.dumpConfigToFileSystem
import com.epam.drill.core.plugin.pluginConfigById
import com.epam.drill.jvmapi.jniName
import com.epam.drill.jvmapi.jniParamName
import com.epam.drill.logger.DLogger
import com.epam.drill.plugin.PluginManager
import com.epam.drill.plugin.api.processing.AgentPart
import com.epam.drill.plugin.api.processing.PluginRepresenter
import com.epam.drill.plugin.api.processing.UnloadReason
import com.soywiz.korio.file.baseName
import com.soywiz.korio.file.std.localVfs
import jvmapi.*
import kotlinx.cinterop.*
import kotlinx.coroutines.runBlocking

fun DrillPluginFile.retrievePluginApiClass() = runBlocking {
    var pluginApiClass: jclass? = null

    this@retrievePluginApiClass.iterateThroughtPluginClasses { findClass ->

        if (com.epam.drill.core.plugin.loader.isSuitablePluginClass(findClass))
            pluginApiClass = findClass

    }

    pluginApiClass ?: throw com.epam.drill.core.exceptions.PluginLoadException("Can't find the plugin API class.")
    pluginApiClass!!
}


fun isSuitablePluginClass(findClass: jclass) = memScoped {
    val targetClass = AgentPart::class.jniParamName()
    var isApiClassFound = false
    var parentClass = GetSuperclass(findClass)
    ExceptionClear()
    if (parentClass != null) {
        var name = alloc<CPointerVar<ByteVar>>()
        GetClassSignature(parentClass, name.ptr, null)
        ExceptionClear()
        while (name.value?.toKString() != "Ljava/lang/Object;") {
            val toKString = name.value?.toKString()
            if (toKString == targetClass) {
                isApiClassFound = true
                break
            }

            parentClass = GetSuperclass(parentClass)
            ExceptionClear()
            name = alloc()
            GetClassSignature(parentClass, name.ptr, null)
            ExceptionClear()
        }
    }
    isApiClassFound
}

fun DrillPluginFile.addPluginsToSystemClassLoader() {
    val segment = this.absolutePath
    AddToSystemClassLoaderSearch(segment)
    plLogger.warn { "System classLoader extends by '$segment' path" }
}

suspend fun DrillPluginFile.retrieveFacilitiesFromPlugin() {
    plLogger.warn { "try to unpack jars" }
    if (!this.parent["static"].exists())
        this.extractPluginFacilitiesTo(localVfs(this.parent.absolutePath)) { vf ->
            !vf.baseName.contains("nativePart") &&
                    !vf.baseName.contains("static")
        }
}

@ExperimentalUnsignedTypes
open class NativePluginController(pf: DrillPluginFile) : PluginRepresenter() {
    val natPluginLogger
        get() = DLogger("NativePluginController")

    val pluginApiClass: jclass = pf.retrievePluginApiClass()
    private val pluginConfig: PluginBean = pf.pluginConfig()
    override val id: String = pf.pluginId()

    override var enabled: Boolean
        get() = pluginConfigById(id).enabled
        set(value) {
            if (value) on() else off()
            val pluginConfigById = pluginConfigById(id)
            pluginConfigById.enabled = value
            pluginConfigById.dumpConfigToFileSystem()
        }

    var userPlugin: jobject =
        NewObjectA(
            pluginApiClass,
            GetMethodID(pluginApiClass, "<init>", "(Ljava/lang/String;)V"),
            nativeHeap.allocArray(1.toLong()) {
                l = NewStringUTF(id)
            })!!


    init {
        updateRawConfig(pluginConfig)
        natPluginLogger.warn { "config updated: ${pf.rawPluginConfig()}" }
        fullLoad(pf)
    }

    override fun on() {
        natPluginLogger.debug { "on" }
        CallVoidMethodA(
            userPlugin, GetMethodID(pluginApiClass, AgentPart<*>::on.name, "()V"), null
        )
        np?.on()
    }

    override fun off() {
        natPluginLogger.debug { "off" }
        CallVoidMethodA(
            userPlugin, GetMethodID(pluginApiClass, AgentPart<*>::off.name, "()V"), null
        )
        np?.off()
    }


    @ExperimentalUnsignedTypes
    override fun load(onImmediately: Boolean) {
        CallVoidMethodA(
            userPlugin,
            GetMethodID(pluginApiClass, AgentPart<*>::load.name, "(Z)V"),
            nativeHeap.allocArray(1.toLong()) {
                z = if (onImmediately) 1.toUByte() else 0.toUByte()
            })

    }

    override fun unload(unloadReason: UnloadReason) = memScoped<Unit> {
        val findClass = FindClass(UnloadReason::class.jniName())
        val getStaticFieldID =
            GetStaticFieldID(findClass, unloadReason.name, UnloadReason::class.jniParamName())
        val getStaticObjectField = GetStaticObjectField(findClass, getStaticFieldID)
        CallVoidMethodA(
            userPlugin,
            GetMethodID(pluginApiClass, AgentPart<*>::unload.name, "(${UnloadReason::class.jniParamName()})V"),
            allocArray(1.toLong()) {
                l = getStaticObjectField
            }
        )
        np?.unload(unloadReason)
    }

    @ExperimentalUnsignedTypes
    fun fullLoad(jar: DrillPluginFile) {

        //fixme raw hack only for native plguins... ohhh.. kill me
        PluginManager.addPlugin(this)

        load(pluginConfig.enabled && agentInfo.isEnable)
        initNativePart(jar)
        try {
            np?.updateRawConfig(pluginConfig)
        } catch (ex: Exception) {
            natPluginLogger.error { "Can't update the config for $id. Config: $pluginConfig" }
        }
        try {
            np?.load(pluginConfig.enabled && agentInfo.isEnable)
        } catch (ex: Exception) {
            natPluginLogger.error { "Can't Load the native part for $id. Immedeatly: ${pluginConfig.enabled && agentInfo.isEnable}" }
        }
    }

    private fun initNativePart(jar: DrillPluginFile) {
        if (jar.hasNativePart())
            CallVoidMethodA(
                userPlugin,
                GetMethodID(pluginApiClass, "init", "(Ljava/lang/String;)V"),
                nativeHeap.allocArray(1.toLong()) {

                    val newStringUTF =
                        NewStringUTF(jar.nativePart().absolutePath)
                    l = newStringUTF

                })
    }

    override fun updateRawConfig(config: PluginBean) {
        notifyJavaPart(config)
    }

    private fun notifyJavaPart(config: PluginBean) {
        CallVoidMethodA(
            userPlugin,
            GetMethodID(pluginApiClass, AgentPart<*>::updateRawConfig.name, "(Ljava/lang/String;)V"),
            nativeHeap.allocArray(1.toLong()) {
                val newStringUTF =
                    NewStringUTF(config.config)
                l = newStringUTF

            })
        ExceptionDescribe()
    }


}

@ExperimentalUnsignedTypes
class Instrumented(pf: DrillPluginFile) : NativePluginController(pf) {

    var qs: jmethodID? = null

    init {
        qs = GetMethodID(pluginApiClass, "instrument", "(Ljava/lang/String;[B)[B")
    }

    fun instrument(className: String, x1: jbyteArray): jobject? {
        val callObjectMethodA =
            CallObjectMethod(userPlugin, qs, NewStringUTF(className), x1)

        ExceptionDescribe()
        return callObjectMethodA

    }

    fun doRawAction(action: String) {
        CallVoidMethod(
            userPlugin,
            GetMethodID(pluginApiClass, "doRawAction", "(Ljava/lang/String;)V"),
            NewStringUTF(action)
        )
        ExceptionDescribe()
    }
}
