package com.example.demo.domain.model

import java.util.UUID

data class User(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val gender: String,
    val birthYear: Int,
    val profileImageUrl: String
)
