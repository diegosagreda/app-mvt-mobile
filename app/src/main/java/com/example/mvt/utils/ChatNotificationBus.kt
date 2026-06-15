package com.example.mvt.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object ChatNotificationBus {
    const val EXTRA_OPEN_CHAT = "extra_open_chat"

    private val _requests = MutableStateFlow(false)
    val requests = _requests.asStateFlow()

    fun publish() {
        _requests.value = true
    }

    fun clear() {
        _requests.value = false
    }
}
