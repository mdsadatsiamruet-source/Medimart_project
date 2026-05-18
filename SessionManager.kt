package com.example.medimart2

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)

    fun saveUsername(username: String) {
        val editor = prefs.edit()
        editor.putString("username", username)
        editor.apply()
    }

    fun getUsername(): String? {
        return prefs.getString("username", null)
    }

    fun logout() {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
    }
}