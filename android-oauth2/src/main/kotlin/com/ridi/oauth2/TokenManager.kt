package com.ridi.oauth2

import android.util.Base64
import com.ridi.books.helper.io.loadObject
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import io.reactivex.android.schedulers.AndroidSchedulers
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileNotFoundException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.security.InvalidParameterException
import java.util.Calendar

data class JWT(var subject: String, var userIndex: Int?, var expiresAt: Int)

class TokenManager {
    companion object {
        private const val DEV_HOST = "account.dev.ridi.io/"
        private const val REAL_HOST = "account.ridibooks.com/"
        internal var BASE_URL = "https://$REAL_HOST"

        internal const val COOKIE_KEY_RIDI_AT = "ridi-at"
        internal const val COOKIE_KEY_RIDI_RT = "ridi-rt"

        internal fun JSONObject.parseCookie(cookieString: String) {
            val cookie = cookieString.split("=", ";")
            val cookieKey = cookie[0]
            val cookieValue = cookie[1]
            if (cookieKey == COOKIE_KEY_RIDI_AT || cookieKey == COOKIE_KEY_RIDI_RT) {
                put(cookieKey, cookieValue)
            }
        }
    }

    private var manager = ApiManager()

    var clientId: String? = null
    var tokenFile: File? = null
        set(value) {
            field = value
            manager.cookieInterceptor.tokenFile = value
        }

    var useDevMode: Boolean = false
        set(value) {
            BASE_URL = "https://" + if (value) DEV_HOST else REAL_HOST
        }

    fun setSessionId(sessionId: String) {
        clearSavedTokens()
        manager.cookieInterceptor.cookies = HashSet()
        manager.cookieInterceptor.cookies.add("PHPSESSID=$sessionId;")
    }

    private fun clearSavedTokens() {
        rawAccessToken = null
        refreshToken = null
    }

    private fun readJSONFile() = tokenFile!!.loadObject<String>() ?: throw FileNotFoundException()

    private fun getSavedJSON() = JSONObject(readJSONFile())

    private var rawAccessToken: String? = null
        get() {
            if (field == null) {
                field = getSavedJSON().getString(COOKIE_KEY_RIDI_AT)
            }
            return field
        }

    private var refreshToken: String? = null
        get() {
            if (field == null) {
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

    private fun parseAccessToken(): JWT {
        val splitString = rawAccessToken!!.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        // splitString[0]에는 필요한 정보가 없다.
        val jsonObject = JSONObject(String(Base64.decode(splitString[1], Base64.DEFAULT)))
        return JWT(jsonObject.getString("sub"),
            jsonObject.getInt("u_idx"),
            jsonObject.getInt("exp"))
    }

    private fun isAccessTokenExpired(): Boolean =
        parsedAccessToken!!.expiresAt < Calendar.getInstance().timeInMillis / 1000

    fun getAccessToken(redirectUri: String): Observable<JWT> {
        return Observable.create(ObservableOnSubscribe<JWT> { emitter ->
            if (tokenFile == null || clientId == null) {
                emitter.onError(IllegalStateException())
                emitter.onComplete()
            } else if (tokenFile!!.exists().not()) {
                requestAuthorization(emitter, redirectUri)
            } else {
                if (isAccessTokenExpired()) {
                    refreshAccessToken(emitter)
                } else {
                    emitter.onNext(parsedAccessToken!!)
                    emitter.onComplete()
                }
            }
        }).subscribeOn(AndroidSchedulers.mainThread())
    }

    private fun requestAuthorization(emitter: ObservableEmitter<JWT>, redirectUri: String) {
        manager.service!!.requestAuthorization(clientId!!, "code", redirectUri)
            .enqueue(object : Callback<ResponseBody> {
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    emitter.onError(t)
                    emitter.onComplete()
                }

                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.code() == HttpURLConnection.HTTP_MOVED_TEMP) {
                        val redirectLocation = response.headers().values("Location")[0]
                        if (redirectLocation == redirectUri) {
                            // 토큰은 이미 ApiManager 내의 CookieInterceptor에서 tokenFile에 저장된 상태이다.
                            emitter.onNext(parsedAccessToken)
                        } else {
                            emitter.onError(MalformedURLException())
                        }
                    } else {
                        emitter.onError(InvalidParameterException("${response.code()}"))
                    }
                    emitter.onComplete()
                }
            })
    }

    private fun refreshAccessToken(emitter: ObservableEmitter<JWT>) {
        clearSavedTokens()
        return manager.service!!.refreshAccessToken(rawAccessToken!!, refreshToken!!)
            .enqueue(object : Callback<ResponseBody> {
                override fun onFailure(call: Call<ResponseBody>, t: Throwable?) {
                    emitter.onError(IllegalStateException())
                    emitter.onComplete()
                }

                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    emitter.onNext(parsedAccessToken)
                    emitter.onComplete()
                }
            })
    }
}
