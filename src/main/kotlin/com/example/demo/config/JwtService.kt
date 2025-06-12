package com.example.demo.config

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.Date

@Component
class JwtService {
    private val algorithm = Algorithm.HMAC256("secret")

    fun generateToken(userId: String, role: String, anonId: String): String {
        val now = Instant.now()
        return JWT.create()
            .withSubject(userId)
            .withClaim("role", role)
            .withClaim("anon", anonId)
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(now.plusSeconds(3600)))
            .sign(algorithm)
    }

    fun verify(token: String): DecodedJWT? = try {
        JWT.require(algorithm).build().verify(token)
    } catch (ex: JWTVerificationException) {
        null
    }
}
