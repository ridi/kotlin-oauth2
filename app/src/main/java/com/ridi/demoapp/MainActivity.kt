package com.ridi.demoapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import com.ridi.oauth2.RidiOAuth2

class MainActivity : Activity() {
    companion object {
        internal var ridiOAuth2 = RidiOAuth2()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.loginButton).setOnClickListener {
            val intent = Intent(this, WebViewActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.getAuthTokenButton).setOnClickListener {
            Thread().run {
                ridiOAuth2.setDevMode()
                ridiOAuth2.createTokenFileFromPath(applicationContext.filesDir.absolutePath +
                    "/tokenJSON.json")
                ridiOAuth2.setClientId("Nkt2Xdc0zMuWmye6MSkYgqCh9q6JjeMCsUiH1kgL")
                ridiOAuth2.getOAuthToken("app://authorized").subscribe({
                    Log.d(javaClass.name, "Received => $it")
                }, {
                    Log.e(javaClass.name, "Error => $it")
                })
            }
        }
    }
}
