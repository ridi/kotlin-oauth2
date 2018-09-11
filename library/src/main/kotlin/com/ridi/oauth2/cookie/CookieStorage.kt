package com.ridi.oauth2.cookie

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

internal class CookieStorage : CookieJar {
    private val _savedCookies = mutableListOf<Cookie>()
    val savedCookies: List<Cookie>
        get() = _savedCookies
    private val additionalCookies = mutableMapOf<String, String>()

    fun add(name: String, value: String) {
        additionalCookies[name] = value
    }

    override fun loadForRequest(url: HttpUrl) =
        savedCookies + additionalCookies.map { Cookie.parse(url, "${it.key}=${it.value}") }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        _savedCookies.addAll(cookies)
    }

    fun reset() {
        _savedCookies.clear()
        additionalCookies.clear()
    }
}
