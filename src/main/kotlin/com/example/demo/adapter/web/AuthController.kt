package com.example.demo.adapter.web

import com.example.demo.domain.port.UserRepository
import com.example.demo.config.JwtService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/auth")
/**
 * Issues JWT tokens that carry the user's id and role. The token also
 * contains a random anonymous id used when creating posts so the real
 * user id never appears in the post data.
 */
class AuthController(
    private val userRepo: UserRepository,
    private val jwtService: JwtService
) {
    data class TokenRequest(val userId: String)
    data class TokenResponse(val token: String)

    @PostMapping("/token")
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun token(@RequestBody req: TokenRequest): TokenResponse {
        val user = userRepo.findById(req.userId)
            ?: throw IllegalArgumentException("user not found")
        val anonId = java.util.UUID.randomUUID().toString()
        val token = jwtService.generateToken(user.id!!, user.role, anonId)
        return TokenResponse(token)
    }
}
