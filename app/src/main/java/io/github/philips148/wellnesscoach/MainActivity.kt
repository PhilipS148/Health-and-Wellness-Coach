package io.github.philips148.wellnesscoach

import android.os.Bundle
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}