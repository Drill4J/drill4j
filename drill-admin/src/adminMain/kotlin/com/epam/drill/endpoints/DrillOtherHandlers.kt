@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.epam.drill.endpoints

import com.epam.drill.plugins.Plugins
import com.epam.drill.router.Routes
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.html.respondHtml
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.routing
import kotlinx.html.*
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance


class DrillOtherHandlers(override val kodein: Kodein) : KodeinAware {
    private val app: Application by instance()
    private val plugins: Plugins by kodein.instance()

    init {
        app.routing {
            authenticate {
                get<Routes.Api.PluginConfiguration> {

                    call.respond(plugins.plugins.keys.toTypedArray())

                }
            }

            authenticate {
                get<Routes.Api.PluginContent> { ll ->

                    call.respondHtml {
                        head {
                            script { src = "/${ll.pluginId}.js" }
                        }
                        body {
                            div {
                                +"CONTENT OF THE \"${ll.pluginId}\" PLUGIN"
                            }
                            div {
                                id = "root"
                            }
                        }
                    }


                }
            }

        }
    }
}
