package dev.tohure.tanayenai.domain.repository

import dev.tohure.tanayenai.domain.model.User

interface UserRepository {
    suspend fun getUser(id: String): User?

    suspend fun saveUser(user: User)

    suspend fun updateUser(user: User)
}
