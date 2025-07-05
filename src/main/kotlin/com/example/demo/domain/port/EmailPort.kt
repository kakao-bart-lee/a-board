package com.example.demo.domain.port

interface EmailPort {
    suspend fun sendVerificationEmail(email: String, code: String)
}
