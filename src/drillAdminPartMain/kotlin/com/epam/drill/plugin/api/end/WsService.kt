package com.epam.drill.plugin.api.end

interface WsService {
    suspend fun convertAndSend(destination: String, message: Any)
    fun getPlWsSession():Set<String>
}
