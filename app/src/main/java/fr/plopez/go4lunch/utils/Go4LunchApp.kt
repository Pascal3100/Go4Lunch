package fr.plopez.go4lunch.utils

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject


@HiltAndroidApp
class Go4LunchApp : Application(), Configuration.Provider {

    companion object {
        lateinit var instance: Application
            private set
    }

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    init {
        instance = this
    }

    override fun getWorkManagerConfiguration() =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}