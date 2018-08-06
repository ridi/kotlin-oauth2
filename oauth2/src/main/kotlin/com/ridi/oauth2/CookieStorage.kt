package com.ridi.oauth2

import android.webkit.CookieManager
import com.ridi.books.helper.io.saveToFile
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import org.json.JSONObject
import java.io.File

internal class CookieStorage : CookieJar {
    var tokenFile: File? = null
    var tokenEncryptionKey: String? = null
    private val cookieManager = CookieManager.getInstance()

    override fun loadForRequest(url: HttpUrl): MutableList<Cookie> {
        val cookies = ArrayList<Cookie>()
        if (cookieManager.getCookie(url.toString()) != null) {
            val splitCookies = cookieManager.getCookie(url.toString()).split("[,;]".toRegex())
                .dropLastWhile { it.isEmpty() }.toTypedArray()
            splitCookies.forEach {
                cookies.add(Cookie.parse(url, it.trim())!!)
            }
        }
        return cookies
    }

    override fun saveFromResponse(url: HttpUrl?, cookies: MutableList<Cookie>) {
        val tokenJSON = JSONObject()
        cookies.forEach { cookie ->
            cookieManager.setCookie(url.toString(), cookie.toString())
            TokenManager.run {
                tokenJSON.parseCookie(cookie.toString())
            }
        }
        if (tokenJSON.has(TokenManager.COOKIE_KEY_RIDI_AT) && tokenJSON.has(TokenManager.COOKIE_KEY_RIDI_RT)) {
            tokenJSON.toString().encodeWithAES256(tokenEncryptionKey).saveToFile(tokenFile!!)
        }
    }

    fun removeCookiesInUrl(url: String) {
        cookieManager.setCookie(url, "")
    }
}
