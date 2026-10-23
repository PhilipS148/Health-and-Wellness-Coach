package io.github.philips148.wellnesscoach.ui.profile

data class ProfileUiState(
    val username: String = "",
    val heightCm: String = "",
    val weightKg: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isNewProfile: Boolean = true,
    val isSetupFlow: Boolean = false,
    val isSetupComplete: Boolean = false,
    val loadFailed: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)