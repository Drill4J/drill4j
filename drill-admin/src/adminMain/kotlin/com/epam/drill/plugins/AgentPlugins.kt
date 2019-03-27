package com.epam.drill.plugins


import com.epam.drill.extractPluginBean
import com.epam.drill.loadInRuntime
import com.epam.drill.plugin.api.end.AdminPluginPart
import com.epam.drill.plugin.api.end.WsService
import com.epam.drill.retrieveApiClass
import io.ktor.util.error
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import org.slf4j.LoggerFactory
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarFile

val logger = LoggerFactory.getLogger(AgentPlugins::class.java)

class AgentPlugins(override val kodein: Kodein) : KodeinAware {
    private val plugins: Plugins by kodein.instance()
    private val wsService: WsService by kodein.instance()
    private val fileList: List<File> = listOf(File("../distr/adminStorage"), File(System.getenv("DRILL_HOME") + "/adminStorage"))

    init {
        try {
            fileList.forEach {
                if (!it.exists()) {
                    logger.warn("didn't find plugin folder. Search directories: ${it.absolutePath}")
                } else {
                    val files = it.listFiles() ?: arrayOf()
                    if (files.isNotEmpty()) {
                        logger.info(
                            "${files.size} were found.\n" +
                                    files.contentToString()
                        )
                    } else {
                        logger.warn("didn't find any plugins...")
                    }
                    for (pluginDir in files) {
                        val jar = JarFile(pluginDir)
                        val tempDir = File(System.getProperty("user.home"), ".drill")
                        val tempDirectory = createTempDirectory(tempDir, pluginDir.nameWithoutExtension)
                        val processAdminPart = processAdminPart(jar, tempDirectory)
                        val loadedPlugins = plugins.plugins.keys

                        if (!loadedPlugins.contains(processAdminPart.id)) {
                            val processAgentPart = processAgentPart(jar, tempDirectory)
                            val dp = DP(processAdminPart, processAgentPart)
                            plugins.plugins[processAdminPart.id] = dp
                            logger.info("plugin '${processAdminPart.id}' was loaded successfully")
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            logger.error(ex)
        }

    }

    private fun processAdminPart(jar: JarFile, tempDirectory: File): AdminPluginPart {
        val adminPartJar: JarEntry = jar.getJarEntry("admin-part.jar")
        val f = extractJarEntityToTempDir(jar, tempDirectory, adminPartJar)
        val cl = ClassLoader.getSystemClassLoader()
        loadInRuntime(f, cl)
        val jarFile = JarFile(f)
        val entrySet = jarFile.entries().iterator().asSequence().toSet()
        val pluginApiClass = retrieveApiClass(AdminPluginPart::class.java, entrySet, cl)
        val constructor = pluginApiClass!!.getConstructor(WsService::class.java, String::class.java)
        val pluginBean = extractPluginBean(jarFile, tempDirectory)
        plugins.pluginBeans.add(pluginBean)
        return constructor.newInstance(wsService, pluginBean.id) as AdminPluginPart
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

    fun createTempDirectory(tempDir: File, id: String): File {
        val temp = File(tempDir, id)
        if (!(temp.mkdirs())) {
//            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }
        return temp
    }


}