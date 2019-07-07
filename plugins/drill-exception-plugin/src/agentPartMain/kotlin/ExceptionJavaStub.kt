package com.epam.drill.plugins.exception

import com.epam.drill.plugin.api.SerDe
import com.epam.drill.plugin.api.processing.AgentPart
import com.epam.drill.plugin.api.processing.UnloadReason
import kotlinx.serialization.Serializable

@Suppress("unused")
class JavaPartOfNativePlguin constructor(
    override val id: String
) : AgentPart<ExceptionConfig, Action>() {

    override val serDe = SerDe(Action.serializer())

    override fun on() {
        println("on")
    }

    override fun off() {
        println("off")
    }

    override fun destroyPlugin(unloadReason: UnloadReason) {
        println("destroyPlugin")
    }


    override fun initPlugin() {
        println("initPlugin")
    }


    override suspend fun doAction(action: Action) {

        println("doAction")
    }


    override val confSerializer: kotlinx.serialization.KSerializer<ExceptionConfig> = ExceptionConfig.serializer()
}


@Serializable
data class ExceptionConfig(val blackList: List<String> = emptyList())

@Serializable
data class Action(val s: String = "")
