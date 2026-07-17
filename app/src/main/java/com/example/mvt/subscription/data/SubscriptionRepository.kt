package com.example.mvt.subscription.data

import android.util.Log
import com.example.mvt.subscription.model.Cobro
import com.example.mvt.subscription.model.PlanCatalogo
import com.example.mvt.subscription.model.UserPlan
import com.example.mvt.subscription.model.VencimientoAutomatico
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class SubscriptionRepository {

    private val db  = FirebaseDatabase.getInstance().reference
    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // === Leer plan activo del usuario ===
    suspend fun getUserPlan(): UserPlan? {
        return try {
            val snapshot = db
                .child("users")
                .child(uid)
                .child("plan")
                .get()
                .await()

            if (!snapshot.exists()) return null

            val nombre         = snapshot.child("nombre").getValue(String::class.java) ?: ""
            val fechaRegistro  = snapshot.child("fecha_registro").value.let {
                when (it) {
                    is Long   -> it
                    is Double -> it.toLong()
                    else      -> 0L
                }
            }
            val caracteres     = snapshot.child("caracteres_chat").value.let {
                when (it) { is Long -> it.toInt(); is Double -> it.toInt(); else -> 0 }
            }
            val fotos          = snapshot.child("fotos").value.let {
                when (it) { is Long -> it.toInt(); is Double -> it.toInt(); else -> 0 }
            }
            val videoMin       = snapshot.child("video_min").value.let {
                when (it) { is Long -> it.toInt(); is Double -> it.toInt(); else -> 0 }
            }
            val solicitudes    = snapshot.child("solicitudes").value.let {
                when (it) { is Long -> it.toInt(); is Double -> it.toInt(); else -> 0 }
            }
            val solEnviadas    = snapshot.child("solicitudesEnviadas").value.let {
                when (it) { is Long -> it.toInt(); is Double -> it.toInt(); else -> 0 }
            }
            val rutinas        = snapshot.child("rutinasComodin")
                .getValue(Boolean::class.java) ?: false

            val vencSnap = snapshot.child("vencimiento_automatico")
            val venc = if (vencSnap.exists()) {
                VencimientoAutomatico(
                    ejecutado     = vencSnap.child("ejecutado")
                        .getValue(Boolean::class.java) ?: false,
                    fechaEjecucion = vencSnap.child("fecha_ejecucion").value.let {
                        when (it) { is Long -> it; is Double -> it.toLong(); else -> 0L }
                    },
                    motivo        = vencSnap.child("motivo")
                        .getValue(String::class.java) ?: ""
                )
            } else null

            UserPlan(
                nombre               = nombre,
                fechaRegistro        = fechaRegistro,
                caracteres_chat      = caracteres,
                fotos                = fotos,
                video_min            = videoMin,
                solicitudes          = solicitudes,
                solicitudesEnviadas  = solEnviadas,
                rutinasComodin       = rutinas,
                vencimientoAutomatico = venc
            )
        } catch (e: Exception) {
            Log.e("SubscriptionRepo", "Error leyendo plan usuario", e)
            null
        }
    }

    // === Leer catálogo del plan desde Planes/{nombre} ===
    suspend fun getPlanCatalogo(nombrePlan: String): PlanCatalogo? {
        return try {
            val snapshot = db
                .child("Planes")
                .child(nombrePlan)
                .get()
                .await()

            if (!snapshot.exists()) return null

            val precio   = snapshot.child("precio").value.let {
                when (it) { is Long -> it.toInt(); is Double -> it.toInt(); else -> 0 }
            }
            val precioN  = snapshot.child("precioNormal").value.let {
                when (it) { is Long -> it.toInt(); is Double -> it.toInt(); else -> 0 }
            }
            val precioUSD = snapshot.child("precioUSD").value.let {
                when (it) {
                    is Double -> it
                    is Long   -> it.toDouble()
                    is String -> it.toDoubleOrNull() ?: 0.0
                    else      -> 0.0
                }
            }
            val disponible = snapshot.child("disponibilidad")
                .getValue(Boolean::class.java) ?: false
            val periodo    = snapshot.child("periodoActualizacion").value.let {
                when (it) { is Long -> it.toInt(); is Double -> it.toInt(); else -> 0 }
            }
            val visualizar = snapshot.child("visualizarPlan").value.let {
                when (it) { is Long -> it.toInt(); is Double -> it.toInt(); else -> 0 }
            }

            PlanCatalogo(
                id                    = nombrePlan,
                precio                = precio,
                precioNormal          = precioN,
                precioUSD             = precioUSD,
                disponibilidad        = disponible,
                periodoActualizacion  = periodo,
                visualizarPlan        = visualizar
            )
        } catch (e: Exception) {
            Log.e("SubscriptionRepo", "Error leyendo catálogo plan", e)
            null
        }
    }

    // === Leer historial de cobros del usuario ===
    suspend fun getCobros(): List<Cobro> {
        return try {
            val snapshot = db
                .child("Cobros")
                .child(uid)
                .get()
                .await()

            if (!snapshot.exists()) return emptyList()

            val cobros = mutableListOf<Cobro>()
            snapshot.children.forEach { child ->
                val fecha       = child.key ?: return@forEach
                val bankName    = child.child("bank_name")
                    .getValue(String::class.java) ?: ""
                val plan        = child.child("plan")
                    .getValue(String::class.java) ?: ""
                val recibo      = child.child("recibo").value?.toString() ?: ""
                val estado      = child.child("estado")
                    .getValue(String::class.java) ?: ""
                val factura     = child.child("factura")
                    .getValue(String::class.java) ?: ""
                val monto       = child.child("monto").value.let {
                    when (it) { is Long -> it.toInt(); is Double -> it.toInt(); else -> 0 }
                }
                val actualizadoEn = child.child("actualizado_en").value.let {
                    when (it) { is Long -> it; is Double -> it.toLong(); else -> 0L }
                }
                val fechaTransaccion = child.child("fecha_transaccion")
                    .getValue(String::class.java) ?: ""

                // Solo incluir cobros aceptados
                if (estado == "Aceptada") {
                    cobros.add(Cobro(
                        fecha            = fechaTransaccion.ifBlank { fecha },
                        bank_name        = bankName,
                        plan             = plan,
                        recibo           = recibo,
                        factura          = factura,
                        monto            = monto,
                        actualizadoEn    = actualizadoEn
                    ))
                }
            }

            cobros.sortedByDescending { it.actualizadoEn }

        } catch (e: Exception) {
            Log.e("SubscriptionRepo", "Error leyendo cobros", e)
            emptyList()
        }
    }
}