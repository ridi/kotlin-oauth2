package com.ridi.demoapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import com.ridi.oauth2.RidiOAuth2
import java.io.File

class MainActivity : Activity() {
    companion object {
        internal var ridiOAuth2 = RidiOAuth2()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.login_button).setOnClickListener {
            val intent = Intent(this, WebViewActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.access_token_button).setOnClickListener {
            Thread().run {
                ridiOAuth2.useDevMode = true
                ridiOAuth2.tokenFile = File(applicationContext.filesDir.absolutePath +
                    "/tokenJSON.json")
                ridiOAuth2.clientId = "Nkt2Xdc0zMuWmye6MSkYgqCh9q6JjeMCsUiH1kgL"
                ridiOAuth2.getAccessToken("app://authorized").subscribe({
                    Log.d(javaClass.name, "Received => $it")
                }, {
                    Log.e(javaClass.name, "Error => $it")
                })
            }
        }
    }
}
