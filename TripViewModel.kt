package com.viajero.app.data

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class TripViewModel(app: Application) : AndroidViewModel(app) {

    private val dataFile = File(app.filesDir, "trips.json")
    val attDir = File(app.filesDir, "attachments")

    var trips by mutableStateOf<List<Trip>>(emptyList())
        private set

    init {
        try {
            if (dataFile.exists()) {
                val arr = JSONArray(dataFile.readText())
                trips = (0 until arr.length()).map { tripFromJson(arr.getJSONObject(it), attDir, false) }
            }
        } catch (e: Exception) {
            // archivo dañado: se empieza vacío
        }
    }

    private fun persist() {
        val arr = JSONArray()
        trips.forEach { arr.put(tripToJson(it, attDir, false)) }
        dataFile.writeText(arr.toString())
    }

    private fun mutate(f: (List<Trip>) -> List<Trip>) {
        trips = f(trips)
        persist()
    }

    private fun updateTrip(id: String, f: (Trip) -> Trip) =
        mutate { l -> l.map { if (it.id == id) f(it) else it } }

    private fun updateCity(tripId: String, cityId: String, f: (City) -> City) =
        updateTrip(tripId) { t -> t.copy(cities = t.cities.map { if (it.id == cityId) f(it) else it }) }

    private fun deleteFiles(list: List<Attachment>) {
        list.forEach { runCatching { File(attDir, it.file).delete() } }
    }

    // ----- Viajes -----
    fun addTrip(country: String) = mutate { it + Trip(country = country.trim()) }

    fun deleteTrip(id: String) {
        trips.find { it.id == id }?.let { deleteFiles(it.allAttachments()) }
        mutate { l -> l.filter { it.id != id } }
    }

    // ----- Ciudades -----
    fun addCity(tripId: String, name: String, lat: Double?, lon: Double?) =
        updateTrip(tripId) { it.copy(cities = it.cities + City(name = name.trim(), lat = lat, lon = lon)) }

    fun deleteCity(tripId: String, cityId: String) {
        trips.find { it.id == tripId }?.cities?.find { it.id == cityId }?.let { c ->
            deleteFiles(c.tickets.mapNotNull { it.attachment })
        }
        updateTrip(tripId) { t -> t.copy(cities = t.cities.filter { it.id != cityId }) }
    }

    // ----- Puntos de interés -----
    fun addPlace(tripId: String, cityId: String, place: Place) =
        updateCity(tripId, cityId) { it.copy(places = it.places + place) }

    fun deletePlace(tripId: String, cityId: String, placeId: String) =
        updateCity(tripId, cityId) { c -> c.copy(places = c.places.filter { it.id != placeId }) }

    fun togglePlace(tripId: String, cityId: String, placeId: String) =
        updateCity(tripId, cityId) { c ->
            c.copy(places = c.places.map { if (it.id == placeId) it.copy(visited = !it.visited) else it })
        }

    // ----- Boletos / pases de la ciudad -----
    fun addTicket(tripId: String, cityId: String, ticket: Ticket) =
        updateCity(tripId, cityId) { it.copy(tickets = it.tickets + ticket) }

    fun deleteTicket(tripId: String, cityId: String, ticketId: String) {
        trips.find { it.id == tripId }?.cities?.find { it.id == cityId }?.tickets
            ?.find { it.id == ticketId }?.attachment?.let { deleteFiles(listOf(it)) }
        updateCity(tripId, cityId) { c -> c.copy(tickets = c.tickets.filter { it.id != ticketId }) }
    }

    // ----- Traslados (avión, tren, camión) -----
    fun addTransport(tripId: String, t: Transport) =
        updateTrip(tripId) { it.copy(transports = it.transports + t) }

    fun deleteTransport(tripId: String, transportId: String) {
        trips.find { it.id == tripId }?.transports?.find { it.id == transportId }?.attachment
            ?.let { deleteFiles(listOf(it)) }
        updateTrip(tripId) { t -> t.copy(transports = t.transports.filter { it.id != transportId }) }
    }

    // ----- Adjuntos -----
    fun copyAttachment(uri: Uri): Attachment? {
        val cr = getApplication<Application>().contentResolver
        val mime = cr.getType(uri) ?: "application/octet-stream"
        var name = "boleto"
        try {
            cr.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (i >= 0) c.getString(i)?.let { name = it }
                }
            }
        } catch (e: Exception) {
        }
        val ext = name.substringAfterLast('.', "").filter { it.isLetterOrDigit() }.take(5).ifEmpty { "bin" }
        attDir.mkdirs()
        val fileName = newId() + "." + ext
        return try {
            cr.openInputStream(uri)?.use { input ->
                File(attDir, fileName).outputStream().use { input.copyTo(it) }
            } ?: return null
            Attachment(name, mime, fileName)
        } catch (e: Exception) {
            null
        }
    }

    fun attachmentFile(a: Attachment): File = File(attDir, a.file)

    // ----- Exportar / importar -----
    fun exportTrip(tripId: String): File? {
        val t = trips.find { it.id == tripId } ?: return null
        val root = JSONObject().put("format", "viajero").put("version", 1)
            .put("trip", tripToJson(t, attDir, true))
        val dir = File(getApplication<Application>().cacheDir, "exports").apply { mkdirs() }
        val safe = t.country.replace(Regex("[^A-Za-z0-9]+"), "_").trim('_').ifEmpty { "viaje" }
        val f = File(dir, "Viajero_$safe.json")
        f.writeText(root.toString())
        return f
    }

    fun importJson(text: String): Trip? = try {
        val root = JSONObject(text)
        if (root.optString("format") != "viajero") {
            null
        } else {
            val t = tripFromJson(root.getJSONObject("trip"), attDir, true)
            mutate { it + t }
            t
        }
    } catch (e: Exception) {
        null
    }

    fun importUri(uri: Uri): Trip? = try {
        getApplication<Application>().contentResolver.openInputStream(uri)?.use {
            importJson(it.bufferedReader().readText())
        }
    } catch (e: Exception) {
        null
    }
}
