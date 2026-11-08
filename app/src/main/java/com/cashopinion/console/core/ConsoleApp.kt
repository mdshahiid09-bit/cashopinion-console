package com.cashopinion.console.core

import android.app.Application
import com.google.firebase.FirebaseApp

class ConsoleApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
        } catch (_: Exception) {}
        container = AppContainer(this)
    }
}
