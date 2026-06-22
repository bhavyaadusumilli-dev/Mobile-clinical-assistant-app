package com.smartglasses.demo.data

/**
 * Represents a single patient's profile and vitals.
 */
data class Patient(
    val name: String,
    val age: Int,
    val gender: String,
    val bloodType: String,
    val diagnosis: String,
    val heartRate: String,
    val bloodPressure: String,
    val temperature: String
)
