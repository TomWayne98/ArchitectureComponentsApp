package cz.pepa.runapp

import android.app.Application
import android.os.StrictMode
import android.support.v7.app.AppCompatDelegate
import cz.pepa.runapp.BuildConfig

/**
 * TODO: Add description
 *
 * @author Josef Hruška (josef@stepuplabs.io)
 */

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        gApp = this
        configureStrictMode()
        // Configure AppCompat
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }

    private fun configureStrictMode() {
        if (BuildConfig.DEBUG) {
            // detect wrong threading
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyDialog()
                    .build())
            // Detect leaked Activities, SqLite and Closeable objects
            // Note that it often reports false positives for Activities. Make a heap dump to check.
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().detectAll()
                    .penaltyLog()
                    .penaltyDropBox()
                    .build())
        }
    }
}

private var gApp: App? = null

fun app(): App {
    return checkNotNull(gApp, { "App not initialized" })
}