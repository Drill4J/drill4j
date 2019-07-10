package com.epam.drill.endpoints

import com.epam.drill.plugin.api.message.*
import java.util.*

class SeqMessage(pluginId: String, drillMessage: DrillMessage) : MessageWrapper(pluginId, drillMessage) {

    var id: String? = UUID.randomUUID().toString()

}
