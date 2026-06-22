package com.smartglasses.demo.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * User entity for Room database
 * Stores doctor credentials for authentication
 */
@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)]
)
data class UserEntity(
    @PrimaryKey
    val id: String,
    val username: String,
    val passwordHash: String, // PBKDF2 hashed password (format: iterations:salt:hash)
    val fullName: String,
    val createdAt: Long = System.currentTimeMillis()
)
