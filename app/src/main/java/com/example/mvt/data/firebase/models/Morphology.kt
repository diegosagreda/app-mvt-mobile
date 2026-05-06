package com.example.mvt.data.firebase.models

import com.google.firebase.Timestamp
import java.io.Serializable

data class Morphology(
    val estatura: String? = "",
    val peso: String? = "",
    val grasa: String? = "",
    val imc: String? = "",
    val somatipo: String? = "",
    val fecha_hombros: String = "",
    val medida_hombros: String = "",
    val fecha_pecho: String = "",
    val medida_pecho: String = "",
    val fecha_brazo: String = "",
    val medida_brazo: String = "",
    val fecha_cintura: String = "",
    val medida_cintura: String = "",
    val fecha_muslo: String = "",
    val medida_muslo: String = "",
    val fecha_gluteos: String = "",
    val medida_gluteos: String = "",
    val fecha_pantorrilla: String = "",
    val medida_pantorrilla: String = ""
)