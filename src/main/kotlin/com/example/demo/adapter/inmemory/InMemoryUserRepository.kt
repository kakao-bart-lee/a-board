package com.example.demo.adapter.inmemory

import com.example.demo.domain.model.User
import com.example.demo.domain.port.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository

@Repository
@Profile("dev")
@Primary
class InMemoryUserRepository : UserRepository {
    private val users = mutableListOf<User>()

    override suspend fun save(user: User): User {
        val idx = users.indexOfFirst { it.id == user.id }
        if (idx >= 0) {
            users[idx] = user
        } else {
            users += user
        }
        return user
    }

    override fun findAll(): Flow<User> = users.asFlow()

    override suspend fun findById(id: String): User? = users.find { it.id == id }

    override suspend fun findByEmail(email: String): User? = users.find { it.email == email }

    override suspend fun deleteById(id: String): Boolean {
        return users.removeIf { it.id == id }
    }

    fun clear() {
        users.clear()
    }
}
