package com.ridi.oauth2

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import io.reactivex.Single
import io.reactivex.SingleEmitter
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
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
            .addConverterFactory(GsonConverterFactory.create(
                GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()))
            .build()
        apiService = retrofit.create(ApiService::class.java)
    }

    fun requestPasswordGrantAuthorization(username: String, password: String): Single<TokenResponse> =
        Single.create<TokenResponse> { emitter ->
            apiService.requestToken(clientId, clientSecret, ApiService.PASSWORD_GRANT_TYPE, username, password, null)
                .enqueue(ApiCallback(emitter))
        }

    fun refreshAccessToken(refreshToken: String): Single<TokenResponse> =
        Single.create<TokenResponse> { emitter ->
            apiService.requestToken(
                clientId, clientSecret, ApiService.REFRESH_TOKEN_GRANT_TYPE, null, null, refreshToken)
                .enqueue(ApiCallback(emitter))
        }

    private class ApiCallback(private val emitter: SingleEmitter<TokenResponse>) : Callback<TokenResponse> {
        override fun onFailure(call: Call<TokenResponse>, t: Throwable) {
            emitter.tryOnError(t)
        }

        override fun onResponse(call: Call<TokenResponse>, response: Response<TokenResponse>) {
            if (response.isSuccessful) {
                response.body()?.let {
                    emitter.onSuccess(it)
                }
            } else {
                val statusCode = response.code()
                emitter.tryOnError(response.errorBody()?.let {
                    val errorObject = Gson().fromJson<JsonObject>(it.charStream(), JsonObject::class.java)
                    val errorCode = errorObject.get("error")?.asString
                    val errorDescription = errorObject.get("error_description")?.asString
                    AuthorizationFailedException(statusCode, errorCode, errorDescription)
                } ?: AuthorizationFailedException(statusCode))
            }
        }
    }
}
