package com.ridi.oauth2

import android.util.Base64
import com.ridi.books.helper.io.loadObject
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.net.URI
import java.util.Calendar

data class JWT(var subject: String, var userIndex: Int, var expiresAt: Int)

class UnexpectedResponseException(val responseCode: Int, val redirectedToUrl: String) : Exception()

class InvalidTokenFileException : Exception()

class InvalidTokenEncryptionKeyException(override var message: String) : Exception()

class TokenManager {
    companion object {
        private const val DEV_HOST = "account.dev.ridi.io/"
        private const val REAL_HOST = "account.ridibooks.com/"
        internal var BASE_URL = "https://$REAL_HOST"

        internal const val COOKIE_KEY_RIDI_AT = "ridi-at"
        internal const val COOKIE_KEY_RIDI_RT = "ridi-rt"

        internal fun JSONObject.addTokensFromCookie(cookieString: String) {
            val cookie = cookieString.split("=", ";")
            val cookieKey = cookie[0]
            val cookieValue = cookie[1]
            if (cookieKey == COOKIE_KEY_RIDI_AT || cookieKey == COOKIE_KEY_RIDI_RT) {
                put(cookieKey, cookieValue)
            }
        }
    }

    private var apiManager = ApiManager()

    var clientId: String? = null
        set(value) {
            field = value
            clearTokens()
        }

    var tokenFile: File? = null
        set(value) {
            clearTokens()
            field = value
            apiManager.cookieStorage.tokenFile = value
        }

    var useDevMode: Boolean = false
        set(value) {
            field = value
            clearTokens()
            BASE_URL = "https://" + if (value) DEV_HOST else REAL_HOST
            apiManager = ApiManager()
        }

    var tokenEncryptionKey: String? = null
        set(value) {
            field = value
            apiManager.cookieStorage.tokenEncryptionKey = value
            clearTokens()
        }

    var sessionId = ""
        set(value) {
            field = value
            clearTokens()
        }

    private fun clearTokens(isDeletingTokenFileNeeded: Boolean = true) {
        rawAccessToken = null
        refreshToken = null
        parsedAccessToken = null
        if (tokenFile != null && tokenFile!!.exists() && isDeletingTokenFileNeeded) {
            tokenFile!!.delete()
        }
    }

    private fun getSavedJSON(): JSONObject {
        tokenFile!!.loadObject<String>()?.let { tokenString ->
            return JSONObject(tokenString.decodeWithAES128(tokenEncryptionKey))
        } ?: throw InvalidTokenFileException()
    }

    private var rawAccessToken: String? = null
        get() {
            if (field == null && getSavedJSON().has(COOKIE_KEY_RIDI_AT)) {
                field = getSavedJSON().getString(COOKIE_KEY_RIDI_AT)
            }
            return field
        }

    private var refreshToken: String? = null
        get() {
            if (field == null && getSavedJSON().has(COOKIE_KEY_RIDI_RT)) {
                field = getSavedJSON().getString(COOKIE_KEY_RIDI_RT)
            }
            return field
        }

    private var parsedAccessToken: JWT? = null
        get() {
            if (field == null) {
                field = parseAccessToken()
            }
            return field
        }

    private fun parseAccessToken(): JWT? {
        if (rawAccessToken == null) {
            return null
        }
        val splitString = rawAccessToken!!.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
        // splitString[0]에는 필요한 정보가 없다.
        val jsonObject = JSONObject(String(Base64.decode(splitString[1], Base64.DEFAULT)))
        return JWT(jsonObject.getString("sub"),
            jsonObject.getInt("u_idx"),
            jsonObject.getInt("exp"))
    }

    private fun isTokenEncryptionKeyAvailable() = tokenEncryptionKey == null ||
        tokenEncryptionKey!!.toByteArray(Charsets.UTF_8).size == 16

    private fun isAccessTokenExpired() =
        parsedAccessToken!!.expiresAt < Calendar.getInstance().timeInMillis / 1000

    fun getAccessToken(redirectUri: String): Observable<JWT> {
        return Observable.create { emitter ->
            if (tokenFile == null || clientId == null) {
                emitter.emitErrorIfNotDisposed(IllegalStateException())
            } else if (isTokenEncryptionKeyAvailable().not()) {
                emitter.emitErrorIfNotDisposed(InvalidTokenEncryptionKeyException(
                    "Unsupported key size. 16 Bytes are required"))
            } else if (tokenFile!!.exists().not()) {
                requestAuthorization(emitter, redirectUri)
            } else if (isAccessTokenExpired()) {
                refreshAccessToken(emitter)
            } else {
                emitter.emitItemAndCompleteIfNotDisposed(parsedAccessToken!!)
            }
        }
    }

    private fun requestAuthorization(emitter: ObservableEmitter<JWT>, redirectUri: String) {
        val sessionCookie = "PHPSESSID=$sessionId;"
        apiManager.service.requestAuthorization(sessionCookie, clientId!!, "code", redirectUri)
            .enqueue(object : Callback<ResponseBody> {
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    apiManager.cookieStorage.removeCookiesInUrl(call.requestUrlString())
                    emitter.emitErrorIfNotDisposed(t)
                }

                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    apiManager.cookieStorage.removeCookiesInUrl(call.requestUrlString())
                    var currentResponse = response.raw()

                    while (currentResponse != null && emitter.isDisposed.not()) {
                        val tokenCookies = currentResponse.headers().values("Set-Cookie").filter {
                            it.startsWith(COOKIE_KEY_RIDI_AT) || it.startsWith(COOKIE_KEY_RIDI_RT)
                        }
                        if (tokenCookies.size >= 2 &&
                            currentResponse.headers().values("Location")[0].normalizedURI()
                            == redirectUri.normalizedURI()) {
                            emitter.emitItemAndCompleteIfNotDisposed(parsedAccessToken!!)
                            return
                        }
                        currentResponse = currentResponse.priorResponse()
                    }
                    emitter.emitErrorIfNotDisposed(UnexpectedResponseException(response.code(),
                        response.raw().request().url().uri().normalize().toString()))
                }
            })
    }

    private fun refreshAccessToken(emitter: ObservableEmitter<JWT>) {
        apiManager.service.refreshAccessToken(rawAccessToken!!, refreshToken!!)
            .enqueue(object : Callback<ResponseBody> {
                override fun onFailure(call: Call<ResponseBody>, t: Throwable?) {
                    apiManager.cookieStorage.removeCookiesInUrl(call.requestUrlString())
                    emitter.emitErrorIfNotDisposed(IllegalStateException(t))
                }

                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    apiManager.cookieStorage.removeCookiesInUrl(call.requestUrlString())
                    clearTokens(false)
                    emitter.emitItemAndCompleteIfNotDisposed(parsedAccessToken!!)
                }
            })
    }

    private fun Call<ResponseBody>.requestUrlString() = request().url().toString()

    private fun ObservableEmitter<JWT>.emitErrorIfNotDisposed(t: Throwable) {
        if (isDisposed.not()) {
            onError(t)
        }
    }

    private fun ObservableEmitter<JWT>.emitItemAndCompleteIfNotDisposed(item: JWT) {
        if (isDisposed.not()) {
            onNext(item)
            onComplete()
        }
    }

    private fun String.normalizedURI() = URI(this).normalize()
}
