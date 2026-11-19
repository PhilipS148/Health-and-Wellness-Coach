package io.github.philips148.wellnesscoach.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.philips148.wellnesscoach.data.summary.SummaryRepository
import io.github.philips148.wellnesscoach.data.wellness.WellnessRepository
import io.github.philips148.wellnesscoach.ui.sleep.SleepViewModel

class ProgressViewModelFactory(
    private val summaryRepository: SummaryRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProgressViewModel::class.java)) {
            return ProgressViewModel(summaryRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}