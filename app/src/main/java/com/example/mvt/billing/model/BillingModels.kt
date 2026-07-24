package com.example.mvt.billing.model

data class BillingPago(
    val recibo: String = "",
    val plan: String = "",
    val monto: Int = 0,
    val descuento: Int = 0,
    val codigoDescuento: String = "",
    val estado: String = "",
    val factura: String = "",
    val fechaTransaccion: String = "",
    val actualizadoEn: Long = 0L,
    val bankName: String = "",
    val medioPago: String = "",
    val franquicia: String = "",
    val referencia: String = ""
)

data class BillingStatus(
    val nombrePlan: String = "",
    val esPlanGratuito: Boolean = true,
    val fechaInicio: String = "",
    val fechaCorte: String = "",
    val diasRestantes: Int = -1,
    val diasTotales: Int = 30,
    val diasUsados: Int = 0,
    val progreso: Float = 1f,
    val totalPagado: Int = 0,
    val totalPagos: Int = 0,
    val totalDescuentos: Int = 0,
    val pagosConDescuento: Int = 0,
    val ultimoPago: BillingPago? = null,
    val historialPagos: List<BillingPago> = emptyList()
)