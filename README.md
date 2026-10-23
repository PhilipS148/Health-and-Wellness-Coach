# Health and Wellness Coach

Android application using Kotlin, Jetpack Compose, Firebase
Authentication, and Cloud Firestore.

## Architecture

- `WellnessCoachApplication` creates the shared `AppContainer`.
- `AppContainer` supplies shared repositories and Firebase data sources.
- Screens display information and send user actions to ViewModels.
- ViewModels manage screen state and call repositories.
- Repositories define application data operations.
- Data sources communicate with Firebase or Android services.
- `AppViewModel` observes authentication state.
- `MainActivity` connects the shared theme and navigation.
- `AppRoutes` and `AppNavHost` define application navigation.
- `ui/theme` contains shared colors and styling.

## Current implementation status

Registration, login, session persistence, logout, password reset,
password validation, authentication error handling, and account
deletion have been implemented and manually checked.

Registration now uses AuthRepository to create the Firebase
Authentication account and save the initial Firestore profile,
including the username, default goals, and selected categories.
The dashboard opens after the profile saves successfully.

If profile saving fails after the account is created, users can
retry saving the profile using the same account. Choosing
Back to Sign In signs them out and clears the registration form.

P2 implements profile loading, saving, and validation.
Profile setup/editing screens, goal and category settings screens,
wellness logging, automatic steps, and the completed dashboard
remain to be implemented.

P2 verification passed:
- Debug build and manual registration/profile-save check.
- UserRepositoryTest: 1 test passed.
- RegistrationProfileTest: 2 tests passed.

## Repository contracts

All repository operations apply to the currently signed-in user.
Implementations must obtain the UID from Firebase Authentication and
return a failure when no user is signed in.

- `UserRepository`: Profile loading and saving are implemented.
  Pass `appContainer.userRepository` to the feature's ViewModel.
  P3 and P4 use it for profile, goal, and category settings.
- `WellnessRepository`: W1 implements manual-record data access.
  W2–W6 use it for their tracking screens.
- `StepRepository`: W7 implements daily step-total storage and retrieval.

Repository operations return `Result`. An empty record query returns
a successful empty list. A failed request returns a failure.
Coroutine cancellation must be rethrown.

`getProfile()` returns a successful null when the signed-in user has
no profile document. `saveProfile()` creates or updates that profile;
its UID must match the authenticated UID. When editing, preserve
profile fields the user is not changing.

## Firestore structure

| Information | Document path |
| --- | --- |
| Profile, goals, category preferences | `users/{uid}` |
| Meals | `users/{uid}/meals/{entryId}` |
| Hydration | `users/{uid}/water/{entryId}` |
| Workouts | `users/{uid}/workouts/{entryId}` |
| Sleep | `users/{uid}/sleep/{entryId}` |
| Check-ins | `users/{uid}/checkIns/{entryId}` |
| Daily steps | `users/{uid}/steps/{date}` |

Use the collection names in `FirestorePaths`.

Workout sets are stored as a list inside the workout document.
Goals and category preferences are fields in the user document.

Firestore Security Rules restrict access to the matching authenticated
UID. Account deletion must cover the user document and all six listed
record collections.

## IDs and timestamps

- Create operations for manual records generate a Firestore document ID.
- Store that document ID in the record's `id` field.
- Create operations return the new ID.
- Updates use an existing ID and preserve `id` and `createdAt`.
- Deletes use the existing record ID.
- Manual records may share a date while having different IDs.
- Repositories assign `createdAt` as Unix epoch milliseconds on creation.

## Dates and history

- Store `date` as `yyyy-MM-dd`, such as `2026-10-19`.
- Use the user's selected local calendar date.
- Sleep entries use the date the user woke up.
- Step totals use the phone's local calendar day.
- Changing the viewing date or time zone does not rewrite saved dates.
- Repository date ranges include both start and end dates.
- Passing the same start and end date retrieves one day.
- Validate real calendar dates and require startDate <= endDate.

## Profile and tracking preferences

- `displayName` stores the username entered by the user.
- Height and weight are optional.
- `wellnessGoals` stores the user's chosen wellness goals or focus areas.
- Numeric goals are editable user preferences.
- `selectedCategories` contains unique identifiers from
  `TrackingCategories.ALL`.
- New profiles default to all categories selected.
- Turning a category off preserves its recorded history.
- Selecting steps does not grant Android activity permission.
  W7 must request that permission separately.
- Passwords belong to Firebase Authentication and are never stored
  in Firestore.

## Units and validation

Validate input in the interface and repository before saving.
Model defaults support object construction; they do not replace
validation.

| Information | Stored unit and rule |
| --- | --- |
| Height | `heightCm`: positive centimeters, or null |
| Weight | `weightKg`: positive kilograms, or null |
| Calories | `calories`: positive whole-number kilocalories |
| Water | `amountOz`: positive whole-number US fluid ounces |
| Workout weight | `weightLbs`: non-negative pounds, or null |
| Walk/run distance | `distanceMiles`: positive miles |
| Walk/run duration | `durationMinutes`: positive whole-number minutes |
| Sleep | `hoursSlept`: greater than 0 and no more than 24 hours |
| Check-in ratings | Whole numbers from 1 through 10, or null |
| Steps | Non-negative whole-number step count |

Daily calorie, water, and step goals must be positive whole numbers.
The sleep goal must be greater than 0 and no more than 24 hours.

Meal names or descriptions must not be blank.
`mealType` and `mealTime` are optional; use an empty string when omitted.
When supplied, `mealTime` uses local 24-hour `HH:mm` format.

Fractional sleep durations are supported: 7.5 means 7 hours 30 minutes.

## Workout records

`workoutType` uses one of: `strength`, `walk`, or `run`.
Each entry represents one exercise or one walk/run activity.

Strength records require an exercise name and at least one set.
Each set stores its own positive repetition count and optional
non-negative weight. List order represents set order.
The number of sets comes from `sets.size`.

Walk/run records use manually entered distance and duration.
Their sets list is empty. Strength records leave distance and
duration null.

## Daily check-ins

At least one rating must be recorded when saving a check-in.

- Stress: 1 = very low stress; 10 = very high stress.
- Mood: 1 = very low mood; 10 = very good mood.
- Energy: 1 = very low energy; 10 = very high energy.

Null means no rating was recorded for that category.
Charts, summaries, and AI context must skip missing ratings.
Do not substitute 0 or 5 for missing information.

New check-ins collect the categories the user selected.
Editing a check-in must preserve existing ratings the user
has not changed.

## Totals and automatic steps

Daily calories and water intake are calculated from saved records.
Recalculate affected totals after creating, editing, or deleting entries.

For steps, use the date as both the document ID and `StepTotal.id`.
Saving a refreshed count replaces that date's stored total.
Do not increment the previous total or create another document.

A missing step record means no saved total is available.
A saved count of 0 represents a recorded zero.

## Adding features

Create each feature's screens and ViewModels in its own package.
Use the shared theme, models, and repository interfaces.

Coordinate changes to shared data sources, dependency containers,
and navigation files before merging.