package com.epam.drill.common

import kotlinx.serialization.Serializable

@Serializable
data class MessageWithId(
    val id: String,
    val message: String
)