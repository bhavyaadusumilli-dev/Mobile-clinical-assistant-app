package com.smartglasses.demo.data.repository

import com.smartglasses.demo.data.local.dao.UserDao
import com.smartglasses.demo.data.local.entity.UserEntity
import com.smartglasses.demo.data.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * AuthRepository - Single source of truth for authentication
 * Manages login/logout and user data operations
 */
class AuthRepository(
    private val userDao: UserDao,
    private val sessionManager: SessionManager
) {
    /**
     * Login result sealed class
     */
    sealed class LoginResult {
        data class Success(val userId: String, val username: String) : LoginResult()
        data class Error(val message: String) : LoginResult()
    }

    /**
     * Session flow from DataStore
     */
    val sessionFlow: Flow<SessionManager.Session> = sessionManager.sessionFlow

    /**
     * Attempt login with credentials
     */
    suspend fun login(username: String, password: String): LoginResult {
        return withContext(Dispatchers.IO) {
            // Validate input
            if (username.isBlank() || password.isBlank()) {
                return@withContext LoginResult.Error("Username and password cannot be empty")
            }

            // Check credentials in database
            val user = userDao.getUserByCredentials(username.trim(), password.trim())
                ?: return@withContext LoginResult.Error("Invalid username or password")

            // Save session
            sessionManager.saveSession(user.id, user.username)

            LoginResult.Success(user.id, user.username)
        }
    }

    /**
     * Logout current user
     */
    suspend fun logout() {
        sessionManager.clearSession()
    }

    /**
     * Check if user is logged in
     */
    suspend fun isLoggedIn(): Boolean {
        return sessionManager.sessionFlow.first().isLoggedIn
    }

    /**
     * Get current username
     */
    suspend fun getCurrentUsername(): String {
        return sessionManager.sessionFlow.first().username
    }

    /**
     * Prepopulate database with demo users
     * Call this on app first launch
     */
    suspend fun prepopulateDemoUsers() {
        withContext(Dispatchers.IO) {
            val count = userDao.getUserCount()
            if (count == 0) {
                // Add demo doctor
                val demoUser = UserEntity(
                    id = UUID.randomUUID().toString(),
                    username = "drsmith",
                    password = "password123",
                    fullName = "Dr. John Smith"
                )
                userDao.insertUser(demoUser)

                // Add another demo doctor
                val demoUser2 = UserEntity(
                    id = UUID.randomUUID().toString(),
                    username = "drjones",
                    password = "password123",
                    fullName = "Dr. Sarah Jones"
                )
                userDao.insertUser(demoUser2)
            }
        }
    }

    /**
     * Register a new user (for future extension)
     */
    suspend fun register(username: String, password: String, fullName: String): LoginResult {
        return withContext(Dispatchers.IO) {
            if (username.isBlank() || password.isBlank()) {
                return@withContext LoginResult.Error("All fields are required")
            }

            // Check if user already exists
            val existingUser = userDao.getUserByUsername(username.trim())
            if (existingUser != null) {
                return@withContext LoginResult.Error("Username already exists")
            }

            // Create new user
            val newUser = UserEntity(
                id = UUID.randomUUID().toString(),
                username = username.trim(),
                password = password.trim(),
                fullName = fullName.trim()
            )
            userDao.insertUser(newUser)

            // Auto-login after registration
            sessionManager.saveSession(newUser.id, newUser.username)

            LoginResult.Success(newUser.id, newUser.username)
        }
    }
}
