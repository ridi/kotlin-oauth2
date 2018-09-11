package com.ridi.oauth2

import android.support.annotation.VisibleForTesting
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.HttpURLConnection

class Authorization {
    companion object {
        private const val DEV_HOST = "account.dev.ridi.io"
        private const val REAL_HOST = "account.ridibooks.com"
        internal const val AUTHORIZATION_REDIRECT_URI = "app://authorized"

        internal const val RIDI_AT_COOKIE_NAME = "ridi-at"
        internal const val RIDI_RT_COOKIE_NAME = "ridi-rt"

        private const val PHP_SESSION_ID_COOKIE_NAME = "PHPSESSID"
    }

    data class RequestResult(val accessToken: String, val refreshToken: String)

    class UnexpectedResponseException(val responseCode: Int) : RuntimeException()

    private val clientId: String
    private val api: Api

    constructor(
        clientId: String,
        devMode: Boolean = false
    ) : this("https://${if (devMode) DEV_HOST else REAL_HOST}/", clientId)

    @VisibleForTesting
    internal constructor(baseUrl: String, clientId: String) {
        this.clientId = clientId
        api = Api(baseUrl)
    }

    fun requestRidiAuthorization(phpSessionId: String): Observable<RequestResult> = Observable.create { emitter ->
        api.cookieStorage.add(PHP_SESSION_ID_COOKIE_NAME, phpSessionId)
        api.service.requestAuthorization(clientId, "code", AUTHORIZATION_REDIRECT_URI)
            .enqueue(object : Callback<ResponseBody> {
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    api.cookieStorage.reset()
                    emitter.emitErrorIfNotDisposed(t)
                }

                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    try {
                        if (emitter.isDisposed.not() && response.code() == HttpURLConnection.HTTP_MOVED_TEMP &&
                            response.headers().values("Location").firstOrNull() == AUTHORIZATION_REDIRECT_URI) {
                            var accessToken: String? = null
                            var refreshToken: String? = null
                            api.cookieStorage.savedCookies.forEach { cookie ->
                                when (cookie.name()) {
                                    RIDI_AT_COOKIE_NAME -> accessToken = cookie.value()
                                    RIDI_RT_COOKIE_NAME -> refreshToken = cookie.value()
                                }
                            }

                            if (accessToken != null && refreshToken != null) {
                                emitter.emitItemAndCompleteIfNotDisposed(RequestResult(accessToken!!, refreshToken!!))
                                return
                            }
                        }

                        emitter.emitErrorIfNotDisposed(UnexpectedResponseException(response.code()))
                    } finally {
                        api.cookieStorage.reset()
                    }
                }
            })
    }

    fun refreshAccessToken(refreshToken: String): Observable<RequestResult> {
        api.cookieStorage.add(RIDI_RT_COOKIE_NAME, refreshToken)
        return Observable.create { emitter ->
            api.service.refreshAccessToken()
                .enqueue(object : Callback<ResponseBody> {
                    override fun onFailure(call: Call<ResponseBody>, t: Throwable?) {
                        api.cookieStorage.reset()
                        emitter.emitErrorIfNotDisposed(IllegalStateException(t))
                    }

                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        try {
                            if (response.isSuccessful) {
                                var newAccessToken: String? = null
                                var newRefreshToken: String? = null
                                api.cookieStorage.savedCookies.forEach { cookie ->
                                    when (cookie.name()) {
                                        RIDI_AT_COOKIE_NAME -> newAccessToken = cookie.value()
                                        RIDI_RT_COOKIE_NAME -> newRefreshToken = cookie.value()
                                    }
                                }

                                if (newAccessToken != null && newRefreshToken != null) {
                                    emitter.emitItemAndCompleteIfNotDisposed(
                                        RequestResult(newAccessToken!!, newRefreshToken!!)
                                    )
                                    return
                                }
                            }

                            emitter.emitErrorIfNotDisposed(UnexpectedResponseException(response.code()))
                        } finally {
                            api.cookieStorage.reset()
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
