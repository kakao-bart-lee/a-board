package com.example.demo.domain.model

import com.fasterxml.jackson.annotation.JsonIgnore
import java.util.UUID

/**
 * Represents a registered user. Posts created by this user reference the id
 * internally but never expose it publicly.
 */
data class User(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val email: String,
    @JsonIgnore
    val password: String,
    val gender: String,
    val birthYear: Int,
    val profileImageUrls: List<String> = emptyList(),
    val location: String? = null,
    val preferredLanguage: String? = null,
    val aboutMe: String? = null,
    val role: String = "USER",
    val suspendedUntil: java.time.Instant? = null,
    val verified: Boolean = false,
    @JsonIgnore
    val verificationCode: String? = null
)
