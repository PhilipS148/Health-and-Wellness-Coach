package io.github.philips148.wellnesscoach.ui.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.philips148.wellnesscoach.data.wellness.WellnessRepository

class SleepViewModelFactory(
    private val wellnessRepository: WellnessRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SleepViewModel::class.java)) {
            return SleepViewModel(wellnessRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
