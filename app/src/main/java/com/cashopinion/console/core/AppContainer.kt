package com.cashopinion.console.core

import android.content.Context
import com.cashopinion.console.data.repo.AuthRepo
import com.cashopinion.console.data.repo.ConsoleRepo
import com.cashopinion.console.data.repo.ServerClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AppContainer(val context: Context) {
    val sessionManager: SessionManager by lazy {
        SessionManager(context)
    }

    val firebaseAuth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    val firebaseDatabase: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance().apply {
            try {
                setPersistenceEnabled(true)
            } catch (_: Exception) {}
        }
    }

    val serverClient: ServerClient by lazy {
        ServerClient(sessionManager, firebaseAuth)
    }

    val authRepo: AuthRepo by lazy {
        AuthRepo(firebaseAuth, firebaseDatabase)
    }

    val consoleRepo: ConsoleRepo by lazy {
        ConsoleRepo(firebaseDatabase, firebaseAuth, serverClient)
    }
}
