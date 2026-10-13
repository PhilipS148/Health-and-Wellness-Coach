# Health and Wellness Coach

Android application using Kotlin, Jetpack Compose, Firebase
Authentication, and Cloud Firestore.

## Initial architecture

- `WellnessCoachApplication` creates the shared `AppContainer`.
- `AppContainer` creates the Firebase authentication data source
  and authentication repository.
- `AuthRepository` defines the authentication operations.
- `DefaultAuthRepository` implements that interface and delegates
  Firebase operations to `FirebaseAuthDataSource`.
- `AppViewModel` exposes the current user ID for the interface.
- `AppViewModelFactory` supplies the repository to the ViewModel.
- `MainActivity` connects the ViewModel, shared theme, and navigation.
- `AppRoutes` defines screen names; `AppNavHost` connects the screens.
- `ui/theme` contains the shared colors and light theme.

## Current implementation status

The app builds and runs. Login, registration, and password-reset
navigation previews have been checked, including Back navigation.

Registration, sign-in, password reset, and account deletion are
not implemented yet. Their repository methods currently return
explicit failures. The preview screens only demonstrate navigation.

The user ID is read when the ViewModel is created and when
`refreshUserId()` is called. Automatic session monitoring remains
part of the login/session task.

Live authentication and Firestore access tests remain to be completed.

## Adding features

Create each feature's screens and ViewModels in its own package.
Use the shared theme and access authentication through the repository.

Replace the corresponding navigation preview with the real screen.
Coordinate changes to shared repository, data source, container,
and navigation files to avoid conflicting edits.