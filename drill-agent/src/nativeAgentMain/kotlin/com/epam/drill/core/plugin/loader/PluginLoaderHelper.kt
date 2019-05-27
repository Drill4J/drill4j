package com.epam.drill.core.plugin.loader

import com.epam.drill.DrillPluginFile
import com.epam.drill.iterateThroughPluginClasses
import com.epam.drill.jvmapi.jniParamName
import com.epam.drill.plugin.api.processing.AgentPart
import jvmapi.AddToSystemClassLoaderSearch
import jvmapi.ExceptionClear
import jvmapi.GetClassSignature
import jvmapi.GetSuperclass
import jvmapi.jclass
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.free
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value

suspend fun DrillPluginFile.retrievePluginApiClass(): jclass {
    var pluginApiClass: jclass? = null

    this@retrievePluginApiClass.iterateThroughPluginClasses { findClass ->
        if (isSuitablePluginClass(findClass))
            pluginApiClass = findClass

    }

    pluginApiClass ?: throw com.epam.drill.core.exceptions.PluginLoadException("Can't find the plugin API class.")
    return pluginApiClass!!
}


fun isSuitablePluginClass(findClass: jclass): Boolean {

    val targetClass = AgentPart::class.jniParamName()
    var isApiClassFound = false
    var parentClass = GetSuperclass(findClass)
    ExceptionClear()
    if (parentClass != null) {
        val name = nativeHeap.alloc<CPointerVar<ByteVar>>()
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
            GetClassSignature(parentClass, name.ptr, null)
            ExceptionClear()
        }
        nativeHeap.free(name)
    }

    return isApiClassFound
}

fun DrillPluginFile.addPluginsToSystemClassLoader() {
    val segment = this.absolutePath
    AddToSystemClassLoaderSearch(segment)
    plLogger.warn { "System classLoader extends by '$segment' path" }
}


