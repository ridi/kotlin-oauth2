package com.ridi.oauth2.demoapp

import android.app.Application
import com.ridi.oauth2.TokenManager
import java.io.File

class DemoAppApplication : Application() {
    companion object {
        private const val DEV_CLIENT_ID = "Nkt2Xdc0zMuWmye6MSkYgqCh9q6JjeMCsUiH1kgL"
        private const val REAL_CLIENT_ID = "ePgbKKRyPvdAFzTvFg2DvrS7GenfstHdkQ2uvFNd"

        private lateinit var instance: DemoAppApplication

        lateinit var tokenManager: TokenManager
            private set

        var isDevMode = false
            set(value) {
                field = value
                val clientId = if (value) DEV_CLIENT_ID else REAL_CLIENT_ID
                tokenManager = TokenManager(clientId, value)
            }

        var phpSessionId = ""
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
