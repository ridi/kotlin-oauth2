package com.ridi.oauth2

import com.ridi.oauth2.TokenManager.Companion.BASE_URL
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

internal class ApiManager {
    val cookieStorage = CookieStorage()
    private val client = OkHttpClient().newBuilder()
        .cookieJar(cookieStorage)
        .build()

    private val retrofit = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create())
        .baseUrl(BASE_URL)
        .client(client)
        .build()

    val service: ApiService = retrofit.create(ApiService::class.java)

    interface ApiService {
        @GET("ridi/authorize")
        fun requestAuthorization(
            @Header("Cookie") sessionCookie: String,
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
}
