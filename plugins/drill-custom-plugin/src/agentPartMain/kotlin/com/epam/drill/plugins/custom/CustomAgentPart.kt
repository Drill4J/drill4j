package com.epam.drill.plugins.custom

import com.epam.drill.plugin.api.processing.AgentPart
import com.epam.drill.plugin.api.processing.UnloadReason
import com.epam.drill.plugin.api.processing.Sender.sendMessage
import kotlinx.serialization.Serializable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


@Suppress("unused")
/**
 * @author Denis Domashenko on 2/22/19.
 */
class CustomAgentPart(override val id: String) : AgentPart<TestD>() {
    private var thread: ExecutorService? = null
    private var isAlive = false

    override fun initPlugin() {
        thread = Executors.newSingleThreadExecutor()
    }

    override fun destroyPlugin(unloadReason: UnloadReason) {
        thread?.shutdownNow()
        thread = null
    }

    override var confSerializer: kotlinx.serialization.KSerializer<TestD> = TestD.serializer()


    override fun on() {
        isAlive = true
        thread?.submit {
            while (isAlive) {
                Thread.sleep(config.delayTime)
                sendMessage(id, config.message)
            }
        }
    }

    override fun off() {
        isAlive = false
        println("JAVA SIDE: Plugin '$id' off")
    }


}

@Serializable
data class TestD(
    val message: String, val delayTime: Long = 5_000
)