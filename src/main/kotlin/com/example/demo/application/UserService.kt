package com.example.demo.application

import com.example.demo.domain.model.User
import com.example.demo.domain.port.UserRepository
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service

@Service
class UserService(private val repository: UserRepository) {
    suspend fun createUser(
        name: String,
        gender: String,
        birthYear: Int,
        profileImageUrls: List<String>,
        location: String?,
        preferredLanguage: String?,
        aboutMe: String?,
        role: String = "USER",
    ): User = repository.save(
        User(
            name = name,
            gender = gender,
            birthYear = birthYear,
            profileImageUrls = profileImageUrls,
            location = location,
            preferredLanguage = preferredLanguage,
            aboutMe = aboutMe,
            role = role
        )
    )

    fun getUsers(): Flow<User> = repository.findAll()

    suspend fun getUser(id: String): User? = repository.findById(id)

    suspend fun deleteUser(id: String): Boolean = repository.deleteById(id)

    suspend fun suspendUser(id: String, until: java.time.Instant): User? {
        val user = repository.findById(id) ?: return null
        val updated = user.copy(suspendedUntil = until)
        repository.save(updated)
        return updated
    }

    suspend fun isSuspended(id: String): Boolean {
        val user = repository.findById(id) ?: return false
        val until = user.suspendedUntil ?: return false
        return java.time.Instant.now().isBefore(until)
    }
}
