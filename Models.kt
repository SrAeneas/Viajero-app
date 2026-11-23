package com.viajero.app.data

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

enum class Category(val label: String, val emoji: String, val color: Long) {
    HOTEL("Hotel", "🏨", 0xFF7C5CFC),
    RESTAURANT("Restaurante", "🍽️", 0xFFFF6B6B),
    MUSEUM("Museo", "🏛️", 0xFFE0A100),
    SIGHT("Sitio de interés", "📸", 0xFF0E7C86),
    RESTROOM("Baño", "🚻", 0xFF3A7BD5),
}

enum class TransportType(val label: String, val emoji: String) {
    FLIGHT("Avión", "✈️"),
    TRAIN("Tren", "🚆"),
    BUS("Camión", "🚌"),
    OTHER("Otro", "🚐"),
}

/** Archivo adjunto (PDF o imagen de un boleto). Se guarda en el almacenamiento interno de la app. */
data class Attachment(val name: String, val mime: String, val file: String)

data class Ticket(
    val id: String = newId(),
    val title: String,
    val date: String = "",
    val notes: String = "",
    val attachment: Attachment? = null,
)

data class Transport(
    val id: String = newId(),
    val type: TransportType,
    val title: String,
    val from: String = "",
    val to: String = "",
    val date: String = "",
    val code: String = "",
    val notes: String = "",
    val attachment: Attachment? = null,
)

data class Place(
    val id: String = newId(),
    val name: String,
    val category: Category,
    val lat: Double,
    val lon: Double,
    val notes: String = "",
    val visited: Boolean = false,
)

data class City(
    val id: String = newId(),
    val name: String,
    val lat: Double? = null,
    val lon: Double? = null,
    val places: List<Place> = emptyList(),
    val tickets: List<Ticket> = emptyList(),
)

data class Trip(
    val id: String = newId(),
    val country: String,
    val cities: List<City> = emptyList(),
    val transports: List<Transport> = emptyList(),
) {
    fun allAttachments(): List<Attachment> =
        transports.mapNotNull { it.attachment } + cities.flatMap { c -> c.tickets.mapNotNull { it.attachment } }
}

// ---------------------------------------------------------------------------
// JSON
// ---------------------------------------------------------------------------

private fun JSONObject.str(k: String): String = optString(k, "")
private fun JSONObject.dbl(k: String): Double? = if (has(k) && !isNull(k)) optDouble(k) else null

private fun Attachment.toJson(dir: File, withData: Boolean): JSONObject = JSONObject().apply {
    put("name", name)
    put("mime", mime)
    put("file", file)
    if (withData) {
        val f = File(dir, file)
        if (f.exists()) put("data", Base64.encodeToString(f.readBytes(), Base64.NO_WRAP))
    }
}

/**
 * [fresh] = true cuando se importa un viaje compartido: se regeneran ids y los
 * archivos adjuntos se restauran desde el contenido en base64.
 */
private fun attachmentFrom(o: JSONObject?, dir: File, fresh: Boolean): Attachment? {
    if (o == null) return null
    val name = o.str("name")
    val mime = o.str("mime").ifEmpty { "application/octet-stream" }
    if (!fresh) return Attachment(name, mime, o.str("file"))
    val data = o.str("data")
    if (data.isEmpty()) return null
    return try {
        dir.mkdirs()
        val ext = name.substringAfterLast('.', "").filter { it.isLetterOrDigit() }.take(5).ifEmpty { "bin" }
        val fileName = newId() + "." + ext
        File(dir, fileName).writeBytes(Base64.decode(data, Base64.DEFAULT))
        Attachment(name, mime, fileName)
    } catch (e: Exception) {
        null
    }
}

fun tripToJson(t: Trip, dir: File, withData: Boolean): JSONObject = JSONObject().apply {
    put("id", t.id)
    put("country", t.country)
    put("transports", JSONArray().also { arr ->
        t.transports.forEach { tr ->
            arr.put(JSONObject().apply {
                put("id", tr.id); put("type", tr.type.name); put("title", tr.title)
                put("from", tr.from); put("to", tr.to); put("date", tr.date)
                put("code", tr.code); put("notes", tr.notes)
                tr.attachment?.let { put("attachment", it.toJson(dir, withData)) }
            })
        }
    })
    put("cities", JSONArray().also { arr ->
        t.cities.forEach { c ->
            arr.put(JSONObject().apply {
                put("id", c.id); put("name", c.name)
                c.lat?.let { put("lat", it) }
                c.lon?.let { put("lon", it) }
                put("places", JSONArray().also { pa ->
                    c.places.forEach { p ->
                        pa.put(JSONObject().apply {
                            put("id", p.id); put("name", p.name); put("category", p.category.name)
                            put("lat", p.lat); put("lon", p.lon); put("notes", p.notes); put("visited", p.visited)
                        })
                    }
                })
                put("tickets", JSONArray().also { ta ->
                    c.tickets.forEach { k ->
                        ta.put(JSONObject().apply {
                            put("id", k.id); put("title", k.title); put("date", k.date); put("notes", k.notes)
                            k.attachment?.let { put("attachment", it.toJson(dir, withData)) }
                        })
                    }
                })
            })
        }
    })
}

fun tripFromJson(o: JSONObject, dir: File, fresh: Boolean): Trip {
    fun id(x: JSONObject) = if (fresh) newId() else x.str("id").ifEmpty { newId() }

    val transports = o.optJSONArray("transports")?.let { arr ->
        (0 until arr.length()).map { i ->
            val x = arr.getJSONObject(i)
            Transport(
                id = id(x),
                type = runCatching { TransportType.valueOf(x.str("type")) }.getOrDefault(TransportType.OTHER),
                title = x.str("title"), from = x.str("from"), to = x.str("to"),
                date = x.str("date"), code = x.str("code"), notes = x.str("notes"),
                attachment = attachmentFrom(x.optJSONObject("attachment"), dir, fresh),
            )
        }
    } ?: emptyList()

    val cities = o.optJSONArray("cities")?.let { arr ->
        (0 until arr.length()).map { i ->
            val c = arr.getJSONObject(i)
            val places = c.optJSONArray("places")?.let { pa ->
                (0 until pa.length()).map { j ->
                    val p = pa.getJSONObject(j)
                    Place(
                        id = id(p), name = p.str("name"),
                        category = runCatching { Category.valueOf(p.str("category")) }.getOrDefault(Category.SIGHT),
                        lat = p.optDouble("lat", 0.0), lon = p.optDouble("lon", 0.0),
                        notes = p.str("notes"), visited = p.optBoolean("visited", false),
                    )
                }
            } ?: emptyList()
            val tickets = c.optJSONArray("tickets")?.let { ta ->
                (0 until ta.length()).map { j ->
                    val k = ta.getJSONObject(j)
                    Ticket(
                        id = id(k), title = k.str("title"), date = k.str("date"), notes = k.str("notes"),
                        attachment = attachmentFrom(k.optJSONObject("attachment"), dir, fresh),
                    )
                }
            } ?: emptyList()
            City(id = id(c), name = c.str("name"), lat = c.dbl("lat"), lon = c.dbl("lon"), places = places, tickets = tickets)
        }
    } ?: emptyList()

    return Trip(id = id(o), country = o.str("country").ifEmpty { "Viaje" }, cities = cities, transports = transports)
}
