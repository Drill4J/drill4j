package com.epam.drill.wsrouters

import com.epam.drill.common.*
import com.epam.drill.plugin.api.processing.*

@Suppress("unused")
class AgentPartStub(var enabledx: Boolean, override val id: String) : PluginRepresenter() {
    override suspend fun doRawAction(rawAction: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun isEnabled(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun setEnabled(value: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun updateRawConfig(config: PluginConfig) {

    }

    override fun on() {

    }

    override fun off() {

    }
}