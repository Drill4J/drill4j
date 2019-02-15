package com.epam.drill.jwt.user

import io.ktor.auth.Principal

data class User(
    val id: Int = 1,
    val name: String = "guest",
    val password: String = "",
    val role: String = "admin"
) : Principal