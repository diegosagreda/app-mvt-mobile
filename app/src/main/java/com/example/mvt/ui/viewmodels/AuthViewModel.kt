package com.example.mvt.ui.viewmodels

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvt.data.firebase.AuthService
import com.example.mvt.data.firebase.GoogleAuthService
import com.example.mvt.domain.repositories.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State

class AuthViewModel : ViewModel() {

    private val repo = AuthRepository(AuthService())

    private val _user = mutableStateOf<FirebaseUser?>(null)
    val user: State<FirebaseUser?> = _user

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    // ===========================
    //  LOGIN CON CORREO
    // ===========================
    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                _user.value = repo.login(email, password)
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            try {
                _user.value = repo.register(email, password)
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun logout() {
        repo.logout()
        _user.value = null
    }

    // ===========================
    //  LOGIN CON GOOGLE
    // ===========================
    fun getGoogleSignInIntent(activity: Activity): Intent {
        val googleService = GoogleAuthService(activity)
        return googleService.getSignInIntent()
    }

    fun handleGoogleSignInResult(
        activity: Activity,
        data: Intent?,
        onSuccess: (FirebaseUser?) -> Unit,
        onError: (String) -> Unit
    ) {
        val googleService = GoogleAuthService(activity)
        googleService.handleSignInResult(
            data = data,
            onSuccess = {
                _user.value = repo.currentUser()
                onSuccess(repo.currentUser())
            },
            onError = {
                _error.value = it
                onError(it)
            }
        )
    }
}
