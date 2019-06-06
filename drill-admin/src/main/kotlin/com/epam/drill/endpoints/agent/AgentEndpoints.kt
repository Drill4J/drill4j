package com.epam.drill.endpoints.agent


import com.epam.drill.agentmanager.AgentInfoWebSocketSingle
import com.epam.drill.common.AgentStatus
import com.epam.drill.endpoints.AgentManager
import com.epam.drill.router.Routes
import com.google.gson.Gson
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.post
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.routing
import kotlinx.serialization.Serializable
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance

@KtorExperimentalLocationsAPI
class AgentEndpoints(override val kodein: Kodein) : KodeinAware {
    private val app: Application by instance()
    private val agentManager: AgentManager by instance()

    init {
        app.routing {

            authenticate {
                post<Routes.Api.UpdateAgentConfig> { ll ->
                    val agentId = ll.agentId
                    if (agentManager[agentId] != null) {
                        val au = Gson().fromJson(call.receive<String>(), AgentInfoWebSocketSingle::class.java)
                        agentManager.updateAgent(agentId, au)
                        call.respond(HttpStatusCode.OK, "agent '$agentId' was updated")
                    } else {
                        call.respond(HttpStatusCode.BadRequest, "agent '$agentId' not found")
                    }
                }
            }

            authenticate {
                post<Routes.Api.RegisterAgent> { ll ->
                    val agentId = ll.agentId
                    val agInfo = agentManager.byId(agentId)
                    if (agInfo != null) {
                        val regInfo = Gson().fromJson(call.receive<String>(), AgentRegistrationInfo::class.java)
                        val au = AgentInfoWebSocketSingle(
                            id = agentId,
                            name = regInfo.name,
                            status = AgentStatus.READY,
                            description = regInfo.description,
                            buildVersion = agInfo.buildVersion
                        )
                        agentManager.updateAgent(agentId, au)
                        call.respond(HttpStatusCode.OK, "agent '$agentId' have been registered")
                    } else {
                        call.respond(HttpStatusCode.BadRequest, "agent '$agentId' not found")
                    }
                }
            }

        }
    }
}

@Serializable
data class AgentRegistrationInfo(
    val name: String,
    val description: String
)