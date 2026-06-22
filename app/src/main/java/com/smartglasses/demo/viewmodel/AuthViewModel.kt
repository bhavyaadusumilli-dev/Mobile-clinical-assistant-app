package com.smartglasses.demo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartglasses.demo.BuildConfig
import com.smartglasses.demo.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * AuthUiState - Immutable state class for Login Screen
 */
data class AuthUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false,
    val loggedInUsername: String = ""
)

/**
 * AuthViewModel - Manages authentication state and operations
 * Follows state hoisting pattern with unidirectional data flow
 */
class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /**
     * Initialize and prepopulate demo users on first launch
     */
    init {
        // Prepopulate demo users only in debug builds to avoid seeding plaintext
        // credentials in production or public releases.
        if (BuildConfig.DEBUG) {
            viewModelScope.launch {
                authRepository.prepopulateDemoUsers()
            }
        }
    }

    /**
     * Handle username input change
     */
    fun onUsernameChange(username: String) {
        _uiState.value = _uiState.value.copy(
            username = username,
            errorMessage = null
        )
    }

    /**
     * Handle password input change
     */
    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(
            password = password,
            errorMessage = null
        )
    }

    /**
     * Handle login button click
     */
    fun onLoginClick() {
        val currentState = _uiState.value

        // Validate input
        if (currentState.username.isBlank() || currentState.password.isBlank()) {
            _uiState.value = currentState.copy(
                errorMessage = "Username and password cannot be empty"
            )
            return
        }

        // Start loading
        _uiState.value = currentState.copy(isLoading = true, errorMessage = null)

        // Perform login
        viewModelScope.launch {
            val result = authRepository.login(
                currentState.username,
                currentState.password
            )

            when (result) {
                is AuthRepository.LoginResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        loggedInUsername = result.username,
                        errorMessage = null
                    )
                }
                is AuthRepository.LoginResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    /**
     * Handle logout
     */
    fun onLogout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = AuthUiState() // Reset to initial state
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Check if user is already logged in (for auto-navigation)
     */
    fun checkSession(onLoggedIn: (String) -> Unit) {
        viewModelScope.launch {
            if (authRepository.isLoggedIn()) {
                val username = authRepository.getCurrentUsername()
                _uiState.value = _uiState.value.copy(
                    isLoggedIn = true,
                    loggedInUsername = username
                )
                onLoggedIn(username)
            }
        }
    }

    /**
     * Factory for creating AuthViewModel with dependencies
     */
    class Factory(
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                return AuthViewModel(authRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
