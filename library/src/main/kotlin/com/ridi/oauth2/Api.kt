package com.ridi.oauth2

import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

internal class Api(baseUrl: String) {
    val cookieStorage = CookieStorage()
    val service: Service

    init {
        val client = OkHttpClient().newBuilder()
            .cookieJar(cookieStorage)
            .build()
        val retrofit = Retrofit.Builder()
            .client(client)
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        service = retrofit.create(Service::class.java)
    }

    interface Service {
        @GET("ridi/authorize")
        fun requestAuthorization(
            @Query("client_id") clientId: String,
            @Query("response_type") responseType: String,
            @Query("redirect_uri") redirectUri: String
        ): Call<ResponseBody>

        @POST("ridi/token")
        fun refreshAccessToken(): Call<ResponseBody>
    }
}
