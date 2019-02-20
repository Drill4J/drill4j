package com.epam.drill.plugin.api.processing

import com.epam.drill.plugin.api.DrillPlugin

actual abstract class NativePluginPart {

    actual abstract var id: String
    actual abstract fun update(someText: String)

}

actual abstract class AgentPluginPart : DrillPlugin(), SwitchablePlugin {


    //    external fun nativePart(): NativePluginPart
    actual open fun init(nativePluginPartPath: String) {
        loadNativePart(nativePluginPartPath)
    }


    actual abstract override fun load()
    actual abstract override fun unload()


    external fun loadNative(ss: Long)
    actual var np: NativePluginPart? = null

}