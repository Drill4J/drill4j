package com.epam.drill.core.messanger

import com.epam.drill.api.drillRequest
import com.epam.drill.common.Message
import com.epam.drill.common.MessageType
import com.epam.drill.core.plugin.dto.DrillMessage
import com.epam.drill.core.plugin.dto.MessageWrapper
import com.epam.drill.core.util.json
import drillInternal.addMessage
import drillInternal.getMessage
import kotlinx.cinterop.Arena
import kotlinx.cinterop.cstr
import kotlinx.cinterop.toKString

//todo think about coroutines
object MessageQueue {

    fun retrieveMessage(): String? {
        return getMessage()?.toKString()
    }

    fun first():String?{
        return drillInternal.first()?.toKString()
    }


    fun sendMessage(pluginId: String, content: String) {
        //fixme create global Arena scope
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
        addMessage(message.cstr.getPointer(Arena()))
    }
}