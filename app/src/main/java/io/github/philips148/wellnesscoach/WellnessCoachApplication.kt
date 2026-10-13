package io.github.philips148.wellnesscoach

import android.app.Application
import io.github.philips148.wellnesscoach.di.AppContainer

class WellnessCoachApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()

        // Create the shared objects when the application starts.
        appContainer = AppContainer()
    }
}