package com.example.multiprofile

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView

class BrowserActivity : AppCompatActivity() {
    private lateinit var session: GeckoSession
    private lateinit var runtime: GeckoRuntime

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getStringExtra("id") ?: return finish()
        val fp = ProfileStore(this).get(id) ?: return finish()

        // Runtime singleton per aplikasi
        runtime = GeckoRuntime.getDefault(this)

        // Session terisolasi per profil:
        // - usePrivateMode(true) → storage & cookie tidak persist, terpisah dari sesi lain
        // - userAgentOverride → set UA di level engine, bukan injeksi JS
        val settings = GeckoSessionSettings.Builder()
            .userAgentOverride(fp.userAgent)
            .usePrivateMode(true)
            .useTrackingProtection(true)
            .build()

        session = GeckoSession(settings)
        session.open(runtime)

        val view = GeckoView(this)
        view.setSession(session)
        setContentView(view)

        session.loadUri("https://abrahamjuliot.github.io/creepjs/")
    }

    override fun onDestroy() {
        try {
            session.close()
        } catch (_: Throwable) {}
        super.onDestroy()
    }
}
