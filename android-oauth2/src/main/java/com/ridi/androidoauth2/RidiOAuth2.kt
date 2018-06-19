package com.ridi.androidoauth2

import android.os.Environment
import com.auth0.android.jwt.JWT
import io.reactivex.Observable
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

object RidiOAuth2 {
    private var clientId = ""
    private val file = File(Environment.getExternalStorageDirectory().absolutePath + "/tokenJSON.json")
    private lateinit var jwt: JWT

    var cookies = HashSet<String>()

    fun setClientId(clientId: String) {
        RidiOAuth2.clientId = clientId
    }

    fun saveJSONFile(tokenJSON: JSONObject) {
        if (file.exists()) {
            file.delete()
        }
        file.createNewFile()
        file.printWriter().use { out ->
            out.println(tokenJSON)
        }
    }

    fun readJSONFile(): String {
        return file.bufferedReader().use {
            it.readText()
        }
    }

    fun getAccessToken(): String {
        val tokenJSON = JSONObject(readJSONFile())
        return tokenJSON.getString("ridi-at")
    }

    fun getRefreshToken(): String {
        val tokenJSON = JSONObject(readJSONFile())
        return tokenJSON.getString("ridi-rt")
    }

    fun isAccessTokenExpired(): Boolean {
        jwt = JWT(getAccessToken())
        return jwt.isExpired(0)
    }

    fun getOAuthToken(redirectUri: String): Observable<String> {
        val manager = ApiManager.create()
        if (file.exists().not()) {
            if (clientId == "") {
                return Observable.create {
                    it.onError(Throwable("Client Id not initialized"))
                    it.onComplete()
                }
            }
            return Observable.create {
                manager.ridiAuthorize(clientId, "code", redirectUri).enqueue(object : Callback<ResponseBody> {
                    override fun onFailure(call: Call<ResponseBody>?, t: Throwable) {
                        it.onError(Throwable("API calls fail"))
                        it.onComplete()
                    }

                    override fun onResponse(call: Call<ResponseBody>?, response: Response<ResponseBody>) {
                        if (response.code() == 302) {
                            if (response.headers().get("Location") == redirectUri) {
                                it.onNext(getAccessToken())
                            } else {
                                it.onError(Throwable(response.headers().get("Location")))
                            }
                        } else {
                            it.onError(Throwable("Status code Error ${response.code()}"))
                        }
                        it.onComplete()
                    }
                })
            }
        } else {
            if (isAccessTokenExpired().not()) {
                return Observable.just(getAccessToken())
            } else {
                return Observable.create {
                    manager.ridiToken(getAccessToken(), getRefreshToken()).enqueue(object : Callback<ResponseBody> {
                        override fun onFailure(call: Call<ResponseBody>, t: Throwable?) {
                            it.onError(Throwable("API calls fail"))
                            it.onComplete()
                        }

                        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                            it.onNext(getAccessToken())
                            it.onComplete()
                        }
                    })
                }
            }
        }
    }
}
