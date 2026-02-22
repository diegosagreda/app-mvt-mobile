package com.example.mvt.data.firebase

import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.database.FirebaseDatabase

class GoogleAuthService(private val activity: Activity) {

    private val auth = Firebase.auth
    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            // Usa el client_id del archivo google-services.json
            .requestIdToken(activity.getString(com.example.mvt.R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(activity, gso)
    }

    /** Retorna el Intent para iniciar el flujo de Google Sign-In */
    fun getSignInIntent(): Intent = googleSignInClient.signInIntent

    /** Procesa el resultado del flujo de Google */
    fun handleSignInResult(
        data: Intent?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account, onSuccess, onError)
        } catch (e: ApiException) {
            onError("Error en Google Sign-In: ${e.message}")
        }
    }

    private fun firebaseAuthWithGoogle(
        account: GoogleSignInAccount,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                val firebaseUser = auth.currentUser
                if (firebaseUser != null) {
                    val userRef = FirebaseDatabase.getInstance()
                        .getReference("users/${firebaseUser.uid}")

                    val userData = mapOf(
                        "nombres" to (firebaseUser.displayName ?: ""),
                        "email" to (firebaseUser.email ?: ""),
                        "foto_url" to (firebaseUser.photoUrl?.toString() ?: "")
                    )

                    userRef.updateChildren(userData)
                }
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Error de autenticación con Google")
            }
    }
}
