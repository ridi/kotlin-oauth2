package com.ridi.oauth2

import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface OAuth2Service {
    @GET("ridi/authorize")
    fun ridiAuthorize(@Query("client_id") client_id: String,
                      @Query("response_type") response_type: String,
                      @Query("redirect_uri") redirect_uri: String): Call<ResponseBody>

    @POST("ridi/token")
    fun ridiToken(@Query("ridi-at") authToken: String,
                  @Query("ridi_rt") refreshToken: String): Call<JSONObject>

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
        override fun intercept(chain: Interceptor.Chain): Response {
            val originalRequest = chain.request()
            val builder = originalRequest.newBuilder().apply {
                addHeader("Cookie", WebViewActivity.cookies)
            }

            val response = chain.proceed(builder.build())

            Log.e("Interceptor", "Response URL => " + response.request().url())

            if (!response.headers("set-cookie").isEmpty()) {
                val cookies = HashSet<String>()
                response.headers("set-cookie").forEach {
                    cookies.add(it)
                }
                Log.e("Interceptor", "Response cookies => " + cookies.toString())
            }
            return response
        }
    }
}