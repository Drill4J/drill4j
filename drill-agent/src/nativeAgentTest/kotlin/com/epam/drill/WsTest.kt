package com.epam.drill

import com.epam.drill.common.AgentInfo
import com.epam.drill.core.agentInfo
import com.epam.drill.core.exec
import com.epam.drill.core.ws.executeCoroutines
import com.epam.drill.core.ws.websocket
import com.epam.drill.core.ws.wsThread
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlin.native.concurrent.freeze

class WsTest {


    @ImplicitReflectionSerializer
//    @Test
    fun wsTest() {
        setUnhandledExceptionHook({ x: Throwable ->
            println("AASJdkljaskldjaskljdklasjdklasjkldjal $x")
        }.freeze())
        exec { drillInstallationDir = "C:\\Users\\Igor_Kuzminykh\\mDrill4J\\distr" }
        wsThread.executeCoroutines {
            agentInfo = AgentInfo("", "", "", "", true, "")
            while (true)
                try {
                    runBlocking {
                        delay(3000)
                        websocket("localhost:8080")
                    }
                } catch (ex: Exception) {
                    println(
                        ex.message + "\n" +
                                "try reconnect\n"
                    )


                }
        }.result
    }


}