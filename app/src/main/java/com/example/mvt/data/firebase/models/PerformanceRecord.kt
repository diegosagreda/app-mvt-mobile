package com.example.mvt.data.firebase.models

data class PerformanceRecord(
    val id: String = "",
    val VAM: String = "",
    val VAM_decimal: Double = 0.0,
    val VO2max: String = "",
    val fecha: Long = 0L,
    val min: Int = 0,
    val seg: String = "00",
    val semicooper: String = ""
)