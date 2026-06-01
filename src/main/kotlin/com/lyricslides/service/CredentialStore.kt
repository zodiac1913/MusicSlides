package com.lyricslides.service

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Stored credentials for external services.
 * SECURITY: Encrypted at rest using AES with a machine-specific key.
 * Stored ONLY in ~/.lyricslides/credentials.enc — never in version control.
 */
@Serializable
data class StoredCredentials(
    val ccliUsername: String = "",
    val ccliPassword: String = "",
    val ccliLicenseNumber: String = ""
)

/**
 * Manages encrypted storage of credentials in the user's home directory.
 * Uses AES-256 encryption with a key derived from machine-specific identifiers.
 * This means the encrypted file is useless if copied to another machine.
 */
class CredentialStore {

    private val storageDir = File(System.getProperty("user.home"), ".lyricslides")
    private val storageFile = File(storageDir, "credentials.enc")
    private val json = Json { ignoreUnknownKeys = true }

    private var _credentials: StoredCredentials = load()

    val credentials: StoredCredentials get() = _credentials

    val hasCcliCredentials: Boolean
        get() = _credentials.ccliUsername.isNotBlank() && _credentials.ccliPassword.isNotBlank()

    /**
     * Generate a machine-specific encryption key.
     * Derived from: username + home dir + OS name (stable across reboots, unique per machine/user).
     */
    private fun deriveKey(): SecretKeySpec {
        val machineId = listOf(
            System.getProperty("user.name", ""),
            System.getProperty("user.home", ""),
            System.getProperty("os.name", ""),
            System.getProperty("os.arch", ""),
            // Add machine hostname for extra uniqueness
            try { java.net.InetAddress.getLocalHost().hostName } catch (_: Exception) { "unknown" }
        ).joinToString("|")

        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(machineId.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun encrypt(plainText: String): String {
        val key = deriveKey()
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        // Store IV + encrypted data together, base64 encoded
        val combined = iv + encrypted
        return Base64.getEncoder().encodeToString(combined)
    }

    private fun decrypt(encryptedText: String): String {
        val key = deriveKey()
        val combined = Base64.getDecoder().decode(encryptedText)
        // First 16 bytes are the IV
        val iv = combined.copyOfRange(0, 16)
        val encrypted = combined.copyOfRange(16, combined.size)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
        val decrypted = cipher.doFinal(encrypted)
        return String(decrypted, Charsets.UTF_8)
    }

    private fun load(): StoredCredentials {
        return try {
            if (!storageFile.exists()) return StoredCredentials()
            val encryptedContent = storageFile.readText().trim()
            if (encryptedContent.isBlank()) return StoredCredentials()
            val decrypted = decrypt(encryptedContent)
            json.decodeFromString<StoredCredentials>(decrypted)
        } catch (_: Exception) {
            // Decryption failed (wrong machine, corrupted file, etc.)
            StoredCredentials()
        }
    }

    fun save(credentials: StoredCredentials) {
        _credentials = credentials
        try {
            storageDir.mkdirs()
            val jsonStr = json.encodeToString(credentials)
            val encrypted = encrypt(jsonStr)
            storageFile.writeText(encrypted)
            // Set file permissions to owner-only (Unix systems)
            try {
                storageFile.setReadable(false, false)
                storageFile.setReadable(true, true)
                storageFile.setWritable(false, false)
                storageFile.setWritable(true, true)
            } catch (_: Exception) {}
        } catch (_: Exception) {}
    }

    fun clearCredentials() {
        save(StoredCredentials())
        // Also delete the file entirely
        try { storageFile.delete() } catch (_: Exception) {}
    }
}
