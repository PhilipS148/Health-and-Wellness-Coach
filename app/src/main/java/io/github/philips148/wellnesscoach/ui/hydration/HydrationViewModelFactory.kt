package io.github.philips148.wellnesscoach.ui.hydration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.philips148.wellnesscoach.data.hydration.WaterRepository
import io.github.philips148.wellnesscoach.data.user.UserRepository

class HydrationViewModelFactory(
    private val waterRepository: WaterRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HydrationViewModel::class.java)) {
            return HydrationViewModel(waterRepository, userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
