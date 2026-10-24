package io.github.philips148.wellnesscoach.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.philips148.wellnesscoach.data.auth.AuthRepository
import io.github.philips148.wellnesscoach.data.user.UserRepository

class ProfileViewModelFactory(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val isSetupFlow: Boolean = false
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            return ProfileViewModel(
                userRepository = userRepository,
                authRepository = authRepository,
                isSetupFlow = isSetupFlow
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}