package com.ridi.demoapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import com.ridi.oauth2.TokenManager
import java.io.File

class MainActivity : Activity() {
    companion object {
        internal var tokenManager = TokenManager()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.login_button).setOnClickListener {
            val intent = Intent(this, WebViewActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.access_token_button).setOnClickListener {
            tokenManager.useDevMode = true
            tokenManager.tokenFile = File(applicationContext.filesDir.absolutePath +
                "/tokenJSON.json")
            tokenManager.clientId = "Nkt2Xdc0zMuWmye6MSkYgqCh9q6JjeMCsUiH1kgL"
            tokenManager.getAccessToken("app://authorized").subscribe({
                Log.d(javaClass.name, "Received => $it")
            }, {
                Log.e(javaClass.name, "Error => $it")
            })
        }
    }
}
