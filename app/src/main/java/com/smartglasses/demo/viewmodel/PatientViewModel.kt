package com.smartglasses.demo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartglasses.demo.data.Patient
import com.smartglasses.demo.data.repository.PatientRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * PatientUiState - Immutable state class for Dashboard Screen
 */
data class PatientUiState(
    val isLoading: Boolean = false,
    val patient: Patient? = null,
    val errorMessage: String? = null
)

/**
 * PatientViewModel - Manages patient data display
 * Follows state hoisting pattern with unidirectional data flow
 */
class PatientViewModel(
    private val patientRepository: PatientRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PatientUiState())
    val uiState: StateFlow<PatientUiState> = _uiState.asStateFlow()

    /**
     * Load patient data for the given doctor
     */
    fun loadPatient(doctorUsername: String) {
        _uiState.value = PatientUiState(isLoading = true)

        viewModelScope.launch {
            val result = patientRepository.getPatientForDoctor(doctorUsername)

            when (result) {
                is PatientRepository.PatientResult.Success -> {
                    _uiState.value = PatientUiState(
                        isLoading = false,
                        patient = result.patient
                    )
                }
                is PatientRepository.PatientResult.Error -> {
                    _uiState.value = PatientUiState(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    /**
     * Retry loading patient data
     */
    fun retry(doctorUsername: String) {
        loadPatient(doctorUsername)
    }

    /**
     * Reset state (on logout)
     */
    fun reset() {
        _uiState.value = PatientUiState()
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Factory for creating PatientViewModel with dependencies
     */
    class Factory(
        private val patientRepository: PatientRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PatientViewModel::class.java)) {
                return PatientViewModel(patientRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
