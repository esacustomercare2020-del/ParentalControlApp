package com.example.parentalcontrolapp

import android.content.Context

object SecurityManager {
    private const val PREFS_NAME = "parental_control_prefs"
    private const val KEY_PIN = "parent_pin"
    private const val DEFAULT_PIN = "1234"

    @Volatile
    var isParentAuthenticated: Boolean = false
        private set

    fun authenticate(pin: String, context: Context): Boolean {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedPin = sharedPrefs.getString(KEY_PIN, DEFAULT_PIN) ?: DEFAULT_PIN
        val success = savedPin == pin
        if (success) {
            isParentAuthenticated = true
        }
        return success
    }

    fun lock() {
        isParentAuthenticated = false
    }

    fun setPin(currentPin: String, newPin: String, context: Context): Boolean {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedPin = sharedPrefs.getString(KEY_PIN, DEFAULT_PIN) ?: DEFAULT_PIN
        if (savedPin == currentPin) {
            sharedPrefs.edit().putString(KEY_PIN, newPin).apply()
            return true
        }
        return false
    }
}
