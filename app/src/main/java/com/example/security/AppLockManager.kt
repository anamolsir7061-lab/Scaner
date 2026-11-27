package com.example.security

import android.content.Context
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Handles local PIN hashing (salted SHA-256) and device BiometricPrompt authentication.
 */
class AppLockManager(context: Context) {

    private val prefs = context.getSharedPreferences("anamol_lock_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_LOCK_ENABLED = "lock_enabled"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    }

    fun isLockEnabled(): Boolean = prefs.getBoolean(KEY_LOCK_ENABLED, false) && hasPinConfigured()

    fun setLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCK_ENABLED, enabled).apply()
    }

    fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun hasPinConfigured(): Boolean = !prefs.getString(KEY_PIN_HASH, null).isNullOrEmpty()

    fun setPin(pin: String) {
        require(pin.length == 4 && pin.all { it.isDigit() }) { "PIN must be 4 digits" }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hashPin(pin, salt)
        prefs.edit()
            .putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_PIN_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .putBoolean(KEY_LOCK_ENABLED, true)
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val saltStr = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val hashStr = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val salt = Base64.decode(saltStr, Base64.NO_WRAP)
        val expected = Base64.decode(hashStr, Base64.NO_WRAP)
        val actual = hashPin(pin, salt)
        return MessageDigest.isEqual(expected, actual)
    }

    fun canUseBiometrics(context: Context): Boolean {
        val manager = BiometricManager.from(context)
        val status = manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        return status == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negativeText: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        errorCode != BiometricPrompt.ERROR_USER_CANCELED
                    ) {
                        onError(errString.toString())
                    }
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeText)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .build()

        prompt.authenticate(promptInfo)
    }

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        return digest.digest(pin.toByteArray(Charsets.UTF_8))
    }
}
