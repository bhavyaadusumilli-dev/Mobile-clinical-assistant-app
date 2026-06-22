package com.smartglasses.demo.data.repository

import com.smartglasses.demo.data.MockDataSource
import com.smartglasses.demo.data.Patient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * ScanRepository - Manages patient scanning operations
 * Handles patient lookup after scan completion
 */
class ScanRepository {

    /**
     * Scan result sealed class
     */
    sealed class ScanResult {
        data class Success(val patient: Patient) : ScanResult()
        data class Error(val message: String) : ScanResult()
    }

    /**
     * Simulate scanning a patient
     * In a real app, this would use camera/barcode scanning
     * For demo, we simulate the scan delay and return mock data
     */
    suspend fun scanPatient(doctorUsername: String): ScanResult {
        return withContext(Dispatchers.IO) {
            try {
                // Simulate scanning delay (already handled in MockDataSource)
                val patient = MockDataSource.fetchPatient(doctorUsername)
                ScanResult.Success(patient)
            } catch (e: Exception) {
                ScanResult.Error("Failed to scan patient: ${e.message}")
            }
        }
    }

    /**
     * Lookup patient by ID (for future real implementation)
     */
    suspend fun lookupPatientById(patientId: String): ScanResult {
        return withContext(Dispatchers.IO) {
            // Simulate network/database lookup
            delay(500)
            
            // For demo, return mock data
            // In production, this would query a real database/API
            try {
                val patient = MockDataSource.fetchPatient(patientId)
                ScanResult.Success(patient)
            } catch (e: Exception) {
                ScanResult.Error("Patient not found")
            }
        }
    }
}
