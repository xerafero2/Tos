package com.example.multiprofile

import org.json.JSONArray
import org.json.JSONObject
import java.util.Random

data class Fingerprint(
    val id: String,
    val name: String,
    val userAgent: String,
    val platform: String,
    val width: Int,
    val height: Int,
    val pixelRatio: Double,
    val hardwareConcurrency: Int,
    val deviceMemory: Int,
    val timezone: String,
    val language: String,
    val webglVendor: String,
    val webglRenderer: String,
    val canvasNoise: Int
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("name", name)
        put("userAgent", userAgent); put("platform", platform)
        put("width", width); put("height", height)
        put("pixelRatio", pixelRatio)
        put("hardwareConcurrency", hardwareConcurrency)
        put("deviceMemory", deviceMemory)
        put("timezone", timezone); put("language", language)
        put("webglVendor", webglVendor); put("webglRenderer", webglRenderer)
        put("canvasNoise", canvasNoise)
    }

    fun toInjectionScript(): String {
        val q = { s: String -> JSONObject.quote(s) }
        val languages = JSONArray(listOf(language, "en")).toString()
        return """
            (function(){
              'use strict';
              const def = (o,p,v)=>{try{Object.defineProperty(o,p,{get:()=>v,configurable:true})}catch(e){}};
              def(navigator,'userAgent',${q(userAgent)});
              def(navigator,'appVersion',${q(userAgent.replace("Mozilla/", ""))});
              def(navigator,'platform',${q(platform)});
              def(navigator,'hardwareConcurrency',$hardwareConcurrency);
              def(navigator,'deviceMemory',$deviceMemory);
              def(navigator,'language',${q(language)});
              def(navigator,'languages',$languages);
              def(navigator,'webdriver',false);
              def(screen,'width',$width);
              def(screen,'height',$height);
              def(screen,'availWidth',$width);
              def(screen,'availHeight',$height);
              def(window,'devicePixelRatio',$pixelRatio);

              const noise = $canvasNoise;
              const _gid = CanvasRenderingContext2D.prototype.getImageData;
              CanvasRenderingContext2D.prototype.getImageData = function(){
                const d = _gid.apply(this, arguments);
                const p = d.data;
                for (let i=0;i<p.length;i+=4){
                  p[i]   = (p[i]   + (noise & 0x03)) & 0xFF;
                  p[i+1] = (p[i+1] + ((noise>>2) & 0x03)) & 0xFF;
                  p[i+2] = (p[i+2] + ((noise>>4) & 0x03)) & 0xFF;
                }
                return d;
              };

              const patchGL = (proto) => {
                if (!proto) return;
                const _gp = proto.getParameter;
                proto.getParameter = function(p){
                  if (p === 37445) return ${q(webglVendor)};
                  if (p === 37446) return ${q(webglRenderer)};
                  return _gp.call(this, p);
                };
              };
              patchGL(window.WebGLRenderingContext && WebGLRenderingContext.prototype);
              patchGL(window.WebGL2RenderingContext && WebGL2RenderingContext.prototype);
            })();
        """.trimIndent()
    }
}

object FingerprintGenerator {
    private val devices = listOf(
        Triple("Linux; Android 13; Pixel 7", "Pixel 7", 1080 to 2400),
        Triple("Linux; Android 13; Pixel 6", "Pixel 6", 1080 to 2400),
        Triple("Linux; Android 12; SM-S908B", "SM-S908B", 1440 to 3200),
        Triple("Linux; Android 11; SM-G991B", "SM-G991B", 1080 to 2340),
        Triple("Linux; Android 12; moto g stylus", "moto g stylus", 1080 to 2460),
        Triple("Linux; Android 13; V2225", "V2225", 1080 to 2400),
        Triple("Linux; Android 11; CPH2211", "CPH2211", 1080 to 2400),
        Triple("Linux; Android 14; Pixel 8", "Pixel 8", 1080 to 2400)
    )
    private val webgl = listOf(
        "Google Inc." to "Adreno (TM) 730",
        "Qualcomm" to "Adreno (TM) 660",
        "ARM" to "Mali-G78 MP20",
        "ARM" to "Mali-G710",
        "Imagination Technologies" to "PowerVR Rogue GE8320"
    )
    private val timezones = listOf("Asia/Jakarta", "Asia/Singapore", "America/New_York", "Europe/London", "Asia/Tokyo")
    private val langs = listOf("en-US", "id-ID", "en-GB", "ja-JP")

    fun generate(seed: Long): Fingerprint {
        val r = Random(seed)
        val dev = devices[r.nextInt(devices.size)]
        val (wglV, wglR) = webgl[r.nextInt(webgl.size)]
        val chromeVer = 110 + r.nextInt(15)
        val ua = "Mozilla/5.0 (${dev.first}) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$chromeVer.0.0.0 Mobile Safari/537.36"
        return Fingerprint(
            id = seed.toString(),
            name = "Profil ${seed.toString().takeLast(5)}",
            userAgent = ua,
            platform = "Linux armv8l",
            width = dev.third.first,
            height = dev.third.second,
            pixelRatio = listOf(2.0, 2.625, 2.75, 3.0)[r.nextInt(4)],
            hardwareConcurrency = listOf(4, 6, 8)[r.nextInt(3)],
            deviceMemory = listOf(4, 6, 8)[r.nextInt(3)],
            timezone = timezones[r.nextInt(timezones.size)],
            language = langs[r.nextInt(langs.size)],
            webglVendor = wglV,
            webglRenderer = wglR,
            canvasNoise = r.nextInt(256)
        )
    }
}
