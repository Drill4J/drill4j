package com.epam.drill

import com.epam.drill.common.AgentInfo
import com.epam.drill.endpoints.*
import com.epam.drill.endpoints.openapi.DevEndpoints
import com.epam.drill.endpoints.openapi.SwaggerDrillAdminServer
import com.epam.drill.jwt.config.JwtConfig
import com.epam.drill.jwt.user.source.UserSource
import com.epam.drill.jwt.user.source.UserSourceImpl
import com.epam.drill.plugin.api.end.WsService
import com.epam.drill.plugins.AgentPlugins
import com.epam.drill.plugins.Plugins
import com.epam.drill.router.Routes
import com.epam.drill.storage.CassandraConnector
import com.epam.drill.storage.MongoClient
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.UserPasswordCredential
import io.ktor.auth.jwt.jwt
import io.ktor.features.CORS
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.gson.GsonConverter
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.cio.websocket.DefaultWebSocketSession
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
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.eagerSingleton
import org.kodein.di.generic.singleton
import java.time.Duration

val storage = Kodein.Module(name = "agentStorage") {
    bind<ObservableMapStorage<String, Pair<AgentInfo, DefaultWebSocketSession>, MutableSet<DrillWsSession>>>() with singleton { ObservableMapStorage<String, Pair<AgentInfo, DefaultWebSocketSession>, MutableSet<DrillWsSession>>() }
    bind<MongoClient>() with singleton { MongoClient(kodein) }
    bind<CassandraConnector>() with singleton { CassandraConnector(kodein) }
    bind<WsTopic>() with singleton { WsTopic(kodein) }
    bind<ServerWsTopics>() with eagerSingleton { ServerWsTopics(kodein) }
    bind<MutableSet<DrillWsSession>>() with eagerSingleton { HashSet<DrillWsSession>() }
    bind<AgentManager>() with eagerSingleton { AgentManager(kodein) }
}

val devContainer = Kodein.Module(name = "devContainer") {
    bind<DevEndpoints>() with eagerSingleton { DevEndpoints(kodein) }
}

val userSource: UserSource = UserSourceImpl()

@KtorExperimentalLocationsAPI
@ObsoleteCoroutinesApi
val handlers = Kodein.Module(name = "handlers") {
    bind<SwaggerDrillAdminServer>() with eagerSingleton { SwaggerDrillAdminServer(kodein) }
    bind<PluginDispatcher>() with eagerSingleton { PluginDispatcher(kodein) }
    bind<DrillOtherHandlers>() with eagerSingleton { DrillOtherHandlers(kodein) }
}


val wsHandlers = Kodein.Module(name = "wsHandlers") {
    bind<AgentHandler>() with eagerSingleton { AgentHandler(kodein) }
    bind<WsService>() with eagerSingleton { DrillPluginWs(kodein) }
    bind<DrillServerWs>() with eagerSingleton { DrillServerWs(kodein) }
}

val pluginServices = Kodein.Module(name = "pluginServices") {
    bind<Plugins>() with singleton { Plugins() }
    bind<AgentPlugins>() with eagerSingleton { AgentPlugins(kodein) }
}

var installation: Application.() -> Unit ={
    @Suppress("UNUSED_VARIABLE") val jwtAudience = environment.config.property("jwt.audience").getString()
    val jwtRealm = environment.config.property("jwt.realm").getString()

    install(ContentNegotiation) {
        register(ContentType.Application.Json, GsonConverter())
    }
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

@ObsoleteCoroutinesApi
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

