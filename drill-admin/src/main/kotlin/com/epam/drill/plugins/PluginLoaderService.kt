package com.epam.drill.plugins


import com.epam.drill.common.PluginBean
import com.epam.drill.drillHomeDir
import com.epam.drill.loadClassesFrom
import com.epam.drill.plugin.api.end.AdminPluginPart
import com.epam.drill.retrieveApiClass
import io.ktor.util.error
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import java.io.File
import java.lang.System.getenv
import java.util.jar.JarEntry
import java.util.jar.JarFile


private val logger = KotlinLogging.logger {}

class PluginLoaderService(override val kodein: Kodein) : KodeinAware {
    private val plugins: Plugins by kodein.instance()
    private val pluginPaths: List<File> =
        listOf(
            File("..", "distr").resolve("adminStorage"),
            drillHomeDir.resolve("adminStorage")
        ).map { it.canonicalFile }

    init {
        try {
            logger.info { "Searching for plugins in paths $pluginPaths" }
            val pluginsFiles = pluginPaths.filter { it.exists() }
                .flatMap { it.listFiles().asIterable() }
                .filter { it.isFile && it.extension.equals("jar", true) }
                .map { it.canonicalFile }
            if (pluginsFiles.isNotEmpty()) {
                logger.info { "Plugin jars found: ${pluginsFiles.count()}." }
                pluginsFiles.forEach { pluginFile ->
                    logger.info { "Loading from $pluginFile." }
                    JarFile(pluginFile).use { jar ->
                        val configPath = "plugin_config.json"
                        val configEntry = jar.getJarEntry(configPath)
                        if (configEntry != null) {
                            val configText = jar.getInputStream(configEntry).reader().readText()
                            val config = Json.parse(PluginBean.serializer(), configText)
                            val pluginId = config.id
                            if (pluginId !in plugins) {
                                val adminPartFile = jar.extractPluginEntry(pluginId, "admin-part.jar")
                                val adminPartClass = JarFile(adminPartFile).use { adminJar ->
                                    processAdminPart(adminPartFile, adminJar)
                                }
                                if (adminPartClass != null) {
                                    val agentFile = jar.extractPluginEntry(pluginId, "agent-part.jar")
                                    val dp = DP(adminPartClass, agentFile, config)
                                    plugins[pluginId] = dp
                                    logger.info { "Plugin '$pluginId' was loaded successfully." }
                                } else {
                                    logger.error { "Admin Plugin API class was not found for $pluginId" }
                                }
                            } else {
                                logger.warn { "Plugin $pluginId has already been loaded. Skipping loading from $pluginFile." }
                            }
                        } else {
                            logger.error { "Error loading plugin from $pluginFile - no $configPath!" }
                        }
                    }
                }
            } else {
                logger.warn { "No plugins found!" }

            }
        } catch (ex: Exception) {
            logger.error(ex)
        }

    }

    private fun processAdminPart(
        adminPartFile: File, adminJar: JarFile
    ): Class<AdminPluginPart>? {
        val sysClassLoader = ClassLoader.getSystemClassLoader()
        sysClassLoader.loadClassesFrom(adminPartFile.toURI().toURL())
        val entrySet = adminJar.entries().iterator().asSequence().toSet()
        val pluginApiClass =
            retrieveApiClass(AdminPluginPart::class.java, entrySet, sysClassLoader)
        @Suppress("UNCHECKED_CAST")
        return pluginApiClass as Class<AdminPluginPart>?
    }

}

fun JarFile.extractPluginEntry(pluginId: String, entry: String): File {
    val jarEntry: JarEntry = getJarEntry(entry)
    return getInputStream(jarEntry).use { istream ->
        val workDir = File(getenv("DRILL_HOME"), "work")
        val pluginDir = workDir.resolve("plugins").resolve(pluginId)
        pluginDir.mkdirs()
        File(pluginDir, jarEntry.name).apply {
            outputStream().use { ostream -> istream.copyTo(ostream) }
            deleteOnExit()
        }
    }
}
