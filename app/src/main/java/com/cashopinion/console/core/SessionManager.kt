package com.cashopinion.console.core

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        Constants.PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean(Constants.KEY_THEME_DARK, true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _isSessionExpired = MutableStateFlow(false)
    val isSessionExpired: StateFlow<Boolean> = _isSessionExpired.asStateFlow()

    var isDarkTheme: Boolean
        get() = prefs.getBoolean(Constants.KEY_THEME_DARK, true)
        set(value) {
            prefs.edit().putBoolean(Constants.KEY_THEME_DARK, value).apply()
            _isDarkMode.value = value
        }

    var serverUrl: String
        get() = prefs.getString(Constants.KEY_SERVER_URL, Constants.DEFAULT_SERVER_URL) ?: Constants.DEFAULT_SERVER_URL
        set(value) = prefs.edit().putString(Constants.KEY_SERVER_URL, value).apply()

    var lastActiveTime: Long
        get() = prefs.getLong(Constants.KEY_LAST_ACTIVE_TIME, System.currentTimeMillis())
        set(value) = prefs.edit().putLong(Constants.KEY_LAST_ACTIVE_TIME, value).apply()

    fun recordUserActivity() {
        lastActiveTime = System.currentTimeMillis()
        if (_isSessionExpired.value) {
            _isSessionExpired.value = false
        }
    }

    fun checkInactivity(): Boolean {
        val elapsed = System.currentTimeMillis() - lastActiveTime
        if (elapsed > Constants.INACTIVITY_TIMEOUT_MS) {
            _isSessionExpired.value = true
            return true
        }
        return false
    }

    fun clearSession() {
        lastActiveTime = 0L
        _isSessionExpired.value = false
    }
}
