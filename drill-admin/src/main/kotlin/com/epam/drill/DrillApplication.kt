package com.epam.drill

import com.epam.drill.cache.*
import com.epam.drill.cache.impl.*
import com.epam.drill.endpoints.*
import com.epam.drill.endpoints.agent.*
import com.epam.drill.endpoints.openapi.*
import com.epam.drill.endpoints.plugin.*
import com.epam.drill.jwt.config.*
import com.epam.drill.jwt.user.source.*
import com.epam.drill.plugin.api.end.*
import com.epam.drill.plugins.*
import com.epam.drill.router.*
import com.epam.drill.service.*
import com.epam.drill.storage.*
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.locations.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.websocket.*
import org.kodein.di.*
import org.kodein.di.generic.*
import java.io.*
import java.time.*


val drillHomeDir = File(System.getenv("DRILL_HOME") ?: "")

val drillWorkDir = drillHomeDir.resolve("work")

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
    bind<Sender>() with eagerSingleton { DrillPluginWs(kodein) }
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

