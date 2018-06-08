package com.ridi.oauth2

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Response

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        findViewById<Button>(R.id.button).setOnClickListener {
            Thread().run {
                val service = OAuth2Service.create()
                val call = service.ridiAuthorize("Nkt2Xdc0zMuWmye6MSkYgqCh9q6JjeMCsUiH1kgL",
                    "code", "app://authorized")
                call.enqueue(object : retrofit2.Callback<ResponseBody> {
                    override fun onFailure(call: retrofit2.Call<ResponseBody>?, t: Throwable?) {
                        Log.e(javaClass.name, "onFailure")
                    }

                    override fun onResponse(call: retrofit2.Call<ResponseBody>?, response: retrofit2.Response<ResponseBody>) {
                        Log.e(javaClass.name, "response => " + response)
                    }
                })
            }
        }
        findViewById<Button>(R.id.loginButton).setOnClickListener {
            val intent = Intent(this, WebViewActivity::class.java)
            startActivity(intent)
        }
        findViewById<Button>(R.id.refreshButton).setOnClickListener {
            val service = OAuth2Service.create()
            Thread().run {
                val call = service.ridiToken(RidiOAuth2.getAuthToken(), RidiOAuth2.getRefreshToken())
                call.enqueue(object : retrofit2.Callback<ResponseBody> {
                    override fun onFailure(call: Call<ResponseBody>?, t: Throwable?) {
                        Log.e(javaClass.name, "failure")
                    }

                    override fun onResponse(call: Call<ResponseBody>?, response: Response<ResponseBody>) {
                        val jsonObject = JSONObject(response.body()?.string())
                        Log.e(javaClass.name, "expires At => ${jsonObject.getString("expires_at")}")
                        Log.e(javaClass.name, "expires_In => ${jsonObject.getString("expires_in")}")
                    }
                })

            }
        }
    }
}
