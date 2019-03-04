package com.epam.drill.plugin.api.processing

import com.epam.drill.plugin.api.DrillPlugin
import kotlinx.serialization.KSerializer

expect abstract class AgentPluginPart<T>() : DrillPlugin, SwitchablePlugin {
    var np: NativePluginPart<T>?

    var enabled: Boolean
    abstract var confSerializer: KSerializer<T>?

    open fun init(nativePluginPartPath: String)


    abstract override fun load()
    abstract override fun unload()
    abstract fun updateConfig(config: T)

//    external fun nativePart(): NativePluginPart
}


interface SwitchablePlugin {
    fun load()
    fun unload()
}

expect abstract class NativePluginPart<T> {
    actual abstract val confSerializer: KSerializer<T>
    abstract fun updateConfig(someText: T)
    fun updateRawConfig(someText: String)
}