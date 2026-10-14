package com.reparateurpro.app

import android.app.Activity
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import com.google.zxing.integration.android.IntentIntegrator

class MainActivity : Activity() {
    private lateinit var web: WebView

    @Suppress("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccess = true
        web.settings.allowContentAccess = false
        web.webViewClient = WebViewClient()
        web.webChromeClient = WebChromeClient()
        web.addJavascriptInterface(BarcodeBridge(), "AndroidBarcode")
        web.loadUrl("file:///android_asset/index.html")
        setContentView(web)
    }

    private inner class BarcodeBridge {
        @JavascriptInterface
        fun scanBarcode() {
            runOnUiThread {
                IntentIntegrator(this@MainActivity)
                    .setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
                    .setPrompt("وجّه الكاميرا نحو الباركود")
                    .setBeepEnabled(false)
                    .setOrientationLocked(false)
                    .initiateScan()
            }
        }
    }

    @Deprecated("Deprecated in Android API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            val value = result.contents
            val safe = org.json.JSONObject.quote(value ?: "")
            web.evaluateJavascript("if(window.onBarcodeScanned)window.onBarcodeScanned($safe)", null)
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        if (::web.isInitialized && web.canGoBack()) web.goBack() else super.onBackPressed()
    }
}