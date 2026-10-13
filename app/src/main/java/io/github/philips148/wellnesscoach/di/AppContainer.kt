package io.github.philips148.wellnesscoach.di

import io.github.philips148.wellnesscoach.data.auth.AuthRepository
import io.github.philips148.wellnesscoach.data.auth.DefaultAuthRepository
import io.github.philips148.wellnesscoach.data.auth.FirebaseAuthDataSource

class AppContainer {

    // Create the object that communicates with Firebase Authentication.
    private val authDataSource = FirebaseAuthDataSource()

    // Give that data source to our repository implementation.
    val authRepository: AuthRepository = DefaultAuthRepository(
        authDataSource = authDataSource
    )
}