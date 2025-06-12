package com.example.demo.domain.port

import com.example.demo.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun save(user: User): User
    fun findAll(): Flow<User>
    suspend fun findById(id: String): User?
    suspend fun deleteById(id: String): Boolean
}
