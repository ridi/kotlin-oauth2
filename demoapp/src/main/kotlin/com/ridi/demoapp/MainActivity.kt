package com.ridi.demoapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.Toast
import com.ridi.demoapp.MainActivity.TokenManager.tokenManager
import com.ridi.oauth2.TokenManager
import java.io.File

class MainActivity : Activity() {
    internal object TokenManager {
        internal var tokenManager = TokenManager()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val switch = findViewById<Switch>(R.id.server_switch)

        findViewById<Button>(R.id.login_button).setOnClickListener {
            tokenManager.useDevMode = switch.isChecked.not()
            val intent = Intent(this, WebViewActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.access_token_button).setOnClickListener {
            tokenManager.tokenFile = File(applicationContext.filesDir.absolutePath +
                "/tokenJSON.json")
            tokenManager.clientId =
                if (tokenManager.useDevMode) "Nkt2Xdc0zMuWmye6MSkYgqCh9q6JjeMCsUiH1kgL"
                else "ePgbKKRyPvdAFzTvFg2DvrS7GenfstHdkQ2uvFNd"

            tokenManager.getAccessToken("app://authorized").subscribe({
                Toast.makeText(this, "Received => $it", Toast.LENGTH_SHORT).show()
            }, {
                Toast.makeText(this, "Error => $it", Toast.LENGTH_SHORT).show()
            })
        }

        switch.setOnCheckedChangeListener { _, isChecked ->
            switch.text = if (isChecked) "REAL" else "TEST"
            tokenManager.useDevMode = isChecked.not()
        }
    }
}
