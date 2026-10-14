package io.github.philips148.wellnesscoach.ui.navigation

import io.github.philips148.wellnesscoach.ui.auth.RegistrationScreen
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.philips148.wellnesscoach.ui.login.LoginScreen

@Composable
fun AppNavHost(
    userId: String?,
    modifier: Modifier = Modifier
) {
    // Recreate navigation when the account state changes.
    key(userId) {
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = if (userId == null) {
                AppRoutes.LOGIN
            } else {
                AppRoutes.DASHBOARD
            },
            modifier = modifier
        ) {
            if (userId == null) {
                // Screens available while signed out.
                composable(AppRoutes.LOGIN) {
                    LoginScreen(
                        onForgotPasswordClick = {
                            navController.navigate(AppRoutes.PASSWORD_RESET)
                        },
                        onCreateAccountClick = {
                            navController.navigate(AppRoutes.REGISTER)
                        }
                    )
                }

                composable(AppRoutes.REGISTER) {
                    RegistrationScreen(
                        onRegistrationSuccess = {
                            navController.popBackStack()
                        }
                    )
                }
                composable(AppRoutes.PASSWORD_RESET) {
                    NavigationPreview(
                        title = "Password reset",
                        onBack = { navController.popBackStack() }
                    ) {
                        Button(
                            onClick = {
                                navController.navigate(
                                    AppRoutes.PASSWORD_RESET_SENT
                                )
                            }
                        ) {
                            Text("Preview confirmation screen")
                        }
                    }
                }

                composable(AppRoutes.PASSWORD_RESET_SENT) {
                    NavigationPreview(
                        title = "Password reset confirmation",
                        onBack = { navController.popBackStack() }
                    )
                }
            } else {
                // Screens available when a signed-in user is detected.
                composable(AppRoutes.DASHBOARD) {
                    NavigationPreview(title = "Dashboard") {
                        Button(
                            onClick = {
                                navController.navigate(AppRoutes.ACCOUNT_SETTINGS)
                            }
                        ) {
                            Text("Account settings")
                        }
                    }
                }

                composable(AppRoutes.ACCOUNT_SETTINGS) {
                    NavigationPreview(
                        title = "Account settings",
                        onBack = { navController.popBackStack() }
                    ) {
                        Button(
                            onClick = {
                                navController.navigate(AppRoutes.DELETE_ACCOUNT)
                            }
                        ) {
                            Text("Preview account deletion screen")
                        }
                    }
                }

                composable(AppRoutes.DELETE_ACCOUNT) {
                    NavigationPreview(
                        title = "Account deletion",
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

// Temporary layout shared by the navigation previews.
@Composable
private fun NavigationPreview(
    title: String,
    onBack: (() -> Unit)? = null,
    content: @Composable () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium
        )

        Text("Temporary screen for navigation testing.")

        content()

        if (onBack != null) {
            TextButton(onClick = onBack) {
                Text("Back")
            }
        }
    }
}