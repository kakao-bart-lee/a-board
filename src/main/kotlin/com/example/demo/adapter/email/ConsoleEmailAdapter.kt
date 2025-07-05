package com.example.demo.adapter.email

import com.example.demo.domain.port.EmailPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ConsoleEmailAdapter : EmailPort {
    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun sendVerificationEmail(email: String, code: String) {
        log.info("--- Email Sending Simulation ---")
        log.info("To: $email")
        log.info("Subject: Please verify your email address")
        log.info("Body: Your verification code is: $code")
        log.info("-----------------------------")
    }
}
