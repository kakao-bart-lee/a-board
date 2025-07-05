package com.example.demo.domain.port

import com.example.demo.domain.model.Notification

interface NotificationRepository {
    suspend fun save(notification: Notification): Notification
    suspend fun findByUserId(userId: String): List<Notification>
    suspend fun markAsRead(notificationId: String)
}
