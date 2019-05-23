package com.epam.drill.wsrouters

import com.epam.drill.common.PluginBean
import com.epam.drill.plugin.api.processing.PluginRepresenter

@Suppress("unused")
class AgentPartStub(var enabled: Boolean, override val id: String) : PluginRepresenter() {
    override suspend fun isEnabled(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun setEnabled(value: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun updateRawConfig(config: PluginBean) {

    }

    override fun on() {

    }

    override fun off() {

    }
}