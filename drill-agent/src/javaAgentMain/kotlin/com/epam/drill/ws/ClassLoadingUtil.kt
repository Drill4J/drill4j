@file:Suppress("unused")

package com.epam.drill.ws

import com.epam.drill.plugin.api.processing.AgentPart
import java.util.jar.JarFile

object ClassLoadingUtil {

    fun retrieveApiClass(jarPath: String): Class<AgentPart<*, *>>? {

        var jf: JarFile? = null
        try {
            jf = JarFile(jarPath)
            @Suppress("UNCHECKED_CAST")
            return com.epam.drill.retrieveApiClass(
                AgentPart::class.java, jf.entries().iterator().asSequence().toSet(),
                ClassLoader.getSystemClassLoader()
            ) as Class<AgentPart<*, *>>?
        } finally {
            jf?.close()
        }

    }

}