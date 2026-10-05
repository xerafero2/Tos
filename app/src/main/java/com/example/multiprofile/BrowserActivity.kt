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
import androidx.webkit.WebSettingsCompat
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

        // === Set Client Hints metadata (kunci utama spoof userAgentData) ===
        if (WebViewFeature.isFeatureSupported(WebViewFeature.USER_AGENT_METADATA)) {
            try {
                val brands = listOf(
                    WebSettingsCompat.UserAgentBrandVersion("Chromium", fp.chromeVer.toString()),
                    WebSettingsCompat.UserAgentBrandVersion("Google Chrome", fp.chromeVer.toString()),
                    WebSettingsCompat.UserAgentBrandVersion("Not-A.Brand", "99")
                )
                val fullVersionList = listOf(
                    WebSettingsCompat.UserAgentBrandVersion("Chromium", "${fp.chromeVer}.0.0.0"),
                    WebSettingsCompat.UserAgentBrandVersion("Google Chrome", "${fp.chromeVer}.0.0.0"),
                    WebSettingsCompat.UserAgentBrandVersion("Not-A.Brand", "99.0.0.0")
                )
                val meta = WebSettingsCompat.UserAgentMetadata(
                    2, // CH_UA
                    brands,
                    fullVersionList,
                    true,          // mobile
                    fp.model,      // model
                    "Android",     // platform
                    "13.0.0",      // platformVersion
                    "arm",         // architecture
                    "64",          // bitness
                    "${fp.chromeVer}.0.0.0",
                    false          // wow64
                )
                WebSettingsCompat.setUserAgentMetadata(web.settings, meta)
                Log.d("MultiProfile", "UserAgentMetadata berhasil di-set")
            } catch (e: Exception) {
                Log.e("MultiProfile", "Gagal set UserAgentMetadata", e)
            }
        } else {
            Log.w("MultiProfile", "USER_AGENT_METADATA tidak didukung")
        }

        val script = fp.toInjectionScript()

        // === Layer 1: document-start (AndroidX WebKit) ===
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            WebViewCompat.addDocumentStartJavaScript(web, script, setOf("*"))
            Log.d("MultiProfile", "Document-start script terpasang")
        }

        // === Layer 2: onPageStarted (fallback) ===
        web.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                view?.evaluateJavascript(script, null)
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                view?.evaluateJavascript(script, null)
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
