package com.epam.drill

import com.epam.drill.common.PluginBean
import com.epam.drill.core.exec
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.baseName
import com.soywiz.korio.file.extension
import com.soywiz.korio.file.std.openAsZip
import com.soywiz.korio.util.OS
import jvmapi.ExceptionClear
import jvmapi.FindClass
import jvmapi.jclass

suspend fun DrillPluginFile.iterateThroughPluginClasses(block: suspend (jclass) -> Unit) {
    this.openAsZip {
        for (jarEntry in it.listRecursive()) {
            if (jarEntry.extension == "class" && !jarEntry.baseName.contains("module-info")) {
                val className = jarEntry.absolutePath.replace(".class", "").drop(1)
                val findClass = FindClass(className)
                ExceptionClear()
                if (findClass != null) {
                    block(findClass)
                }
            }
        }
    }
}

fun DrillPluginFile.pluginId(): String {
    return this.parent.baseName
}

suspend fun DrillPluginFile.hasNativePart() = this@hasNativePart.nativePart().exists()

fun DrillPluginFile.nativePart(): VfsFile {
    val ext = if (OS.isWindows) "dll" else if (OS.isLinux) "so" else "dylib"
    val pref = if (OS.isWindows) "" else "lib"
    return parent["nativePart"]["${pref}main.$ext"]
}

fun DrillPluginFile.pluginConfig(): PluginBean {
    val pluginId = this.pluginId()
    val exec = exec {
        pl[pluginId]
    }
    return exec!!
}

typealias DrillPluginFile = VfsFile