package com.example.mvt.data.firebase.models

import com.google.firebase.Timestamp
import java.io.Serializable

data class Morphology(
    val estatura: String? = null,
    val peso: String? = null,
    val grasa: String? = null,
    val imc: String? = null,
    val somatotipo: String? = null
)