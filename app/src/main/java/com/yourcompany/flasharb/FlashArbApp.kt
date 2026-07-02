package com.yourcompany.flasharb

import android.app.Application
import com.yourcompany.flasharb.BuildConfig
import timber.log.Timber

/**
 * @title FlashArbApp
 * @dev Main Application class for initializing global libraries like Timber.
 */
class FlashArbApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
