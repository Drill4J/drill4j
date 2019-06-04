package com.epam.drill.core.util

import kotlinx.serialization.json.Json
import kotlin.native.concurrent.SharedImmutable


@SharedImmutable
val json: Json.Companion = Json.Companion