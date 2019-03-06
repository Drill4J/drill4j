package com.epam.drill.exception

import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable

@Serializable
data class ExceptionConfig(@Optional val id: String="", val blackList: Set<String>)