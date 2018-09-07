package com.ridi.oauth2.demoapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Switch
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        DemoAppApplication.isDevMode = true

        val switch = findViewById<Switch>(R.id.server_switch)

        findViewById<Button>(R.id.login_button).setOnClickListener {
            val intent = Intent(this, WebViewActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.access_token_button).setOnClickListener {
            DemoAppApplication.tokenManager.getAccessToken().subscribe({
                Toast.makeText(this, "Received => $it", Toast.LENGTH_SHORT).show()
                Log.e(javaClass.name, "Received => $it")
            }, {
                Toast.makeText(this, "Error => $it", Toast.LENGTH_SHORT).show()
                Log.e(javaClass.name, "Error => $it")
            })
        }

        switch.setOnCheckedChangeListener { _, isChecked ->
            switch.text = if (isChecked) "REAL" else "TEST"
            DemoAppApplication.isDevMode = isChecked.not()
        }
    }
}
