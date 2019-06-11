package com.epam.drill.plugins


import com.epam.drill.common.PluginBean
import com.epam.drill.extractPluginBean
import com.epam.drill.loadInRuntime
import com.epam.drill.plugin.api.end.AdminPluginPart
import com.epam.drill.retrieveApiClass
import io.ktor.util.error
import mu.KotlinLogging
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarFile


private val logger = KotlinLogging.logger {}

class PluginLoaderService(override val kodein: Kodein) : KodeinAware {
    private val plugins: Plugins by kodein.instance()
    private val fileList: List<File> =
        listOf(File("../distr/adminStorage"), File(System.getenv("DRILL_HOME") + "/adminStorage"))

    init {
        try {
            fileList.forEach {
                if (!it.exists()) {
                    logger.debug { "didn't find plugin folder. Search directories: ${it.absolutePath}" }
                } else {
                    val files = it.listFiles() ?: arrayOf()
                    if (files.isNotEmpty()) {
                        logger.info {
                            "${files.size} were found.\n" +
                                    files.contentToString()
                        }
                    } else {
                        logger.warn { "didn't find any plugins..." }
                    }
                    for (pluginDir in files) {
                        val jar = JarFile(pluginDir)
                        val tempDir = File("stuff", ".drill")
                        val tempDirectory = createTempDirectory(tempDir, pluginDir.nameWithoutExtension)
                        val (processAdminPart, pluginBean) = processAdminPart(jar, tempDirectory)
                        val loadedPlugins = plugins.plugins.keys

                        if (!loadedPlugins.contains(pluginBean.id)) {
                            val processAgentPart = processAgentPart(jar, tempDirectory)
                            val dp = DP(processAdminPart, processAgentPart, pluginBean)
                            plugins.plugins[pluginBean.id] = dp
                            logger.info { "plugin '${pluginBean.id}' was loaded successfully" }
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            logger.error(ex)
        }

    }

    private fun processAdminPart(jar: JarFile, tempDirectory: File): Pair<Class<AdminPluginPart>, PluginBean> {
        val adminPartJar: JarEntry = jar.getJarEntry("admin-part.jar")
        val f = extractJarEntityToTempDir(jar, tempDirectory, adminPartJar)
        val cl = ClassLoader.getSystemClassLoader()
        loadInRuntime(f, cl)
        val jarFile = JarFile(f)
        val entrySet = jarFile.entries().iterator().asSequence().toSet()
        val pluginApiClass = retrieveApiClass(AdminPluginPart::class.java, entrySet, cl)
        val pluginBean = extractPluginBean(jarFile, tempDirectory)
        @Suppress("UNCHECKED_CAST")
        return pluginApiClass as Class<AdminPluginPart> to pluginBean
    }

    private fun processAgentPart(jar: JarFile, tempDirectory: File): File {
        val agentPartJar: JarEntry = jar.getJarEntry("agent-part.jar")
        return extractJarEntityToTempDir(jar, tempDirectory, agentPartJar)
    }


    private fun extractJarEntityToTempDir(jar: JarFile, tempDirectory: File, jarEntry: JarEntry): File {
        val file = File(tempDirectory, jarEntry.name)
        if (file.createNewFile()) {
//            log.log(Level.SEVERE, "New file {0} has created.", file);
        }
        jar.getInputStream(jarEntry).use { input ->
            file.outputStream().use { fileOut ->
                input.copyTo(fileOut)
            }
        }
        return file
    }

    private fun createTempDirectory(tempDir: File, id: String): File {
        val temp = File(tempDir, id)
        if (!(temp.mkdirs())) {
//            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }
        return temp
    }
}