package com.ridi.demoapp

import android.annotation.TargetApi
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.ridi.demoapp.TokenManager.tokenManager

class WebViewActivity : Activity() {

    private val DEV_HOST = "account.dev.ridi.io/"
    private val REAL_HOST = "account.ridibooks.com/"
    private val BASE_URL = if (tokenManager.useDevMode) "https://$DEV_HOST" else "https://$REAL_HOST"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)

        val webView = findViewById<WebView>(R.id.webView)
        webView.webViewClient = object : WebViewClient() {

            @Suppress("OverridingDeprecatedMember")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                view.loadUrl(url)
                @Suppress("DEPRECATION")
                return super.shouldOverrideUrlLoading(view, url)
            }

            @TargetApi(Build.VERSION_CODES.N)
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                view.loadUrl(request.url.toString())
                return true
            }

            override fun onPageFinished(view: WebView, url: String) {
                val totalCookies = CookieManager.getInstance().getCookie(url)
                if (totalCookies != null) {
                    val splitCookies = totalCookies.split(";")
                    splitCookies.forEach { cookie ->
                        val keyValue = cookie.split("=")
                        if (keyValue[0] == "PHPSESSID") {
                            tokenManager.setSessionId(keyValue[1])
                        }
                    }
                }
            }
        }
        webView.settings.javaScriptEnabled = true
        webView.loadUrl(BASE_URL)
    }
}
