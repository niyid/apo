package com.techducat.apo

import android.app.Application
import android.content.Context
import com.bugfender.android.BuildConfig
import com.bugfender.sdk.Bugfender

class ApoApp : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)

        Bugfender.init(this, "h4GfYZySBJL4yTkxfXoulSGnHImPPmaQ", BuildConfig.DEBUG)
        Bugfender.enableCrashReporting()
        Bugfender.enableUIEventLogging(this)
        Bugfender.enableLogcatLogging()
    }
}
