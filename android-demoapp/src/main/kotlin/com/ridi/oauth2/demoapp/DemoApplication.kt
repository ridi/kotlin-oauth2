package com.ridi.oauth2.demoapp

import android.app.Application
import com.ridi.oauth2.Authorization

class DemoApplication : Application() {
    companion object {
        private lateinit var instance: DemoApplication

        lateinit var authorization: Authorization
            private set

        var isDevMode = false
            set(value) {
                field = value
                val clientId = if (value) "" else ""
                authorization = Authorization(clientId, "", value)
            }

        var phpSessionId = ""
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
