package com.example.mvt.utils

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object StravaAuthRedirectBus {
    private val _redirects = MutableStateFlow<Uri?>(null)

    val redirects = _redirects.asStateFlow()

    fun publish(uri: Uri) {
        _redirects.value = uri
    }

    fun clear() {
        _redirects.value = null
    }
}
