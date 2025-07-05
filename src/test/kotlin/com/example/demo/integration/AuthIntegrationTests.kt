package com.example.demo.integration

import com.example.demo.adapter.web.AuthController
import com.example.demo.domain.port.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.time.temporal.ChronoUnit

@SpringBootTest
class AuthIntegrationTests {

    @Autowired
    private lateinit var context: ApplicationContext

    @Autowired
    private lateinit var repository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    private lateinit var client: WebTestClient

    @BeforeEach
    fun setup() {
        client = WebTestClient.bindToApplicationContext(context).build()
        // In-memory repository is used for tests, clearing it ensures test isolation
        (repository as? com.example.demo.adapter.inmemory.InMemoryUserRepository)?.clear()
    }

    @Test
    fun `test full signup, verification and login flow`() {
        // 1. Signup
        val signupRequest = AuthController.SignupRequest(
            name = "Test User",
            email = "test@example.com",
            password = "password123",
            gender = "OTHER",
            birthYear = 1990
        )

        client.post().uri("/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(signupRequest)
            .exchange()
            .expectStatus().isCreated
            .expectBody<Map<String, Any>>()
            .consumeWith { result ->
                val body = result.responseBody!!
                assert(body["name"] == "Test User")
                assert(body["email"] == "test@example.com")
                assert(body["verified"] == false)
                assert(!body.containsKey("password")) // Password should not be returned
            }

        // 2. Verify user is in repository with verification code
        val user = kotlinx.coroutines.runBlocking { repository.findByEmail("test@example.com") }
        assert(user != null)
        assert(user!!.verified == false)
        assert(user.verificationCode != null)
        assert(passwordEncoder.matches("password123", user.password))

        // 3. Verify email with correct code
        val verifyRequest = AuthController.VerifyRequest(
            email = "test@example.com",
            code = user.verificationCode!!
        )

        client.post().uri("/auth/verify")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(verifyRequest)
            .exchange()
            .expectStatus().isOk

        val verifiedUser = kotlinx.coroutines.runBlocking { repository.findByEmail("test@example.com") }
        assert(verifiedUser != null)
        assert(verifiedUser!!.verified == true)
        assert(verifiedUser.verificationCode == null)

        // 4. Login with correct credentials
        val loginRequest = AuthController.LoginRequest(
            email = "test@example.com",
            password = "password123"
        )

        client.post().uri("/auth/token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(loginRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody<Map<String, String>>()
            .consumeWith { result ->
                val token = result.responseBody!!["token"]
                assert(token != null && token.isNotEmpty())
            }
    }

    @Test
    fun `test re-signup with unverified email replaces old account`() {
        // 1. Create initial unverified user
        val initialSignup = AuthController.SignupRequest(
            name = "Old User", email = "unverified-res-signup@example.com", password = "password123",
            gender = "OTHER", birthYear = 1990
        )
        client.post().uri("/auth/signup").contentType(MediaType.APPLICATION_JSON).bodyValue(initialSignup).exchange().expectStatus().isCreated
        val oldUser = kotlinx.coroutines.runBlocking { repository.findByEmail("unverified-res-signup@example.com") }
        assert(oldUser != null && oldUser.name == "Old User")

        // 2. Attempt to sign up again with the same email but different data
        val newSignup = AuthController.SignupRequest(
            name = "New User", email = "unverified-res-signup@example.com", password = "password456",
            gender = "MALE", birthYear = 1995
        )
        client.post().uri("/auth/signup").contentType(MediaType.APPLICATION_JSON).bodyValue(newSignup).exchange().expectStatus().isCreated

        // 3. Verify that the old user is replaced by the new one
        val newUser = kotlinx.coroutines.runBlocking { repository.findByEmail("unverified-res-signup@example.com") }
        assert(newUser != null)
        assert(newUser!!.id != oldUser!!.id)
        assert(newUser.name == "New User")
        assert(passwordEncoder.matches("password456", newUser.password))
    }

    @Test
    fun `test signup with verified email fails`() {
        // 1. Create and verify a user
        val signupRequest = AuthController.SignupRequest(
            name = "Verified User", email = "verified-duplicate@example.com", password = "password123",
            gender = "FEMALE", birthYear = 1980
        )
        client.post().uri("/auth/signup").contentType(MediaType.APPLICATION_JSON).bodyValue(signupRequest).exchange().expectStatus().isCreated
        val user = kotlinx.coroutines.runBlocking { repository.findByEmail("verified-duplicate@example.com") }!!
        val verifyRequest = AuthController.VerifyRequest(email = user.email, code = user.verificationCode!!)
        client.post().uri("/auth/verify").contentType(MediaType.APPLICATION_JSON).bodyValue(verifyRequest).exchange().expectStatus().isOk

        // 2. Attempt to sign up again with the same email
        val duplicateSignup = AuthController.SignupRequest(
            name = "Another User", email = "verified-duplicate@example.com", password = "password456",
            gender = "MALE", birthYear = 1995
        )
        client.post().uri("/auth/signup").contentType(MediaType.APPLICATION_JSON).bodyValue(duplicateSignup).exchange().expectStatus().is5xxServerError
    }


    @Test
    fun `test email verification with wrong code fails`() {
        // 1. Signup
        val signupRequest = AuthController.SignupRequest(
            name = "Verify User", email = "verify@example.com", password = "password123",
            gender = "FEMALE", birthYear = 2000
        )
        client.post().uri("/auth/signup").contentType(MediaType.APPLICATION_JSON).bodyValue(signupRequest).exchange().expectStatus().isCreated

        // 2. Attempt to verify with a wrong code
        val verifyRequest = AuthController.VerifyRequest(email = "verify@example.com", code = "000000")
        client.post().uri("/auth/verify").contentType(MediaType.APPLICATION_JSON).bodyValue(verifyRequest).exchange().expectStatus().isBadRequest

        // 3. Check user is still not verified
        val user = kotlinx.coroutines.runBlocking { repository.findByEmail("verify@example.com") }
        assert(user != null && user.verified == false)
    }

    @Test
    fun `test login with unverified user fails`() {
        // 1. Signup but do not verify
        val signupRequest = AuthController.SignupRequest(
            name = "Unverified User", email = "unverified@example.com", password = "password123",
            gender = "OTHER", birthYear = 1990
        )
        client.post().uri("/auth/signup").contentType(MediaType.APPLICATION_JSON).bodyValue(signupRequest).exchange().expectStatus().isCreated

        // 2. Attempt to login
        val loginRequest = AuthController.LoginRequest(email = "unverified@example.com", password = "password123")
        client.post().uri("/auth/token").contentType(MediaType.APPLICATION_JSON).bodyValue(loginRequest).exchange().expectStatus().isForbidden
    }

    @Test
    fun `test login with wrong password fails`() {
        // 1. Signup and verify
        val signupRequest = AuthController.SignupRequest(
            name = "WrongPass User", email = "wrongpass@example.com", password = "password123",
            gender = "MALE", birthYear = 1985
        )
        client.post().uri("/auth/signup").contentType(MediaType.APPLICATION_JSON).bodyValue(signupRequest).exchange().expectStatus().isCreated
        val user = kotlinx.coroutines.runBlocking { repository.findByEmail("wrongpass@example.com") }!!
        val verifyRequest = AuthController.VerifyRequest(email = user.email, code = user.verificationCode!!)
        client.post().uri("/auth/verify").contentType(MediaType.APPLICATION_JSON).bodyValue(verifyRequest).exchange().expectStatus().isOk

        // 2. Attempt to login with wrong password
        val loginRequest = AuthController.LoginRequest(email = "wrongpass@example.com", password = "wrongpassword")
        client.post().uri("/auth/token").contentType(MediaType.APPLICATION_JSON).bodyValue(loginRequest).exchange().expectStatus().isUnauthorized
    }

    @Test
    fun `test verification with expired code fails`() {
        // 1. Signup
        val signupRequest = AuthController.SignupRequest(
            name = "Expired User", email = "expired@example.com", password = "password123",
            gender = "FEMALE", birthYear = 1999
        )
        client.post().uri("/auth/signup").contentType(MediaType.APPLICATION_JSON).bodyValue(signupRequest).exchange().expectStatus().isCreated

        // 2. Manually expire the code
        val user = kotlinx.coroutines.runBlocking { repository.findByEmail("expired@example.com") }!!
        val expiredUser = user.copy(verificationCodeExpiresAt = java.time.Instant.now().minus(1, ChronoUnit.DAYS))
        kotlinx.coroutines.runBlocking { repository.save(expiredUser) }

        // 3. Attempt to verify with the (now expired) code
        val verifyRequest = AuthController.VerifyRequest(email = "expired@example.com", code = user.verificationCode!!)
        client.post().uri("/auth/verify").contentType(MediaType.APPLICATION_JSON).bodyValue(verifyRequest).exchange().expectStatus().isBadRequest

        // 4. Check user is still not verified
        val finalUser = kotlinx.coroutines.runBlocking { repository.findByEmail("expired@example.com") }
        assert(finalUser != null && finalUser.verified == false)
    }

    @Test
    fun `test resend verification email and cooldown`() {
        // 1. Signup
        val signupRequest = AuthController.SignupRequest(
            name = "Resend User", email = "resend@example.com", password = "password123",
            gender = "MALE", birthYear = 1992
        )
        client.post().uri("/auth/signup").contentType(MediaType.APPLICATION_JSON).bodyValue(signupRequest).exchange().expectStatus().isCreated

        val initialUser = kotlinx.coroutines.runBlocking { repository.findByEmail("resend@example.com") }!!
        val initialCode = initialUser.verificationCode

        // 2. Immediately try to resend, expecting cooldown
        val resendRequest = AuthController.ResendVerificationRequest(email = "resend@example.com")
        client.post().uri("/auth/resend-verification").contentType(MediaType.APPLICATION_JSON).bodyValue(resendRequest).exchange().expectStatus().isEqualTo(429)

        // 3. Manually advance the sent time to bypass cooldown
        val userWithAdvancedTime = initialUser.copy(verificationEmailSentAt = java.time.Instant.now().minus(1, ChronoUnit.MINUTES))
        kotlinx.coroutines.runBlocking { repository.save(userWithAdvancedTime) }

        // 4. Request to resend again, this time it should succeed
        client.post().uri("/auth/resend-verification").contentType(MediaType.APPLICATION_JSON).bodyValue(resendRequest).exchange().expectStatus().isOk

        // 5. Check that the verification code has changed
        val updatedUser = kotlinx.coroutines.runBlocking { repository.findByEmail("resend@example.com") }!!
        assert(updatedUser.verificationCode != null)
        assert(updatedUser.verificationCode != initialCode)

        // 6. Verify with the new code
        val verifyRequest = AuthController.VerifyRequest(email = "resend@example.com", code = updatedUser.verificationCode!!)
        client.post().uri("/auth/verify").contentType(MediaType.APPLICATION_JSON).bodyValue(verifyRequest).exchange().expectStatus().isOk

        // 7. Check user is now verified
        val finalUser = kotlinx.coroutines.runBlocking { repository.findByEmail("resend@example.com") }!!
        assert(finalUser.verified)
    }
}