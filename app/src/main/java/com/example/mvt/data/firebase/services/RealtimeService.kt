package com.example.mvt.data.firebase.services

import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class RealtimeService {
    private val db = FirebaseDatabase.getInstance().reference

    // Obtiene el nodo 'zonas' del usuario
    suspend fun getZonasDeportista(idDeportista: String): Map<String, Any>? {
        val snapshot = db.child("users").child(idDeportista).child("zonas").get().await()
        return snapshot.value as? Map<String, Any>
    }

    // Obtiene el nodo 'ritmos' del usuario
    suspend fun getRitmosDeportista(idDeportista: String): Map<String, Any>? {
        val snapshot = db.child("users").child(idDeportista).child("ritmos").get().await()
        return snapshot.value as? Map<String, Any>
    }
}
