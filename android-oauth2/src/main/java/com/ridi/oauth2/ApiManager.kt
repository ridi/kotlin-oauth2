package com.ridi.oauth2

import com.ridi.books.helper.io.saveToFile
import com.ridi.oauth2.RidiOAuth2.Companion.BASE_URL
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

        var cookies = HashSet<String>()
        private fun JSONObject.parseCookie(cookieString: String) {
            val cookie = cookieString.split("=", ";")
            if (cookie[0] == "ridi-at" || cookie[0] == "ridi-rt") {
                put(cookie[0], cookie[1])
            }
        }
    }

    private class CookieInterceptor : Interceptor {
        private val USER_AGENT_FOR_OKHTTP =
            String(System.getProperty("http.agent").toCharArray().filter { it in ' '..'~' }.toCharArray())

        override fun intercept(chain: Interceptor.Chain): Response {
            val originalRequest = chain.request()
            val builder = originalRequest.newBuilder().apply {
                addHeader("User-Agent", USER_AGENT_FOR_OKHTTP)
                cookies.forEach {
                    addHeader("Cookie", it)
                }
            }

            val response = chain.proceed(builder.build())
            val cookieHeaders = response.headers().values("Set-Cookie")
            if (cookieHeaders.isNotEmpty()) {
                val tokenJSON = JSONObject()
                cookieHeaders.forEach {
                    cookies.add(it)
                    tokenJSON.parseCookie(it)
                }
                if (!tokenJSON.isNull("ridi-at") && !tokenJSON.isNull("ridi-rt")) {
                    tokenJSON.toString().saveToFile(RidiOAuth2.tokenFile)
                }
            }
            return response
        }
    }
}
