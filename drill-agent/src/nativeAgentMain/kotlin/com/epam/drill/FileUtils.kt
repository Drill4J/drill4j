package com.epam.drill

import com.epam.drill.common.PluginBean
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.baseName
import com.soywiz.korio.file.extension
import com.soywiz.korio.file.std.MemoryVfs
import com.soywiz.korio.file.std.openAsZip
import com.soywiz.korio.util.OS
import jvmapi.ExceptionClear
import jvmapi.FindClass
import jvmapi.jclass
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json


suspend fun DrillPluginFile.extractPluginFacilitiesTo(destination: VfsFile, filter: (VfsFile) -> Boolean = { true }) {
    val mem = MemoryVfs()
    this.openAsZip { pz -> pz.copyToTree(mem) }
    for (it in mem.list()) {
        if (filter(it))
            it.delete()
    }
    mem.copyToTree(destination)
}


suspend fun DrillPluginFile.iterateThroughtPluginClasses(block: suspend (jclass) -> Unit) {
    this.openAsZip {
        for (x in it.listRecursive()) {
            if (x.extension == "class") {
                val className = x.absolutePath.replace(".class", "").drop(1)
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

fun DrillPluginFile.hasNativePart() = runBlocking {
    this@hasNativePart.nativePart().exists()
}

fun DrillPluginFile.nativePart(): VfsFile {
    val ext = if (OS.isWindows) "dll" else if (OS.isLinux) "so" else "dylib"
    val pref = if (OS.isWindows) "" else "lib"
    return parent["nativePart"]["${pref}main.$ext"]
}

fun DrillPluginFile.pluginConfig() = runBlocking {
    val pluginContent = this@pluginConfig.parent["static"]["plugin_config.json"].readString()
    Json().parse(PluginBean.serializer(), pluginContent)
}

fun DrillPluginFile.rawPluginConfig() = runBlocking {
    this@rawPluginConfig.parent["static"]["plugin_config.json"].readString()
}

typealias DrillPluginFile = VfsFile