
package io.github.philips148.wellnesscoach.ui.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.philips148.wellnesscoach.data.coach.CoachRepository

// Creates the AI Coach ViewModel with the repository it needs.
class CoachViewModelFactory(
    private val coachRepository: CoachRepository
) : ViewModelProvider.Factory {

    // Android calls this when it needs to create our ViewModel.
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        // Check that Android requested the correct ViewModel.
        if (modelClass.isAssignableFrom(CoachViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CoachViewModel(coachRepository) as T
        }

        // Report an error if an unexpected ViewModel was requested.
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
