package io.github.philips148.wellnesscoach.ui.steps

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.common.GoogleApiAvailability
import io.github.philips148.wellnesscoach.data.auth.AuthRepository
import io.github.philips148.wellnesscoach.data.steps.PhoneStepDataSource
import io.github.philips148.wellnesscoach.data.steps.StepRepository
import io.github.philips148.wellnesscoach.data.user.UserRepository

@Composable
fun StepRoute(
    authRepository: AuthRepository,
    userRepository: UserRepository,
    stepRepository: StepRepository,
    phoneStepDataSource: PhoneStepDataSource,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = LocalActivity.current

    // Create the ViewModel with the app's shared dependencies.
    val stepViewModel: StepViewModel = viewModel(
        factory = StepViewModelFactory(
            authRepository = authRepository,
            userRepository = userRepository,
            stepRepository = stepRepository,
            phoneStepDataSource = phoneStepDataSource
        )
    )

    // Android displays the permission dialog and reports the user's choice.
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        stepViewModel.onPermissionResult(granted)
    }

    // Recheck permission, date, and totals when returning to this screen.
    // This also handles permission changes made in the phone's Settings.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        stepViewModel.refresh()
    }

    StepScreen(
        uiState = stepViewModel.uiState,
        onRefreshClick = stepViewModel::refresh,
        onRetrySaveClick = stepViewModel::retrySave,

        onEnableStepsClick = {
            permissionLauncher.launch(
                Manifest.permission.ACTIVITY_RECOGNITION
            )
        },

        onOpenSettingsClick = {
            // Open Settings for this application.
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            ).apply {
                data = Uri.fromParts(
                    "package",
                    context.packageName,
                    null
                )
            }

            context.startActivity(intent)
        },

        onFixPlayServicesClick = {
            val errorCode =
                stepViewModel.uiState.playServicesErrorCode

            if (activity != null && errorCode != null) {
                // Let Google explain and offer the appropriate service fix.
                GoogleApiAvailability.getInstance()
                    .getErrorDialog(
                        activity,
                        errorCode,
                        1001
                    )
                    ?.show()
            }
        },

        onBackClick = onBackClick,
        modifier = modifier
    )
}