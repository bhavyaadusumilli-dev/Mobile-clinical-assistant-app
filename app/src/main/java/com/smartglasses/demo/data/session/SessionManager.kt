package com.smartglasses.demo.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * SessionManager - Manages user session using DataStore
 * Provides persistent storage for login state and user information
 */
class SessionManager(private val context: Context) {

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session_prefs")
        
        private val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_USERNAME = stringPreferencesKey("username")
    }

    /**
     * Session data class representing the current session state
     */
    data class Session(
        val isLoggedIn: Boolean,
        val userId: String,
        val username: String
    )

    /**
     * Flow of session state - observes changes automatically
     */
    val sessionFlow: Flow<Session> = context.dataStore.data.map { preferences ->
        Session(
            isLoggedIn = preferences[KEY_IS_LOGGED_IN] ?: false,
            userId = preferences[KEY_USER_ID] ?: "",
            username = preferences[KEY_USERNAME] ?: ""
        )
    }

    /**
     * Save login session
     */
    suspend fun saveSession(userId: String, username: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_LOGGED_IN] = true
            preferences[KEY_USER_ID] = userId
            preferences[KEY_USERNAME] = username
        }
    }

    /**
     * Clear session (logout)
     */
    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_LOGGED_IN] = false
            preferences[KEY_USER_ID] = ""
            preferences[KEY_USERNAME] = ""
        }
    }

    /**
     * Check if user is currently logged in
     */
    suspend fun isLoggedIn(): Boolean {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_IS_LOGGED_IN] ?: false
        }.map { it }.toString().toBoolean()
    }

    /**
     * Get current username
     */
    suspend fun getCurrentUsername(): String {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_USERNAME] ?: ""
        }.toString()
    }
}
