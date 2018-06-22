package com.ridi.androidoauth2

import com.auth0.android.jwt.JWT
import com.ridi.books.helper.io.loadObject
import io.reactivex.Observable
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

object RidiOAuth2 {
    private var clientId = ""

    private var refreshToken = ""
    private var rawAccessToken = ""
    private lateinit var parsedAccessToken: JWT
    internal lateinit var tokenFile: File

    internal var cookies = HashSet<String>()

    private const val DEV_HOST = "account.dev.ridi.io/"
    private const val REAL_HOST = "account.ridibooks.com/"
    internal var BASE_URL = "https://$REAL_HOST"

    fun setDev() {
        BASE_URL = "https://$DEV_HOST"
    }

    fun setSessionId(sessionId: String) {
        RidiOAuth2.cookies = HashSet()
        RidiOAuth2.cookies.add("PHPSESSID=$sessionId;")
    }

    fun setClientId(clientId: String) {
        RidiOAuth2.clientId = clientId
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
        val manager = ApiManager.create()

        if (tokenFile.exists().not()) {
            return if (clientId == "") {
                Observable.create { emitter ->
                    emitter.onError(IllegalStateException())
                    emitter.onComplete()
                }
            } else {
                Observable.create { emitter ->
                    manager.requestAuthorization(clientId, "code", redirectUri)
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
                }
            }
        } else {
            return if (isAccessTokenExpired()) {
                Observable.create { emitter ->
                    manager.refreshAccessToken(getAccessToken(), getRefreshToken())
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
            } else {
                Observable.just(parsedAccessToken)
            }
        }
    }

    fun parseCookie(cookieString: String): JSONObject {
        val jsonObject = JSONObject()
        val cookie = cookieString.split("=", ";")
        if (cookie[0] == "ridi-at" || cookie[0] == "ridi-rt") {
            jsonObject.put(cookie[0], cookie[1])
        }
        return jsonObject
    }
}
