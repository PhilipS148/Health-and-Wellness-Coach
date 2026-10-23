package io.github.philips148.wellnesscoach.data.auth

// Distinguish a failed profile save from a failed account creation.
class RegistrationProfileException(
    cause: Throwable
) : Exception(
    "Your account was created, but your profile could not be saved.",
    cause
)