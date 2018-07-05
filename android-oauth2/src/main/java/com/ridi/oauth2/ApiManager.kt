package com.ridi.oauth2

import com.ridi.books.helper.io.saveToFile
import com.ridi.oauth2.RidiOAuth2.Companion.BASE_URL
import com.ridi.oauth2.RidiOAuth2.Companion.COOKIE_RIDI_AT
import com.ridi.oauth2.RidiOAuth2.Companion.COOKIE_RIDI_RT
import com.ridi.oauth2.RidiOAuth2.Companion.cookies
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
import java.io.File

class ApiManager {
    private val retrofit: Retrofit
    internal val cookieInterceptor = CookieInterceptor()

    init {
        val client = OkHttpClient().newBuilder()
            .addNetworkInterceptor(cookieInterceptor)
            .build()
        retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(BASE_URL)
            .client(client)
            .build()
    }

    private fun <T> createServiceLazy(service: Class<T>) = lazy { retrofit.create(service) }

    internal val service: ApiService by createServiceLazy(ApiService::class.java)

    internal interface ApiService {
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
    }

    internal class CookieInterceptor : Interceptor {
        private val USER_AGENT_FOR_OKHTTP =
            String(System.getProperty("http.agent").toCharArray().filter { it in ' '..'~' }.toCharArray())
        var tokenFile: File? = null

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
                    RidiOAuth2.run {
                        tokenJSON.parseCookie(it)
                    }
                }

                if (tokenJSON.isNull(COOKIE_RIDI_AT).not() && tokenJSON.isNull(COOKIE_RIDI_RT).not()) {
                    tokenJSON.toString().saveToFile(tokenFile!!)
                }
            }
            return response
        }
    }
}
