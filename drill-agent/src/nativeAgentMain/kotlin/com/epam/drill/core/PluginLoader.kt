package com.epam.drill.core

import com.epam.drill.JarVfsFile
import com.epam.drill.core.callbacks.vminit.initLogger
import com.epam.drill.extractPluginFacilitiesTo
import com.epam.drill.iterateThrougthPlugins
import com.epam.drill.plugin.PluginManager
import com.epam.drill.plugin.api.processing.PluginRepresenter
import com.epam.drill.plugin.api.processing.Reason
import com.soywiz.korio.file.baseName
import com.soywiz.korio.file.extension
import com.soywiz.korio.file.std.localVfs
import com.soywiz.korio.file.std.openAsZip
import com.soywiz.korio.util.OS
import jvmapi.*
import kotlinx.cinterop.*
import kotlinx.coroutines.runBlocking


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
        ex.printStackTrace()
        initLogger.warn { "Can't run javaPluginLoader" }
    }
}

suspend fun loadPlugin(jar: JarVfsFile) {
    jar.openAsZip {
        for (x in it.listRecursive()) {

            if (x.extension == "class") {
                println(x)
                memScoped {
                    val className = x.absolutePath.replace(".class", "").drop(1)
                    val findClass = FindClass(className)!!
                    println(findClass)
                    if (isSutablePluginClass(findClass)) {
                        createPluginAndLoad(findClass, jar)
                    }
                }

            }
        }

    }
}

fun isSutablePluginClass(findClass: jclass) = memScoped {

    val targetClass = "Lcom/epam/drill/plugin/api/processing/AgentPart;"
    var parentClass = GetSuperclass(findClass)
    var name = alloc<CPointerVar<ByteVar>>()
    GetClassSignature(parentClass, name.ptr, null)
    var isApiClassFound = false
    while (parentClass != null) {
        val toKString = name.value?.toKString()
        println(toKString)
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

private suspend fun createPluginAndLoad(findClass: jclass?, jar: JarVfsFile) {
    val getMethodID =
        GetMethodID(findClass, "<init>", "(Ljava/lang/String;)V")
    val pluginName = jar.parent.baseName
    val userPlugin =
        NewObjectA(findClass, getMethodID, nativeHeap.allocArray(1.toLong()) {
            l = NewStringUTF(pluginName)
        })
    println("sss")
    val plugin = NativeStub(pluginName, findClass!!, userPlugin!!)
    println("sss1")
    PluginManager.addPlugin(plugin)
    println(pluginName)
    val pluginContent = jar.parent["static"]["plugin_config.json"].readString()
    plugin.updateRawConfig(pluginContent)
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
    println("____________________")

    println(pluginContent)
    plugin.np?.updateRawConfig(pluginContent)
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


class NativeStub(override val id: String, val findClass: jclass, val userPlugin: jobject) : PluginRepresenter() {

    override fun on() {
        PluginManager.activate(id)
        CallVoidMethodA(
            userPlugin, GetMethodID(findClass, "on", "()V"), null
        )
    }

    override fun off() {
        PluginManager.deactivate(id)
        CallVoidMethodA(
            userPlugin, GetMethodID(findClass, "off", "()V"), null
        )
    }


    override fun load() {
        PluginManager.activate(id)
        CallVoidMethodA(
            userPlugin, GetMethodID(findClass, "load", "()V"), null
        )
    }

    override fun unload(reason: Reason) {
        PluginManager.deactivate(id)
//        CallVoidMethodA(
//            fixme!!!!
//            userPlugin, GetMethodID(findClass, "unload", "()V"), null
//        )
        np?.unload(reason)
    }

    override fun updateRawConfig(config: String) {
        notifyJavaPart(config)
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
