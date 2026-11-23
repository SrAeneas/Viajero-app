package com.viajero.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class GeoResult(val name: String, val lat: Double, val lon: Double)

/** Búsqueda de lugares con Nominatim (OpenStreetMap). No requiere API key. */
suspend fun geocode(query: String): List<GeoResult> = withContext(Dispatchers.IO) {
    try {
        val url = URL(
            "https://nominatim.openstreetmap.org/search?format=json&limit=5&accept-language=es&q=" +
                URLEncoder.encode(query, "UTF-8")
        )
        val c = url.openConnection() as HttpURLConnection
        c.setRequestProperty("User-Agent", "Viajero-Android/1.0")
        c.connectTimeout = 8000
        c.readTimeout = 8000
        val text = c.inputStream.bufferedReader().use { it.readText() }
        val arr = JSONArray(text)
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            GeoResult(o.optString("display_name"), o.getString("lat").toDouble(), o.getString("lon").toDouble())
        }
    } catch (e: Exception) {
        emptyList()
    }
}
