package io.github.philips148.wellnesscoach

import android.app.Application
import io.github.philips148.wellnesscoach.di.AppContainer

class WellnessCoachApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()

        // Create shared objects and provide the application's context.
        // This avoids keeping a reference to an individual screen.
        appContainer = AppContainer(context = this)
    }
}