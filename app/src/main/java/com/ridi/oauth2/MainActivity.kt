package com.ridi.oauth2

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import okhttp3.ResponseBody

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val service = OAuth2Service.create()

        findViewById<Button>(R.id.button).setOnClickListener {
            Thread().run {
                val call = service.ridiAuthorize("Nkt2Xdc0zMuWmye6MSkYgqCh9q6JjeMCsUiH1kgL",
                    "code", "app://authorized")
                call.enqueue(object : retrofit2.Callback<ResponseBody> {
                    override fun onFailure(call: retrofit2.Call<ResponseBody>?, t: Throwable?) {
                        Log.e("MainActivity", "onFailure")
                    }

                    override fun onResponse(call: retrofit2.Call<ResponseBody>?, response: retrofit2.Response<ResponseBody>?) {
                        Log.e("MainActivity", "response => " + response)
                    }
                })
            }
        }
        findViewById<Button>(R.id.loginButton).setOnClickListener {
            val intent = Intent(this, WebViewActivity::class.java)
            startActivity(intent)
        }
    }
}
