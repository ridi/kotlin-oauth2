package com.ridi.oauth2

import android.support.annotation.VisibleForTesting
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.HttpURLConnection

class UnexpectedResponseException(val responseCode: Int, val redirectedToUrl: String) : RuntimeException()

class InvalidTokenFileException : RuntimeException()

class InvalidTokenEncryptionKeyException(message: String) : RuntimeException(message)

class Authorization {
    companion object {
        private const val DEV_HOST = "account.dev.ridi.io"
        private const val REAL_HOST = "account.ridibooks.com"
        internal const val AUTHORIZATION_REDIRECT_URI = "app://authorized"

        internal const val COOKIE_NAME_RIDI_AT = "ridi-at"
        internal const val COOKIE_NAME_RIDI_RT = "ridi-rt"
    }

    data class RequestResult(val accessToken: String, val refreshToken: String)

    private val clientId: String
    private val apiManager: ApiManager

    constructor(
        clientId: String,
        devMode: Boolean = false
    ) : this("https://${if (devMode) DEV_HOST else REAL_HOST}/", clientId)

    @VisibleForTesting
    internal constructor(baseUrl: String, clientId: String) {
        this.clientId = clientId
        apiManager = ApiManager(baseUrl)
    }

    fun requestRidiAuthorization(phpSessionId: String): Observable<RequestResult> = Observable.create { emitter ->
        apiManager.cookieStorage.phpSessionId = phpSessionId
        apiManager.service.requestAuthorization(clientId, "code", AUTHORIZATION_REDIRECT_URI)
            .enqueue(object : Callback<ResponseBody> {
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    apiManager.cookieStorage.clear()
                    emitter.emitErrorIfNotDisposed(t)
                }

                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    try {
                        if (emitter.isDisposed.not() && response.code() == HttpURLConnection.HTTP_MOVED_TEMP &&
                            response.headers().values("Location").firstOrNull() == AUTHORIZATION_REDIRECT_URI) {
                            var accessToken: String? = null
                            var refreshToken: String? = null
                            apiManager.cookieStorage.savedCookies.forEach { cookie ->
                                when (cookie.name()) {
                                    COOKIE_NAME_RIDI_AT -> accessToken = cookie.value()
                                    COOKIE_NAME_RIDI_RT -> refreshToken = cookie.value()
                                }
                            }

                            if (accessToken != null && refreshToken != null) {
                                emitter.emitItemAndCompleteIfNotDisposed(RequestResult(accessToken!!, refreshToken!!))
                                return
                            }
                        }

                        emitter.emitErrorIfNotDisposed(
                            UnexpectedResponseException(response.code(), response.raw().request().url().toString()))
                    } finally {
                        apiManager.cookieStorage.clear()
                    }
                }
            })
    }

    fun refreshAccessToken(refreshToken: String): Observable<RequestResult> {
        apiManager.cookieStorage.refreshToken = refreshToken
        return Observable.create { emitter ->
            apiManager.service.refreshAccessToken()
                .enqueue(object : Callback<ResponseBody> {
                    override fun onFailure(call: Call<ResponseBody>, t: Throwable?) {
                        apiManager.cookieStorage.clear()
                        emitter.emitErrorIfNotDisposed(IllegalStateException(t))
                    }

                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        try {
                            if (response.isSuccessful) {
                                var newAccessToken: String? = null
                                var newRefreshToken: String? = null
                                apiManager.cookieStorage.savedCookies.forEach { cookie ->
                                    when (cookie.name()) {
                                        COOKIE_NAME_RIDI_AT -> newAccessToken = cookie.value()
                                        COOKIE_NAME_RIDI_RT -> newRefreshToken = cookie.value()
                                    }
                                }

                                if (newAccessToken != null && newRefreshToken != null) {
                                    emitter.emitItemAndCompleteIfNotDisposed(
                                        RequestResult(newAccessToken!!, newRefreshToken!!)
                                    )
                                    return
                                }
                            }

                            emitter.emitErrorIfNotDisposed(
                                UnexpectedResponseException(response.code(), response.raw().request().url().toString())
                            )
                        } finally {
                            apiManager.cookieStorage.clear()
                        }
                    }
                })
        }
    }

    private fun ObservableEmitter<RequestResult>.emitErrorIfNotDisposed(t: Throwable) {
        if (isDisposed.not()) {
            onError(t)
        }
    }

    private fun ObservableEmitter<RequestResult>.emitItemAndCompleteIfNotDisposed(item: RequestResult) {
        if (isDisposed.not()) {
            onNext(item)
            onComplete()
        }
    }
}
