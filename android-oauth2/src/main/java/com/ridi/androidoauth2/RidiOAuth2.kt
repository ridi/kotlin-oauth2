package com.ridi.androidoauth2

import android.os.Environment
import android.util.Base64
import io.reactivex.Observable
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.util.Calendar

object RidiOAuth2 {
    private var clientId = ""
    private val file = File(Environment.getExternalStorageDirectory().absolutePath + "/tokenJSON.json")
    private var accessTokenHeader = ""
    private var accessTokenBody = ""

    var cookies = HashSet<String>()

    fun setClientId(clientId: String) {
        RidiOAuth2.clientId = clientId
    }

    fun getClientId(): String {
        return clientId
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

    fun parseAccessToken() {
        val splitString = getAccessToken().split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val base64EncodedHeader = splitString[0]
        val base64EncodedBody = splitString[1]
        accessTokenHeader = String(Base64.decode(base64EncodedHeader, Base64.DEFAULT))
        accessTokenBody = String(Base64.decode(base64EncodedBody, Base64.DEFAULT))
    }

    fun getJSONObject(jsonString: String): JSONObject {
        var jsonObject = JSONObject()
        try {
            jsonObject = JSONObject(jsonString)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return jsonObject
    }

    fun isAccessTokenExpired(): Boolean {
        parseAccessToken()
        val body = getJSONObject(accessTokenBody)
        return body.getString("exp").toInt() < Calendar.getInstance().timeInMillis / 1000
    }

    fun getOAuthToken(redirectUri: String): Observable<String> {
        val service = OAuth2Service.create()
        if (file.exists().not()) {
            if (clientId == "") {
                return Observable.create {
                    it.onError(Throwable("Client Id not initialized"))
                    it.onComplete()
                }
            }
            return Observable.create {
                service.ridiAuthorize(clientId, "code", redirectUri).enqueue(object : Callback<ResponseBody> {
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
                    service.ridiToken(getAccessToken(), getRefreshToken()).enqueue(object : Callback<ResponseBody> {
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