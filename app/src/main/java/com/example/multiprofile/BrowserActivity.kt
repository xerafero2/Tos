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
import androidx.webkit.UserAgentMetadata
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

        // === Set Client Hints metadata (kunci utama spoof userAgentData) ===
        if (WebViewFeature.isFeatureSupported(WebViewFeature.USER_AGENT_METADATA)) {
            try {
                // BrandVersion menggunakan Builder pattern dari class mandiri UserAgentMetadata.BrandVersion
                val brandVersionList = listOf(
                    UserAgentMetadata.BrandVersion.Builder()
                        .setBrand("Chromium")
                        .setMajorVersion(fp.chromeVer.toString())
                        .setFullVersion("${fp.chromeVer}.0.0.0")
                        .build(),
                    UserAgentMetadata.BrandVersion.Builder()
                        .setBrand("Google Chrome")
                        .setMajorVersion(fp.chromeVer.toString())
                        .setFullVersion("${fp.chromeVer}.0.0.0")
                        .build(),
                    UserAgentMetadata.BrandVersion.Builder()
                        .setBrand("Not-A.Brand")
                        .setMajorVersion("99")
                        .setFullVersion("99.0.0.0")
                        .build()
                )

                val meta = UserAgentMetadata.Builder()
                    .setBrandVersionList(brandVersionList)
                    .setMobile(true)
                    .setModel(fp.model)
                    .setPlatform("Android")
                    .setPlatformVersion("13.0.0")
                    .setArchitecture("arm")
                    .setBitness(UserAgentMetadata.BITNESS_64)
                    .setFullVersion("${fp.chromeVer}.0.0.0")
                    .build()

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

        // === Layer 2: onPageStarted + onPageFinished (fallback) ===
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
