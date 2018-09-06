package com.ridi.oauth2

import com.ridi.books.helper.io.saveToFile
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import org.json.JSONObject
import java.io.File

internal class CookieStorage : CookieJar {
    var phpSessionId = ""
    lateinit var tokenFile: File
    var tokenEncryptionKey: String? = null

    private val savedCookies = mutableListOf<Cookie>()

    override fun loadForRequest(url: HttpUrl) =
        savedCookies + listOf(Cookie.parse(url, "$PHP_SESSION_ID_COOKIE_NAME=$phpSessionId")!!)

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        savedCookies.addAll(cookies)

        val tokenJSON = JSONObject()
        cookies.forEach { cookie ->
            TokenManager.run {
                tokenJSON.addTokensFromCookie(cookie)
            }
        }

        if (tokenJSON.has(TokenManager.COOKIE_NAME_RIDI_AT) && tokenJSON.has(TokenManager.COOKIE_NAME_RIDI_RT)) {
            tokenJSON.toString().encodeWithAES128(tokenEncryptionKey).saveToFile(tokenFile)
        }
    }

    fun clear() {
        savedCookies.clear()
    }

    companion object {
        private const val PHP_SESSION_ID_COOKIE_NAME = "PHPSESSID"
    }
}
