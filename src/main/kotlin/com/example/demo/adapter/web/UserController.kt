package com.example.demo.adapter.web

import com.example.demo.application.UserService
import com.example.demo.domain.model.User
import kotlinx.coroutines.flow.Flow
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/users")
/**
 * Public endpoints for managing users. Only user creation is open to
 * unauthenticated clients; everything else requires a valid JWT.
 */
class UserController(private val service: UserService) {
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
    suspend fun create(@RequestBody req: CreateUserRequest): User =
        service.createUser(
            req.name,
            req.gender,
            req.birthYear,
            req.profileImageUrls,
            req.location,
            req.preferredLanguage,
            req.aboutMe,
            req.role
        )

    @GetMapping
    suspend fun list(): Flow<User> = service.getUsers()

    @GetMapping("/{id}")
    suspend fun get(@PathVariable id: String): User? = service.getUser(id)

    @DeleteMapping("/{id}")
    suspend fun delete(@PathVariable id: String) {
        service.deleteUser(id)
    }

    data class SuspendRequest(val minutes: Long)

    @PostMapping("/{id}/suspend")
    suspend fun suspend(@PathVariable id: String, @RequestBody req: SuspendRequest): User? {
        val until = java.time.Instant.now().plusSeconds(req.minutes * 60)
        return service.suspendUser(id, until)
    }
}
