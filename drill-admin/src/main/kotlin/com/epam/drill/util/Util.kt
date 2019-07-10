package com.epam.drill.util

import com.epam.drill.common.*
import io.ktor.application.*
import io.ktor.request.*
import kotlinx.serialization.*


//todo temp fixed for client side.
suspend inline fun <reified T : Any> ApplicationCall.parse(ser: KSerializer<T>): T =
    ser parse receive()