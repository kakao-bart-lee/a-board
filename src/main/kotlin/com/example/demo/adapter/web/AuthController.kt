package com.example.demo.adapter.web

import com.example.demo.application.UserService
import com.example.demo.config.JwtService
import com.example.demo.domain.model.User
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/auth")
class AuthController(
    private val userService: UserService,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder
) {
    private val log = LoggerFactory.getLogger(javaClass)

    data class SignupRequest(
        val name: String,
        val email: String,
        val password: String,
        val gender: String,
        val birthYear: Int,
        val profileImageUrls: List<String> = emptyList(),
        val location: String? = null,
        val preferredLanguage: String? = null,
        val aboutMe: String? = null,
    )

    data class VerifyRequest(
        val email: String,
        val code: String
    )

    data class LoginRequest(
        val email: String,
        val password: String
    )

    data class TokenResponse(val token: String)

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun signup(@RequestBody req: SignupRequest): User {
        log.info("Signup request for email: ${req.email}")
        return userService.signup(
            name = req.name,
            email = req.email,
            password = req.password,
            gender = req.gender,
            birthYear = req.birthYear,
            profileImageUrls = req.profileImageUrls,
            location = req.location,
            preferredLanguage = req.preferredLanguage,
            aboutMe = req.aboutMe
        )
    }

    @PostMapping("/verify")
    suspend fun verify(@RequestBody req: VerifyRequest): ResponseEntity<Void> {
        log.info("Verification request for email: ${req.email}")
        val success = userService.verifyEmail(req.email, req.code)
        return if (success) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/token")
    suspend fun token(@RequestBody req: LoginRequest): ResponseEntity<TokenResponse> {
        log.info("Token request for email: ${req.email}")
        val user = userService.getUserByEmail(req.email)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        if (!user.verified) {
            log.warn("User not verified: ${req.email}")
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        if (passwordEncoder.matches(req.password, user.password)) {
            val anonId = java.util.UUID.randomUUID().toString()
            val token = jwtService.generateToken(user.id, user.role, anonId)
            log.info("Token generated for user: ${user.id}")
            return ResponseEntity.ok(TokenResponse(token))
        } else {
            log.warn("Invalid password for user: ${req.email}")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
    }
}
