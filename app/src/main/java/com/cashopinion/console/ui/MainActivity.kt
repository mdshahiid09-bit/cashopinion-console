package com.cashopinion.console.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.cashopinion.console.core.ConsoleApp
import com.cashopinion.console.ui.screens.auth.LoginScreen
import com.cashopinion.console.ui.theme.CashTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val container by lazy {
        (application as ConsoleApp).container
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            while (isActive) {
                delay(30_000)
                if (container.authRepo.currentUser != null) {
                    if (container.sessionManager.checkInactivity()) {
                        container.authRepo.signOut()
                    }
                }
            }
        }

        setContent {
            val isDarkMode by container.sessionManager.isDarkMode.collectAsState()
            val isSessionExpired by container.sessionManager.isSessionExpired.collectAsState()

            var currentUser by remember {
                mutableStateOf(container.authRepo.currentUser)
            }

            LaunchedEffect(isSessionExpired) {
                if (isSessionExpired) {
                    currentUser = null
                }
            }

            CashTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CashTheme.colors.background
                ) {
                    Crossfade(
                        targetState = currentUser != null && !isSessionExpired,
                        label = "authTransition"
                    ) { isAuthenticated ->
                        if (isAuthenticated) {
                            MainAppScreen(
                                container = container,
                                onSignOut = {
                                    container.authRepo.signOut()
                                    container.sessionManager.clearSession()
                                    currentUser = null
                                }
                            )
                        } else {
                            LoginScreen(
                                authRepo = container.authRepo,
                                onLoginSuccess = {
                                    container.sessionManager.recordUserActivity()
                                    currentUser = container.authRepo.currentUser
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (container.authRepo.currentUser != null) {
            if (container.sessionManager.checkInactivity()) {
                container.authRepo.signOut()
            } else {
                container.sessionManager.recordUserActivity()
            }
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (container.authRepo.currentUser != null) {
            container.sessionManager.recordUserActivity()
        }
    }
}
