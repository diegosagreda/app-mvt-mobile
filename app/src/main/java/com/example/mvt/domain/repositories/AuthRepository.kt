package com.example.mvt.domain.repositories

import com.example.mvt.data.firebase.AuthService
import com.google.firebase.auth.FirebaseUser

class AuthRepository(private val authService: AuthService) {

    suspend fun login(email: String, password: String): FirebaseUser? {
        return authService.signIn(email, password)
    }

    suspend fun register(email: String, password: String): FirebaseUser? {
        return authService.signUp(email, password)
    }

    suspend fun resetPassword(email: String) {
        authService.sendPasswordReset(email)
    }

    fun currentUser(): FirebaseUser? = authService.getCurrentUser()

    fun logout() = authService.signOut()
}
