package com.ridi.oauth2

import android.util.Base64
import com.ridi.books.helper.io.loadObject
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.android.schedulers.AndroidSchedulers
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileNotFoundException
import java.net.MalformedURLException
import java.security.InvalidParameterException
import java.util.Calendar

data class JWT(var subject: String, var userIndex: Int?, var expiresAt: Int)

class RidiOAuth2 {
    companion object {
        private const val DEV_HOST = "account.dev.ridi.io/"
        private const val REAL_HOST = "account.ridibooks.com/"
        internal var BASE_URL = "https://$REAL_HOST"

        internal const val STATUS_CODE_REDIRECT = 302
        internal const val COOKIE_RIDI_AT = "ridi-at"
        internal const val COOKIE_RIDI_RT = "ridi-rt"

        internal var cookies = HashSet<String>()
        internal fun JSONObject.parseCookie(cookieString: String) {
            val cookie = cookieString.split("=", ";")
            if (cookie[0] == COOKIE_RIDI_AT || cookie[0] == COOKIE_RIDI_RT) {
                put(cookie[0], cookie[1])
            }
        }
    }

    var clientId: String? = null
    var tokenFile: File? = null

    private var refreshToken: String? = null
    private var rawAccessToken: String? = null
    private var parsedAccessToken: JWT? = null

    fun setDevMode() {
        BASE_URL = "https://$DEV_HOST"
    }

    fun setSessionId(sessionId: String) {
        cookies = HashSet()
        cookies.add("PHPSESSID=$sessionId;")
    }

    private fun readJSONFile() = tokenFile!!.loadObject<String>() ?: throw FileNotFoundException()

    fun getAccessToken(): String {
        if (rawAccessToken == null) {
            val jsonObject = JSONObject(readJSONFile())
            if (jsonObject.has(COOKIE_RIDI_AT)) {
                rawAccessToken = jsonObject.getString(COOKIE_RIDI_AT)
            }
        }
        parsedAccessToken = parseAccessToken()
        return rawAccessToken!!
    }

    fun parseAccessToken(): JWT {
        val splitString = rawAccessToken!!.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        // splitString[0]에는 필요한 정보가 없다.
        val jsonObject = JSONObject(String(Base64.decode(splitString[1], Base64.DEFAULT)))
        return JWT(jsonObject.getString("sub"),
            jsonObject.getInt("u_idx"),
            jsonObject.getInt("exp"))
    }

    fun getRefreshToken(): String {
        if (refreshToken == null) {
            val jsonObject = JSONObject(readJSONFile())
            if (jsonObject.has(COOKIE_RIDI_RT)) {
                refreshToken = jsonObject.getString(COOKIE_RIDI_RT)
            }
        }
        return refreshToken!!
    }

    private fun isAccessTokenExpired(): Boolean {
        getAccessToken()
        return parsedAccessToken!!.expiresAt < Calendar.getInstance().timeInMillis / 1000
    }

    fun getOAuthToken(redirectUri: String): Observable<JWT> {
        val manager = ApiManager()
        manager.cookieInterceptor.tokenFile = tokenFile
        return Observable.create(ObservableOnSubscribe<JWT> { emitter ->
            if (tokenFile == null) {
                emitter.onError(FileNotFoundException())
                emitter.onComplete()
            } else if (clientId == null) {
                emitter.onError(IllegalStateException())
                emitter.onComplete()
            } else if (tokenFile!!.exists().not()) {
                manager.service.requestAuthorization(clientId!!, "code", redirectUri)
                    .enqueue(object : Callback<ResponseBody> {
                        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                            emitter.onError(t)
                            emitter.onComplete()
                        }

                        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                            if (response.code() == STATUS_CODE_REDIRECT) {
                                val redirectLocation = response.headers().values("Location")[0]
                                if (redirectLocation == redirectUri) {
                                    // 토큰은 이미 ApiManager 내의 CookieInterceptor에서 tokenFile에 저장된 상태이다.
                                    getAccessToken()
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
            } else {
                if (isAccessTokenExpired()) {
                    manager.service.refreshAccessToken(getAccessToken(), getRefreshToken())
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
                } else {
                    emitter.onNext(parsedAccessToken!!)
                    emitter.onComplete()
                }
            }
        }).subscribeOn(AndroidSchedulers.mainThread())
    }
}
