#!/bin/bash

echo "=== Creating Flavor-Specific ApoApp Files ==="

# Create F-Droid version (without Firebase)
mkdir -p app/src/fdroid/java/com/techducat/apo
cat > app/src/fdroid/java/com/techducat/apo/ApoApp.kt << 'EOF'
package com.techducat.apo

import android.app.Application
import android.content.Context
import timber.log.Timber

class ApoApp : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // For F-Droid release builds, use basic logging
            Timber.plant(ReleaseTree())
        }
        
        Timber.d("ApoApp initialized (F-Droid build)")
    }
    
    /**
     * Simple release tree for F-Droid builds
     * Only logs errors and warnings to logcat
     */
    private class ReleaseTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            // Only log errors and warnings in release builds
            if (priority == android.util.Log.ERROR || priority == android.util.Log.WARN) {
                android.util.Log.println(priority, tag ?: "ApoApp", message)
                t?.let { android.util.Log.println(priority, tag ?: "ApoApp", android.util.Log.getStackTraceString(it)) }
            }
        }
    }
}
EOF

echo "✓ Created F-Droid ApoApp.kt"

# Create Play Store version (with Firebase)
mkdir -p app/src/playstore/java/com/techducat/apo
cat > app/src/playstore/java/com/techducat/apo/ApoApp.kt << 'EOF'
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
EOF

echo "✓ Created Play Store ApoApp.kt"

# Remove the main ApoApp.kt (we now have flavor-specific versions)
if [ -f app/src/main/java/com/techducat/apo/ApoApp.kt ]; then
    mv app/src/main/java/com/techducat/apo/ApoApp.kt app/src/main/java/com/techducat/apo/ApoApp.kt.backup
    echo "✓ Backed up and removed main ApoApp.kt (now using flavor-specific versions)"
fi

echo ""
echo "=== ApoApp Setup Complete ==="
echo "F-Droid: app/src/fdroid/java/com/techducat/apo/ApoApp.kt"
echo "Play Store: app/src/playstore/java/com/techducat/apo/ApoApp.kt"
