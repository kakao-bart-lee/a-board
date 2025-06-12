package com.example.demo.adapter.web

import com.example.demo.application.UserService
import com.example.demo.domain.model.User
import kotlinx.coroutines.flow.Flow
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/users")
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
}
