package com.epam.drill.plugin

import com.epam.drill.plugin.api.processing.AgentPart
import drillInternal.config
import kotlinx.cinterop.asStableRef

actual object PluginStorage {

    actual val storage: MutableMap<String, AgentPart<*>>
        get() = config.pstorage?.asStableRef<MutableMap<String, AgentPart<*>>>()?.get()!!


}