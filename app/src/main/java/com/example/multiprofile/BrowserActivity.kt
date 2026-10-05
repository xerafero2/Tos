package com.example.multiprofile

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout

class BrowserActivity : AppCompatActivity() {
    private lateinit var web: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getStringExtra("id") ?: return finish()
        val fp = ProfileStore(this).get(id) ?: return finish()

        // Isolasi sesi: buang cookie & storage lama
        CookieManager.getInstance().removeAllCookies(null)
        WebStorage.getInstance().deleteAllData()

        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.userAgentString = fp.userAgent
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
        }

        // Injeksi fingerprint sebelum script situs dijalankan
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            WebViewCompat.addDocumentStartJavaScript(web, fp.toInjectionScript(), setOf("*"))
        } else {
            // Fallback: re-inject di setiap page start
            web.webViewClient = object : android.webkit.WebViewClient() {
                override fun onPageStarted(v: WebView?, url: String?, f: android.graphics.Bitmap?) {
                    v?.evaluateJavascript(fp.toInjectionScript(), null)
                }
            }
        }

        val urlBar = EditText(this).apply {
            setText("https://abrahamjuliot.github.io/creepjs/")
        }
        val goBtn = Button(this).apply {
            text = "Go"
            setOnClickListener {
                var u = urlBar.text.toString().trim()
                if (!u.startsWith("http")) u = "https://$u"
                web.loadUrl(u)
            }
        }
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(urlBar, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(goBtn)
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(topBar)
            addView(web, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0
            ).apply { weight = 1f })
        }
        setContentView(root)

        web.loadUrl(urlBar.text.toString())
    }

    override fun onDestroy() {
        web.destroy()
        super.onDestroy()
    }
}
