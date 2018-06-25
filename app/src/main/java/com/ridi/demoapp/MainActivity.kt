package com.ridi.demoapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import com.ridi.oauth2.RidiOAuth2

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.loginButton).setOnClickListener {
            val intent = Intent(this, WebViewActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.getAuthTokenButton).setOnClickListener {
            Thread().run {
                RidiOAuth2.instance.setDev()
                RidiOAuth2.instance.setTokenFilePath(Environment.getExternalStorageDirectory().absolutePath +
                    "/tokenJSON.json")
                RidiOAuth2.instance.setClientId("Nkt2Xdc0zMuWmye6MSkYgqCh9q6JjeMCsUiH1kgL")
                RidiOAuth2.instance.getOAuthToken("app://authorized").subscribe({
                    Log.d(javaClass.name, "Received => $it")
                }, {
                    Log.e(javaClass.name, "Error => $it")
                })
            }
        }
    }
}
