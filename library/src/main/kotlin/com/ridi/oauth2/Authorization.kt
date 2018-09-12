package com.ridi.oauth2

import com.ridi.oauth2.cookie.CookieTokenExtractor
import io.reactivex.Single
import okhttp3.ResponseBody
import retrofit2.Response
import java.net.HttpURLConnection

class Authorization {
    companion object {
        private const val DEV_HOST = "account.dev.ridi.io"
        private const val REAL_HOST = "account.ridibooks.com"

        private const val PHP_SESSION_ID_COOKIE_NAME = "PHPSESSID"

        internal const val AUTHORIZATION_REDIRECT_URI = "app://authorized"
    }

    private val clientId: String
    private val api: Api

    constructor(
        clientId: String,
        devMode: Boolean = false
    ) : this("https://${if (devMode) DEV_HOST else REAL_HOST}/", clientId)

    // For Test
    internal constructor(baseUrl: String, clientId: String) {
        this.clientId = clientId
        api = Api(baseUrl)
    }

    fun requestRidiAuthorization(phpSessionId: String): Single<TokenPair> = Single.create { emitter ->
        api.cookieStorage.add(PHP_SESSION_ID_COOKIE_NAME, phpSessionId)
        api.service.requestAuthorization(clientId, "code", AUTHORIZATION_REDIRECT_URI)
            .enqueue(object : CookieTokenExtractor(api.cookieStorage, emitter) {
                override fun isExtractionAvailable(response: Response<ResponseBody>) =
                    response.code() == HttpURLConnection.HTTP_MOVED_TEMP &&
                        response.headers().values("Location").firstOrNull() == AUTHORIZATION_REDIRECT_URI
            })
    }

    fun refreshAccessToken(refreshToken: String): Single<TokenPair> {
        api.cookieStorage.add(CookieTokenExtractor.RIDI_RT_COOKIE_NAME, refreshToken)
        return Single.create { emitter ->
            api.service.refreshAccessToken()
                .enqueue(object : CookieTokenExtractor(api.cookieStorage, emitter) {
                    override fun isExtractionAvailable(response: Response<ResponseBody>) = response.isSuccessful
                })
        }
    }
}
