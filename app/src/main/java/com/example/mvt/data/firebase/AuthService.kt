package com.example.mvt.data.firebase

import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import com.google.firebase.database.FirebaseDatabase

class AuthService {
    private val auth = Firebase.auth
    private val users = FirebaseDatabase.getInstance().getReference("users")

    suspend fun signIn(email: String, password: String): FirebaseUser? {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        return result.user
    }

    suspend fun signUp(email: String, password: String): FirebaseUser? {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        return result.user
    }

    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    suspend fun getUserRole(uid: String): String {
        return users.child(uid).child("rol").get().await().value?.toString().orEmpty()
    }

    suspend fun hasCompletedWelcome(uid: String): Boolean {
        val value = users.child(uid).child("bienvenida").get().await().value
        return when (value) {
            is Number -> value.toInt() == 0
            is String -> value.toIntOrNull() == 0
            else -> false
        }
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun signOut() = auth.signOut()
}
