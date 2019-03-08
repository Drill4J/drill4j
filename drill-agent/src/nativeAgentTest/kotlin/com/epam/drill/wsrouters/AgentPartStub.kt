package com.epam.drill.wsrouters

import com.epam.drill.common.PluginBean
import com.epam.drill.plugin.api.processing.PluginRepresenter

@Suppress("unused")
class AgentPartStub(override var enabled: Boolean, override val id: String) : PluginRepresenter() {
    override fun updateRawConfig(config: PluginBean) {

    }

    override fun on() {

    }

    override fun off() {

    }
}