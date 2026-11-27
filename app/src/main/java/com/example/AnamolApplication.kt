package com.example

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.database.AppDatabase
import com.example.data.repository.PreferencesRepository
import com.example.data.repository.ScanRepository
import com.example.security.AppLockManager
import com.example.security.CryptoEngine
import java.util.Locale

class AnamolApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var cryptoEngine: CryptoEngine
        private set

    lateinit var scanRepository: ScanRepository
        private set

    lateinit var preferencesRepository: PreferencesRepository
        private set

    lateinit var appLockManager: AppLockManager
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        cryptoEngine = CryptoEngine.createAndroidKeystoreEngine()
        scanRepository = ScanRepository(database.scanItemDao(), cryptoEngine)
        preferencesRepository = PreferencesRepository(this)
        appLockManager = AppLockManager(this)
    }

    @Suppress("DEPRECATION")
    fun applyLanguage(langCode: String) {
        val locale = Locale(langCode)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    fun playScanFeedback(vibrate: Boolean, beep: Boolean) {
        if (vibrate) {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(45)
                }
            }
        }
        if (beep) {
            try {
                val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75)
                tone.startTone(ToneGenerator.TONE_PROP_ACK, 90)
            } catch (_: Exception) {}
        }
    }
}
