package com.example.demo.adapter.web

import com.example.demo.application.UserService
import com.example.demo.domain.model.User
import kotlinx.coroutines.flow.Flow
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/users")
/**
 * Public endpoints for managing users. Only user creation is open to
 * unauthenticated clients; everything else requires a valid JWT.
 */
class UserController(private val service: UserService) {
    private val log = LoggerFactory.getLogger(javaClass)

    data class CreateUserRequest(
        val name: String,
        val gender: String,
        val birthYear: Int,
        val profileImageUrls: List<String> = emptyList(),
        val location: String? = null,
        val preferredLanguage: String? = null,
        val aboutMe: String? = null,
        val role: String = "USER",
    )

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun create(@RequestBody req: CreateUserRequest): User {
        log.info("Creating user with name: ${req.name}")
        return service.createUser(
            req.name,
            req.gender,
            req.birthYear,
            req.profileImageUrls,
            req.location,
            req.preferredLanguage,
            req.aboutMe,
            req.role
        )
    }

    @GetMapping
    suspend fun list(): Flow<User> {
        log.info("Listing all users")
        return service.getUsers()
    }

    @GetMapping("/{id}")
    suspend fun get(@PathVariable id: String): User? {
        log.info("Getting user with id: $id")
        return service.getUser(id)
    }

    @DeleteMapping("/{id}")
    suspend fun delete(@PathVariable id: String) {
        log.info("Deleting user with id: $id")
        service.deleteUser(id)
    }

    data class SuspendRequest(val minutes: Long)

    @PostMapping("/{id}/suspend")
    suspend fun suspend(@PathVariable id: String, @RequestBody req: SuspendRequest): User? {
        log.info("Suspending user with id: $id for ${req.minutes} minutes")
        val until = java.time.Instant.now().plusSeconds(req.minutes * 60)
        return service.suspendUser(id, until)
    }
}
