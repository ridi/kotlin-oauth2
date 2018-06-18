package com.ridi.androidoauth2

import com.ridi.androidoauth2.RidiOAuth2.cookies
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

interface ApiManager {
    @GET("ridi/authorize")
    fun ridiAuthorize(@Query("client_id") client_id: String,
                      @Query("response_type") response_type: String,
                      @Query("redirect_uri") redirect_uri: String): Call<ResponseBody>

    @POST("ridi/token")
    fun ridiToken(@Header("Cookie") authToken: String,
                  @Header("Cookie") refreshToken: String): Call<ResponseBody>

    companion object Factory {
        val DEV_HOST = "https://account.dev.ridi.io/"
        val REAL_HOST = "https://account.ridibooks.com/"
        val HOST = DEV_HOST

        fun create(): ApiManager {
            val client = OkHttpClient().newBuilder()
                .addNetworkInterceptor(Intercept())
                .build()
            val retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(HOST)
                .client(client)
                .build()
            return retrofit.create(ApiManager::class.java)
        }
    }

    class Intercept : Interceptor {
        var tokenJSON = JSONObject()
        val USER_AGENT_FOR_OKHTTP = String(System.getProperty("http.agent").toCharArray().filter { it in ' '..'~' }.toCharArray())

        override fun intercept(chain: Interceptor.Chain): Response {
            val originalRequest = chain.request()
            val builder = originalRequest.newBuilder().apply {
                addHeader("User-Agent", USER_AGENT_FOR_OKHTTP)
                RidiOAuth2.cookies.forEach {
                    try {
                        addHeader("Cookie", it)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            val response = chain.proceed(builder.build())
            if (response.headers("set-cookie").isEmpty().not()) {
                tokenJSON = JSONObject()
                response.headers("set-cookie").forEach {
                    cookies.add(it)
                    if (it.split("=", ";")[0] == "ridi-at" || it.split("=", ";")[0] == "ridi-rt") {
                        tokenJSON.put(it.split("=", ";")[0], it.split("=", ";")[1])
                    }
                }
                if (tokenJSON.isNull("ridi-at").not()) {
                    RidiOAuth2.saveJSONFile(tokenJSON)
                }
            }
            return response
        }
    }
}