package com.example.demo.domain.model

import java.time.Instant
import java.util.UUID

data class Notification(
    val id: String = UUID.randomUUID().toString(),
    val userId: String, // The user who should receive the notification
    val sourcePostId: String,
    val sourceCommentId: String?,
    val triggeringAnonymousId: String,
    val message: String,
    val read: Boolean = false,
    val createdAt: Instant = Instant.now()
)
