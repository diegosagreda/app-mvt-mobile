package com.example.mvt.registration.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvt.registration.data.AthleteRegistrationService
import com.google.firebase.FirebaseNetworkException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VerificationActionState(
    val isWorking: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
    val updatedEmail: String? = null
)

class EmailVerificationViewModel(
    private val service: AthleteRegistrationService = AthleteRegistrationService()
) : ViewModel() {
    private val _action = MutableStateFlow(VerificationActionState())
    val action: StateFlow<VerificationActionState> = _action.asStateFlow()

    fun resend(email: String, password: String) {
        if (_action.value.isWorking) return
        viewModelScope.launch {
            _action.value = VerificationActionState(isWorking = true)
            runCatching { service.resendVerification(email, password) }
                .onSuccess { _action.value = VerificationActionState(message = "Correo de verificación reenviado.") }
                .onFailure { _action.value = VerificationActionState(message = friendlyError(it), isError = true) }
        }
    }

    fun changeEmail(currentEmail: String, newEmail: String, password: String) {
        if (_action.value.isWorking) return
        viewModelScope.launch {
            _action.value = VerificationActionState(isWorking = true)
            runCatching { service.requestEmailChange(currentEmail, password, newEmail) }
                .onSuccess {
                    _action.value = VerificationActionState(
                        message = "Enviamos la confirmación al nuevo correo.",
                        updatedEmail = newEmail.trim().lowercase()
                    )
                }
                .onFailure { _action.value = VerificationActionState(message = friendlyError(it), isError = true) }
        }
    }

    fun clearMessage() { _action.value = _action.value.copy(message = null, isError = false) }

    private fun friendlyError(error: Throwable) = when (error) {
        is FirebaseNetworkException -> "Revisa tu conexión e inténtalo nuevamente."
        else -> "No fue posible completar la acción. Verifica tu contraseña."
    }
}
