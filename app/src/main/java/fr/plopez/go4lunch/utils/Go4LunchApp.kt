package fr.plopez.go4lunch.utils

import android.app.Application
import dagger.hilt.android.HiltAndroidApp


@HiltAndroidApp
class Go4LunchApp : Application() {

    companion object {
        lateinit var instance: Application
            private set
    }

    init {
        instance = this
    }
}