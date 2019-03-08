package com.epam.drill.wsrouters

import com.epam.drill.common.AgentInfo
import com.epam.drill.core.agentInfo
import com.epam.drill.core.ws.toggleStandby
import kotlin.test.Test

class WsRoutersTest {

    @Test
    fun toggleStandbyTest() {
        val disabledAgent = AgentInfo("test","test","test",true)
        toggleStandby(disabledAgent)
    }


}