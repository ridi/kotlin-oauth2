package com.ridi.androidoauth2

import com.ridi.androidoauth2.RidiOAuth2.BASE_URL
import com.ridi.androidoauth2.RidiOAuth2.cookies
import com.ridi.androidoauth2.RidiOAuth2.parseCookie
import com.ridi.books.helper.io.saveToFile
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

internal interface ApiManager {
    @GET("ridi/authorize")
    fun requestAuthorization(
        @Query("client_id") clientId: String,
        @Query("response_type") responseType: String,
        @Query("redirect_uri") redirectUri: String
    ): Call<ResponseBody>

    @POST("ridi/token")
    fun refreshAccessToken(
        @Header("Cookie") accessToken: String,
        @Header("Cookie") refreshToken: String
    ): Call<ResponseBody>

    companion object Factory {
        fun create(): ApiManager {
            val client = OkHttpClient().newBuilder()
                .addNetworkInterceptor(CookieInterceptor())
                .build()
            val retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(BASE_URL)
                .client(client)
                .build()
            return retrofit.create(ApiManager::class.java)
        }
    }

    private class CookieInterceptor : Interceptor {
        private val USER_AGENT_FOR_OKHTTP =
            String(System.getProperty("http.agent").toCharArray().filter { it in ' '..'~' }.toCharArray())

        override fun intercept(chain: Interceptor.Chain): Response {
            val tokenJSON = JSONObject()
            val originalRequest = chain.request()
            val builder = originalRequest.newBuilder().apply {
                addHeader("User-Agent", USER_AGENT_FOR_OKHTTP)
                RidiOAuth2.cookies.forEach {
                    addHeader("Cookie", it)
                }
            }

            val response = chain.proceed(builder.build())
            val cookieHeaders = response.headers().values("Set-Cookie")
            if (cookieHeaders.isNotEmpty()) {
                cookieHeaders.forEach {
                    cookies.add(it)
                    parseCookie(it)
                }
                if (tokenJSON.isNull("ridi-at").not() && tokenJSON.isNull("ridi-rt").not()) {
                    tokenJSON.toString().saveToFile(RidiOAuth2.tokenFile)
                }
            }
            return response
        }
    }
}
