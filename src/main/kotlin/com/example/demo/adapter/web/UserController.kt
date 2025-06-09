package com.example.demo.adapter.web

import com.example.demo.application.UserService
import com.example.demo.domain.model.User
import kotlinx.coroutines.flow.Flow
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/users")
class UserController(private val service: UserService) {
    data class CreateUserRequest(val name: String)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun create(@RequestBody req: CreateUserRequest): User =
        service.createUser(req.name)

    @GetMapping
    suspend fun list(): Flow<User> = service.getUsers()

    @GetMapping("/{id}")
    suspend fun get(@PathVariable id: String): User? = service.getUser(id)
}
