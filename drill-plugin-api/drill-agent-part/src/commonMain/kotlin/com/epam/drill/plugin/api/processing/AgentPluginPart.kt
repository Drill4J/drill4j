package com.epam.drill.plugin.api.processing

import com.epam.drill.plugin.api.DrillPlugin

expect abstract class AgentPluginPart() : DrillPlugin, SwitchablePlugin {
    var np: NativePluginPart?

    open fun init(nativePluginPartPath: String)


    abstract override fun load()
    abstract override fun unload()

//    external fun nativePart(): NativePluginPart
}


interface SwitchablePlugin {
    fun load()
    fun unload()
}

expect abstract class NativePluginPart {

    abstract var id: String
    abstract fun update(someText: String)
}