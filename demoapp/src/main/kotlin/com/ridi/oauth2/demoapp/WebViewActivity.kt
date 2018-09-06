package com.ridi.oauth2.demoapp

import android.app.Activity
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.ridi.oauth2.demoapp.TokenManager.tokenManager

class WebViewActivity : Activity() {
    companion object {
        private const val DEV_HOST = "account.dev.ridi.io/"
        private const val REAL_HOST = "account.ridibooks.com/"
        private val BASE_URL = if (tokenManager.useDevMode) "https://$DEV_HOST" else "https://$REAL_HOST"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)

        val webView = findViewById<WebView>(R.id.webView)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val totalCookies = CookieManager.getInstance().getCookie(url)
                if (totalCookies != null) {
                    val splitCookies = totalCookies.split(";")
                    splitCookies.forEach { cookie ->
                        val keyValue = cookie.trim().split("=")
                        if (keyValue[0] == "PHPSESSID") {
                            tokenManager.sessionId = keyValue[1]
                        }
                    }
                }
            }
        }
        webView.settings.javaScriptEnabled = true
        webView.loadUrl(BASE_URL)
    }
}
