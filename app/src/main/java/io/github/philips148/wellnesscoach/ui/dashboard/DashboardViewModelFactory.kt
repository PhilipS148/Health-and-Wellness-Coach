package io.github.philips148.wellnesscoach.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.philips148.wellnesscoach.data.summary.SummaryRepository
import io.github.philips148.wellnesscoach.data.user.UserRepository

class DashboardViewModelFactory(
    private val summaryRepository: SummaryRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (
            modelClass.isAssignableFrom(
                DashboardViewModel::class.java
            )
        ) {
            return DashboardViewModel(
                summaryRepository = summaryRepository,
                userRepository = userRepository
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}