package com.example.mvt.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

object PushTokenManager {
    private const val PREFS_NAME = "mvt_push_tokens"
    private const val PREF_LAST_UID = "last_uid"
    private const val PREF_LAST_TOKEN = "last_token"
    private const val PREF_RETRY_COUNT = "retry_count"
    private const val TAG = "MVT_PushToken"
    private const val MAX_RETRY_COUNT = 6

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var initialized = false

    private lateinit var appContext: Context
    private var authListener: FirebaseAuth.AuthStateListener? = null
    private var pendingRetry: Runnable? = null

    fun start(context: Context) {
        if (initialized) return

        appContext = context.applicationContext
        Log.d(TAG, "start()")
        authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val currentUid = firebaseAuth.currentUser?.uid.orEmpty()
            val prefs = prefs()
            val previousUid = prefs.getString(PREF_LAST_UID, "").orEmpty()
            Log.d(TAG, "authState currentUid=$currentUid previousUid=$previousUid")
            cancelPendingRetry()

            if (previousUid.isNotBlank() && previousUid != currentUid) {
                unregisterCurrentTokenForUser(previousUid)
            }

            if (currentUid.isNotBlank()) {
                resetRetryCount()
                syncCurrentToken(currentUid)
            } else {
                prefs.edit()
                    .remove(PREF_LAST_UID)
                    .remove(PREF_LAST_TOKEN)
                    .remove(PREF_RETRY_COUNT)
                    .apply()
            }
        }

        auth.addAuthStateListener(authListener!!)
        auth.currentUser?.uid
            ?.takeIf { it.isNotBlank() }
            ?.let(::syncCurrentToken)

        initialized = true
    }

    fun onNewToken(token: String) {
        val uid = auth.currentUser?.uid.orEmpty()
        Log.d(TAG, "onNewToken uid=$uid tokenLength=${token.length}")
        if (uid.isBlank() || token.isBlank()) {
            Log.w(TAG, "onNewToken skipped uidBlank=${uid.isBlank()} tokenBlank=${token.isBlank()}")
            return
        }
        saveToken(uid, token)
    }

    fun unregisterCurrentTokenForUser(uid: String) {
        if (uid.isBlank()) return

        FirebaseMessaging.getInstance().token.addOnSuccessListener { currentToken ->
            val tokenToRemove = currentToken
                ?.takeIf { it.isNotBlank() }
                ?: prefs().getString(PREF_LAST_TOKEN, null)
                ?: return@addOnSuccessListener

            Log.d(TAG, "unregisterCurrentTokenForUser uid=$uid tokenLength=${tokenToRemove.length}")
            removeToken(uid, tokenToRemove)
        }.addOnFailureListener { error ->
            Log.e(TAG, "unregisterCurrentTokenForUser failed to fetch current token", error)
        }
    }

    private fun syncCurrentToken(uid: String) {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            if (token.isNullOrBlank()) {
                Log.w(TAG, "syncCurrentToken uid=$uid returned blank token")
                scheduleRetry(uid, "blank_token")
                return@addOnSuccessListener
            }
            Log.d(TAG, "syncCurrentToken uid=$uid tokenLength=${token.length}")
            resetRetryCount()
            saveToken(uid, token)
        }.addOnFailureListener { error ->
            Log.e(TAG, "syncCurrentToken failed uid=$uid", error)
            scheduleRetry(uid, error.message ?: error.javaClass.simpleName)
        }
    }

    private fun saveToken(uid: String, token: String) {
        val prefs = prefs()
        val previousUid = prefs.getString(PREF_LAST_UID, "").orEmpty()
        val previousToken = prefs.getString(PREF_LAST_TOKEN, "").orEmpty()

        val docRef = firestore.collection("fcmTokens").document(uid)
        val updates = linkedMapOf<String, Any>(
            "updatedAt" to FieldValue.serverTimestamp(),
            "tokens" to FieldValue.arrayUnion(token)
        )

        Log.d(TAG, "saveToken uid=$uid previousUid=$previousUid previousTokenLength=${previousToken.length}")
        docRef.set(updates, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "saveToken success uid=$uid")
                resetRetryCount()
                if (previousUid == uid && previousToken.isNotBlank() && previousToken != token) {
                    removeToken(uid, previousToken)
                }

                prefs.edit()
                    .putString(PREF_LAST_UID, uid)
                    .putString(PREF_LAST_TOKEN, token)
                    .apply()
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "saveToken failed uid=$uid", error)
            }
    }

    private fun removeToken(uid: String, token: String) {
        if (uid.isBlank() || token.isBlank()) return

        Log.d(TAG, "removeToken uid=$uid tokenLength=${token.length}")
        firestore.collection("fcmTokens")
            .document(uid)
            .set(
                mapOf(
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "tokens" to FieldValue.arrayRemove(token)
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
            .addOnSuccessListener {
                Log.d(TAG, "removeToken success uid=$uid")
                val prefs = prefs()
                if (prefs.getString(PREF_LAST_UID, "").orEmpty() == uid &&
                    prefs.getString(PREF_LAST_TOKEN, "").orEmpty() == token
                ) {
                    prefs.edit()
                        .remove(PREF_LAST_TOKEN)
                        .apply()
                }
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "removeToken failed uid=$uid", error)
            }
    }

    private fun prefs() =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun scheduleRetry(uid: String, reason: String) {
        if (uid.isBlank()) return

        val currentUid = auth.currentUser?.uid.orEmpty()
        if (currentUid != uid) return

        val prefs = prefs()
        val nextRetryCount = prefs.getInt(PREF_RETRY_COUNT, 0) + 1
        if (nextRetryCount > MAX_RETRY_COUNT) {
            Log.e(TAG, "scheduleRetry aborted uid=$uid reason=$reason attempts=$nextRetryCount")
            return
        }

        prefs.edit().putInt(PREF_RETRY_COUNT, nextRetryCount).apply()
        cancelPendingRetry()

        val delayMs = when (nextRetryCount) {
            1 -> 2_000L
            2 -> 5_000L
            3 -> 10_000L
            4 -> 20_000L
            5 -> 30_000L
            else -> 60_000L
        }

        val retryRunnable = Runnable {
            Log.d(TAG, "retrying token fetch uid=$uid attempt=$nextRetryCount reason=$reason")
            syncCurrentToken(uid)
        }
        pendingRetry = retryRunnable
        Log.w(TAG, "scheduleRetry uid=$uid attempt=$nextRetryCount delayMs=$delayMs reason=$reason")
        mainHandler.postDelayed(retryRunnable, delayMs)
    }

    private fun cancelPendingRetry() {
        pendingRetry?.let(mainHandler::removeCallbacks)
        pendingRetry = null
    }

    private fun resetRetryCount() {
        cancelPendingRetry()
        prefs().edit().putInt(PREF_RETRY_COUNT, 0).apply()
    }
}
