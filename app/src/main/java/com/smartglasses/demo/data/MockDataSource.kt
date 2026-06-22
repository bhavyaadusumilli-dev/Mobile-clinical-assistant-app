package com.smartglasses.demo.data

import kotlinx.coroutines.delay

/**
 * Simulates a remote data source with a realistic network delay.
 */
object MockDataSource {

    private val patients = listOf(
        Patient(
            name = "James Harrington",
            age = 52,
            gender = "Male",
            bloodType = "O+",
            diagnosis = "Type 2 Diabetes – Stable",
            heartRate = "78 bpm",
            bloodPressure = "124 / 82 mmHg",
            temperature = "36.8 °C"
        ),
        Patient(
            name = "Sophia Caldwell",
            age = 45,
            gender = "Female",
            bloodType = "A-",
            diagnosis = "Hypertension – Under Observation",
            heartRate = "92 bpm",
            bloodPressure = "138 / 90 mmHg",
            temperature = "37.1 °C"
        ),
        Patient(
            name = "Marcus Reynolds",
            age = 67,
            gender = "Male",
            bloodType = "B+",
            diagnosis = "Chronic Kidney Disease – Stage 3",
            heartRate = "65 bpm",
            bloodPressure = "118 / 76 mmHg",
            temperature = "36.5 °C"
        )
    )

    /**
     * Fetches a patient assigned to the given [doctorUsername].
     * Simulates 800–1200 ms network latency.
     */
    suspend fun fetchPatient(doctorUsername: String): Patient {
        delay((800..1200).random().toLong())
        // Simple deterministic assignment based on username length
        val index = doctorUsername.length % patients.size
        return patients[index]
    }
}
