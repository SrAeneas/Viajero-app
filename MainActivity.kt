package com.viajero.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.viajero.app.data.TripViewModel
import com.viajero.app.ui.CityScreen
import com.viajero.app.ui.TicketsScreen
import com.viajero.app.ui.TripScreen
import com.viajero.app.ui.TripsScreen
import com.viajero.app.ui.ViajeroTheme
import org.osmdroid.config.Configuration
import java.io.File

class MainActivity : ComponentActivity() {

    private val vm: TripViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Configuration.getInstance().apply {
            load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
            userAgentValue = packageName
            // filesDir (no cacheDir) para que Android no borre los mapas descargados
            val base = File(filesDir, "osm").apply { mkdirs() }
            osmdroidBasePath = base
            osmdroidTileCache = File(base, "tiles").apply { mkdirs() }
            // Los mosaicos guardados se consideran vigentes 90 días y se pueden usar sin internet
            expirationOverrideDuration = 1000L * 60 * 60 * 24 * 90
            tileFileSystemCacheMaxBytes = 2L * 1024 * 1024 * 1024
            tileFileSystemCacheTrimBytes = 1800L * 1024 * 1024
        }

        if (savedInstanceState == null) handleIntent(intent)

        setContent {
            ViajeroTheme { App(vm) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /** Permite abrir un archivo .json de viaje compartido directamente con Viajero. */
    private fun handleIntent(i: Intent?) {
        val uri = i?.data ?: return
        if (i.action != Intent.ACTION_VIEW) return
        val trip = vm.importUri(uri)
        Toast.makeText(
            this,
            if (trip != null) "Viaje importado: ${trip.country}" else "El archivo no es un viaje válido",
            Toast.LENGTH_LONG,
        ).show()
        setIntent(Intent())
    }
}

@Composable
fun App(vm: TripViewModel) {
    var tripId by rememberSaveable { mutableStateOf<String?>(null) }
    var cityId by rememberSaveable { mutableStateOf<String?>(null) }
    var showTickets by rememberSaveable { mutableStateOf(false) }

    val trip = vm.trips.find { it.id == tripId }
    val city = trip?.cities?.find { it.id == cityId }

    BackHandler(enabled = trip != null) {
        when {
            showTickets -> showTickets = false
            city != null -> cityId = null
            else -> tripId = null
        }
    }

    when {
        trip == null -> TripsScreen(vm, onOpen = { tripId = it; cityId = null; showTickets = false })
        city == null -> TripScreen(vm, trip, onBack = { tripId = null }, onOpenCity = { cityId = it })
        showTickets -> TicketsScreen(vm, trip, city, onBack = { showTickets = false })
        else -> CityScreen(vm, trip, city, onBack = { cityId = null }, onTickets = { showTickets = true })
    }
}
