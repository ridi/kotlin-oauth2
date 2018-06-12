package com.ridi.oauth2

import android.os.Environment
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

    fun setClientId(clientId: String) {
        this.clientId = clientId
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

    fun getAuthToken(): String {
        val tokenJSON = JSONObject(readJSONFile())
        return tokenJSON.getString("ridi-at")
    }

    fun getRefreshToken(): String {
        val tokenJSON = JSONObject(readJSONFile())
        return tokenJSON.getString("ridi-rt")
    }

    fun isTokenExpired(): Boolean {
        val authToken = getAuthToken()

        return true
    }

    fun getOAuthToken(redirectUri: String): Observable<String> {
//        if (file.exists().not()) {
        val service = OAuth2Service.create()
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
                            it.onNext(getAuthToken())
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
    }
}