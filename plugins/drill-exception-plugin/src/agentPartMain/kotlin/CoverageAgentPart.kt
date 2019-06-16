package com.epam.drill.plugins.coverage

import com.epam.drill.common.parse
import com.epam.drill.plugin.api.processing.AgentPart
import com.epam.drill.plugin.api.processing.UnloadReason
import kotlinx.serialization.Serializable

@Suppress("unused")
class JavaPartOfNativePlguin constructor(
    override val id: String
) : AgentPart<CoverConfig, Action>() {


    override fun doRawAction(action: String) {
        doAction(actionSerializer parse action)
    }

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


    override fun doAction(action: Action) {

        println("doAction")
    }


    override var confSerializer: kotlinx.serialization.KSerializer<CoverConfig> = CoverConfig.serializer()
    override var actionSerializer: kotlinx.serialization.KSerializer<Action> = Action.serializer()
}

@Serializable
data class CoverConfig(val s: String = "")

@Serializable
data class Action(val s: String = "")
