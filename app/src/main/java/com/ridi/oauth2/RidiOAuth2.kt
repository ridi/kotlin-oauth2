package com.ridi.oauth2

import android.os.Environment
import android.util.Log
import org.json.JSONObject
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
            Log.e("Write tokenJSONFILE", tokenJSON.toString())
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
}