package com.epam.drill

import com.epam.drill.core.drillInstallationDir
import com.soywiz.korio.file.baseName
import com.soywiz.korio.file.std.localVfs

@Suppress("ClassName")
object iterateThrougthPlugins {
   suspend inline operator fun invoke(block: (JarVfsFile) -> Unit) {
        val pluginsDir = localVfs(drillInstallationDir)["drill-plugins"]
        for (pluginRawFolder in pluginsDir.list()) {
            val pluginFolder = localVfs(pluginsDir.absolutePath + pluginRawFolder.absolutePath)
            if (pluginFolder.isDirectory()) {
                for (pluginJar in pluginFolder.listRecursive { it.baseName.contains("agent-part.jar") }) {
                    val jar: JarVfsFile = localVfs(pluginFolder.absolutePath + pluginJar.absolutePath)
                    block(jar)
                }
            }
        }
    }
}