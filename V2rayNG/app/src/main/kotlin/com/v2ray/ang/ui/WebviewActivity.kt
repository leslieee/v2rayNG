package com.v2ray.ang.ui

import android.graphics.Bitmap
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.v2ray.ang.R
import kotlinx.android.synthetic.main.activity_webview.*

class WebviewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        webview.loadUrl("https://www.google.com")
        // webview.loadUrl("https://www.google.com/search?q=freedom&source=lnms&tbm=isch")
        // 支持通过JS打开新窗口
        webview.settings.javaScriptCanOpenWindowsAutomatically = true
        webview.settings.javaScriptEnabled = true
        webview.setWebViewClient(object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                view?.loadUrl(url)
                return true
            }
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                progressBar.visibility = View.VISIBLE
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.INVISIBLE
                super.onPageFinished(view, url)
            }

        })
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && webview.canGoBack()) {
            webview.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event)
    }
}
