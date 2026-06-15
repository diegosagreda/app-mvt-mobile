package com.example.mvt.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvt.data.firebase.models.AppNotification
import com.example.mvt.data.firebase.models.NotificationSenderPreview
import com.example.mvt.data.firebase.repositories.NotificationRepository
import com.example.mvt.utils.NotificationSelectionBus
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val notifications: List<AppNotification> = emptyList(),
    val unreadCount: Int = 0,
    val hasMore: Boolean = true,
    val errorMessage: String? = null
)

class NotificationsViewModel(
    private val repository: NotificationRepository = NotificationRepository()
) : ViewModel() {
    private val pageSize = 5
    private val fetchBatchSize = 10
    private val realtimeWindowSize = 12
    private val bufferedNotifications = mutableListOf<AppNotification>()
    private val senderCache = mutableMapOf<String, NotificationSenderPreview>()
    private var activeUid: String = ""
    private var lastVisibleDocument: DocumentSnapshot? = null
    private var isMarkingAllAsRead = false
    private var recentNotificationsRegistration: ListenerRegistration? = null
    private var unreadNotificationsRegistration: ListenerRegistration? = null

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState

    fun start(uid: String) {
        if (uid.isBlank()) {
            stop()
            _uiState.value = NotificationsUiState(isLoading = false, hasMore = false)
            return
        }

        if (uid == activeUid && _uiState.value.notifications.isNotEmpty()) return

        stop()
        activeUid = uid
        bufferedNotifications.clear()
        senderCache.clear()
        _uiState.value = NotificationsUiState(isLoading = true)
        bindRealtimeListeners(uid)

        viewModelScope.launch {
            runCatching {
                val page = fetchVisiblePage(
                    uid = uid,
                    startAfter = null
                )
                val enriched = enrichNotifications(
                    notifications = page.notifications,
                    currentUid = uid
                )

                lastVisibleDocument = page.lastDocument
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        notifications = mergeNotifications(
                            incoming = enriched,
                            existing = state.notifications
                        ),
                        hasMore = page.hasMore,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        hasMore = false,
                        errorMessage = error.message ?: "No fue posible cargar notificaciones."
                    )
                }
            }
        }
    }

    fun stop() {
        recentNotificationsRegistration?.remove()
        unreadNotificationsRegistration?.remove()
        recentNotificationsRegistration = null
        unreadNotificationsRegistration = null
        activeUid = ""
        lastVisibleDocument = null
        isMarkingAllAsRead = false
        bufferedNotifications.clear()
    }

    fun loadMore() {
        if (activeUid.isBlank()) return

        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return

        _uiState.update { it.copy(isLoadingMore = true, errorMessage = null) }

        viewModelScope.launch {
            runCatching {
                val page = fetchVisiblePage(
                    uid = activeUid,
                    startAfter = lastVisibleDocument
                )
                val enriched = enrichNotifications(
                    notifications = page.notifications,
                    currentUid = activeUid
                )

                if (page.lastDocument != null) {
                    lastVisibleDocument = page.lastDocument
                }

                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        notifications = mergeNotifications(
                            incoming = enriched,
                            existing = current.notifications
                        ),
                        hasMore = page.hasMore,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        errorMessage = error.message ?: "No fue posible cargar mas notificaciones."
                    )
                }
            }
        }
    }

    fun onNotificationOpened(notification: AppNotification) {
        NotificationSelectionBus.publish(notification)
    }

    fun onNotificationsPanelOpened() {
        if (activeUid.isBlank() || isMarkingAllAsRead) return

        val current = _uiState.value
        val hasUnread = current.unreadCount > 0 || current.notifications.any { it.isUnread }
        if (!hasUnread) return

        isMarkingAllAsRead = true

        _uiState.update { state ->
            state.copy(
                notifications = state.notifications.map { item ->
                    if (item.isUnread) item.copy(estado = "leido") else item
                },
                unreadCount = 0
            )
        }

        viewModelScope.launch {
            runCatching {
                val unreadIds = repository.getUnreadNotificationsByRecipient(activeUid)
                    .filterNot { it.isMessageLike }
                    .map { it.id }

                if (unreadIds.isNotEmpty()) {
                    repository.markAsRead(unreadIds)
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "No fue posible marcar las notificaciones como leidas.")
                }
            }.also {
                isMarkingAllAsRead = false
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun bindRealtimeListeners(uid: String) {
        recentNotificationsRegistration = repository.listenRecentNotificationsByRecipient(
            uid = uid,
            limit = realtimeWindowSize,
            onUpdate = { notifications ->
                viewModelScope.launch {
                    val enriched = enrichNotifications(
                        notifications = notifications.filterNot { it.isMessageLike },
                        currentUid = uid
                    )

                    _uiState.update { current ->
                        current.copy(
                            notifications = mergeNotifications(
                                incoming = enriched,
                                existing = current.notifications
                            ),
                            errorMessage = null
                        )
                    }
                }
            },
            onError = { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "No fue posible escuchar notificaciones en tiempo real.")
                }
            }
        )

        unreadNotificationsRegistration = repository.listenUnreadNotificationsByRecipient(
            uid = uid,
            onUpdate = { notifications ->
                _uiState.update { current ->
                    current.copy(
                        unreadCount = notifications
                            .filterNot { it.isMessageLike }
                            .count { it.isUnread },
                        errorMessage = null
                    )
                }
            },
            onError = { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "No fue posible actualizar el contador de notificaciones.")
                }
            }
        )
    }

    private suspend fun enrichNotifications(
        notifications: List<AppNotification>,
        currentUid: String
    ): List<AppNotification> {
        notifications
            .map { it.remitente }
            .filter { it.isNotBlank() && it != currentUid }
            .distinct()
            .forEach { senderUid ->
                if (senderCache.containsKey(senderUid)) return@forEach

                val preview = runCatching {
                    repository.getSenderPreview(senderUid)
                }.getOrElse {
                    NotificationSenderPreview(uid = senderUid)
                }

                senderCache[senderUid] = preview
            }

        return notifications.map { notification ->
            if (notification.isStravaRoutine) {
                notification.copy(
                    senderName = "Strava",
                    senderAvatarUrl = ""
                )
            } else {
                val preview = senderCache[notification.remitente]
                notification.copy(
                    senderName = resolveSenderName(
                        notification = notification,
                        currentUid = currentUid,
                        preview = preview
                    ),
                    senderAvatarUrl = preview?.avatarUrl.orEmpty()
                )
            }
        }
    }

    private suspend fun fetchVisiblePage(
        uid: String,
        startAfter: DocumentSnapshot?
    ): NotificationPage {
        val collected = mutableListOf<AppNotification>()
        var cursor = startAfter
        var hasMore = true

        while (bufferedNotifications.isNotEmpty() && collected.size < pageSize) {
            collected += bufferedNotifications.removeAt(0)
        }

        if (collected.size >= pageSize) {
            return NotificationPage(
                notifications = collected,
                lastDocument = cursor,
                hasMore = bufferedNotifications.isNotEmpty() || hasMore
            )
        }

        while (collected.size < pageSize && hasMore) {
            val page = repository.getNotificationsPageByRecipient(
                uid = uid,
                pageSize = fetchBatchSize,
                startAfter = cursor
            )

            val visibleBatch = page.notifications.filterNot { it.isMessageLike }

            val remainingSlots = pageSize - collected.size
            collected += visibleBatch.take(remainingSlots)
            if (visibleBatch.size > remainingSlots) {
                bufferedNotifications += visibleBatch.drop(remainingSlots)
            }
            cursor = page.lastDocument
            hasMore = page.hasMore

            if (page.notifications.isEmpty()) break
        }

        return NotificationPage(
            notifications = collected,
            lastDocument = cursor,
            hasMore = hasMore || bufferedNotifications.isNotEmpty()
        )
    }

    private fun mergeNotifications(
        incoming: List<AppNotification>,
        existing: List<AppNotification>
    ): List<AppNotification> {
        return (incoming + existing)
            .distinctBy { it.id }
            .sortedByDescending { it.timestampMillis ?: 0L }
    }

    private fun resolveSenderName(
        notification: AppNotification,
        currentUid: String,
        preview: NotificationSenderPreview?
    ): String {
        if (!preview?.name.isNullOrBlank()) return preview?.name.orEmpty()
        if (notification.remitente == currentUid) return "Tu"

        return when {
            notification.tipo.equals("match", ignoreCase = true) -> "Entrenador"
            notification.tipo.equals("rutina", ignoreCase = true) -> "Tu entrenador"
            notification.isMessageLike -> "Mensajes"
            else -> "Sistema MVT"
        }
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }
}

private data class NotificationPage(
    val notifications: List<AppNotification>,
    val lastDocument: DocumentSnapshot?,
    val hasMore: Boolean
)
