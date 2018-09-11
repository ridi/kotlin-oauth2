package com.ridi.oauth2.cookie

import com.ridi.oauth2.TokenPair
import com.ridi.oauth2.UnexpectedResponseException
import io.reactivex.SingleEmitter
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

internal abstract class CookieTokenExtractor(
    private val cookieStorage: CookieStorage,
    private val emitter: SingleEmitter<TokenPair>
) : Callback<ResponseBody> {
    private var extractedTokenPair: TokenPair? = null

    abstract fun isExtractionAvailable(response: Response<ResponseBody>): Boolean

    final override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
        cookieStorage.reset()
        emitter.tryOnError(t)
    }

    final override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
        var accessToken: String? = null
        var refreshToken: String? = null

        if (isExtractionAvailable(response)) {
            cookieStorage.savedCookies.forEach { cookie ->
                when (cookie.name()) {
                    RIDI_AT_COOKIE_NAME -> accessToken = cookie.value()
                    RIDI_RT_COOKIE_NAME -> refreshToken = cookie.value()
                }
            }
        }

        if (accessToken != null && refreshToken != null) {
            emitter.onSuccess(TokenPair(accessToken!!, refreshToken!!))
        } else {
            emitter.tryOnError(UnexpectedResponseException(response.code()))
        }
        cookieStorage.reset()
    }

    companion object {
        internal const val RIDI_AT_COOKIE_NAME = "ridi-at"
        internal const val RIDI_RT_COOKIE_NAME = "ridi-rt"
    }
}
