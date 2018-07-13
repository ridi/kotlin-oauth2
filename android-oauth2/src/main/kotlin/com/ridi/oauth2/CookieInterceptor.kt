package com.ridi.oauth2

import com.ridi.books.helper.io.saveToFile
import okhttp3.Interceptor
import okhttp3.Response
import org.json.JSONObject
import java.io.File

class CookieInterceptor : Interceptor {
    var tokenFile: File? = null
    var cookies = HashSet<String>()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val builder = originalRequest.newBuilder().apply {
            cookies.forEach {
                addHeader("Cookie", it)
            }
        }

        val response = chain.proceed(builder.build())
        val cookieHeaders = response.headers().values("Set-Cookie")
        if (cookieHeaders.isNotEmpty()) {
            val tokenJSON = JSONObject()
            cookieHeaders.forEach {
                cookies.add(it)
                TokenManager.run {
                    tokenJSON.parseCookie(it)
                }
            }

            if (tokenJSON.has(TokenManager.COOKIE_KEY_RIDI_AT) && tokenJSON.has(TokenManager.COOKIE_KEY_RIDI_RT)) {
                tokenJSON.toString().saveToFile(tokenFile!!)
            }
        }
        return response
    }
}
