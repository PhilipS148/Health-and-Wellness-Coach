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

import io.github.philips148.wellnesscoach.data.hydration.FirebaseWaterRepository
import io.github.philips148.wellnesscoach.data.hydration.WaterRepository

import io.github.philips148.wellnesscoach.data.steps.DefaultStepRepository
import io.github.philips148.wellnesscoach.data.steps.FirestoreStepDataSource
import io.github.philips148.wellnesscoach.data.steps.StepRepository

import android.content.Context
import io.github.philips148.wellnesscoach.data.steps.PhoneStepDataSource

class AppContainer(context: Context) {

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
    // W3 hydration records use the signed-in user's water collection.
    val waterRepository: WaterRepository = FirebaseWaterRepository(authRepository)

    val wellnessRepository: WellnessRepository =
        DefaultWellnessRepository(
            authDataSource = authDataSource,
            wellnessDataSource = wellnessDataSource
        )

    // Communicates with Firestore to save and load daily step totals.
    private val stepDataSource = FirestoreStepDataSource()

    // Shared step repository for ViewModels.
    // Uses the existing authentication source to identify the account.
    val stepRepository: StepRepository = DefaultStepRepository(
        authDataSource = authDataSource,
        stepDataSource = stepDataSource
    )

    // Shared reader for steps recorded on this phone.
    // The application supplies the Android context it needs.
    val phoneStepDataSource = PhoneStepDataSource(context)

}