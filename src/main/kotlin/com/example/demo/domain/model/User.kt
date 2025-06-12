package com.example.demo.domain.model

import java.util.UUID

data class User(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val gender: String,
    val birthYear: Int,
    val profileImageUrls: List<String> = emptyList(),
    val location: String? = null,
    val preferredLanguage: String? = null,
    val aboutMe: String? = null,
    val role: String = "USER",
    val suspendedUntil: java.time.Instant? = null,
)
