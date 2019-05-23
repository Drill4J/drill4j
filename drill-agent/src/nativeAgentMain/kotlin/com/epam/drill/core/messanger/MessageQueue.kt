package com.epam.drill.core.messanger

import com.epam.drill.api.drillRequest
import com.epam.drill.common.Message
import com.epam.drill.common.MessageType
import com.epam.drill.core.plugin.dto.DrillMessage
import com.epam.drill.core.plugin.dto.MessageWrapper
import com.epam.drill.core.util.json

object MessageQueue {

    fun sendMessage(pluginId: String, content: String) {
        val message =
            json.stringify(
                Message.serializer(), Message(
                    MessageType.PLUGIN_DATA, "",

                    json.stringify(
                        MessageWrapper.serializer(),
                        MessageWrapper(
                            pluginId, DrillMessage(
                                drillRequest()?.drillSessionId ?: "", content
                            )
                        )
                    )
                )
            )
        com.epam.drill.core.ws.sendMessage(message)
    }
}