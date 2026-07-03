package com.example.mvt.ui.viewmodels

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvt.data.firebase.AuthService
import com.example.mvt.data.firebase.GoogleAuthService
import com.example.mvt.domain.repositories.AuthRepository
import com.example.mvt.registration.data.AthleteRegistrationService
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State

class AuthViewModel : ViewModel() {

    private val repo = AuthRepository(AuthService())
    private val registrationService = AthleteRegistrationService()

    private val _user = mutableStateOf<FirebaseUser?>(null)
    val user: State<FirebaseUser?> = _user

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _verificationEmail = mutableStateOf<String?>(null)
    val verificationEmail: State<String?> = _verificationEmail

    // ===========================
    //  LOGIN CON CORREO
    // ===========================
    fun login(email: String, password: String) {
        val cleanEmail = email.trim()
        val cleanPassword = password

        when {
            cleanEmail.isEmpty() && cleanPassword.isEmpty() -> {
                _isLoading.value = false
                _error.value = "Ingresa tu correo electrónico y contraseña."
                return
            }
            cleanEmail.isEmpty() -> {
                _isLoading.value = false
                _error.value = "Ingresa tu correo electrónico."
                return
            }
            cleanPassword.isEmpty() -> {
                _isLoading.value = false
                _error.value = "Ingresa tu contraseña."
                return
            }
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val signedInUser = repo.login(cleanEmail, cleanPassword)
                    ?: error("No fue posible iniciar sesión.")
                when {
                    !signedInUser.isEmailVerified -> {
                        _verificationEmail.value = signedInUser.email ?: cleanEmail
                        repo.logout()
                        _user.value = null
                        _error.value = "Debes verificar tu correo antes de ingresar."
                        return@launch
                    }
                    !repo.getUserRole(signedInUser.uid).equals("Deportista", ignoreCase = true) -> {
                        repo.logout()
                        _user.value = null
                        _error.value = "Esta aplicación está disponible únicamente para deportistas."
                        return@launch
                    }
                    else -> _user.value = signedInUser
                }
                _error.value = null
            } catch (e: Exception) {
                _error.value = mapAuthError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            try {
                _user.value = repo.register(email, password)
                _error.value = null
            } catch (e: Exception) {
                _error.value = mapAuthError(e)
            }
        }
    }

    fun logout() {
        repo.logout()
        _user.value = null
    }

    fun clearVerificationNavigation() { _verificationEmail.value = null }

    // ===========================
    //  LOGIN CON GOOGLE
    // ===========================
    fun prepareGoogleSignInIntent(
        activity: Activity,
        onReady: (Intent) -> Unit,
        onError: (String) -> Unit
    ) {
        val googleService = GoogleAuthService(activity)
        googleService.prepareSignInIntent(onReady, onError)
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
            onSuccess = { googleProfile ->
                viewModelScope.launch {
                    try {
                        val signedInUser = repo.currentUser()
                            ?: error("No fue posible iniciar sesión con Google.")
                        val role = repo.getUserRole(signedInUser.uid)

                        when {
                            role.isBlank() -> registrationService.initializeGoogleAthlete(
                                user = signedInUser,
                                googleProfile = googleProfile
                            )
                            !role.equals("Deportista", ignoreCase = true) -> {
                                repo.logout()
                                _user.value = null
                                val message = "Esta aplicación está disponible únicamente para deportistas."
                                _error.value = message
                                onError(message)
                                return@launch
                            }
                        }

                        _user.value = signedInUser
                        _error.value = null
                        onSuccess(signedInUser)
                    } catch (error: Exception) {
                        repo.logout()
                        _user.value = null
                        val message = mapAuthError(error)
                        _error.value = message
                        onError(message)
                    }
                }
            },
            onError = {
                _error.value = it
                onError(it)
            }
        )
    }

    private fun mapAuthError(error: Exception): String {
        return when (error) {
            is FirebaseAuthInvalidUserException,
            is FirebaseAuthInvalidCredentialsException -> "Correo o contraseña incorrectos."
            is FirebaseNetworkException -> "No fue posible conectarse. Revisa tu internet."
            is FirebaseTooManyRequestsException -> "Demasiados intentos. Intenta de nuevo en unos minutos."
            else -> error.message ?: "Ocurrió un error al iniciar sesión."
        }
    }
}
