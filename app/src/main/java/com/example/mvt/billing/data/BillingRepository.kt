package com.example.mvt.billing.data

import android.util.Log
import com.example.mvt.billing.model.BillingPago
import com.example.mvt.billing.model.BillingStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BillingRepository {

    private val db  = FirebaseDatabase.getInstance().reference
    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // === Stream en tiempo real de facturación ===
    fun getBillingStream(): Flow<BillingStatus> = callbackFlow {
        val planRef   = db.child("users").child(uid).child("plan")
        val cobrosRef = db.child("Cobros").child(uid)
        val planesRef = db.child("Planes")

        var nombrePlan    = ""
        var fechaRegistro = 0L
        var esPlanGratuito = true
        var pagos = mutableListOf<BillingPago>()

        fun updateAndSend() {
            val pagosOrdenados = pagos.sortedByDescending { it.actualizadoEn }
            val ultimoPago     = pagosOrdenados.firstOrNull()
            val fechaInicioMs  = ultimoPago?.actualizadoEn?.takeIf { it > 0 } ?: fechaRegistro
            val ciclo          = if (esPlanGratuito) 0 else 30

            val fechaInicio: String
            val fechaCorte: String
            val diasRestantes: Int
            val diasUsados: Int
            val progreso: Float

            if (esPlanGratuito || ciclo == 0) {
                fechaInicio   = if (fechaInicioMs > 0) formatMs(fechaInicioMs) else "—"
                fechaCorte    = "No aplica"
                diasRestantes = -1
                diasUsados    = 0
                progreso      = 1f
            } else {
                val msCorte    = fechaInicioMs + (ciclo * 24L * 60 * 60 * 1000)
                val msAhora    = System.currentTimeMillis()
                val msPasados  = msAhora - fechaInicioMs
                
                val usados     = (msPasados / (1000L * 60 * 60 * 24)).toInt().coerceIn(0, ciclo)
                val restantes  = ciclo - usados

                fechaInicio   = formatMs(fechaInicioMs)
                fechaCorte    = formatMs(msCorte)
                diasUsados    = usados
                diasRestantes = restantes
                progreso      = (msPasados.toFloat() / (ciclo * 24L * 60 * 60 * 1000).toFloat()).coerceIn(0f, 1f)
            }

            val totalPagado       = pagosOrdenados.sumOf { it.monto }
            val totalDescuentos   = pagosOrdenados.sumOf { it.descuento }
            val pagosConDescuento = pagosOrdenados.count { it.codigoDescuento.isNotBlank() }

            trySend(BillingStatus(
                nombrePlan        = nombrePlan,
                esPlanGratuito    = esPlanGratuito,
                fechaInicio       = fechaInicio,
                fechaCorte        = fechaCorte,
                diasRestantes     = diasRestantes,
                diasTotales       = ciclo,
                diasUsados        = diasUsados,
                progreso          = progreso,
                totalPagado       = totalPagado,
                totalPagos        = pagosOrdenados.size,
                totalDescuentos   = totalDescuentos,
                pagosConDescuento = pagosConDescuento,
                ultimoPago        = ultimoPago,
                historialPagos    = pagosOrdenados
            ))
        }

        val planListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                nombrePlan    = snapshot.child("nombre").getValue(String::class.java) ?: ""
                fechaRegistro = snapshot.child("fecha_registro").value.let {
                    when (it) { is Long -> it; is Double -> it.toLong(); else -> 0L }
                }
                // Consultar precio del plan para saber si es gratuito
                planesRef.child(nombrePlan).child("precio").get().addOnSuccessListener {
                    val p = it.value.let { v -> when (v) { is Long -> v.toInt(); is Double -> v.toInt(); else -> 0 } }
                    esPlanGratuito = p == 0
                    updateAndSend()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }

        val cobrosListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempPagos = mutableListOf<BillingPago>()
                snapshot.children.forEach { child ->
                    if (child.child("estado").getValue(String::class.java) == "Aceptada") {
                        tempPagos.add(mapPago(child))
                    }
                }
                pagos = tempPagos
                updateAndSend()
            }
            override fun onCancelled(error: DatabaseError) {}
        }

        planRef.addValueEventListener(planListener)
        cobrosRef.addValueEventListener(cobrosListener)

        awaitClose {
            planRef.removeEventListener(planListener)
            cobrosRef.removeEventListener(cobrosListener)
        }
    }

    private fun mapPago(child: DataSnapshot): BillingPago {
        return BillingPago(
            recibo           = child.child("recibo").value?.toString() ?: "",
            plan             = child.child("plan").getValue(String::class.java) ?: "",
            monto            = child.child("monto").value.let { when (it) { is Long -> it.toInt(); is Double -> it.toInt(); is String -> it.toIntOrNull() ?: 0; else -> 0 } },
            descuento        = child.child("descuento").value.let { when (it) { is Long -> it.toInt(); is Double -> it.toInt(); is String -> it.toIntOrNull() ?: 0; else -> 0 } },
            codigoDescuento  = child.child("codigo_descuento").getValue(String::class.java) ?: "",
            estado           = child.child("estado").getValue(String::class.java) ?: "",
            fechaTransaccion = child.child("fecha_transaccion").getValue(String::class.java) ?: "",
            actualizadoEn    = child.child("actualizado_en").value.let { when (it) { is Long -> it; is Double -> it.toLong(); else -> 0L } },
            bankName         = child.child("bank_name").getValue(String::class.java) ?: "",
            medioPago        = child.child("medio_pago").getValue(String::class.java) ?: "",
            franquicia       = child.child("franquicia").getValue(String::class.java) ?: ""
        )
    }

    private fun formatMs(ms: Long): String {
        return try {
            val sdf = SimpleDateFormat("d 'de' MMM 'de' yyyy", Locale("es", "CO"))
            sdf.format(Date(ms))
        } catch (e: Exception) { "—" }
    }
}
