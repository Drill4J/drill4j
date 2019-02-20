package com.epam.drill.jwt.user

import io.ktor.auth.Principal

data class User(
    val id: Int,
    val name: String,
    val password: String ,
    val role: String
) : Principal