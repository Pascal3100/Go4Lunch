package fr.plopez.go4lunch.utils

import android.app.Application


class App : Application() {

    override fun onCreate() {
        super.onCreate()
        application = this
    }

    companion object {
        var application: Application? = null
            private set
    }
}