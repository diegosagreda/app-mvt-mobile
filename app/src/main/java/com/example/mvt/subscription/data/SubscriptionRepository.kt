package com.example.mvt.subscription.data

import com.example.mvt.subscription.model.Cobro
import com.example.mvt.subscription.model.PlanCatalogo
import com.example.mvt.subscription.model.UserPlan
import com.example.mvt.subscription.model.VencimientoAutomatico
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SubscriptionRepository {

    private val db  = FirebaseDatabase.getInstance().reference
    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    fun getSubscriptionStream(): Flow<Triple<UserPlan, PlanCatalogo, List<Cobro>>> = callbackFlow {
        val userPlanRef = db.child("users").child(uid).child("plan")
        val planesRef   = db.child("Planes")
        val cobrosRef   = db.child("Cobros").child(uid)

        var currentUserPlan = UserPlan()
        var currentCatalogo = PlanCatalogo()
        var currentCobros   = emptyList<Cobro>()
        var lastPlanesSnapshot: DataSnapshot? = null

        fun sendUpdate() {
            trySend(Triple(currentUserPlan, currentCatalogo, currentCobros))
        }

        // Listener para el catálogo completo para tenerlo en memoria
        val planesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                lastPlanesSnapshot = snapshot
                val planName = currentUserPlan.nombre
                if (planName.isNotBlank() && snapshot.child(planName).exists()) {
                    currentCatalogo = mapPlanCatalogo(snapshot.child(planName))
                }
                sendUpdate()
            }
            override fun onCancelled(error: DatabaseError) {}
        }

        // Listener para el plan del usuario
        val userListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                currentUserPlan = mapUserPlan(snapshot)
                
                // IMPORTANTE: Cuando cambia el plan del usuario, actualizamos el catálogo 
                // usando el último snapshot de Planes que tenemos en memoria
                val planName = currentUserPlan.nombre
                lastPlanesSnapshot?.let { snap ->
                    if (planName.isNotBlank() && snap.child(planName).exists()) {
                        currentCatalogo = mapPlanCatalogo(snap.child(planName))
                    }
                }
                sendUpdate()
            }
            override fun onCancelled(error: DatabaseError) {}
        }

        val cobrosListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                currentCobros = mapCobros(snapshot)
                sendUpdate()
            }
            override fun onCancelled(error: DatabaseError) {}
        }

        planesRef.addValueEventListener(planesListener)
        userPlanRef.addValueEventListener(userListener)
        cobrosRef.addValueEventListener(cobrosListener)

        awaitClose {
            userPlanRef.removeEventListener(userListener)
            planesRef.removeEventListener(planesListener)
            cobrosRef.removeEventListener(cobrosListener)
        }
    }

    private fun mapUserPlan(snapshot: DataSnapshot): UserPlan {
        if (!snapshot.exists()) return UserPlan()
        val nombre = snapshot.child("nombre").getValue(String::class.java) ?: ""
        val fechaRegistro = snapshot.child("fecha_registro").value.let {
            when (it) { is Long -> it; is Double -> it.toLong(); else -> 0L }
        }
        return UserPlan(nombre = nombre, fechaRegistro = fechaRegistro)
    }

    private fun mapPlanCatalogo(snapshot: DataSnapshot): PlanCatalogo {
        if (!snapshot.exists()) return PlanCatalogo()
        val precio = snapshot.child("precio").value.let {
            when (it) { is Long -> it.toInt(); is Double -> it.toInt(); else -> 0 }
        }
        val periodo = snapshot.child("periodoActualizacion").value.let {
            when (it) { is Long -> it.toInt(); is Double -> it.toInt(); else -> 0 }
        }
        return PlanCatalogo(id = snapshot.key ?: "", precio = precio, periodoActualizacion = periodo)
    }

    private fun mapCobros(snapshot: DataSnapshot): List<Cobro> {
        val cobros = mutableListOf<Cobro>()
        snapshot.children.forEach { child ->
            val estado = child.child("estado").getValue(String::class.java) ?: ""
            if (estado == "Aceptada") {
                val monto = child.child("monto").value.let {
                    when (it) { is Long -> it.toInt(); is Double -> it.toInt(); is String -> it.toIntOrNull() ?: 0; else -> 0 }
                }
                val actualizadoEn = child.child("actualizado_en").value.let {
                    when (it) { is Long -> it; is Double -> it.toLong(); else -> 0L }
                }
                cobros.add(Cobro(
                    fecha = child.child("fecha_transaccion").getValue(String::class.java) ?: child.key ?: "",
                    plan = child.child("plan").getValue(String::class.java) ?: "",
                    actualizadoEn = actualizadoEn,
                    monto = monto
                ))
            }
        }
        return cobros.sortedByDescending { it.actualizadoEn }
    }
}
