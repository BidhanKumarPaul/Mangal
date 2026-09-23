package com.bkpit.mangal.data.db

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

/**
 * The SQLCipher passphrase itself must not sit in plaintext SharedPreferences
 * — that would defeat the point of encrypting the DB. We generate a random
 * 256-bit key once and store IT inside EncryptedSharedPreferences, which is
 * backed by a hardware Keystore-protected master key.
 */
object DatabasePassphrase {
    private const val PREFS_NAME = "mangal_secure_prefs"
    private const val KEY_NAME = "sqlcipher_passphrase"

    fun getOrCreate(context: Context): ByteArray {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val existing = prefs.getString(KEY_NAME, null)
        if (existing != null) {
            return existing.split(",").map { it.toByte() }.toByteArray()
        }

        val newKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
        prefs.edit().putString(KEY_NAME, newKey.joinToString(",")).apply()
        return newKey
    }
}
