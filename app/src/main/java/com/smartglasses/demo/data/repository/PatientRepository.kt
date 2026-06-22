package com.smartglasses.demo.data.repository

import com.smartglasses.demo.data.MockDataSource
import com.smartglasses.demo.data.Patient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * PatientRepository - Manages patient data operations
 * Single source of truth for patient information
 */
class PatientRepository {

    /**
     * Patient result sealed class
     */
    sealed class PatientResult {
        data class Success(val patient: Patient) : PatientResult()
        data class Error(val message: String) : PatientResult()
    }

    /**
     * Get patient data for a specific doctor
     * Uses MockDataSource to simulate API call
     */
    suspend fun getPatientForDoctor(doctorUsername: String): PatientResult {
        return withContext(Dispatchers.IO) {
            try {
                val patient = MockDataSource.fetchPatient(doctorUsername)
                PatientResult.Success(patient)
            } catch (e: Exception) {
                PatientResult.Error("Failed to load patient data: ${e.message}")
            }
        }
    }

    /**
     * Get patient by ID (for future real implementation)
     */
    suspend fun getPatientById(patientId: String): PatientResult {
        return withContext(Dispatchers.IO) {
            try {
                // For demo, fetch from MockDataSource
                val patient = MockDataSource.fetchPatient(patientId)
                PatientResult.Success(patient)
            } catch (e: Exception) {
                PatientResult.Error("Patient not found: ${e.message}")
            }
        }
    }
}
