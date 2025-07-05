package com.example.demo.adapter.inmemory

import com.example.demo.domain.model.Notification
import com.example.demo.domain.port.NotificationRepository
import org.springframework.stereotype.Repository

@Repository
class InMemoryNotificationRepository : NotificationRepository {
    private val notifications = mutableListOf<Notification>()

    override suspend fun save(notification: Notification): Notification {
        notifications.add(notification)
        return notification
    }

    override suspend fun findByUserId(userId: String): List<Notification> {
        return notifications.filter { it.userId == userId }
    }

    override suspend fun markAsRead(notificationId: String) {
        notifications.find { it.id == notificationId }?.let {
            val index = notifications.indexOf(it)
            notifications[index] = it.copy(read = true)
        }
    }
}
