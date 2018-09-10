package com.ridi.oauth2

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.io.File

internal class CookieStorage : CookieJar {
    var phpSessionId = ""
    lateinit var tokenFile: File
    var tokenEncryptionKey: String? = null
    var refreshToken = ""

    val savedCookies = mutableListOf<Cookie>()

    override fun loadForRequest(url: HttpUrl) =
        savedCookies + listOf(
            Cookie.parse(url, "$PHP_SESSION_ID_COOKIE_NAME=$phpSessionId")!!,
            Cookie.parse(url, "${Authorization.COOKIE_NAME_RIDI_RT}=$refreshToken")
        )

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        savedCookies.addAll(cookies)
    }

    fun clear() {
        savedCookies.clear()
    }

    companion object {
        private const val PHP_SESSION_ID_COOKIE_NAME = "PHPSESSID"
    }
}
