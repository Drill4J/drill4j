package com.epam.drill

import com.epam.drill.cache.CacheService
import com.epam.drill.cache.impl.HazelcastCacheService
import com.epam.drill.endpoints.AgentEntry
import com.epam.drill.endpoints.AgentManager
import com.epam.drill.endpoints.DrillWsSession
import com.epam.drill.endpoints.ServerWsTopics
import com.epam.drill.endpoints.WsTopic
import com.epam.drill.endpoints.agent.AgentEndpoints
import com.epam.drill.endpoints.agent.AgentHandler
import com.epam.drill.endpoints.agent.DrillServerWs
import com.epam.drill.endpoints.openapi.SwaggerDrillAdminServer
import com.epam.drill.endpoints.plugin.DrillPluginWs
import com.epam.drill.endpoints.plugin.PluginDispatcher
import com.epam.drill.jwt.config.JwtConfig
import com.epam.drill.jwt.user.source.UserSource
import com.epam.drill.jwt.user.source.UserSourceImpl
import com.epam.drill.plugin.api.end.WsService
import com.epam.drill.plugins.PluginLoaderService
import com.epam.drill.plugins.Plugins
import com.epam.drill.router.Routes
import com.epam.drill.service.DataSourceRegistry
import com.epam.drill.storage.ObservableMapStorage
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.UserPasswordCredential
import io.ktor.auth.jwt.jwt
import io.ktor.features.CORS
import io.ktor.features.CallLogging
import io.ktor.features.StatusPages
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Locations
import io.ktor.locations.post
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.routing.routing
import io.ktor.websocket.WebSockets
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.eagerSingleton
import org.kodein.di.generic.singleton
import java.time.Duration

val storage = Kodein.Module(name = "agentStorage") {
    bind<DataSourceRegistry>() with eagerSingleton { DataSourceRegistry() }
    bind<ObservableMapStorage<String, AgentEntry, MutableSet<DrillWsSession>>>() with singleton { ObservableMapStorage<String, AgentEntry, MutableSet<DrillWsSession>>() }
    bind<WsTopic>() with singleton { WsTopic(kodein) }
    bind<ServerWsTopics>() with eagerSingleton { ServerWsTopics(kodein) }
    bind<MutableSet<DrillWsSession>>() with eagerSingleton { HashSet<DrillWsSession>() }
    bind<AgentManager>() with eagerSingleton { AgentManager(kodein) }
    bind<CacheService>() with eagerSingleton { HazelcastCacheService() }
    bind<AgentEndpoints>() with eagerSingleton {
        AgentEndpoints(
            kodein
        )
    }
}

val devContainer = Kodein.Module(name = "devContainer") {
}

val userSource: UserSource = UserSourceImpl()

@KtorExperimentalLocationsAPI
val handlers = Kodein.Module(name = "handlers") {
    bind<SwaggerDrillAdminServer>() with eagerSingleton { SwaggerDrillAdminServer(kodein) }
    bind<PluginDispatcher>() with eagerSingleton { PluginDispatcher(kodein) }
}


val wsHandlers = Kodein.Module(name = "wsHandlers") {
    bind<AgentHandler>() with eagerSingleton {
        AgentHandler(
            kodein
        )
    }
    bind<WsService>() with eagerSingleton { DrillPluginWs(kodein) }
    bind<DrillServerWs>() with eagerSingleton {
        DrillServerWs(
            kodein
        )
    }
}

val pluginServices = Kodein.Module(name = "pluginServices") {
    bind<Plugins>() with singleton { Plugins() }
    bind<PluginLoaderService>() with eagerSingleton { PluginLoaderService(kodein) }
}

var installation: Application.() -> Unit = {
    @Suppress("UNUSED_VARIABLE") val jwtAudience = environment.config.property("jwt.audience").getString()
    val jwtRealm = environment.config.property("jwt.realm").getString()

    install(StatusPages) {
        exception<Throwable> { cause ->
            call.respond(HttpStatusCode.InternalServerError, "Internal Server Error")
            throw cause
        }
    }
    install(CallLogging)
    install(Locations)
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(150)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    install(Authentication) {
        jwt {
            realm = jwtRealm
            verifier(JwtConfig.verifier)
            validate {
                it.payload.getClaim("id").asInt()?.let(userSource::findUserById)
            }
        }
    }

    install(CORS) {
        anyHost()
        allowCredentials = true
        method(HttpMethod.Post)
        method(HttpMethod.Get)
        method(HttpMethod.Delete)
        method(HttpMethod.Put)
        method(HttpMethod.Patch)
        header(HttpHeaders.Authorization)
        exposeHeader(HttpHeaders.Authorization)
    }
}
var kodeinConfig: Kodein.MainBuilder.() -> Unit = {
    import(storage, allowOverride = true)
    import(handlers, allowOverride = true)
    import(pluginServices, allowOverride = true)
    import(wsHandlers, allowOverride = true)
    import(devContainer, allowOverride = true)
}


@Suppress("unused")
@KtorExperimentalLocationsAPI
@ExperimentalCoroutinesApi
fun Application.module(): Kodein {
    return kodeinApplication {
        installation()
        routing {
            post<Routes.Api.Login> {
                val username = "guest"
                val password = ""
                val credentials = UserPasswordCredential(username, password)
                val user = userSource.findUserByCredentials(credentials)
                val token = JwtConfig.makeToken(user)
                call.response.header(HttpHeaders.Authorization, token)
                call.respond(HttpStatusCode.OK)
            }

            static {
                resources("public")
            }
        }
        kodeinConfig()

    }
}


fun Application.kodeinApplication(kodeinMapper: Kodein.MainBuilder.(Application) -> Unit = {}): Kodein {
    val app = this
    return Kodein {
        bind<Application>() with singleton { app }
        kodeinMapper(this, app)
    }
}

