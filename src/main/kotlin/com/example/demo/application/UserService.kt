package com.example.demo.application

import com.example.demo.domain.model.User
import com.example.demo.domain.port.UserRepository
import kotlinx.coroutines.flow.Flow
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Handles user persistence and suspension logic.
 * This service abstracts the underlying repository implementations.
 */
@Service
class UserService(private val repository: UserRepository) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun createUser(
        name: String,
        gender: String,
        birthYear: Int,
        profileImageUrls: List<String>,
        location: String?,
        preferredLanguage: String?,
        aboutMe: String?,
        role: String = "USER",
    ): User {
        log.info("Creating user with name: $name, role: $role")
        return repository.save(
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
    }

    fun getUsers(): Flow<User> {
        log.info("Getting all users")
        return repository.findAll()
    }

    suspend fun getUser(id: String): User? {
        log.info("Getting user with id: $id")
        return repository.findById(id)
    }

    suspend fun deleteUser(id: String): Boolean {
        log.info("Deleting user with id: $id")
        return repository.deleteById(id)
    }

    suspend fun suspendUser(id: String, until: java.time.Instant): User? {
        log.info("Suspending user with id: $id until: $until")
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
