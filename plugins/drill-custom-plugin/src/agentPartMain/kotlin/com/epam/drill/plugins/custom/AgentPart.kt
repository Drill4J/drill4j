package com.epam.drill.plugins.custom

import com.epam.drill.plugin.api.processing.AgentPluginPart
import com.epam.drill.plugin.api.processing.Sender.sendMessage
import kotlinx.serialization.Serializable


@Suppress("unused")
/**
 * @author Denis Domashenko on 2/22/19.
 */
class AgentPart(override val id: String) : AgentPluginPart<TestD>() {
    override fun updateConfig(config: TestD) {
        println("we got some update: $config")
    }

    override var confSerializer: kotlinx.serialization.KSerializer<TestD>? = TestD.serializer()

    var thread: Thread? = null

    override fun load() {
        println("Plugin $id loaded")
        thread = Thread(Runnable {
            while (true) {
                // send message every 10 seconds
                Thread.sleep(10_000)
                sendMessage(id, "Hello from custom plugin! I'm still alive!")
            }
        })
        thread?.start()

    }

    override fun unload() {
        thread?.stop()
        thread = null
        println("JAVA SIDE: Plugin '$id' unloaded")
    }


}

@Serializable
data class TestD(val st: String)