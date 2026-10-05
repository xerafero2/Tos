package com.example.multiprofile

import android.content.Context
import org.json.JSONArray

class ProfileStore(ctx: Context) {
    private val sp = ctx.getSharedPreferences("profiles", Context.MODE_PRIVATE)

    fun list(): List<Fingerprint> {
        val arr = JSONArray(sp.getString("list", "[]") ?: "[]")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Fingerprint(
                o.getString("id"), o.getString("name"),
                o.getString("userAgent"), o.getString("platform"),
                o.getInt("width"), o.getInt("height"),
                o.getString("timezone"), o.getString("language"),
                o.getString("webglVendor"), o.getString("webglRenderer"),
                o.optString("model", "Pixel 7"),
                o.optInt("chromeVer", 121)
            )
        }
    }

    private fun save(l: List<Fingerprint>) {
        val arr = JSONArray()
        l.forEach { arr.put(it.toJson()) }
        sp.edit().putString("list", arr.toString()).apply()
    }

    fun add(fp: Fingerprint) = save(list() + fp)
    fun remove(id: String) = save(list().filterNot { it.id == id })
    fun get(id: String): Fingerprint? = list().find { it.id == id }
}
