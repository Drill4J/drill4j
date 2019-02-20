package com.epam.drill.plugin.api.processing

import com.epam.drill.plugin.api.DrillPlugin


actual abstract class NativePluginPart {

    open fun load(id: Long) {
        this.id = "test"
    }

   abstract fun unload(id: Long)

    actual abstract var id: String
    actual abstract fun update(someText: String)

}

actual abstract class AgentPluginPart : DrillPlugin(), SwitchablePlugin {


    //    external fun nativePart(): NativePluginPart
    actual open fun init(nativePluginPartPath: String) {
    }


    actual abstract override fun load()
    actual abstract override fun unload()
    actual var np: NativePluginPart? = null
}