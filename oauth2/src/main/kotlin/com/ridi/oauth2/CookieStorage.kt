package com.ridi.oauth2

import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class CookieStorage: CookieJar {
    override fun loadForRequest(url: HttpUrl): MutableList<Cookie> {
        val cookieManager = CookieManager.getInstance()
        val cookies = ArrayList<Cookie>()
        if (cookieManager.getCookie(url.toString()) != null) {
            val splitCookies = cookieManager.getCookie(url.toString()).split("[,;]".toRegex())
                .dropLastWhile { it.isEmpty() }.toTypedArray()
            splitCookies.indices.forEach {
                cookies.add(Cookie.parse(url, splitCookies[it].trim { it <= ' ' })!!)
            }
        }
        return cookies
    }

    override fun saveFromResponse(url: HttpUrl?, cookies: MutableList<Cookie>) {
        val cookieManager = CookieManager.getInstance()
        cookies.forEach { cookie ->
            cookieManager.setCookie(url.toString(), cookie.toString())
        }
    }
}
