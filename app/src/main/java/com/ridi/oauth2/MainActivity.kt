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

        val service = OAuth2Service.create()

        findViewById<Button>(R.id.loginButton).setOnClickListener {
            val intent = Intent(this, WebViewActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.button).setOnClickListener {
            Thread().run {
                RidiOAuth2.setClientId("Nkt2Xdc0zMuWmye6MSkYgqCh9q6JjeMCsUiH1kgL")
                RidiOAuth2.getOAuthToken("app://authorized").subscribe({
                    Log.e(javaClass.name, "Received => ${it}")
                }, {
                    Log.e(javaClass.name, "Error => ${it}")
                })
            }
        }
        findViewById<Button>(R.id.refreshButton).setOnClickListener {
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
