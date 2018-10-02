package com.ridi.oauth2.demoapp

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Switch
import android.widget.TextView
import com.auth0.android.jwt.JWT
import com.ridi.oauth2.Authorization
import com.ridi.oauth2.AuthorizationFailedException
import com.ridi.oauth2.TokenResponse
import io.reactivex.SingleObserver
import io.reactivex.disposables.Disposable

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val switch = findViewById<Switch>(R.id.server_switch)
        switch.setOnCheckedChangeListener { _, isChecked ->
            switch.text = if (isChecked) "REAL SERVER" else "DEV SERVER"
        }

        findViewById<View>(R.id.request_button).setOnClickListener {
            val clientId = findViewById<TextView>(R.id.client_id).text.toString()
            val clientSecret = findViewById<TextView>(R.id.client_secret).text.toString()
            val devMode = switch.isChecked.not()
            val username = findViewById<TextView>(R.id.username).text.toString()
            val password = findViewById<TextView>(R.id.password).text.toString()
            val authorization = Authorization(clientId, clientSecret, devMode)
            authorization.requestPasswordGrantAuthorization(username, password).subscribe(TokenObserver(authorization))
        }
    }

    private inner class TokenObserver(private val authorization: Authorization) : SingleObserver<TokenResponse> {
        override fun onSuccess(t: TokenResponse) {
            val jwt = JWT(t.accessToken)
            val description =
                "Subject=${jwt.subject}, u_idx=${jwt.getClaim("u_idx").asInt()}, expiresAt=${jwt.expiresAt}"

            AlertDialog.Builder(this@MainActivity)
                .setMessage(description)
                .setNeutralButton("Refresh") { _, _ ->
                    authorization.refreshAccessToken(t.refreshToken).subscribe(TokenObserver(authorization))
                }
                .setNegativeButton("Close", null)
                .show()
        }

        override fun onError(e: Throwable) {
            val description = when (e) {
                is AuthorizationFailedException -> "HTTP status code : ${e.httpStatusCode}\n" +
                    "Error : ${e.errorCode}\n" +
                    "Description : ${e.message}"
                else -> "$e"
            }
            AlertDialog.Builder(this@MainActivity)
                .setMessage(description)
                .setNegativeButton("Close", null)
                .show()
        }

        override fun onSubscribe(d: Disposable) {}
    }
}
