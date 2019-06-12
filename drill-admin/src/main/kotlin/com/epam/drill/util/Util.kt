package com.epam.drill.util

import com.epam.drill.common.parse
import io.ktor.application.ApplicationCall
import io.ktor.request.receive
import kotlinx.serialization.KSerializer


//todo temp fixed for client side.
suspend inline fun <reified T : Any> ApplicationCall.parse(ser: KSerializer<T>): T =
    ser parse receive()