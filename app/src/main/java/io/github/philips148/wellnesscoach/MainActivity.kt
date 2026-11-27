package io.github.philips148.wellnesscoach

import android.os.Bundle
import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.philips148.wellnesscoach.ui.app.AppViewModel
import io.github.philips148.wellnesscoach.ui.app.AppViewModelFactory
import io.github.philips148.wellnesscoach.ui.theme.HealthAndWellnessCoachTheme
import io.github.philips148.wellnesscoach.ui.navigation.AppNavHost

class MainActivity : ComponentActivity() {
    private var reminderRequest by mutableStateOf<Pair<String, String>?>(null)

    private fun readReminderIntent(intent: Intent?) {
        val owner = intent?.getStringExtra("reminder_owner")
        val route = intent?.getStringExtra("reminder_route")
        reminderRequest = if (owner != null && route in setOf("hydration", "check_ins", "workouts")) owner to route!! else null
        intent?.removeExtra("reminder_owner")
        intent?.removeExtra("reminder_route")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readReminderIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        io.github.philips148.wellnesscoach.reminders.ReminderDelivery(this).restore()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        readReminderIntent(intent)

        // Get the shared dependencies created when the app starts.
        val appContainer =
            (application as WellnessCoachApplication).appContainer

        setContent {
            HealthAndWellnessCoachTheme {

                // Let Android manage the ViewModel and its lifetime.
                val appViewModel: AppViewModel = viewModel(
                    factory = AppViewModelFactory(
                        appContainer.authRepository
                    )
                )

                // Observe the user ID while the screen is active.
                val userId by appViewModel.userId.collectAsStateWithLifecycle()

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    AppNavHost(
                        userId = userId,
                        authRepository = appContainer.authRepository,
                        userRepository = appContainer.userRepository,
                        wellnessRepository = appContainer.wellnessRepository,
                        waterRepository = appContainer.waterRepository,
                        stepRepository = appContainer.stepRepository,
                        phoneStepDataSource = appContainer.phoneStepDataSource,
                        summaryRepository = appContainer.summaryRepository,
                        onSignOutClick = appViewModel::signOut,
                        reminderRequest = reminderRequest,
                        onReminderConsumed = { reminderRequest = null },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}