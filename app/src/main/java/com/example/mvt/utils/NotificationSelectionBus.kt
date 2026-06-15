package com.example.mvt.utils

import com.example.mvt.data.firebase.models.AppNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NotificationSelectionBus {
    private val _selected = MutableStateFlow<AppNotification?>(null)
    val selected: StateFlow<AppNotification?> = _selected.asStateFlow()

    fun publish(notification: AppNotification) {
        _selected.value = notification
    }

    fun clear() {
        _selected.value = null
    }
}
