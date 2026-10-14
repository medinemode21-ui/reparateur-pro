package com.reparateurpro.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.CaptureActivity
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.ByteArrayOutputStream

class MainActivity : Activity() {
    private lateinit var web: WebView
    private val cameraRequestCode = 7301

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

    private fun startBarcodeScan() {
        try {
            // Force the embedded scanner activity from zxing-android-embedded.
            // This avoids relying on a separate Barcode Scanner app being installed.
            IntentIntegrator(this)
                .setCaptureActivity(CaptureActivity::class.java)
                .setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
                .setPrompt("وجّه الكاميرا نحو الباركود أو QR")
                .setBeepEnabled(false)
                .setOrientationLocked(false)
                .initiateScan()
        } catch (e: Exception) {
            val message = org.json.JSONObject.quote(
                "تعذر فتح الكاميرا: " + (e.localizedMessage ?: "خطأ غير معروف")
            )
            web.post {
                web.evaluateJavascript(
                    "if(window.onBarcodeScanError)window.onBarcodeScanError($message)",
                    null
                )
            }
        }
    }

    private inner class BarcodeBridge {
        @JavascriptInterface
        fun scanBarcode() {
            runOnUiThread {
                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(Manifest.permission.CAMERA), cameraRequestCode)
                } else {
                    startBarcodeScan()
                }
            }
        }

        @JavascriptInterface
        fun qrPngBase64(text: String): String {
            return try {
                val hints = mapOf(
                    EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                    EncodeHintType.MARGIN to 1
                )
                val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 280, 280, hints)
                val bitmap = Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
                for (x in 0 until matrix.width) {
                    for (y in 0 until matrix.height) {
                        bitmap.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                    }
                }
                val output = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                bitmap.recycle()
                Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
            } catch (_: Exception) {
                ""
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == cameraRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startBarcodeScan()
            } else {
                web.evaluateJavascript(
                    "if(window.onBarcodeScanError)window.onBarcodeScanError('يرجى السماح باستخدام الكاميرا من إعدادات التطبيق')",
                    null
                )
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