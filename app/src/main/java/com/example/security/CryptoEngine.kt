package com.example.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Developer-level AES-256-GCM encryption engine backed by the Android Keystore.
 * Stores all scanned document pages and thumbnails encrypted at rest.
 */
class CryptoEngine(private val keyProvider: () -> SecretKey) {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "anamol_scanner_aes256_vault_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_BITS = 128

        fun createAndroidKeystoreEngine(): CryptoEngine {
            return CryptoEngine { getOrCreateKeystoreKey() }
        }

        fun createWithRawKey(rawKey32Bytes: ByteArray): CryptoEngine {
            require(rawKey32Bytes.size == 32) { "AES-256 requires a 32-byte key" }
            val spec = SecretKeySpec(rawKey32Bytes, "AES")
            return CryptoEngine { spec }
        }

        @Synchronized
        private fun getOrCreateKeystoreKey(): SecretKey {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
            if (existing != null) return existing

            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
            keyGenerator.init(spec)
            return keyGenerator.generateKey()
        }
    }

    fun encryptBytes(plainData: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val secretKey = keyProvider()
        // Note: AndroidKeyStore generates its own random IV when randomizedEncryptionRequired = true;
        // raw SecretKeySpec uses an explicit SecureRandom IV or JCE default IV.
        if (secretKey is SecretKeySpec) {
            val iv = ByteArray(GCM_IV_LENGTH)
            SecureRandom().nextBytes(iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            val encrypted = cipher.doFinal(plainData)
            return ByteArray(GCM_IV_LENGTH + encrypted.size).apply {
                System.arraycopy(iv, 0, this, 0, GCM_IV_LENGTH)
                System.arraycopy(encrypted, 0, this, GCM_IV_LENGTH, encrypted.size)
            }
        } else {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encrypted = cipher.doFinal(plainData)
            return ByteArray(GCM_IV_LENGTH + encrypted.size).apply {
                System.arraycopy(iv, 0, this, 0, GCM_IV_LENGTH)
                System.arraycopy(encrypted, 0, this, GCM_IV_LENGTH, encrypted.size)
            }
        }
    }

    fun decryptBytes(payload: ByteArray): ByteArray {
        require(payload.size > GCM_IV_LENGTH) { "Invalid encrypted payload size" }
        val iv = payload.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = payload.copyOfRange(GCM_IV_LENGTH, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, keyProvider(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    fun encryptToFile(targetFile: File, plainData: ByteArray) {
        targetFile.parentFile?.mkdirs()
        val encrypted = encryptBytes(plainData)
        targetFile.writeBytes(encrypted)
    }

    fun decryptFromFile(sourceFile: File): ByteArray {
        val payload = sourceFile.readBytes()
        return decryptBytes(payload)
    }

    fun encryptString(plainText: String): String {
        val encrypted = encryptBytes(plainText.toByteArray(Charsets.UTF_8))
        val hexChars = CharArray(encrypted.size * 2)
        val digits = "0123456789abcdef"
        for (i in encrypted.indices) {
            val v = encrypted[i].toInt() and 0xFF
            hexChars[i * 2] = digits[v ushr 4]
            hexChars[i * 2 + 1] = digits[v and 0x0F]
        }
        return "AESGCM:" + String(hexChars)
    }

    fun decryptString(encoded: String): String {
        if (!encoded.startsWith("AESGCM:")) return encoded
        val hex = encoded.removePrefix("AESGCM:")
        if (hex.length % 2 != 0) return encoded
        return try {
            val bytes = ByteArray(hex.length / 2)
            for (i in bytes.indices) {
                val hi = hex[i * 2].digitToInt(16)
                val lo = hex[i * 2 + 1].digitToInt(16)
                bytes[i] = ((hi shl 4) or lo).toByte()
            }
            String(decryptBytes(bytes), Charsets.UTF_8)
        } catch (_: Exception) {
            encoded
        }
    }
}
