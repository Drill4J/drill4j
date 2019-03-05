package com.epam.drill.plugins.custom

import com.epam.drill.plugin.api.processing.AgentPluginPart
import com.epam.drill.plugin.api.processing.Sender.sendMessage
import kotlinx.serialization.Serializable
import java.util.concurrent.Executors


@Suppress("unused")
/**
 * @author Denis Domashenko on 2/22/19.
 */
class AgentPart(override val id: String) : AgentPluginPart<TestD>() {

    private var config = TestD("Hello from custom plugin! I'm still alive!", 10_000)
    private val executor = Executors.newSingleThreadExecutor()

    override fun updateConfig(config: TestD) {
        this.config = config
        println("we got some update: $config")
    }

    override var confSerializer: kotlinx.serialization.KSerializer<TestD>? = TestD.serializer()

    override fun load() {
        println("Plugin $id loaded")
        executor.execute(Thread(Runnable {
            while (!executor.isShutdown) {
                // send message every config.delayTime seconds (10 by default)
                Thread.sleep(config.delayTime)
                sendMessage(id, config.message)
            }
        }))


    }

    override fun unload() {
        executor.shutdown()
        println("JAVA SIDE: Plugin '$id' unloaded")
    }


}

@Serializable
data class TestD(val message: String, val delayTime: Long = 10_000)