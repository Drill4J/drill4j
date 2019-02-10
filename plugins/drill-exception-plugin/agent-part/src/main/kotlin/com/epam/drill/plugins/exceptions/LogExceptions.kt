package com.epam.drill.plugins.exceptions

import com.epam.drill.common.PluginBean
import com.epam.drill.plugin.api.processing.AgentPluginPart
import com.epam.drill.plugin.api.processing.natives.NativeApi
import com.epam.drill.storage.PluginsStorage
import kotlin.reflect.KClass


@Suppress("unused")
/**
 * @author Igor Kuzminykh on 8/8/17.
 */
class LogExceptions : AgentPluginPart() {


    init {
        println("try to enable exception catch")
        NativeApi.enableJvmtiEventException()

    }

    override val configClass: KClass<out PluginBean>
        get() = PluginBean::class

    override fun unload() {
        NativeApi.disableJvmtiEventException()
        pluginInfo().enabled = false
        PluginsStorage.pluginsMapping.remove(pluginInfo().id)
    }


    override fun updateClientConfig(pc: PluginBean) {
        if (pc is PluginBean) {
            println("new settongs")
            println(pc)
//            DrillNativeManager.setFrameDepth(pc.frameDepth)
//            pc.blacklistPackages.forEach {
//                DrillNativeManager.addPackageToFilter(it)
//            }
        }
    }

}