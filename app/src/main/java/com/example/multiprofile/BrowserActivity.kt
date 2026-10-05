package com.example.multiprofile

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
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

        val script = fp.toInjectionScript()
        val isSupported = WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)
        Log.d("MultiProfile", "DOCUMENT_START_SCRIPT supported: $isSupported")

        if (isSupported) {
            WebViewCompat.addDocumentStartJavaScript(web, script, setOf("*"))
        } else {
            // Fallback: injeksi di onPageStarted (meskipun agak terlambat untuk beberapa properti)
            web.webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    view?.evaluateJavascript(script, null)
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
