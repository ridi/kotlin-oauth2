package com.ridi.oauth2.demoapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Switch
import android.widget.Toast
import com.auth0.android.jwt.JWT
import com.ridi.oauth2.TokenPair
import io.reactivex.SingleObserver
import io.reactivex.disposables.Disposable

class MainActivity : Activity() {
    private var refreshToken = ""

    private val observer = object : SingleObserver<TokenPair> {
        override fun onSuccess(t: TokenPair) {
            refreshToken = t.refreshToken
            val jwt = JWT(t.accessToken)
            val description =
                "Subject=${jwt.subject}, u_idx=${jwt.getClaim("u_idx").asInt()}, expiresAt=${jwt.expiresAt}"
            Toast.makeText(this@MainActivity, "Received => $description", Toast.LENGTH_SHORT).show()
        }

        override fun onError(e: Throwable) {
            Toast.makeText(this@MainActivity, "Error => $e", Toast.LENGTH_SHORT).show()
            Log.e(javaClass.name, e.message, e)
        }

        override fun onSubscribe(d: Disposable) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        DemoApplication.isDevMode = true

        val switch = findViewById<Switch>(R.id.server_switch)

        findViewById<View>(R.id.login_button).setOnClickListener {
            val intent = Intent(this, WebViewActivity::class.java)
            startActivity(intent)
        }

        findViewById<View>(R.id.access_token_button).setOnClickListener {
            DemoApplication.authorization.requestRidiAuthorization(DemoApplication.phpSessionId).subscribe(observer)
        }

        findViewById<View>(R.id.refresh_token_button).setOnClickListener {
            DemoApplication.authorization.refreshAccessToken(refreshToken).subscribe(observer)
        }

        switch.setOnCheckedChangeListener { _, isChecked ->
            switch.text = if (isChecked) "REAL" else "TEST"
            DemoApplication.isDevMode = isChecked.not()
        }
    }
}
