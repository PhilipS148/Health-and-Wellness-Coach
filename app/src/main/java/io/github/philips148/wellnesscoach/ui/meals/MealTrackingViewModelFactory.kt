package io.github.philips148.wellnesscoach.ui.meals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.philips148.wellnesscoach.data.wellness.WellnessRepository

class MealTrackingViewModelFactory(
    private val wellnessRepository: WellnessRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (
            modelClass.isAssignableFrom(
                MealTrackingViewModel::class.java
            )
        ) {
            return MealTrackingViewModel(
                wellnessRepository
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}