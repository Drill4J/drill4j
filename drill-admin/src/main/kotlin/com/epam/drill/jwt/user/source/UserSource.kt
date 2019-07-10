package com.epam.drill.jwt.user.source

import com.epam.drill.jwt.user.*
import io.ktor.auth.*

interface UserSource {
    fun findUserById(id: Int): User

    fun findUserByCredentials(credential: UserPasswordCredential): User
}