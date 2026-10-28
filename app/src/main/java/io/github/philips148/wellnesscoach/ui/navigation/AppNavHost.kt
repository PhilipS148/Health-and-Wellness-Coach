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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.philips148.wellnesscoach.data.auth.AuthRepository
import io.github.philips148.wellnesscoach.data.user.UserRepository
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
import io.github.philips148.wellnesscoach.ui.profile.ProfileScreen
import io.github.philips148.wellnesscoach.ui.profile.ProfileViewModel
import io.github.philips148.wellnesscoach.ui.profile.ProfileViewModelFactory
import io.github.philips148.wellnesscoach.ui.auth.RegistrationViewModel
import io.github.philips148.wellnesscoach.ui.auth.RegistrationViewModelFactory
import io.github.philips148.wellnesscoach.data.wellness.WellnessRepository
import io.github.philips148.wellnesscoach.ui.meals.MealTrackingScreen
import io.github.philips148.wellnesscoach.ui.meals.MealTrackingViewModel
import io.github.philips148.wellnesscoach.ui.meals.MealTrackingViewModelFactory

@Composable
fun AppNavHost(
    userId: String?,
    authRepository: AuthRepository,
    userRepository: UserRepository,
    wellnessRepository: WellnessRepository,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val registrationViewModel: RegistrationViewModel = viewModel(
        factory = RegistrationViewModelFactory(authRepository)
    )

    val registrationState = registrationViewModel.uiState

    var pendingSetupUserId by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    val handleRegistrationSuccess: () -> Unit = {
        if (registrationViewModel.uiState.registrationSuccessful) {
            val registeredUserId = authRepository.getCurrentUserId()

            if (registeredUserId != null) {
                pendingSetupUserId = registeredUserId
                registrationViewModel.resetForm()
            }
        }
    }

    // Remember setup before clearing the successful registration form.
    LaunchedEffect(userId, registrationState.registrationSuccessful) {
        if (registrationState.registrationSuccessful) {
            handleRegistrationSuccess()
        }
    }

    // Wait for profile storage and setup routing before signed-in navigation.
    val navigationUserId = if (
        registrationState.isLoading ||
        registrationState.isProfileSavePending ||
        (
                registrationState.registrationSuccessful &&
                        pendingSetupUserId != userId
                )
    ) {
        null
    } else if (userId == authRepository.getCurrentUserId()) {
        userId
    } else {
        null
    }

    // Recreate navigation when the account state changes.
    key(navigationUserId) {
        val navController = rememberNavController()

        // Keep the initial destination stable while this account is active.
        val startDestination = remember {
            when {
                navigationUserId == null -> AppRoutes.LOGIN

                pendingSetupUserId == navigationUserId ->
                    "${AppRoutes.PROFILE}?setup=true"

                else -> AppRoutes.DASHBOARD
            }
        }

        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = modifier
        ) {
            if (navigationUserId == null) {
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
                            pendingSetupUserId = null
                            registrationViewModel.resetForm()
                            navController.navigate(AppRoutes.REGISTER)
                        }
                    )
                }

                composable(AppRoutes.REGISTER) {
                    RegistrationScreen(
                        onRegistrationSuccess = handleRegistrationSuccess,
                        viewModel = registrationViewModel,
                        onBackClick = {
                            if (!registrationViewModel.uiState.isLoading) {
                                registrationViewModel.cancelRegistration()
                                navController.popBackStack()
                            }
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
                        Button(
                            onClick = {
                                navController.navigate(AppRoutes.MEALS)
                            }
                        ) {
                            Text("Meals")
                        }
                    }
                }

                composable(AppRoutes.MEALS) {

                    val mealViewModel: MealTrackingViewModel =
                        viewModel(
                            factory = MealTrackingViewModelFactory(
                                wellnessRepository
                            )
                        )

                    val mealState = mealViewModel.uiState.value

                    MealTrackingScreen(
                        uiState = mealState,
                        onDateChange = mealViewModel::onDateChange,
                        onMealNameChange = mealViewModel::onMealNameChange,
                        onCaloriesChange = mealViewModel::onCaloriesChange,
                        onLoadDateClick = mealViewModel::loadMeals,
                        onSaveClick = mealViewModel::saveMeal,
                        onEditClick = mealViewModel::editMeal,
                        onDeleteClick = mealViewModel::deleteMeal,
                        onCancelEditClick = mealViewModel::cancelEdit,
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(AppRoutes.ACCOUNT_SETTINGS) {
                    NavigationPreview(
                        title = "Account settings",
                        onBack = { navController.popBackStack() }
                    ) {
                        Button(
                            onClick = {
                                navController.navigate(AppRoutes.PROFILE)
                            }
                        ) {
                            Text("Edit Profile")
                        }

                        Button(
                            onClick = {
                                pendingSetupUserId = null
                                onSignOutClick()
                            }
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

                composable(
                    route = "${AppRoutes.PROFILE}?setup={setup}",
                    arguments = listOf(
                        navArgument("setup") {
                            type = NavType.BoolType
                            defaultValue = false
                        }
                    )
                ) { backStackEntry ->
                    val isSetupFlow =
                        backStackEntry.arguments?.getBoolean("setup") ?: false

                    val profileViewModel: ProfileViewModel = viewModel(
                        factory = ProfileViewModelFactory(
                            userRepository = userRepository,
                            authRepository = authRepository,
                            isSetupFlow = isSetupFlow
                        )
                    )

                    val uiState = profileViewModel.uiState

                    LaunchedEffect(uiState.isSetupComplete) {
                        if (uiState.isSetupComplete) {
                            pendingSetupUserId = null

                            navController.navigate(AppRoutes.DASHBOARD) {
                                popUpTo(backStackEntry.destination.id) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
                    }

                    ProfileScreen(
                        uiState = uiState,
                        onUsernameChange = profileViewModel::onUsernameChange,
                        onHeightChange = profileViewModel::onHeightChange,
                        onWeightChange = profileViewModel::onWeightChange,
                        onSaveClick = profileViewModel::saveProfile,
                        onRetryClick = profileViewModel::loadProfile,
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
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