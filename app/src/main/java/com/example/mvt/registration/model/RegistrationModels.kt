package com.example.mvt.registration.model

data class AthleteRegistrationForm(
    val firstName: String = "",
    val lastName: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val passwordConfirmation: String = "",
    val acceptedTerms: Boolean = false,
    val acceptedPrivacyPolicy: Boolean = false
)

data class AthleteRegistrationCommand(
    val firstName: String,
    val lastName: String,
    val nameUser: String,
    val email: String,
    val password: String
)

data class RegistrationErrors(
    val firstName: String? = null,
    val lastName: String? = null,
    val username: String? = null,
    val email: String? = null,
    val password: String? = null,
    val passwordConfirmation: String? = null,
    val legal: String? = null
) {
    val hasPersonalErrors get() = firstName != null || lastName != null || username != null || legal != null
    val hasAccessErrors get() = email != null || password != null || passwordConfirmation != null
}

enum class RegistrationProgress {
    EDITING, VALIDATING, CREATING_AUTH_USER, CREATING_PROFILE, SENDING_VERIFICATION, SUCCESS, ERROR
}

data class RegistrationUiState(
    val form: AthleteRegistrationForm = AthleteRegistrationForm(),
    val step: Int = 1,
    val errors: RegistrationErrors = RegistrationErrors(),
    val progress: RegistrationProgress = RegistrationProgress.EDITING,
    val errorMessage: String? = null,
    val canRetryInitialization: Boolean = false
) {
    val isSubmitting get() = progress in setOf(
        RegistrationProgress.CREATING_AUTH_USER,
        RegistrationProgress.CREATING_PROFILE,
        RegistrationProgress.SENDING_VERIFICATION
    )
}

data class PasswordChecks(
    val minimumLength: Boolean,
    val uppercase: Boolean,
    val number: Boolean,
    val special: Boolean
) {
    val allValid get() = minimumLength && uppercase && number && special
}

fun passwordChecks(password: String) = PasswordChecks(
    minimumLength = password.length >= 8,
    uppercase = password.any(Char::isUpperCase),
    number = password.any(Char::isDigit),
    special = password.any { it in " !\"#${'$'}%&'()*+,-./:;<=>?@[\\]^_`{|}~" }
)

fun validatePersonalStep(form: AthleteRegistrationForm): RegistrationErrors {
    val nameRegex = Regex("^[\\p{L}][\\p{L}\\s'-]{1,49}${'$'}")
    val usernameRegex = Regex("^[a-zA-Z0-9._-]{3,30}${'$'}")
    return RegistrationErrors(
        firstName = if (!nameRegex.matches(form.firstName.trim()) || form.firstName.trim().length < 3) {
            "Ingresa un nombre válido de 3 a 50 caracteres"
        } else null,
        lastName = if (!nameRegex.matches(form.lastName.trim()) || form.lastName.trim().length < 3) {
            "Ingresa un apellido válido de 3 a 50 caracteres"
        } else null,
        username = if (!usernameRegex.matches(form.username.trim())) {
            "Usa entre 3 y 30 letras, números, puntos, guiones o guion bajo"
        } else null,
        legal = if (!form.acceptedTerms || !form.acceptedPrivacyPolicy) {
            "Debes aceptar los términos y la política de privacidad"
        } else null
    )
}

fun validateAccessStep(form: AthleteRegistrationForm): RegistrationErrors {
    val emailRegex = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+${'$'}")
    val checks = passwordChecks(form.password)
    return RegistrationErrors(
        email = if (!emailRegex.matches(form.email.trim())) "Ingresa un correo válido" else null,
        password = if (!checks.allValid) "La contraseña no cumple todos los requisitos" else null,
        passwordConfirmation = if (form.password != form.passwordConfirmation) "Las contraseñas no coinciden" else null
    )
}

fun AthleteRegistrationForm.toCommand() = AthleteRegistrationCommand(
    firstName = firstName.trim(),
    lastName = lastName.trim(),
    nameUser = username.trim(),
    email = email.trim().lowercase(),
    password = password
)
