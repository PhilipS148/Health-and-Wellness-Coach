package io.github.philips148.wellnesscoach

import android.app.Application
import kotlinx.coroutines.launch
import io.github.philips148.wellnesscoach.di.AppContainer

class WellnessCoachApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()

        // Create shared objects and provide the application's context.
        // This avoids keeping a reference to an individual screen.
        appContainer = AppContainer(context = this)
        com.google.firebase.auth.FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val uid = auth.currentUser?.uid
            io.github.philips148.wellnesscoach.reminders.ReminderDelivery(this).onAccountChanged(uid)
            refreshReminderPreferences()
        }
    }

    private val reminderScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main.immediate)
    private var reminderRefresh: kotlinx.coroutines.Job? = null

    fun refreshReminderPreferences() {
        reminderRefresh?.cancel()
        val uid = appContainer.authRepository.getCurrentUserId() ?: return
        reminderRefresh = reminderScope.launch {
            appContainer.reminderPreferencesRepository.load(uid)
        }
    }
}
