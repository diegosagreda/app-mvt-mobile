package com.example.mvt.subscription.model

data class UserPlan(
    val nombre: String = "",
    val fechaRegistro: Long = 0L,
    val caracteres_chat: Int = 0,
    val fotos: Int = 0,
    val video_min: Int = 0,
    val solicitudes: Int = 0,
    val solicitudesEnviadas: Int = 0,
    val rutinasComodin: Boolean = false,
    val vencimientoAutomatico: VencimientoAutomatico? = null
)

data class VencimientoAutomatico(
    val ejecutado: Boolean = false,
    val fechaEjecucion: Long = 0L,
    val motivo: String = ""
)

data class PlanCatalogo(
    val id: String = "",
    val precio: Int = 0,
    val precioNormal: Int = 0,
    val precioUSD: Double = 0.0,
    val disponibilidad: Boolean = false,
    val periodoActualizacion: Int = 0,
    val visualizarPlan: Int = 0,
    val caracteristicas: Map<String, Any> = emptyMap()
)

data class Cobro(
    val fecha: String = "",
    val bank_name: String = "",
    val plan: String = "",
    val recibo: String = "",
    val factura: String = "",
    val monto: Int = 0,
    val actualizadoEn: Long = 0L
)

data class SubscriptionStatus(
    val userPlan: UserPlan = UserPlan(),
    val planCatalogo: PlanCatalogo = PlanCatalogo(),
    val cobros: List<Cobro> = emptyList(),
    val esPlanGratuito: Boolean = true,
    val fechaInicio: String = "",
    val fechaCorte: String = "No aplica",
    val diasRestantes: Int = -1,
    val diasTotales: Int = 0,
    val progresoPlan: Float = 1f
)