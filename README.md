# Health and Wellness Coach

A capstone Android application for tracking everyday wellness habits, reviewing progress, and receiving personalized, general-purpose wellness guidance from an AI Coach.

Built for **CPSC 491/492** using **Kotlin**, **Jetpack Compose**, **Firebase Authentication**, and **Cloud Firestore**. The AI backend uses **TypeScript**, **Cloudflare Workers**, and **Cloudflare Workers AI**.

> **Project status — November 25, 2026:** Core account, tracking, dashboard, logs, progress, and AI Coach backend (A1) functionality is integrated into `master`. The Android AI Coach interface (A2) has been implemented and tested on the `feature/ai-coach-ui` branch and is awaiting merge. Reminders, final integration testing, and UI/UX polish remain outstanding.

## Features and development status

| Area | Current status | Notes |
| --- | --- | --- |
| Account management | Implemented | Registration, login, session persistence, logout, password reset, and account deletion flows |
| User profile and preferences | Implemented | Profile loading/saving, personal goals, category choices, numeric targets |
| Wellness logging | Integrated | Meals, hydration, workouts, sleep, and stress/mood/energy check-ins |
| Automatic steps | Integrated | Daily step totals and Android activity-permission requirements |
| Dashboard (D2) | Integrated | Overview of calories, hydration, steps, and sleep; intentionally a functional prototype |
| Logs/navigation (D3) | Integrated | Central access to tracking and history workflows |
| Progress (D4) | Merged into `master` | Daily/weekly summaries, charts, history, date navigation, refresh, and empty/error states |
| AI Coach backend (A1) | Merged and deployed | Authenticated Firestore context and AI-generated responses verified from the Android app |
| AI Coach Android UI (A2) | Implemented and tested; awaiting merge | Chat interface, authenticated networking, input validation, loading, errors, retry, and account isolation |
| Reminders | Pending | To be completed and tested with remaining functional requirements |
| Final UI/UX polish | Planned | Consistency, usability, styling, and professional appearance after features are working |

*“Integrated” means the feature is in the Android codebase; it does not imply that every edge case or device configuration has been tested. Some older feature-specific acceptance tests still need final end-to-end verification.*

## Application architecture

The Android application uses a repository and ViewModel structure:

```text
Jetpack Compose screens
        ↓ user actions / displayed UI state
ViewModels
        ↓
Repositories
        ↓
Firebase data sources / Android services
```

- `WellnessCoachApplication` creates the shared `AppContainer`.
- `AppContainer` provides repositories and Firebase-backed data sources.
- `MainActivity` applies the shared Compose theme and hosts navigation.
- `AppRoutes` and `AppNavHost` define destinations and navigation.
- `AppViewModel` observes authentication state.
- Feature ViewModels manage loading, success, error, and input state.
- Repositories define application data operations; data sources access Firebase or Android services.
- `ui/theme` contains shared visual styling.

The AI Coach introduces a **separate server-side component**. Android does not call a hosted model directly:

```text
Android app (Firebase signed-in user)
   │ Authorization: Bearer <Firebase ID token>
   ▼
Cloudflare Worker (backend/ai-coach/)
   ├── Validate Firebase token and identify verified UID
   ├── Read that user's Firestore profile and selected records
   ├── Build seven-day wellness context
   └── Call Cloudflare Workers AI
   │
   ▼
JSON response to Android
```

The Worker is independently deployed; the app user does **not** need Node.js, Wrangler, a Cloudflare account, or terminal commands.

## Developer setup

### Android app

Requirements:

- Android Studio with a compatible Android SDK and JDK
- Access to the project's Firebase configuration and enabled Authentication/Firestore services
- An emulator or Android device for manual testing

From the repository root, open the project in Android Studio and sync Gradle. Ensure the correct Firebase `google-services.json` is available under `app/` for this project. Run the `app` configuration on an emulator or device and sign in with a test account.

Common checks:

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebugAndroidTest
```

`assembleDebugAndroidTest` **compiles** Android instrumentation tests; it does not execute them. To execute instrumentation tests, use an appropriate connected emulator/device and the corresponding test task or Android Studio runner.

### AI backend

Backend source: `backend/ai-coach/`

Requirements:

- Node.js and npm
- Cloudflare account with Workers and Workers AI access
- Firebase project with Authentication and Cloud Firestore
- Permission to deploy to the team's designated Cloudflare account

```bash
cd backend/ai-coach
npm install
npx wrangler login
npm run cf-typegen
npx tsc --noEmit
npm run dev
```

The local Worker listens at `http://localhost:8787` when Wrangler starts successfully. The AI binding invokes **remote** Cloudflare inference even in local development and can consume account usage.

The `wrangler.jsonc` file identifies the Worker entry point and required bindings:

```jsonc
{
  "name": "ai-coach",
  "main": "src/index.ts",
  "vars": {
    "FIREBASE_PROJECT_ID": "health-and-wellness-coac-74bee"
  },
  "ai": {
    "binding": "AI"
  }
}
```

This example shows the relevant settings, not the entire file. The Firebase project ID is configuration, not a secret. The AI binding gives the **Worker** access to Cloudflare inference; it is not an API credential placed in Android.

Deploy a tested version with:

```bash
npm run deploy
```

Current deployed endpoint:

`https://ai-coach.steep-mountain-2405.workers.dev`

Deployments are maintained by developers. Keep Cloudflare permissions, usage, and service availability in mind. Never commit passwords, session tokens, `.env*`, `.dev.vars*`, or local Cloudflare state. `node_modules/` and `.wrangler/` must remain ignored.

## Authentication and account boundaries

- Firebase Authentication owns passwords and sign-in state; passwords are never stored in Firestore.
- Protected Android repository operations must use the currently authenticated user's UID.
- The AI backend verifies the Firebase ID token before using any identity claim.
- `FirestoreReader` builds the account path from the **verified UID**, rather than accepting a user ID from the request body.
- Firestore requests carry the authenticated user's Firebase ID token, allowing Firebase Security Rules to enforce access controls.
- The context sent to the model contains relevant measurements, category preferences, and goals—not the Firebase token or UID.
- Account-specific API responses use `Cache-Control: no-store`.

Firestore Security Rules are expected to restrict each account to `users/{uid}` and its own collections. Rules and malicious cross-account access attempts should be rechecked during the final security/integration test pass.

### Repository contracts

Android repositories operate for the currently signed-in user and return failures when no authenticated user exists. Normal data queries return `Result` values; a successful empty query is distinct from a failed request. Coroutine cancellation should be rethrown.

- `UserRepository`: user profile loading and saving; a missing profile can be a successful `null` result.
- `WellnessRepository` and feature-specific repositories: manual wellness record operations.
- `StepRepository`: saving and retrieving daily step totals.
- `SummaryRepository`: daily and weekly aggregates for dashboard/progress features.
- `CoachRepository`: sends authenticated wellness questions to the Cloudflare backend and returns AI-generated responses or request failures.

Saving a profile must preserve unrelated existing fields, and its UID must match the authenticated account.

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

Use the collection names defined by `FirestorePaths`. Workout sets are stored inside the workout record. Preferences and daily goal targets are saved on the user profile. Account deletion must handle the user profile and relevant subcollections as implemented by the app's deletion flow.

### IDs, timestamps, and dates

- Manual record creation generates a Firestore document ID; updates and deletes use the existing ID.
- Preserve an existing record's `id` and `createdAt` on updates.
- Multiple manual records may share a calendar date.
- Creation timestamps are Unix epoch milliseconds.
- Store record dates as `yyyy-MM-dd` (for example, `2026-11-22`).
- Record dates follow the user's local calendar. Sleep entries use the date the user woke up; automatic steps use the phone's local day.
- Date-range reads include both start and end dates. Reject invalid calendar dates and inverted ranges.
- Viewing a different date or changing time zones must not rewrite saved record dates.

### Profile and tracking preferences

- `displayName` stores the chosen username; height and weight are optional.
- `wellnessGoals` stores chosen wellness focus areas.
- Numeric goals are editable preferences.
- `selectedCategories` contains identifiers from `TrackingCategories.ALL`; new profiles default to all selected.
- Unselecting a category does not erase its previously saved history.
- Selecting step tracking does not itself grant Android activity permission. The app must request that permission separately.

The AI context uses the profile's selected categories and goals. An explicitly empty category selection stays empty; missing category settings use the app's defaults.

### Units and input validation

Validate values in the interface and repository before saving. Constructor defaults do not replace validation.

| Information | Stored field and rule |
| --- | --- |
| Height | `heightCm`: positive centimeters, or `null` |
| Weight | `weightKg`: positive kilograms, or `null` |
| Calories | `calories`: positive whole-number kilocalories |
| Water | `amountOz`: positive whole-number US fluid ounces |
| Workout weight | `weightLbs`: non-negative pounds, or `null` |
| Walk/run distance | `distanceMiles`: positive miles |
| Walk/run duration | `durationMinutes`: positive whole-number minutes |
| Sleep | `hoursSlept`: greater than 0 and no more than 24 hours |
| Check-in ratings | Whole numbers from 1 through 10, or `null` |
| Steps | Non-negative whole-number daily total |

Calorie, water, and step goals must be positive whole numbers. The sleep goal must be greater than 0 and no more than 24 hours. Fractional sleep is supported (for example, `7.5` hours). A meal name/description cannot be blank; optional `mealTime` uses local 24-hour `HH:mm` format.

### Workouts

- `workoutType` can be `strength`, `walk`, or `run`.
- Each record represents an exercise or a walk/run activity; a collection of exercise records does not necessarily mean separate workout sessions.
- Strength records require an exercise name and at least one set. Each set stores repetitions and optional non-negative weight; ordering matters.
- Walk/run records use manually entered distance and whole-number duration; the set list is empty.
- Records can be reviewed, edited, and deleted through the logging/history workflows.

### Daily check-ins

- Stress: 1 = very low stress, 10 = very high stress.
- Mood: 1 = very low mood, 10 = very good mood.
- Energy: 1 = very low energy, 10 = very high energy.
- At least one rating is needed to save a check-in.
- `null` means *not recorded*; do not substitute zero or a neutral value.
- When editing, preserve ratings that were previously saved but are no longer part of the current selection.
- Summaries and AI context skip missing ratings. When multiple check-ins exist for a day, the context uses the latest one.

### Calculated totals and steps

- Calories and water totals come from saved records, not a separately incremented counter.
- Recalculate affected totals after adding, editing, deleting, or moving a record between dates.
- Steps use the calendar date as both the document ID and `StepTotal.id`; refreshed totals replace the saved day's total.
- A missing steps document is **not** the same as a recorded step count of `0`.

## Dashboard, logs, and progress

### Dashboard (D2)

The dashboard provides a functional overview of calories, hydration, steps, and sleep. It uses the existing data/repository layer. Some visuals and layout details are intentionally deferred until the final UI/UX pass.

### Logs and navigation (D3)

The Logs area provides access to tracking workflows, while the application's bottom navigation connects major destinations. Feature screens are responsible for record entry and detailed history.

### Progress (D4)

The Progress screen is connected to `SummaryRepository` through `ProgressViewModel` and supports:

- Day and week modes with previous/next date navigation.
- Daily goal progress and weekly averages/totals.
- Weekly charts and day-by-day history.
- Refresh, loading, empty, and error states.
- Account-specific data after switching signed-in users.

The PR #72 review included successful debug build and unit tests, compilation of Android instrumentation tests, emulator review of day/week views, hydration refresh checks, and a two-account check. That does **not** mean instrumentation tests were run or every edge case was covered.

Deferred polish items include denser chart/history labels and optionally showing individual mood, stress, and energy ratings in the weekly day-by-day cards. These are not blockers for the completed core D4 workflow.

## AI Coach backend (A1)

### Purpose and behavior

The Worker accepts questions about nutrition, exercise, hydration, sleep, stress, mood, energy, and general wellness. It uses recent user-specific records when relevant and is instructed to:

- Offer **short, practical, general wellness advice** rather than medical diagnoses or prescriptions.
- Avoid inventing missing measurements or treating an unlogged activity as something that definitely did not happen.
- Politely redirect unrelated questions toward wellness topics.
- Avoid claiming access to live news, prices, weather, or other real-time internet data.
- Treat stored wellness content as data, not as instructions.

These model instructions are **not** a formal guarantee of every response. Responses are generated dynamically and can vary.

### Seven-day context

`src/coach-context.ts` builds an inclusive seven-day window ending at the requested date and reads relevant fields for selected categories only:

| Category | Summary behavior |
| --- | --- |
| Meals | Daily calorie sums; seven-day calendar average |
| Hydration | Daily fluid-ounce sums; seven-day calendar average |
| Sleep | Recorded hours; average of days with saved measurements |
| Steps | Saved daily total; average of measured days |
| Workouts | Entry count and available recorded durations |
| Stress, mood, energy | Latest recorded daily check-in rating; average of measured days |

The context also includes the selected category identifiers, up to five shortened wellness goals, daily target values where applicable, date range, and notes describing missing measurements. The context intentionally excludes authentication credentials and personal identifiers.

### API contract

**Base URL:** `https://ai-coach.steep-mountain-2405.workers.dev`

All endpoints except `GET /` require a Firebase ID token from the configured Firebase project:

```http
Authorization: Bearer <FIREBASE_ID_TOKEN>
```

| Method and path | Purpose |
| --- | --- |
| `GET /` | Public Worker availability check (`Hello World!`) |
| `GET /auth/check` | Verify token; return `authenticated` and verified `uid` |
| `GET /coach/context?date=yyyy-MM-dd` | Return seven-day context for verified user |
| `POST /coach/chat` | Return AI-generated reply using verified user's context |

Chat request:

```http
POST /coach/chat
Authorization: Bearer <FIREBASE_ID_TOKEN>
Content-Type: application/json
```

```json
{
  "message": "How can I improve my hydration habits?",
  "date": "2026-11-22"
}
```

- `message`: non-blank string, maximum 500 characters.
- `date`: real calendar date in `yyyy-MM-dd` format; context ends on this date.
- The user ID is **not** supplied by the client; it comes from verified Firebase authentication.

Example success (`200 OK`, illustrative wording):

```json
{
  "reply": "Based on your recorded hydration, try setting a reminder to drink water throughout the day."
}
```

Authentication check success (`200 OK`):

```json
{
  "authenticated": true,
  "uid": "<VERIFIED_USER_UID>"
}
```

Context endpoint success (`200 OK`, abbreviated illustration):

```json
{
  "uid": "<VERIFIED_USER_UID>",
  "context": {
    "startDate": "2026-11-16",
    "endDate": "2026-11-22",
    "selectedCategories": ["hydration", "sleep"],
    "wellnessGoals": [],
    "hasRecordedData": true,
    "categories": {
      "hydration": {
        "unit": "US fl oz",
        "dailyGoal": 64,
        "daysWithMeasurements": 2,
        "average": 8
      }
    }
  }
}
```

The real context includes day-by-day arrays and additional summary/metadata fields. Examples above are not actual account records.

### Errors and reliability

The backend returns `{"error":"..."}` with an appropriate HTTP status when possible:

| Status | Meaning |
| --- | --- |
| `400` | Invalid message, missing/invalid date, or bad request |
| `401` | Missing, expired, or invalid Firebase authentication |
| `403` | Firestore access denied or account/profile mismatch |
| `404` | Route not found |
| `405` | Unsupported method (`Allow` header included) |
| `409` | Missing required profile or conflicting saved records |
| `422` | Too many records in requested range |
| `500` | Unexpected backend error |
| `502` | Invalid saved data or AI response not usable |
| `503` | Firestore or AI service temporarily unavailable |
| `504` | AI response deadline exceeded |

Firestore network requests use a **10-second timeout**. The chat inference path uses a **60-second response deadline**; the `Promise.race` timeout makes the Worker return an understandable `504` if AI inference stalls. This does **not** guarantee that remote inference is canceled or that usage stops.

Example:

```json
{
  "error": "The AI coach took too long to respond. Please try again."
}
```

Android should show a useful message and offer a retry when appropriate. Actual inference latency may vary; repeated first-request delays were observed during manual testing and should be monitored rather than assumed solved by timeout handling.

### A1 verification to date

Manually checked:

- Valid Firebase sign-in and successful authenticated context/chat requests.
- Rejection of missing authentication (`401`).
- Firestore-backed personalized responses from the deployed Worker (`200`).
- Separate test accounts returning different, account-relevant information.
- Appropriate handling of absent logged records.
- Redirection of an unrelated question and resistance to one basic prompt-injection attempt.
- Controlled local timeout returning the intended `504` response.
- TypeScript compilation and successful Cloudflare deployment.

Authenticated requests initiated inside the signed-in Android app have now been verified during A2 testing.

**Still outstanding for complete end-to-end sign-off:** broader account-isolation/security-rule tests and ongoing reliability checks. Successful manual tests are not a substitute for exhaustive security or load testing.

## AI Coach Android interface (A2)

The Android AI Coach connects the signed-in user to the deployed Cloudflare Worker through the existing AI Coach navigation destination.

### Android implementation

- `FirebaseAuthDataSource`: retrieves the signed-in user's Firebase ID token.
- `CoachRepository`: sends authenticated HTTPS requests to the AI backend and handles responses, connection failures, and timeouts.
- `CoachViewModel`: manages conversation messages, input validation, loading, errors, and retry.
- `CoachViewModelFactory`: creates the ViewModel with its required repository.
- `CoachScreen`: displays the conversation, question input, loading indicator, error messages, and retry button.
- `AppContainer` and `AppNavHost`: provide the repository and connect the AI Coach to the existing application navigation.

### Chat behavior

- Questions must not be blank and cannot exceed 500 characters.
- Duplicate submissions are prevented while a request is in progress.
- Failed requests can be retried without duplicating the original user message.
- Responses use the authenticated user's recent wellness records, selected tracking categories, and goals.
- Other application screens remain accessible while the AI Coach is loading or unavailable.
- Chat messages are stored temporarily in the ViewModel and are not saved to Firestore.
- Chat state is cleared when the user logs out or switches accounts.
- Each question is processed independently. Previous conversation messages are not sent to the AI backend.

### A2 verification

Manually checked:

- Successful authenticated AI responses from the Android app.
- Personalized feedback based on saved wellness records.
- Respect for enabled and disabled tracking categories.
- Account switching and conversation reset.
- Handling of empty and whitespace-only questions.
- Loading indicators and duplicate submission prevention.
- Connection failure handling and successful retry after restoring connectivity.
- Navigation to other application screens while the AI Coach is loading or unavailable.

Automated tests in `CoachViewModelTest.kt` cover:

- Empty, whitespace-only, and oversized questions.
- Successful responses and conversation updates.
- Loading state.
- Duplicate submission prevention.
- Failed requests and successful retry without duplicate messages.

Run the AI Coach unit tests with:

```bash
./gradlew :app:testDebugUnitTest --tests "*CoachViewModelTest"
```

All eight AI Coach unit tests and the complete Android unit test suite passed on November 25, 2026. These ViewModel tests use a fake repository and do not contact Firebase or Cloudflare.

### Remaining work

Reminders, final cross-feature integration testing, and a dedicated UI/UX polish pass remain outstanding. Short-term AI conversation memory is a possible future enhancement and is not part of the current A2 implementation.

## Working on the project

- Create and review feature branches/PRs; coordinate changes to shared navigation, dependency containers, and Firebase data sources.
- Keep manual acceptance checks and automated tests distinct in PR descriptions.
- Do not commit `node_modules/`, `.wrangler/`, credentials, tokens, or local environment files.
- Verify sign-in, sign-out, and account switching when adding user-specific features.
- Prioritize correct, maintainable functionality before expanding scope or making purely cosmetic changes.

---


## Reminder preferences

Open Profile > Reminders to enable hydration, daily check-in, or workout reminders
and choose times. Each account saves its choices in `users/{uid}.reminderPreferences`:
`hydration`, `checkIn`, and `workout` each contain `enabled` (Boolean), `hour` (0–23),
and `minute` (0–59). Missing settings default to disabled at 10:00, 20:00, and 17:00.
Times follow the phone's local time zone, including daylight saving changes.
Notification delivery is connected separately.
