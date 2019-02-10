package com.epam.drill.plugin.api

import com.epam.drill.common.PluginBean
import com.epam.drill.common.util.DJSON
import com.epam.drill.storage.PluginsStorage
import java.io.File
import kotlin.reflect.KClass


abstract class DrillPlugin protected constructor() {
    fun pluginInfo(): PluginBean {
        val lastModified = file.lastModified()
        if (timeStamp <= lastModified) {
            timeStamp = lastModified
            pluginBean = retrievePluginInfo()
        }
        return pluginBean

    }

    private val file = File(File(this.javaClass.protectionDomain.codeSource.location.toURI().path).parentFile, "static/plugin_config.json")

    private var pluginBean: PluginBean

    private var timeStamp: Long = 0

    abstract val configClass: KClass<out PluginBean>

    init {
        timeStamp = file.lastModified()
        pluginBean = retrievePluginInfo()
        PluginsStorage.addPlugin(this)
    }

    private fun retrievePluginInfo(): PluginBean {
        try {
            val readText = file.readText()

            @Suppress("UNCHECKED_CAST")
            val parse = DJSON.parse(readText, configClass as KClass<Any>)
            return parse as PluginBean
        } catch (ex: Exception) {
            //fixme
//            logError("Can't load config for plugin")
            throw ex
        }
    }

    abstract fun callBack()


    fun updateConfig(pc: PluginBean) {
        file.writeText(DJSON.stringify(pc))
        updateClientConfig(pc)
    }

    open fun updateClientConfig(pc: PluginBean) {

    }

}