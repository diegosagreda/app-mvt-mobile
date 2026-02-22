package com.example.mvt.data.firebase.models

data class User(
    val UserID: Int? = null,
    val nombres: String? = null,          // 👈 Campo usado en el saludo
    val apellidos: String? = null,
    val email: String? = null,
    val ciudadActual: String? = null,
    val direccion: String? = null,
    val estado: String? = null,
    val estrellas: Int? = null,
    val fecha_nacimiento: String? = null,
    val fecha_registro: Long? = null,
    val descripcion: String? = null,
    val NotiR: Boolean? = null,
    val foto_url: String? = null

    )
