package com.techducat.apo

import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber
import android.util.Log

// Playstore flavor — Firebase + Crashlytics initialisation
class ApoAppFlavor : ApoApp() {

    override fun productionTree(): Timber.Tree = CrashlyticsTree()

    override fun onCreateFlavor() {
        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Configure Crashlytics
        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)

        crashlytics.setCustomKey("app_version",    BuildConfig.VERSION_NAME)
        crashlytics.setCustomKey("version_code",   BuildConfig.VERSION_CODE)
        crashlytics.setCustomKey("build_type",     if (BuildConfig.DEBUG) "debug" else "release")
        crashlytics.setCustomKey("device_model",   android.os.Build.MODEL)
        crashlytics.setCustomKey("android_version",android.os.Build.VERSION.RELEASE)
        crashlytics.setCustomKey("app_start_time", System.currentTimeMillis())

        crashlytics.log("App started - Version ${BuildConfig.VERSION_NAME}")

        Timber.d("ApoApp initialized with Firebase Crashlytics")
    }

    private class CrashlyticsTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority == Log.ERROR || priority == Log.WARN) {
                val crashlytics = FirebaseCrashlytics.getInstance()

                val priorityStr = when (priority) {
                    Log.ERROR -> "ERROR"
                    Log.WARN  -> "WARN"
                    else      -> "INFO"
                }

                val timestamp = java.text.SimpleDateFormat(
                    "HH:mm:ss.SSS",
                    java.util.Locale.getDefault()
                ).format(java.util.Date())

                crashlytics.log("[$timestamp] [$priorityStr] $tag: $message")

                t?.let {
                    crashlytics.recordException(it)
                    crashlytics.setCustomKey("last_error_tag",  tag ?: "unknown")
                    crashlytics.setCustomKey("last_error_time", timestamp)
                }
            }
        }
    }
}
