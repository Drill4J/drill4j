@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.epam.drill.router

import io.ktor.locations.Location

object DevRoutes {
    @Location("/api-dev")
    class Api {
        @Location("/agent")
        class Agent {
            @Location("/get-all-agents")
            class Agents
        }
    }
}