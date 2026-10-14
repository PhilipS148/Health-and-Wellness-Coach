package io.github.philips148.wellnesscoach.util

object PasswordValidator {
    fun validate(password: String): String? {
        return when {
            password.length < 8 -> "Password must be at least 8 characters."
            password.none { it.isUpperCase() } -> "Password must include an uppercase letter."
            password.none { it.isLowerCase() } -> "Password must include a lowercase letter."
            password.none { it.isDigit() } -> "Password must include a number."
            else -> null
        }
    }

}