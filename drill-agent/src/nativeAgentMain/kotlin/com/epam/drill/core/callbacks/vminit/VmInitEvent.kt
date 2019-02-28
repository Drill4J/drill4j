@file:Suppress("unused")

package com.epam.drill.core.callbacks.vminit

import com.epam.drill.JarVfsFile
import com.epam.drill.extractPluginFacilitiesTo
import com.epam.drill.iterateThrougthPlugins
import com.epam.drill.logger.DLogger
import com.epam.drill.plugin.PluginManager
import com.epam.drill.plugin.api.processing.AgentPluginPart
import com.soywiz.klogger.Logger
import com.soywiz.korio.file.baseName
import com.soywiz.korio.file.extension
import com.soywiz.korio.file.std.localVfs
import com.soywiz.korio.file.std.openAsZip
import com.soywiz.korio.util.OS
import jvmapi.*
import kotlinx.cinterop.*
import kotlinx.coroutines.runBlocking


val initLogger: Logger
    get() = DLogger("initLogger")

@Suppress("UNUSED_PARAMETER")
@CName("jvmtiEventVMInitEvent")
fun jvmtiEventVMInitEvent(env: CPointer<jvmtiEnvVar>?, jniEnv: CPointer<JNIEnvVar>?, thread: jthread?) = runBlocking {
    initRuntimeIfNeeded()
    fake()
}

suspend fun fake() {
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
                        val plugin = object : AgentPluginPart() {
                            override fun load() {

                                CallVoidMethodA(
                                    userPlugin, GetMethodID(findClass, "load", "()V"), null
                                )

                            }

                            override fun unload() {
                                CallVoidMethodA(
                                    userPlugin, GetMethodID(findClass, "unload", "()V"), null
                                )
                            }

                            override lateinit var id: String

                        }
                        plugin.id = pluginName
                        PluginManager.addPlugin(plugin)
                        println(pluginName)
                        println("added")
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


