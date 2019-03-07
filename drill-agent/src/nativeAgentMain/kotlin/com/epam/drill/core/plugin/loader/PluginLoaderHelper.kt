package com.epam.drill.core.plugin.loader

import com.epam.drill.*
import com.epam.drill.common.PluginBean
import com.epam.drill.plugin.PluginManager
import com.epam.drill.plugin.api.processing.PluginRepresenter
import com.epam.drill.plugin.api.processing.Reason
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
    val targetClass = "Lcom/epam/drill/plugin/api/processing/AgentPart;"
    var parentClass = GetSuperclass(findClass)
    var name = alloc<CPointerVar<ByteVar>>()
    GetClassSignature(parentClass, name.ptr, null)
    var isApiClassFound = false
    while (parentClass != null) {
        val toKString = name.value?.toKString()
        if (toKString == targetClass) {
            isApiClassFound = true
            break
        }

        parentClass = GetSuperclass(parentClass)
        name = alloc()
        GetClassSignature(parentClass, name.ptr, null)
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
class NativePluginController(pf: DrillPluginFile) : PluginRepresenter() {
    private val pluginApiClass: jclass = pf.retrievePluginApiClass()
    private val pluginConfig: PluginBean = pf.pluginConfig()
    override val id: String = pf.pluginId()

    private var userPlugin: jobject =
        NewObjectA(
            pluginApiClass,
            GetMethodID(pluginApiClass, "<init>", "(Ljava/lang/String;)V"),
            nativeHeap.allocArray(1.toLong()) {
                l = NewStringUTF(id)
            })!!


    init {
        updateRawConfig(pluginConfig)
        plLogger.warn { "config updated: ${pf.rawPluginConfig()}" }
        fullLoad(pf)
    }

    override fun on() {
        PluginManager.activate(id)
        CallVoidMethodA(
            userPlugin, GetMethodID(pluginApiClass, "on", "()V"), null
        )
    }

    override fun off() {
        PluginManager.deactivate(id)
        CallVoidMethodA(
            userPlugin, GetMethodID(pluginApiClass, "off", "()V"), null
        )
    }


    @ExperimentalUnsignedTypes
    override fun load(onImmediately: Boolean) {
        PluginManager.activate(id)
        CallVoidMethodA(
            userPlugin, GetMethodID(pluginApiClass, "load", "(Z)V"), nativeHeap.allocArray(1.toLong()) {
                z = if (onImmediately) 1.toUByte() else 0.toUByte()
            })

    }

    override fun unload(reason: Reason) {
        PluginManager.deactivate(id)
//        CallVoidMethodA(
//            fixme!!!!
//            userPlugin, GetMethodID(pluginApiClass, "unload", "()V"), null
//        )
        np?.unload(reason)
    }

    @ExperimentalUnsignedTypes
    fun fullLoad(jar: DrillPluginFile) {
        load(pluginConfig.enabled)
        initNativePart(jar)
        np?.updateRawConfig(pluginConfig)
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
            GetMethodID(pluginApiClass, "updateRawConfig", "(Ljava/lang/String;)V"),
            nativeHeap.allocArray(1.toLong()) {
                val newStringUTF =
                    NewStringUTF(config.config)
                l = newStringUTF

            })
        ExceptionDescribe()
    }


}
