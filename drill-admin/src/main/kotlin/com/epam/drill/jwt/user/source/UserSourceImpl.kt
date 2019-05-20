package com.epam.drill.jwt.user.source

import com.epam.drill.jwt.user.User
import io.ktor.auth.UserPasswordCredential

class UserSourceImpl : UserSource {

    val testUser = User(1, "guest", "", "admin")

    override fun findUserById(id: Int): User = users.getValue(id)

    override fun findUserByCredentials(credential: UserPasswordCredential): User = testUser

    private val users = listOf(testUser).associateBy(User::id)


}