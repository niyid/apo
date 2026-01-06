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
