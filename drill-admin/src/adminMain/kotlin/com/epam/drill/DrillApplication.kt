package com.epam.drill

import com.epam.drill.agentmanager.AgentStorage
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
import com.epam.drill.storage.MongoClient
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.UserPasswordCredential
import io.ktor.auth.authenticate
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
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Locations
import io.ktor.locations.post
import io.ktor.locations.get
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.eagerSingleton
import org.kodein.di.generic.singleton
import java.time.Duration

val storage = Kodein.Module(name = "agentStorage") {
    bind<AgentStorage>() with singleton { AgentStorage(kodein) }
    bind<MongoClient>() with singleton { MongoClient() }
}

val devContainer = Kodein.Module(name = "devContainer") {
    bind<DevEndpoints>() with eagerSingleton { DevEndpoints(kodein) }
}

val userSource: UserSource = UserSourceImpl()

@KtorExperimentalLocationsAPI
@ObsoleteCoroutinesApi
val handlers = Kodein.Module(name = "handlers") {
    bind<AgentHandler>() with eagerSingleton { AgentHandler(kodein) }
    bind<SwaggerDrillAdminServer>() with eagerSingleton { SwaggerDrillAdminServer(kodein) }
    bind<PluginDispatcher>() with eagerSingleton { PluginDispatcher(kodein) }
    bind<WsService>() with eagerSingleton { DrillPluginWs(kodein) }
    bind<DrillServerWs>() with eagerSingleton { DrillServerWs(kodein) }
    bind<DrillOtherHandlers>() with eagerSingleton { DrillOtherHandlers(kodein) }
}

val pluginServices = Kodein.Module(name = "pluginServices") {
    bind<Plugins>() with singleton { Plugins() }
    bind<AgentPlugins>() with eagerSingleton { AgentPlugins(kodein) }
}

@ObsoleteCoroutinesApi
@KtorExperimentalLocationsAPI
@ExperimentalCoroutinesApi
fun Application.module(
    installation: Application.() -> Unit = {
        val jwtAudience = environment.config.property("jwt.audience").getString()
        val jwtRealm = environment.config.property("jwt.realm").getString()

        install(ContentNegotiation) {
            register(ContentType.Application.Json, GsonConverter())
        }
        install(StatusPages)
        install(CallLogging)
        install(Locations)
        install(io.ktor.websocket.WebSockets) {
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
            exposeHeader(HttpHeaders.Authorization)
        }
    }
) {
    kodeinApplication {
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

        import(storage)
        import(handlers)
        import(pluginServices)

        val devMode = true
        if (devMode) {
            import(devContainer)
        }
    }

}


fun Application.kodeinApplication(kodeinMapper: Kodein.MainBuilder.(Application) -> Unit = {}) {
    val app = this
    Kodein {
        bind<Application>() with singleton { app }
        kodeinMapper(this, app)
    }
}

