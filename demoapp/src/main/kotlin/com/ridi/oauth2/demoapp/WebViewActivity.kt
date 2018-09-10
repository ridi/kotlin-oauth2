package com.ridi.oauth2.demoapp

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient

@SuppressLint("SetJavaScriptEnabled")
class WebViewActivity : Activity() {
    companion object {
        private const val DEV_HOST = "dev.ridi.io"
        private const val REAL_HOST = "ridibooks.com"
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
                            DemoAppApplication.phpSessionId = keyValue[1]
                        }
                    }
                }
            }
        }
        webView.settings.javaScriptEnabled = true

        val url = "https://${if (DemoAppApplication.isDevMode) DEV_HOST else REAL_HOST}/account/login"
        CookieManager.getInstance().setCookie(url, "")
        webView.loadUrl(url)
    }
}
