package io.github.philips148.wellnesscoach.ui.steps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.philips148.wellnesscoach.data.auth.AuthRepository
import io.github.philips148.wellnesscoach.data.steps.PhoneStepDataSource
import io.github.philips148.wellnesscoach.data.steps.StepRepository
import io.github.philips148.wellnesscoach.data.user.UserRepository

class StepViewModelFactory(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val stepRepository: StepRepository,
    private val phoneStepDataSource: PhoneStepDataSource
) : ViewModelProvider.Factory {

    // Supply the shared dependencies when Android requests this ViewModel.
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StepViewModel::class.java)) {
            return StepViewModel(
                authRepository = authRepository,
                userRepository = userRepository,
                stepRepository = stepRepository,
                phoneStepDataSource = phoneStepDataSource
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}