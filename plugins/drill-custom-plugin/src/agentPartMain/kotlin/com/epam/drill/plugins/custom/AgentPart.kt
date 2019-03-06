package com.epam.drill.plugins.custom

import com.epam.drill.plugin.api.processing.AgentPluginPart
import com.epam.drill.plugin.api.processing.Sender.sendMessage
import kotlinx.serialization.Serializable


@Suppress("unused")
/**
 * @author Denis Domashenko on 2/22/19.
 */
class AgentPart(override val id: String) : AgentPluginPart<TestD>() {

    var config = TestD("Hello from custom plugin! I'm still alive!", 10_000)

    override fun updateConfig(config: TestD) {
        this.config = config
        println("we got some update: $config")
    }

    override var confSerializer: kotlinx.serialization.KSerializer<TestD>? = TestD.serializer()

    var thread: Thread? = null

    override fun on() {
        println("Plugin $id loaded")
        thread = Thread(Runnable {
            while (true) {
                // send message every config.delayTime seconds (10 by default)
                Thread.sleep(config.delayTime)
                sendMessage(id, config.message)
            }
        })
        thread?.start()

    }

    override fun off() {
        thread?.stop()
        thread = null
        println("JAVA SIDE: Plugin '$id' unloaded")
    }


}

@Serializable
data class TestD(val message: String, val delayTime: Long = 10_000)