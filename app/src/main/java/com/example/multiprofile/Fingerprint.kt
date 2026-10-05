package com.example.multiprofile

import org.json.JSONObject
import java.util.Random

data class Fingerprint(
    val id: String,
    val name: String,
    val userAgent: String,
    val platform: String,
    val width: Int,
    val height: Int,
    val timezone: String,
    val language: String,
    val webglVendor: String,
    val webglRenderer: String,
    val model: String,
    val chromeVer: Int
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("name", name)
        put("userAgent", userAgent); put("platform", platform)
        put("width", width); put("height", height)
        put("timezone", timezone); put("language", language)
        put("webglVendor", webglVendor); put("webglRenderer", webglRenderer)
        put("model", model); put("chromeVer", chromeVer)
    }
}

object FingerprintGenerator {
    private data class Dev(
        val uaPart: String, val model: String,
        val w: Int, val h: Int
    )

    private val devices = listOf(
        Dev("Linux; Android 13; Pixel 7", "Pixel 7", 1080, 2400),
        Dev("Linux; Android 13; Pixel 6", "Pixel 6", 1080, 2400),
        Dev("Linux; Android 12; SM-S908B", "SM-S908B", 1440, 3200),
        Dev("Linux; Android 11; SM-G991B", "SM-G991B", 1080, 2340),
        Dev("Linux; Android 12; moto g stylus", "moto g stylus", 1080, 2460),
        Dev("Linux; Android 13; V2225", "V2225", 1080, 2400),
        Dev("Linux; Android 11; CPH2211", "CPH2211", 1080, 2400),
        Dev("Linux; Android 14; Pixel 8", "Pixel 8", 1080, 2400)
    )
    private val webgl = listOf(
        "Google Inc." to "Adreno (TM) 730",
        "Qualcomm" to "Adreno (TM) 660",
        "ARM" to "Mali-G78 MP20",
        "ARM" to "Mali-G710",
        "Imagination Technologies" to "PowerVR Rogue GE8320"
    )
    private val timezones = listOf(
        "Asia/Jakarta", "Asia/Singapore", "America/New_York",
        "Europe/London", "Asia/Tokyo"
    )
    private val langs = listOf("en-US", "id-ID", "en-GB", "ja-JP")

    fun generate(seed: Long): Fingerprint {
        val r = Random(seed)
        val dev = devices[r.nextInt(devices.size)]
        val (wglV, wglR) = webgl[r.nextInt(webgl.size)]
        val chromeVer = 110 + r.nextInt(15)
        val ua = "Mozilla/5.0 (${dev.uaPart}) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/$chromeVer.0.0.0 Mobile Safari/537.36"
        return Fingerprint(
            id = seed.toString(),
            name = "Profil ${seed.toString().takeLast(5)}",
            userAgent = ua,
            platform = "Linux armv8l",
            width = dev.w,
            height = dev.h,
            timezone = timezones[r.nextInt(timezones.size)],
            language = langs[r.nextInt(langs.size)],
            webglVendor = wglV,
            webglRenderer = wglR,
            model = dev.model,
            chromeVer = chromeVer
        )
    }
}
