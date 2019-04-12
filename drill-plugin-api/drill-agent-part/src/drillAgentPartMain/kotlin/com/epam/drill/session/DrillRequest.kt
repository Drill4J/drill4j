package com.epam.drill.session

object DrillRequest {

    external fun currentSession(): String?

    external operator fun get(key: String): String?

}