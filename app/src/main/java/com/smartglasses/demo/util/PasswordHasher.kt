package com.smartglasses.demo.util

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Simple PBKDF2 password hasher/validator.
 * Stored format: iterations:base64(salt):base64(hash)
 */
object PasswordHasher {
    private const val ITERATIONS = 10000
    private const val KEY_LENGTH = 256 // bits

    fun hash(password: String): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = skf.generateSecret(spec).encoded
        val saltB64 = Base64.getEncoder().encodeToString(salt)
        val hashB64 = Base64.getEncoder().encodeToString(hash)
        return "${ITERATIONS}:$saltB64:$hashB64"
    }

    fun isLegacy(stored: String): Boolean {
        val parts = stored.split(":")
        return parts.size != 3
    }

    fun verify(password: String, stored: String): Boolean {
        return try {
            if (isLegacy(stored)) {
                password == stored
            } else {
                val parts = stored.split(":")
                val iterations = parts[0].toInt()
                val salt = Base64.getDecoder().decode(parts[1])
                val hash = Base64.getDecoder().decode(parts[2])
                val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH)
                val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                val testHash = skf.generateSecret(spec).encoded
                if (testHash.size != hash.size) return false
                var diff = 0
                for (i in hash.indices) {
                    diff = diff or (hash[i].toInt() xor testHash[i].toInt())
                }
                diff == 0
            }
        } catch (e: Exception) {
            false
        }
    }
}
