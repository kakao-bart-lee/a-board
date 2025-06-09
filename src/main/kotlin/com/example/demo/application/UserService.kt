package com.example.demo.application

import com.example.demo.domain.model.User
import com.example.demo.domain.port.UserRepository
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service

@Service
class UserService(private val repository: UserRepository) {
    suspend fun createUser(name: String): User = repository.save(User(name = name))
    fun getUsers(): Flow<User> = repository.findAll()
    suspend fun getUser(id: String): User? = repository.findById(id)
}
