package com.example.mvt.data.firebase.repositories

import com.example.mvt.data.firebase.models.AppNotification
import com.example.mvt.data.firebase.models.NotificationSenderPreview
import com.example.mvt.data.firebase.services.NotificationPageResult
import com.example.mvt.data.firebase.services.NotificationService
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration

class NotificationRepository(
    private val service: NotificationService = NotificationService()
) {
    fun listenRecentNotificationsByRecipient(
        uid: String,
        limit: Int,
        onUpdate: (List<AppNotification>) -> Unit,
        onError: (Throwable) -> Unit
    ): ListenerRegistration {
        return service.listenRecentNotificationsByRecipient(uid, limit, onUpdate, onError)
    }

    fun listenUnreadNotificationsByRecipient(
        uid: String,
        onUpdate: (List<AppNotification>) -> Unit,
        onError: (Throwable) -> Unit
    ): ListenerRegistration {
        return service.listenUnreadNotificationsByRecipient(uid, onUpdate, onError)
    }

    suspend fun getNotificationsPageByRecipient(
        uid: String,
        pageSize: Int,
        startAfter: DocumentSnapshot? = null
    ): NotificationPageResult {
        return service.getNotificationsPageByRecipient(uid, pageSize, startAfter)
    }

    suspend fun markAsRead(notificationId: String) {
        service.markAsRead(notificationId)
    }

    suspend fun markAsRead(notificationIds: List<String>) {
        service.markAsRead(notificationIds)
    }

    suspend fun getSenderPreview(uid: String): NotificationSenderPreview {
        return service.getSenderPreview(uid)
    }

    suspend fun getUnreadNotificationsByRecipient(uid: String): List<AppNotification> {
        return service.getUnreadNotificationsByRecipient(uid)
    }
}
