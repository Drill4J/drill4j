package com.epam.drill.session

object DrillRequest {

    external fun currentSession(): String?
    external fun currentHeaders(): String?

}