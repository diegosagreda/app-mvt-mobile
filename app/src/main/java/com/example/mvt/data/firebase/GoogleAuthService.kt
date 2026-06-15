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

    /** Limpia la cuenta recordada por Google antes de abrir el selector de cuentas. */
    fun prepareSignInIntent(
        onReady: (Intent) -> Unit,
        onError: (String) -> Unit
    ) {
        googleSignInClient.signOut()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onReady(googleSignInClient.signInIntent)
                } else {
                    onError(task.exception?.message ?: "No fue posible preparar el inicio de sesión con Google.")
                }
            }
    }

    /** Procesa el resultado del flujo de Google */
    fun handleSignInResult(
        data: Intent?,
        onSuccess: (GoogleAccountProfile) -> Unit,
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
        onSuccess: (GoogleAccountProfile) -> Unit,
        onError: (String) -> Unit
    ) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                onSuccess(account.toGoogleAccountProfile())
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Error de autenticación con Google")
            }
    }

    private fun GoogleSignInAccount.toGoogleAccountProfile(): GoogleAccountProfile {
        return GoogleAccountProfile(
            email = email.orEmpty(),
            displayName = displayName.orEmpty(),
            givenName = givenName.orEmpty(),
            familyName = familyName.orEmpty(),
            photoUrl = photoUrl?.toString().orEmpty()
        )
    }
}

data class GoogleAccountProfile(
    val email: String,
    val displayName: String,
    val givenName: String,
    val familyName: String,
    val photoUrl: String,
    val gender: String = "",
    val phone: String = "",
    val phonePrefix: String = ""
)
