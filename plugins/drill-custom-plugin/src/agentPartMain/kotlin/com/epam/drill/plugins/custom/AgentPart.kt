package com.epam.drill.plugins.custom

import com.epam.drill.plugin.api.processing.AgentPluginPart
import com.epam.drill.plugin.api.processing.Sender.sendMessage


@Suppress("unused")
/**
 * @author Denis Domashenko on 2/22/19.
 */
class AgentPart(override val id: String) : AgentPluginPart() {
    override fun load() {
        println("Plugin $id loaded")

        // send message every 10 seconds
        Thread(Runnable {
            while (true) {
                Thread.sleep(10_000)
                sendMessage(id, "Hello from custom plugin! I'm still alive!")
            }
        }).start()

    }


    override fun unload() {
        println("JAVA SIDE: Plugin '$id' unloaded")
    }


}