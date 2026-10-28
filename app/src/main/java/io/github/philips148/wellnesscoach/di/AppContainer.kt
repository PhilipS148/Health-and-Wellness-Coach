package io.github.philips148.wellnesscoach.di

import io.github.philips148.wellnesscoach.data.auth.AuthRepository
import io.github.philips148.wellnesscoach.data.auth.DefaultAuthRepository
import io.github.philips148.wellnesscoach.data.auth.FirebaseAuthDataSource
import io.github.philips148.wellnesscoach.data.user.FirestoreUserDataSource
import io.github.philips148.wellnesscoach.data.user.DefaultUserRepository
import io.github.philips148.wellnesscoach.data.user.UserRepository
import io.github.philips148.wellnesscoach.data.wellness.DefaultWellnessRepository
import io.github.philips148.wellnesscoach.data.wellness.FirestoreWellnessDataSource
import io.github.philips148.wellnesscoach.data.wellness.WellnessRepository

class AppContainer {

    // Create the object that communicates with Firebase Authentication.
    private val authDataSource = FirebaseAuthDataSource()
    private val userDataSource = FirestoreUserDataSource()

    private val wellnessDataSource = FirestoreWellnessDataSource()
    // Give that data source to our repository implementation.
    val authRepository: AuthRepository = DefaultAuthRepository(
        authDataSource = authDataSource,
        userDataSource = userDataSource
    )
    // Shared repository for profile information, goals, and categories.
    val userRepository: UserRepository = DefaultUserRepository(
        authDataSource = authDataSource,
        userDataSource = userDataSource
    )

    val wellnessRepository: WellnessRepository =
        DefaultWellnessRepository(
            authDataSource = authDataSource,
            wellnessDataSource = wellnessDataSource
        )
}