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
    val tzOffsetMinutes: Int,
    val language: String,
    val webglVendor: String,
    val webglRenderer: String,
    val canvasNoise: Int,
    val chromeVer: Int,
    val model: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("name", name)
        put("userAgent", userAgent); put("platform", platform)
        put("width", width); put("height", height)
        put("pixelRatio", pixelRatio)
        put("hardwareConcurrency", hardwareConcurrency)
        put("deviceMemory", deviceMemory)
        put("timezone", timezone); put("tzOffsetMinutes", tzOffsetMinutes)
        put("language", language)
        put("webglVendor", webglVendor); put("webglRenderer", webglRenderer)
        put("canvasNoise", canvasNoise); put("chromeVer", chromeVer)
        put("model", model)
    }

    fun toInjectionScript(): String {
        val q = { s: String -> JSONObject.quote(s) }
        val languages = JSONArray(listOf(language, "en")).toString()
        val brands = JSONArray()
            .put(JSONObject().put("brand", "Chromium").put("version", chromeVer.toString()))
            .put(JSONObject().put("brand", "Google Chrome").put("version", chromeVer.toString()))
            .put(JSONObject().put("brand", "Not-A.Brand").put("version", "99"))
            .toString()

        return """
        (function(){
          'use strict';
          if (window.__mp_injected) return;
          window.__mp_injected = true;

          const defGet = (obj, prop, val) => {
            try {
              Object.defineProperty(obj, prop, {
                get: () => val, set: () => {}, configurable: true
              });
            } catch(e){}
          };
          const defFn = (obj, prop, fn) => {
            try {
              Object.defineProperty(obj, prop, {
                value: fn, writable: true, configurable: true
              });
            } catch(e){}
          };

          // ---------- NAVIGATOR ----------
          defGet(Navigator.prototype, 'userAgent', ${q(userAgent)});
          defGet(Navigator.prototype, 'appVersion', ${q(userAgent.replace("Mozilla/", ""))});
          defGet(Navigator.prototype, 'platform', ${q(platform)});
          defGet(Navigator.prototype, 'vendor', 'Google Inc.');
          defGet(Navigator.prototype, 'hardwareConcurrency', $hardwareConcurrency);
          defGet(Navigator.prototype, 'deviceMemory', $deviceMemory);
          defGet(Navigator.prototype, 'language', ${q(language)});
          defGet(Navigator.prototype, 'languages', $languages);
          defGet(Navigator.prototype, 'webdriver', false);
          defGet(Navigator.prototype, 'maxTouchPoints', 5);
          defGet(Navigator.prototype, 'onLine', true);

          // ---------- USER AGENT DATA (Client Hints) ----------
          const uaBrands = $brands;
          const uaDataObj = {
            brands: uaBrands,
            mobile: true,
            platform: "Android",
            getHighEntropyValues: function(hints){
              const out = { brands: uaBrands, mobile: true, platform: "Android" };
              const map = {
                architecture: "arm",
                bitness: "64",
                model: ${q(model)},
                platformVersion: "13.0.0",
                uaFullVersion: "${chromeVer}.0.0.0",
                fullVersionList: uaBrands,
                wow64: false
              };
              (hints||[]).forEach(h => { if (map[h] !== undefined) out[h] = map[h]; });
              return Promise.resolve(out);
            },
            toJSON: function(){ return { brands: uaBrands, mobile: true, platform: "Android" }; }
          };
          defGet(Navigator.prototype, 'userAgentData', uaDataObj);

          // ---------- SCREEN / WINDOW ----------
          defGet(Screen.prototype, 'width', $width);
          defGet(Screen.prototype, 'height', $height);
          defGet(Screen.prototype, 'availWidth', $width);
          defGet(Screen.prototype, 'availHeight', $height);
          defGet(Screen.prototype, 'colorDepth', 24);
          defGet(Screen.prototype, 'pixelDepth', 24);
          defGet(window, 'devicePixelRatio', $pixelRatio);
          defGet(window, 'outerWidth', $width);
          defGet(window, 'outerHeight', $height);
          defGet(window, 'screenX', 0);
          defGet(window, 'screenY', 0);

          // ---------- TIMEZONE ----------
          const _tzOffset = $tzOffsetMinutes;
          defFn(Date.prototype, 'getTimezoneOffset', function(){ return _tzOffset; });

          const _resolved = Intl.DateTimeFormat.prototype.resolvedOptions;
          defFn(Intl.DateTimeFormat.prototype, 'resolvedOptions', function(){
            const r = _resolved.call(this);
            r.timeZone = ${q(timezone)};
            return r;
          });
          try {
            Object.defineProperty(Intl.DateTimeFormat.prototype, 'resolvedOptions', {
              value: Intl.DateTimeFormat.prototype.resolvedOptions,
              configurable: true, writable: true
            });
          } catch(e){}

          // ---------- WEBGL ----------
          const patchGL = (proto) => {
            if (!proto) return;
            const _gp = proto.getParameter;
            defFn(proto, 'getParameter', function(p){
              if (p === 37445) return ${q(webglVendor)};
              if (p === 37446) return ${q(webglRenderer)};
              if (p === 7936) return ${q(webglVendor)};
              if (p === 7937) return ${q(webglRenderer)};
              return _gp.call(this, p);
            });
            const _ge = proto.getExtension;
            defFn(proto, 'getExtension', function(name){
              if (name === 'WEBGL_debug_renderer_info') {
                return { UNMASKED_VENDOR_WEBGL: 37445, UNMASKED_RENDERER_WEBGL: 37446 };
              }
              return _ge.call(this, name);
            });
          };
          patchGL(window.WebGLRenderingContext && WebGLRenderingContext.prototype);
          patchGL(window.WebGL2RenderingContext && WebGL2RenderingContext.prototype);

          // ---------- CANVAS (noise konsisten per profil) ----------
          const noise = $canvasNoise;
          const _toDataURL = HTMLCanvasElement.prototype.toDataURL;
          defFn(HTMLCanvasElement.prototype, 'toDataURL', function(){
            const ctx = this.getContext && this.getContext('2d');
            if (ctx) {
              try {
                const img = ctx.getImageData(0, 0, this.width, this.height);
                const p = img.data;
                for (let i = 0; i < p.length; i += 4) {
                  p[i]   = (p[i]   + (noise & 0x03)) & 0xFF;
                  p[i+1] = (p[i+1] + ((noise>>2) & 0x03)) & 0xFF;
                  p[i+2] = (p[i+2] + ((noise>>4) & 0x03)) & 0xFF;
                }
                ctx.putImageData(img, 0, 0);
              } catch(e){}
            }
            return _toDataURL.apply(this, arguments);
          });

          const _gid = CanvasRenderingContext2D.prototype.getImageData;
          defFn(CanvasRenderingContext2D.prototype, 'getImageData', function(){
            const d = _gid.apply(this, arguments);
            const p = d.data;
            for (let i = 0; i < p.length; i += 4) {
              p[i]   = (p[i]   + (noise & 0x03)) & 0xFF;
              p[i+1] = (p[i+1] + ((noise>>2) & 0x03)) & 0xFF;
              p[i+2] = (p[i+2] + ((noise>>4) & 0x03)) & 0xFF;
            }
            return d;
          });

          // ---------- AUDIO CONTEXT (noise konsisten) ----------
          const _getChannelData = AudioBuffer.prototype.getChannelData;
          defFn(AudioBuffer.prototype, 'getChannelData', function(){
            const arr = _getChannelData.apply(this, arguments);
            const n = noise / 10000.0;
            for (let i = 0; i < arr.length; i += 100) arr[i] += n;
            return arr;
          });
        })();
        """.trimIndent()
    }
}

object FingerprintGenerator {
    private data class Dev(
        val uaPart: String, val model: String,
        val w: Int, val h: Int, val platform: String
    )

    private val devices = listOf(
        Dev("Linux; Android 13; Pixel 7", "Pixel 7", 1080, 2400, "Linux armv8l"),
        Dev("Linux; Android 13; Pixel 6", "Pixel 6", 1080, 2400, "Linux armv8l"),
        Dev("Linux; Android 12; SM-S908B", "SM-S908B", 1440, 3200, "Linux armv8l"),
        Dev("Linux; Android 11; SM-G991B", "SM-G991B", 1080, 2340, "Linux armv8l"),
        Dev("Linux; Android 12; moto g stylus", "moto g stylus", 1080, 2460, "Linux armv8l"),
        Dev("Linux; Android 13; V2225", "V2225", 1080, 2400, "Linux armv8l"),
        Dev("Linux; Android 11; CPH2211", "CPH2211", 1080, 2400, "Linux armv8l"),
        Dev("Linux; Android 14; Pixel 8", "Pixel 8", 1080, 2400, "Linux armv8l")
    )
    private val webgl = listOf(
        "Google Inc." to "Adreno (TM) 730",
        "Qualcomm" to "Adreno (TM) 660",
        "ARM" to "Mali-G78 MP20",
        "ARM" to "Mali-G710",
        "Imagination Technologies" to "PowerVR Rogue GE8320"
    )
    private val timezones = listOf(
        "Asia/Jakarta" to -420,
        "Asia/Singapore" to -480,
        "America/New_York" to 300,
        "Europe/London" to 0,
        "Asia/Tokyo" to -540
    )
    private val langs = listOf("en-US", "id-ID", "en-GB", "ja-JP")

    fun generate(seed: Long): Fingerprint {
        val r = Random(seed)
        val dev = devices[r.nextInt(devices.size)]
        val (wglV, wglR) = webgl[r.nextInt(webgl.size)]
        val (tz, tzOff) = timezones[r.nextInt(timezones.size)]
        val chromeVer = 110 + r.nextInt(15)
        val ua = "Mozilla/5.0 (${dev.uaPart}) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$chromeVer.0.0.0 Mobile Safari/537.36"
        return Fingerprint(
            id = seed.toString(),
            name = "Profil ${seed.toString().takeLast(5)}",
            userAgent = ua,
            platform = dev.platform,
            width = dev.w,
            height = dev.h,
            pixelRatio = listOf(2.0, 2.625, 2.75, 3.0)[r.nextInt(4)],
            hardwareConcurrency = listOf(4, 6, 8)[r.nextInt(3)],
            deviceMemory = listOf(4, 6, 8)[r.nextInt(3)],
            timezone = tz,
            tzOffsetMinutes = tzOff,
            language = langs[r.nextInt(langs.size)],
            webglVendor = wglV,
            webglRenderer = wglR,
            canvasNoise = r.nextInt(256),
            chromeVer = chromeVer,
            model = dev.model
        )
    }
}
