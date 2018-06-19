package com.ridi.oauth2

import android.app.Activity
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.ridi.androidoauth2.RidiOAuth2

class WebViewActivity : Activity() {

    companion object {
        var url: String = "https://account.dev.ridi.io/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)

        val webView = findViewById<WebView>(R.id.webView)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                view.loadUrl(request.url.toString())
                return true
            }

            override fun onPageFinished(view: WebView, url: String) {
                RidiOAuth2.cookies.add(CookieManager.getInstance().getCookie(url))
            }
        }
        webView.settings.javaScriptEnabled = true
        webView.loadUrl(url)
    }
}
