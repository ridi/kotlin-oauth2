package com.ridi.oauth2

import android.util.Log
import com.ridi.oauth2.WebViewActivity.Companion.cookies
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface OAuth2Service {
    @GET("ridi/authorize")
    fun ridiAuthorize(@Query("client_id") client_id: String,
                      @Query("response_type") response_type: String,
                      @Query("redirect_uri") redirect_uri: String): Call<ResponseBody>

    @POST("ridi/token")
    fun ridiToken(@Header("Cookie") authToken: String,
                  @Header("Cookie") refreshToken: String): Call<ResponseBody>

    companion object Factory {
        fun create(): OAuth2Service {
            val client = OkHttpClient().newBuilder()
                .addNetworkInterceptor(Intercept())
                .build()
            val retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl("https://account.dev.ridi.io/")
                .client(client)
                .build()
            return retrofit.create(OAuth2Service::class.java)
        }
    }

    class Intercept : Interceptor {
        var tokenJSON = JSONObject()

        override fun intercept(chain: Interceptor.Chain): Response {
            val originalRequest = chain.request()
            val builder = originalRequest.newBuilder().apply {
                WebViewActivity.cookies.forEach {
                    addHeader("Cookie", it)
                }
            }

            val response = chain.proceed(builder.build())

            if (originalRequest.url().toString().contains("ridi/token")) {
                return response
            }

            if (response.headers("set-cookie").isEmpty().not()) {
                tokenJSON = JSONObject()
                response.headers("set-cookie").forEach {
                    cookies.add(it)
                    if (it.split("=", ";")[0] == "ridi-at" || it.split("=", ";")[0] == "ridi-rt") {
                        tokenJSON.put(it.split("=", ";")[0], it.split("=", ";")[1])
                        Log.e(javaClass.name, "${it.split("=", ";")[0]} => ${it.split("=", ";")[1]}")
                    }
                }
                if (tokenJSON.isNull("ridi-at").not()) {
                    Log.e(javaClass.name, "saveJSONFILE")
                    RidiOAuth2.saveJSONFile(tokenJSON)
                }
            }
            return response
        }
    }
}