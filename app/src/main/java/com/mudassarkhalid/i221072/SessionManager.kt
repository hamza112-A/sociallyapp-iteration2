package com.mudassarkhalid.i221072

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PROFILE = "user_profile"
    }

    fun saveSession(userId: String, userName: String, userEmail: String, userProfile: String) {
        prefs.edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USER_NAME, userName)
            .putString(KEY_USER_EMAIL, userEmail)
            .putString(KEY_USER_PROFILE, userProfile)
            .apply()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun isSessionActive(): Boolean {
        return prefs.getString(KEY_USER_ID, null) != null
    }

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)
    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)
    fun getUserProfile(): String? = prefs.getString(KEY_USER_PROFILE, null)
}

