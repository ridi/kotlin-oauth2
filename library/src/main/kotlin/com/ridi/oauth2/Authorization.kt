package com.ridi.oauth2

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class Authorization(private val clientId: String, private val clientSecret: String, devMode: Boolean = false) {
    companion object {
        private const val DEV_HOST = "account.dev.ridi.io"
        private const val REAL_HOST = "account.ridibooks.com"

        private const val CONNECT_TIMEOUT_SECONDS = 5L
        private const val READ_TIMEOUT_SECONDS = 10L
    }

    private val apiService: ApiService

    init {
        val client = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
        val retrofit = Retrofit.Builder()
            .client(client)
            .baseUrl("https://${if (devMode) DEV_HOST else REAL_HOST}/")
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(
                GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()))
            .build()
        apiService = retrofit.create(ApiService::class.java)
    }

    fun requestPasswordGrantAuthorization(username: String, password: String) =
        apiService.requestToken(clientId, clientSecret, ApiService.PASSWORD_GRANT_TYPE, username, password, null)

    fun refreshAccessToken(refreshToken: String) =
        apiService.requestToken(clientId, clientSecret, ApiService.REFRESH_TOKEN_GRANT_TYPE, null, null, refreshToken)
}
