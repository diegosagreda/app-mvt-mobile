package com.example.mvt.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvt.data.firebase.models.User
import com.example.mvt.domain.usecases.GetUserInfoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UserViewModel(
    private val getUserInfoUseCase: GetUserInfoUseCase
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    fun loadUserInfo() {
        viewModelScope.launch {
            val userInfo = getUserInfoUseCase()
            Log.d("UserViewModel", "Usuario cargado: ${userInfo?.nombres}, Foto: ${userInfo?.foto_url}")
            _user.value = userInfo
        }
    }
}
