package io.github.philips148.wellnesscoach.ui.navigation

import android.net.Uri
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.philips148.wellnesscoach.data.auth.AuthRepository
import io.github.philips148.wellnesscoach.ui.login.LoginScreen
import io.github.philips148.wellnesscoach.ui.login.LoginViewModel
import io.github.philips148.wellnesscoach.ui.login.LoginViewModelFactory
import io.github.philips148.wellnesscoach.ui.passwordreset.PasswordResetScreen
import io.github.philips148.wellnesscoach.ui.passwordreset.PasswordResetSentScreen
import io.github.philips148.wellnesscoach.ui.passwordreset.PasswordResetViewModel
import io.github.philips148.wellnesscoach.ui.passwordreset.PasswordResetViewModelFactory
import io.github.philips148.wellnesscoach.ui.profile.DeleteAccountScreen
import io.github.philips148.wellnesscoach.ui.profile.DeleteAccountViewModel
import io.github.philips148.wellnesscoach.ui.profile.DeleteAccountViewModelFactory

@Composable
fun AppNavHost(
    userId: String?,
    authRepository: AuthRepository,
    onSignOutClick: () -> Unit,
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
                    val loginViewModel: LoginViewModel = viewModel(
                        factory = LoginViewModelFactory(authRepository)
                    )

                    LoginScreen(
                        uiState = loginViewModel.uiState,
                        onEmailChange = loginViewModel::onEmailChange,
                        onPasswordChange = loginViewModel::onPasswordChange,
                        onSignInClick = loginViewModel::signIn,
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
                    val passwordResetViewModel: PasswordResetViewModel =
                        viewModel(
                            factory = PasswordResetViewModelFactory(
                                authRepository
                            )
                        )

                    val uiState = passwordResetViewModel.uiState

                    LaunchedEffect(uiState.requestSuccessful) {
                        if (uiState.requestSuccessful) {
                            val email = Uri.encode(uiState.email)

                            navController.navigate(
                                "${AppRoutes.PASSWORD_RESET_SENT}?email=$email"
                            ) {
                                launchSingleTop = true
                            }

                            passwordResetViewModel.onRequestSuccessHandled()
                        }
                    }

                    PasswordResetScreen(
                        uiState = uiState,
                        onEmailChange =
                            passwordResetViewModel::onEmailChange,
                        onSendResetLinkClick =
                            passwordResetViewModel::sendPasswordReset,
                        onBackToSignInClick = {
                            navController.popBackStack(
                                AppRoutes.LOGIN,
                                false
                            )
                        }
                    )
                }

                composable(
                    route =
                        "${AppRoutes.PASSWORD_RESET_SENT}?email={email}",
                    arguments = listOf(
                        navArgument("email") {
                            type = NavType.StringType
                            defaultValue = ""
                        }
                    )
                ) { backStackEntry ->
                    val email =
                        backStackEntry.arguments?.getString("email").orEmpty()

                    PasswordResetSentScreen(
                        email = email,
                        onBackToSignInClick = {
                            navController.popBackStack(
                                AppRoutes.LOGIN,
                                false
                            )
                        },
                        onUseDifferentEmailClick = {
                            navController.popBackStack(
                                AppRoutes.PASSWORD_RESET,
                                false
                            )
                        }
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
                            onClick = onSignOutClick
                        ) {
                            Text("Log Out")
                        }

                        Button(
                            onClick = {
                                navController.navigate(AppRoutes.DELETE_ACCOUNT)
                            }
                        ) {
                            Text("Delete Account")
                        }
                    }
                }

                composable(AppRoutes.DELETE_ACCOUNT) {
                    val deleteAccountViewModel: DeleteAccountViewModel = viewModel(
                        factory = DeleteAccountViewModelFactory(authRepository)
                    )

                    DeleteAccountScreen(
                        uiState = deleteAccountViewModel.uiState,
                        onPasswordChange = deleteAccountViewModel::onPasswordChange,
                        onDeleteClick = deleteAccountViewModel::deleteAccount,
                        onCancelClick = { navController.popBackStack() }
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