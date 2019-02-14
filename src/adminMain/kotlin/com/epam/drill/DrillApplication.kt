package com.epam.drill

import com.epam.drill.agentmanager.AgentStorage
import com.epam.drill.endpoints.*
import com.epam.drill.endpoints.openapi.DevEndpoints
import com.epam.drill.endpoints.openapi.SwaggerDrillAdminServer
import com.epam.drill.jwt.config.JwtConfig
import com.epam.drill.plugin.api.end.WsService
import com.epam.drill.plugins.AgentPlugins
import com.epam.drill.plugins.Plugins
import com.epam.drill.storage.MongoClient
import com.soywiz.klogger.Logger
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.*
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.features.CORS
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.gson.GsonConverter
import io.ktor.html.respondHtml
import io.ktor.http.ContentType
import io.ktor.http.ContentType.Application.Xml
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.defaultResource
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Locations
import io.ktor.locations.url
import io.ktor.response.respondBytes
import io.ktor.response.respondFile
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.html.*
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.eagerSingleton
import org.kodein.di.generic.singleton
import java.io.File
import java.time.Duration

val logger = Logger("DrillApplication").apply { level =Logger.Level.TRACE }

val storage = Kodein.Module(name = "agentStorage") {
    bind<AgentStorage>() with singleton { AgentStorage(kodein) }
    bind<MongoClient>() with singleton { MongoClient() }
}

val devContainer = Kodein.Module(name = "devContainer") {
    bind<DevEndpoints>() with eagerSingleton { DevEndpoints(kodein) }
}
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
fun Application.module(installation: Application.() -> Unit = {
    val jwtAudience = environment.config.property("jwt.audience").getString()
    val jwtRealm = environment.config.property("jwt.realm").getString()

    install(ContentNegotiation) {
        register(ContentType.Application.Json, GsonConverter())
    }
//    install(CORS) {
//
//        method(HttpMethod.Options)
//        method(HttpMethod.Put)
//        method(HttpMethod.Delete)
//        method(HttpMethod.Patch)
//        header(HttpHeaders.Authorization)
//        header("MyCustomHeader")
//        allowCredentials = true
//        anyHost()
//    }
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
            validate { credential ->
                if (credential.payload.audience.contains(jwtAudience)) JWTPrincipal(credential.payload) else null
            }
        }
    }
}) {
    kodeinApplication {
        installation()

        routing {
            get("/login") {
                val resource = this::class.java.getResource("/public/index.html")
                call.respondBytes(resource.openStream().readBytes(), contentType = ContentType.Text.Html)
            }

            authenticate {
                get("/plugin/{xx}/{x1}") {
                    val resource = this::class.java.getResource("/public/index.html")
                    call.respondBytes(resource.openStream().readBytes(), contentType = ContentType.Any)
                }
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
        logger.debug { "applicaton is running" }
    }

}


fun Application.kodeinApplication(kodeinMapper: Kodein.MainBuilder.(Application) -> Unit = {}) {
    val app = this
    Kodein {
        bind<Application>() with singleton { app }
        kodeinMapper(this, app)
    }
}

