package com.epam.drill.endpoints

import com.epam.drill.agentmanager.AgentStorage
import com.epam.drill.router.WsRoutes
import kotlinx.coroutines.runBlocking
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance

class ServerWsTopics(override val kodein: Kodein) : KodeinAware {
    private val wsTopic: WsTopic by instance()
    private val storage: AgentStorage by instance()

    init {
        //fixme run blocking is very bad:)
        runBlocking {
            wsTopic {
                wsTopic {
                    topic<WsRoutes.GetAllAgents> {
                        val map = storage.agents.values.map { it.agentInfo }
                        println(map)
                        map
                    }

                    topic<WsRoutes.GetAgent> {
                        val map = storage.agents[it.agentId]
                        println(map)
                        map?.agentInfo
                    }
                }

            }
        }
    }

}