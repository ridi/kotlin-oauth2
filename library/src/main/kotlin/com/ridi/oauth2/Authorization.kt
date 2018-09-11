package com.ridi.oauth2

import android.support.annotation.CallSuper
import android.support.annotation.VisibleForTesting
import io.reactivex.Single
import io.reactivex.SingleEmitter
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

    abstract inner class CookieTokenExtractor(
        private val emitter: SingleEmitter<RequestResult>
    ) : Callback<ResponseBody> {
        private var extractedResult: RequestResult? = null

        @CallSuper
        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
            api.cookieStorage.reset()
            emitter.tryOnError(t)
        }

        @CallSuper
        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
            extractedResult?.let {
                emitter.onSuccess(it)
            } ?: emitter.tryOnError(UnexpectedResponseException(response.code()))
            api.cookieStorage.reset()
        }

        protected fun extractTokens() {
            var accessToken: String? = null
            var refreshToken: String? = null

            api.cookieStorage.savedCookies.forEach { cookie ->
                when (cookie.name()) {
                    RIDI_AT_COOKIE_NAME -> accessToken = cookie.value()
                    RIDI_RT_COOKIE_NAME -> refreshToken = cookie.value()
                }
            }

            if (accessToken != null && refreshToken != null) {
                extractedResult = RequestResult(accessToken!!, refreshToken!!)
            }
        }
    }

    fun requestRidiAuthorization(phpSessionId: String): Single<RequestResult> = Single.create { emitter ->
        api.cookieStorage.add(PHP_SESSION_ID_COOKIE_NAME, phpSessionId)
        api.service.requestAuthorization(clientId, "code", AUTHORIZATION_REDIRECT_URI)
            .enqueue(object : CookieTokenExtractor(emitter) {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.code() == HttpURLConnection.HTTP_MOVED_TEMP &&
                        response.headers().values("Location").firstOrNull() == AUTHORIZATION_REDIRECT_URI) {
                        extractTokens()
                    }
                    super.onResponse(call, response)
                }
            })
    }

    fun refreshAccessToken(refreshToken: String): Single<RequestResult> {
        api.cookieStorage.add(RIDI_RT_COOKIE_NAME, refreshToken)
        return Single.create { emitter ->
            api.service.refreshAccessToken()
                .enqueue(object : CookieTokenExtractor(emitter) {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        if (response.isSuccessful) {
                            extractTokens()
                        }
                        super.onResponse(call, response)
                    }
                })
        }
    }
}
