package com.techducat.apo

import android.app.Application
import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

class ApoApp : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // In production, log to Crashlytics
            Timber.plant(CrashlyticsTree())
        }
        
        // Configure Firebase Crashlytics
        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        
        // Enhanced custom keys for better crash reporting
        crashlytics.setCustomKey("app_version", BuildConfig.VERSION_NAME)
        crashlytics.setCustomKey("version_code", BuildConfig.VERSION_CODE)
        crashlytics.setCustomKey("build_type", if (BuildConfig.DEBUG) "debug" else "release")
        crashlytics.setCustomKey("device_model", android.os.Build.MODEL)
        crashlytics.setCustomKey("android_version", android.os.Build.VERSION.RELEASE)
        crashlytics.setCustomKey("app_start_time", System.currentTimeMillis())
        
        // Log app initialization
        crashlytics.log("App started - Version ${BuildConfig.VERSION_NAME}")
        
        Timber.d("ApoApp initialized with Firebase Crashlytics")
    }
    
    /**
     * Enhanced Custom Timber tree that logs errors to Firebase Crashlytics
     * with timestamps and better context
     */
    private class CrashlyticsTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            // Only log warnings and errors to conserve Firebase quota
            if (priority == android.util.Log.ERROR || priority == android.util.Log.WARN) {
                val crashlytics = FirebaseCrashlytics.getInstance()
                
                // Add priority level to log
                val priorityStr = when(priority) {
                    android.util.Log.ERROR -> "ERROR"
                    android.util.Log.WARN -> "WARN"
                    else -> "INFO"
                }
                
                // Enhanced log format with timestamp
                val timestamp = java.text.SimpleDateFormat(
                    "HH:mm:ss.SSS", 
                    java.util.Locale.getDefault()
                ).format(java.util.Date())
                
                // Log the message with enhanced formatting
                crashlytics.log("[$timestamp] [$priorityStr] $tag: $message")
                
                // If there's a throwable, record it
                t?.let { 
                    crashlytics.recordException(it)
                    
                    // Add breadcrumb context for debugging
                    crashlytics.setCustomKey("last_error_tag", tag ?: "unknown")
                    crashlytics.setCustomKey("last_error_time", timestamp)
                }
            }
        }
    }
}
