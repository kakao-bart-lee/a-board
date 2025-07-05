package com.example.demo.application

import com.example.demo.domain.model.User
import com.example.demo.domain.port.EmailPort
import com.example.demo.domain.port.UserRepository
import kotlinx.coroutines.flow.Flow
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import kotlin.random.Random

/**
 * Handles user persistence and suspension logic.
 * This service abstracts the underlying repository implementations.
 */
@Service
class UserService(
    private val repository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val emailPort: EmailPort
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun signup(
        name: String,
        email: String,
        password: String,
        gender: String,
        birthYear: Int,
        profileImageUrls: List<String>,
        location: String?,
        preferredLanguage: String?,
        aboutMe: String?,
        role: String = "USER",
    ): User {
        log.info("Signing up user with email: $email")
        if (repository.findByEmail(email) != null) {
            throw IllegalArgumentException("User with email $email already exists")
        }

        val verificationCode = Random.nextInt(100000, 999999).toString()
        val user = User(
            name = name,
            email = email,
            password = passwordEncoder.encode(password),
            gender = gender,
            birthYear = birthYear,
            profileImageUrls = profileImageUrls,
            location = location,
            preferredLanguage = preferredLanguage,
            aboutMe = aboutMe,
            role = role,
            verificationCode = verificationCode
        )

        val savedUser = repository.save(user)
        emailPort.sendVerificationEmail(email, verificationCode)
        return savedUser
    }

    suspend fun verifyEmail(email: String, code: String): Boolean {
        log.info("Verifying email for: $email")
        val user = repository.findByEmail(email) ?: return false
        if (user.verificationCode == code) {
            val updatedUser = user.copy(verified = true, verificationCode = null)
            repository.save(updatedUser)
            return true
        }
        return false
    }

    fun getUsers(): Flow<User> {
        log.info("Getting all users")
        return repository.findAll()
    }

    suspend fun getUser(id: String): User? {
        log.info("Getting user with id: $id")
        return repository.findById(id)
    }

    suspend fun getUserByEmail(email: String): User? {
        log.info("Getting user with email: $email")
        return repository.findByEmail(email)
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
