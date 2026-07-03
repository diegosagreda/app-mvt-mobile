package com.example.mvt.registration.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvt.registration.data.AthleteRegistrationService
import com.example.mvt.registration.model.AthleteRegistrationCommand
import com.example.mvt.registration.model.AthleteRegistrationForm
import com.example.mvt.registration.model.RegistrationProgress
import com.example.mvt.registration.model.RegistrationUiState
import com.example.mvt.registration.model.toCommand
import com.example.mvt.registration.model.validateAccessStep
import com.example.mvt.registration.model.validatePersonalStep
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegistrationViewModel(
    private val service: AthleteRegistrationService = AthleteRegistrationService()
) : ViewModel() {
    private val _state = MutableStateFlow(RegistrationUiState())
    val state: StateFlow<RegistrationUiState> = _state.asStateFlow()
    private var pendingCommand: AthleteRegistrationCommand? = null
    private var pendingUser: FirebaseUser? = null
    private var reservedNumericId: Long? = null
    private var profileInitialized = false

    fun updateForm(transform: (AthleteRegistrationForm) -> AthleteRegistrationForm) {
        if (_state.value.isSubmitting) return
        _state.value = _state.value.copy(
            form = transform(_state.value.form),
            errors = com.example.mvt.registration.model.RegistrationErrors(),
            progress = RegistrationProgress.EDITING,
            errorMessage = null
        )
    }

    fun continueToAccess() {
        val current = _state.value
        val errors = validatePersonalStep(current.form)
        _state.value = current.copy(
            errors = errors,
            progress = if (errors.hasPersonalErrors) RegistrationProgress.VALIDATING else RegistrationProgress.EDITING,
            step = if (errors.hasPersonalErrors) 1 else 2
        )
    }

    fun backToPersonal() {
        if (!_state.value.isSubmitting) _state.value = _state.value.copy(step = 1, errors = com.example.mvt.registration.model.RegistrationErrors())
    }

    fun submit() {
        val current = _state.value
        if (current.isSubmitting) return
        val errors = validateAccessStep(current.form)
        if (errors.hasAccessErrors) {
            _state.value = current.copy(errors = errors, progress = RegistrationProgress.VALIDATING)
            return
        }
        val command = current.form.toCommand()
        pendingCommand = command
        viewModelScope.launch {
            _state.value = current.copy(errors = errors, progress = RegistrationProgress.CREATING_AUTH_USER, errorMessage = null)
            runCatching {
                pendingUser = service.createAuthUser(command)
                reservedNumericId = service.reserveNumericId()
                completeRegistration()
            }.onFailure(::handleFailure)
        }
    }

    fun retryInitialization() {
        if (_state.value.isSubmitting || pendingUser == null || pendingCommand == null) return
        viewModelScope.launch {
            runCatching {
                if (reservedNumericId == null) reservedNumericId = service.reserveNumericId()
                completeRegistration()
            }.onFailure(::handleFailure)
        }
    }

    private suspend fun completeRegistration() {
        val user = requireNotNull(pendingUser)
        val command = requireNotNull(pendingCommand)
        val numericId = requireNotNull(reservedNumericId)
        if (!profileInitialized) {
            _state.value = _state.value.copy(progress = RegistrationProgress.CREATING_PROFILE, errorMessage = null)
            service.initializeAthlete(user.uid, numericId, command)
            profileInitialized = true
        }
        _state.value = _state.value.copy(progress = RegistrationProgress.SENDING_VERIFICATION, errorMessage = null)
        service.sendVerification(user)
        service.signOut()
        _state.value = _state.value.copy(progress = RegistrationProgress.SUCCESS, canRetryInitialization = false)
    }

    private fun handleFailure(error: Throwable) {
        _state.value = _state.value.copy(
            progress = RegistrationProgress.ERROR,
            errorMessage = mapRegistrationError(error),
            canRetryInitialization = pendingUser != null
        )
    }

    private fun mapRegistrationError(error: Throwable): String = when (error) {
        is FirebaseAuthUserCollisionException -> "Esta cuenta ya existe."
        is FirebaseAuthInvalidCredentialsException -> "El correo no es válido."
        is FirebaseAuthWeakPasswordException -> "La contraseña no cumple los requisitos."
        is FirebaseNetworkException -> "Revisa tu conexión e inténtalo nuevamente."
        else -> if (pendingUser != null) "La cuenta no pudo configurarse completamente. Puedes reintentar." else
            (error.message ?: "No fue posible crear la cuenta.")
    }
}
