package com.epam.drill.plugin.api.processing

import com.epam.drill.plugin.api.DrillPlugin
import kotlinx.serialization.KSerializer

expect abstract class AgentPluginPart<T>() : DrillPlugin, SwitchablePlugin {
    var np: NativePluginPart<T>?

    var enabled: Boolean
    abstract var confSerializer: KSerializer<T>?

    open fun init(nativePluginPartPath: String)


    override fun load()
    override fun unload()
    abstract fun updateConfig(config: T)

//    external fun nativePart(): NativePluginPart
    abstract override fun on()
    abstract override fun off()
}


interface SwitchablePlugin {
    fun load()
    fun unload()
    fun on()
    fun off()
}

expect abstract class NativePluginPart<T> {
    actual abstract val confSerializer: KSerializer<T>
    fun updateRawConfig(someText: String)
}