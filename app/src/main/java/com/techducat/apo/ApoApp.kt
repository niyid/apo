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
            Timber.plant(productionTree())
        }

        onCreateFlavor()
    }

    // Overridden per flavor (playstore / fdroid)
    protected open fun productionTree(): Timber.Tree = Timber.DebugTree()
    protected open fun onCreateFlavor() {}
}
