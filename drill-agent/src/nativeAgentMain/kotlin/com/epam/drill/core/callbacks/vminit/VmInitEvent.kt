package com.epam.drill.core.callbacks.vminit

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
import jvmapi.*
import kotlinx.cinterop.*
import kotlinx.coroutines.runBlocking


val initLogger: Logger
    get() = DLogger("initLogger")

@CName("xx")
fun xxs(x1: COpaquePointer?, x2: COpaquePointer?) {
    println("LLLLLLLLLLLLLLLLLLOOOLS")

}

val initVmEvent = staticCFunction<CPointer<jvmtiEnvVar>?, CPointer<JNIEnvVar>?, jthread?, Unit> { x1, x2, x3 ->
    runBlocking {
        initRuntimeIfNeeded()
//    com.epam.drill.core.currentEnvs()
//    val findClass = FindClass("com/epam/drill/ws/Ws")
//
//
//    val allocArray = nativeHeap.allocArray<JNINativeMethod>(1.toLong()) {
//        name = "asdasd".cstr.getPointer(Arena()).reinterpret()
//        signature = "()V".cstr.getPointer(Arena()).reinterpret()
//        fnPtr = nrTest
//    }
//
//    RegisterNatives(findClass, allocArray, 1)
//    val instanceField = GetStaticFieldID(findClass, "INSTANCE", "Lcom/epam/drill/ws/Ws;")
//    val pluginLoader = GetStaticObjectField(findClass, instanceField)
//    val registerMethodId = GetMethodID(findClass, "asdasd", "()V")
//    CallVoidMethod(pluginLoader, registerMethodId)


        fake()
        Unit
    }

}

suspend fun fake() {
    retrieveFacilitiesFromPlugin()
    addPluginsToSystemClassLoader()
    try {
        iterateThrougthPlugins { jar ->
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

                                CallVoidMethodA(
                                    userPlugin,
                                    GetMethodID(findClass, "init", "(Ljava/lang/String;)V"),
                                    nativeHeap.allocArray(1.toLong()) {
                                        val newStringUTF =
                                            NewStringUTF(jar.parent["nativePart"]["main.dll"].absolutePath)
                                        l = newStringUTF

                                    })
                            }
                        }

                    }
                }

            }
        }
    } catch (ex: Throwable) {
        initLogger.warn { "Can't run javaPluginLoader" }
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


