package com.epam.drill.jwt.user.source

import com.epam.drill.jwt.user.User
import io.ktor.auth.UserPasswordCredential

interface UserSource {
    fun findUserById(id: Int): User

    fun findUserByCredentials(credential: UserPasswordCredential): User
}