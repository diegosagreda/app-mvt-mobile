package com.example.mvt.trainer.profile.model

data class PersonalInfoModel(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val identification: String = "",
    val gender: String = "",
    val birthDate: String = "",
    val photoUrl: String = "",
    val email: String = "",
    val alias: String = "",
    val mvtId: String = ""
) {
    val fullName: String
        get() = listOf(firstName, lastName)
            .filter(String::isNotBlank)
            .joinToString(" ")
}