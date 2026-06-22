package com.smartglasses.demo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.smartglasses.demo.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * UserDao - Data Access Object for User entity
 * Provides database operations for user authentication
 */
@Dao
interface UserDao {

    /**
     * Insert a new user
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    /**
     * Update an existing user record
     */
    @Update
    suspend fun updateUser(user: UserEntity)

    /**
     * Get user by username only
     */
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    /**
     * Get user by ID
     */
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    /**
     * Get all users as Flow (for observing changes)
     */
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    /**
     * Delete all users
     */
    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()

    /**
     * Count users
     */
    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int
}
