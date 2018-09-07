package com.ridi.oauth2

import android.support.annotation.VisibleForTesting
import android.util.Base64
import com.ridi.books.helper.io.loadObject
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import okhttp3.Cookie
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.net.HttpURLConnection
import java.util.Date

data class JWT(val subject: String, val userIndex: Int, val expiresAt: Date)

class UnexpectedResponseException(val responseCode: Int, val redirectedToUrl: String) : RuntimeException()

class InvalidTokenFileException : RuntimeException()

class InvalidTokenEncryptionKeyException(message: String) : RuntimeException(message)

class TokenManager {
    companion object {
        private const val DEV_HOST = "account.dev.ridi.io"
        private const val REAL_HOST = "account.ridibooks.com"
        private const val SECONDS_TO_MILLISECONDS = 1000
        internal const val AUTHORIZATION_REDIRECT_URI = "app://authorized"

        internal const val COOKIE_NAME_RIDI_AT = "ridi-at"
        internal const val COOKIE_NAME_RIDI_RT = "ridi-rt"

        internal fun JSONObject.addTokensFromCookie(cookie: Cookie) {
            val name = cookie.name()
            if (name == COOKIE_NAME_RIDI_AT || name == COOKIE_NAME_RIDI_RT) {
                put(name, cookie.value())
            }
        }
    }

    private val clientId: String
    private val tokenSaveFile: File
    private val tokenSaveEncryptionKey: String?
    private val apiManager: ApiManager

    constructor(
        clientId: String,
        tokenSaveFile: File,
        tokenSaveEncryptionKey: String? = null,
        devMode: Boolean = false
    ) : this("https://${if (devMode) DEV_HOST else REAL_HOST}/", clientId, tokenSaveFile, tokenSaveEncryptionKey)

    @VisibleForTesting
    internal constructor(
        baseUrl: String,
        clientId: String,
        tokenSaveFile: File,
        tokenSaveEncryptionKey: String? = null
    ) {
        this.clientId = clientId
        this.tokenSaveFile = tokenSaveFile
        this.tokenSaveEncryptionKey = tokenSaveEncryptionKey
        apiManager = ApiManager(baseUrl).apply {
            cookieStorage.tokenFile = tokenSaveFile
            cookieStorage.tokenEncryptionKey = tokenSaveEncryptionKey
        }
    }

    var phpSessionId = ""
        set(value) {
            apiManager.cookieStorage.phpSessionId = value
            clearTokens()
        }

    private fun clearTokens(isDeletingTokenFileNeeded: Boolean = true) {
        rawAccessToken = null
        refreshToken = null
        parsedAccessToken = null
        if (tokenSaveFile.exists() && isDeletingTokenFileNeeded) {
            tokenSaveFile.delete()
        }
    }

    private fun getSavedJSON(): JSONObject {
        tokenSaveFile.loadObject<String>()?.let { tokenString ->
            return JSONObject(tokenString.decodeWithAES128(tokenSaveEncryptionKey))
        } ?: throw InvalidTokenFileException()
    }

    private var rawAccessToken: String? = null
        get() {
            if (field == null && getSavedJSON().has(COOKIE_NAME_RIDI_AT)) {
                field = getSavedJSON().getString(COOKIE_NAME_RIDI_AT)
            }
            return field
        }

    private var refreshToken: String? = null
        get() {
            if (field == null && getSavedJSON().has(COOKIE_NAME_RIDI_RT)) {
                field = getSavedJSON().getString(COOKIE_NAME_RIDI_RT)
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
            Date(jsonObject.getLong("exp") * SECONDS_TO_MILLISECONDS))
    }

    private fun isTokenEncryptionKeyValid() = tokenSaveEncryptionKey?.run {
        toByteArray(Charsets.UTF_8).count() == 16
    } != false

    private fun isAccessTokenExpired() = parsedAccessToken!!.expiresAt.before(Date())

    fun getAccessToken(): Observable<JWT> {
        return Observable.create { emitter ->
            when {
                isTokenEncryptionKeyValid().not() ->
                    emitter.emitErrorIfNotDisposed(
                        InvalidTokenEncryptionKeyException("Unsupported key size. 16 Bytes are required"))
                tokenSaveFile.exists().not() -> requestAuthorization(emitter)
                isAccessTokenExpired() -> refreshAccessToken(emitter)
                else -> emitter.emitItemAndCompleteIfNotDisposed(parsedAccessToken!!)
            }
        }
    }

    private fun requestAuthorization(emitter: ObservableEmitter<JWT>) {
        apiManager.service.requestAuthorization(clientId, "code", AUTHORIZATION_REDIRECT_URI)
            .enqueue(object : Callback<ResponseBody> {
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    apiManager.cookieStorage.clear()
                    emitter.emitErrorIfNotDisposed(t)
                }

                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    apiManager.cookieStorage.clear()

                    if (emitter.isDisposed.not() && response.code() == HttpURLConnection.HTTP_MOVED_TEMP &&
                        response.headers().values("Location").firstOrNull() == AUTHORIZATION_REDIRECT_URI) {
                        emitter.emitItemAndCompleteIfNotDisposed(parsedAccessToken!!)
                    } else {
                        emitter.emitErrorIfNotDisposed(UnexpectedResponseException(response.code(),
                            response.raw().request().url().toString()))
                    }
                }
            })
    }

    private fun refreshAccessToken(emitter: ObservableEmitter<JWT>) {
        apiManager.service.refreshAccessToken(rawAccessToken!!, refreshToken!!)
            .enqueue(object : Callback<ResponseBody> {
                override fun onFailure(call: Call<ResponseBody>, t: Throwable?) {
                    apiManager.cookieStorage.clear()
                    emitter.emitErrorIfNotDisposed(IllegalStateException(t))
                }

                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    apiManager.cookieStorage.clear()
                    clearTokens(false)
                    emitter.emitItemAndCompleteIfNotDisposed(parsedAccessToken!!)
                }
            })
    }

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
}
