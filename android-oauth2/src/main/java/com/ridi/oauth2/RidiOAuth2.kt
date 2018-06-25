package com.ridi.oauth2

import com.auth0.android.jwt.JWT
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

class RidiOAuth2 {
    private var clientId = ""
    private var manager = ApiManager

    companion object {
        private const val DEV_HOST = "account.dev.ridi.io/"
        private const val REAL_HOST = "account.ridibooks.com/"
        internal var BASE_URL = "https://$REAL_HOST"

        var instance = RidiOAuth2()

        internal lateinit var tokenFile: File
        internal fun JSONObject.parseCookie(cookieString: String) {
            val cookie = cookieString.split("=", ";")
            if (cookie[0] == "ridi-at" || cookie[0] == "ridi-rt") {
                put(cookie[0], cookie[1])
            }
        }
    }

    private var refreshToken = ""
    private var rawAccessToken = ""
    private lateinit var parsedAccessToken: JWT

    fun setDev() {
        BASE_URL = "https://$DEV_HOST"
    }

    fun setSessionId(sessionId: String) {
        manager.cookies = HashSet()
        manager.cookies.add("PHPSESSID=$sessionId;")
    }

    fun setClientId(clientId: String) {
        this.clientId = clientId
    }

    fun setTokenFilePath(path: String) {
        tokenFile = File(path)
    }

    private fun readJSONFile() = tokenFile.loadObject<String>() ?: ""

    fun getAccessToken(): String {
        if (rawAccessToken == "") {
            rawAccessToken = JSONObject(readJSONFile()).getString("ridi-at")
        }
        parsedAccessToken = JWT(rawAccessToken)
        return rawAccessToken
    }

    fun getRefreshToken(): String {
        if (refreshToken == "") {
            refreshToken = JSONObject(readJSONFile()).getString("ridi-rt")
        }
        return refreshToken
    }

    private fun isAccessTokenExpired(): Boolean {
        getAccessToken()
        return parsedAccessToken.isExpired(0)
    }

    fun getOAuthToken(redirectUri: String): Observable<JWT> {
        if (tokenFile.exists().not()) {
            return if (clientId == "") {
                Observable.create(ObservableOnSubscribe<JWT> { emitter ->
                    emitter.onError(IllegalStateException())
                    emitter.onComplete()
                }).subscribeOn(AndroidSchedulers.mainThread())
            } else {
                Observable.create(ObservableOnSubscribe<JWT> { emitter ->
                    manager.create().requestAuthorization(clientId, "code", redirectUri)
                        .enqueue(object : Callback<ResponseBody> {
                            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                emitter.onError(t)
                                emitter.onComplete()
                            }

                            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                if (response.code() == 302) {
                                    val redirectLocation = response.headers().values("Location")[0]
                                    if (redirectLocation == redirectUri) {
                                        // 토큰은 이미 ApiManager 내의 CookieInterceptor에서 tokenFile에 저장된 상태이다.
                                        getAccessToken()
                                        emitter.onNext(parsedAccessToken)
                                    } else {
                                        emitter.onError(IllegalAccessException())
                                    }
                                } else {
                                    emitter.onError(Throwable("Status code Error ${response.code()}"))
                                }
                                emitter.onComplete()
                            }
                        })
                }).subscribeOn(AndroidSchedulers.mainThread())
            }
        } else {
            return if (isAccessTokenExpired()) {
                Observable.create(ObservableOnSubscribe<JWT> { emitter ->
                    manager.create().refreshAccessToken(getAccessToken(), getRefreshToken())
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
                }).subscribeOn(AndroidSchedulers.mainThread())
            } else {
                Observable.just(parsedAccessToken)
                    .subscribeOn(AndroidSchedulers.mainThread())
            }
        }
    }
}
