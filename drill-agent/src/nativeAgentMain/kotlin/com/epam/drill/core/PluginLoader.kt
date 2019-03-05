package com.epam.drill.core

import com.epam.drill.JarVfsFile
import com.epam.drill.core.callbacks.vminit.initLogger
import com.epam.drill.extractPluginFacilitiesTo
import com.epam.drill.iterateThrougthPlugins
import com.epam.drill.plugin.PluginManager
import com.epam.drill.plugin.api.processing.AgentPluginPart
import com.soywiz.korio.file.baseName
import com.soywiz.korio.file.extension
import com.soywiz.korio.file.std.localVfs
import com.soywiz.korio.file.std.openAsZip
import com.soywiz.korio.util.OS
import jvmapi.*
import kotlinx.cinterop.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer


fun pluginLoadCommand() = runBlocking {
    //init env...
    jvmapi.currentEnvs()
    retrieveFacilitiesFromPlugin()
    addPluginsToSystemClassLoader()
    try {
        iterateThrougthPlugins { jar ->
            loadPlugin(jar)
        }
    } catch (ex: Throwable) {
        initLogger.warn { "Can't run javaPluginLoader" }
    }
}

suspend fun loadPlugin(jar: JarVfsFile) {
    jar.openAsZip {
        for (x in it.listRecursive()) {

            if (x.extension == "class") {
                val className = x.absolutePath.replace(".class", "").drop(1)
                val findClass = FindClass(className)
                val getSuperclass = GetSuperclass(findClass)
                memScoped {
                    val name = alloc<CPointerVar<ByteVar>>()
                    GetClassSignature(getSuperclass, name.ptr, null)

                    val agentPluginPartClass = "Lcom/epam/drill/plugin/api/processing/AgentPluginPart;"
                    //fixme do it via recursive call...
                    if (name.value?.toKString() == agentPluginPartClass) {
                        val getMethodID =
                            GetMethodID(findClass, "<init>", "(Ljava/lang/String;)V")
                        val pluginName = jar.parent.baseName
                        val userPlugin =
                            NewObjectA(findClass, getMethodID, nativeHeap.allocArray(1.toLong()) {
                                l = NewStringUTF(pluginName)
                            })
                        val plugin = PluginNativeStub(pluginName, findClass!!, userPlugin!!)
                        PluginManager.addPlugin(plugin)
                        println(pluginName)
                        plugin.load()

                        val ext = if (OS.isWindows) "dll" else "so"
                        val pref = if (OS.isWindows) "" else "lib"
                        val vfsFile = jar.parent["nativePart"]["${pref}main.$ext"]
                        if (vfsFile.exists())
                            CallVoidMethodA(
                                userPlugin,
                                GetMethodID(findClass, "init", "(Ljava/lang/String;)V"),
                                nativeHeap.allocArray(1.toLong()) {

                                    val newStringUTF =
                                        NewStringUTF(vfsFile.absolutePath)
                                    l = newStringUTF

                                })
                    }
                }

            }
        }

    }
}


private suspend fun addPluginsToSystemClassLoader() {
    iterateThrougthPlugins { jar ->
        val segment = jar.absolutePath
        AddToSystemClassLoaderSearch(segment)
        initLogger.info { "System classLoader extends by '$segment' path" }
    }
}

suspend fun retrieveFacilitiesFromPlugin() {
    initLogger.info { "try to unpack jars" }
    iterateThrougthPlugins { jar ->
        jar.extractPluginFacilitiesTo(localVfs(jar.parent.absolutePath)) { vf ->
            !vf.baseName.contains("nativePart") &&
                    !vf.baseName.contains("static")
        }
    }
}


class PluginNativeStub(override val id: String, val findClass: jclass, val userPlugin: jobject) :
    AgentPluginPart<Any>() {
    override var confSerializer: KSerializer<Any>? = null

    override fun load() {
        PluginManager.activate(id)
        CallVoidMethodA(
            userPlugin, GetMethodID(findClass, "load", "()V"), null
        )
    }

    override fun unload() {
        PluginManager.deactivate(id)
        CallVoidMethodA(
            userPlugin, GetMethodID(findClass, "unload", "()V"), null
        )
    }

    override fun updateConfig(config: Any) {

    }

    override fun updateRawConfig(config: String) {
        notifyJavaPart(config)
        notifyNativePart(config)
    }

    private fun notifyNativePart(config: String) {
        np?.updateRawConfig(config)
    }

    private fun notifyJavaPart(config: String) {
        CallVoidMethodA(
            userPlugin,
            GetMethodID(findClass, "updateRawConfig", "(Ljava/lang/String;)V"),
            nativeHeap.allocArray(1.toLong()) {

                val newStringUTF =
                    NewStringUTF(config)
                l = newStringUTF

            })
        ExceptionDescribe()
    }


}
