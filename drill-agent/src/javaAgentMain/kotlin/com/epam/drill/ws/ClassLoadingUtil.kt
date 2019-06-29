@file:Suppress("unused")

package com.epam.drill.ws

import com.epam.drill.plugin.api.processing.AgentPart
import java.util.jar.JarFile

object ClassLoadingUtil {

    fun retrieveApiClass(jarPath: String): Class<AgentPart<*, *>>? {
        val use = JarFile(jarPath).use { jf ->
            com.epam.drill.retrieveApiClass(
                AgentPart::class.java, jf.entries().iterator().asSequence().toSet(),
                ClassLoader.getSystemClassLoader()
            )
        }
        @Suppress("UNCHECKED_CAST")
        return use as Class<AgentPart<*, *>>?
    }

}