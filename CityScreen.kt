@file:OptIn(ExperimentalMaterial3Api::class)

package com.viajero.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.mutableIntStateOf
import org.osmdroid.tileprovider.cachemanager.CacheManager
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.viajero.app.data.Category
import com.viajero.app.data.City
import com.viajero.app.data.GeoResult
import com.viajero.app.data.Place
import com.viajero.app.data.Trip
import com.viajero.app.data.TripViewModel
import com.viajero.app.data.geocode
import kotlinx.coroutines.launch
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun CityScreen(vm: TripViewModel, trip: Trip, city: City, onBack: () -> Unit, onTickets: () -> Unit) {
    val ctx = LocalContext.current
    val colors = gradientFor(trip.id)
    val holder = remember { MapHolder() }

    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var presetPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var showOffline by remember { mutableStateOf(false) }

    // ----- Permiso de ubicación -----
    var hasPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        hasPerm = r.values.any { it }
    }
    LaunchedEffect(Unit) {
        if (!hasPerm) {
            permLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    fun centerOnMe() {
        if (!hasPerm) {
            permLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            return
        }
        val me = holder.loc?.myLocation
        if (me != null) {
            holder.map?.controller?.setZoom(16.0)
            holder.map?.controller?.animateTo(me)
        } else {
            Toast.makeText(ctx, "Buscando tu ubicación… activa el GPS", Toast.LENGTH_SHORT).show()
        }
    }

    fun focusPlace(p: Place) {
        holder.map?.controller?.setZoom(17.0)
        holder.map?.controller?.animateTo(GeoPoint(p.lat, p.lon))
    }

    val shown = city.places.filter { filter == null || it.category.name == filter }
    val visited = city.places.count { it.visited }
    val total = city.places.size
    val cityCenter = if (city.lat != null && city.lon != null) GeoPoint(city.lat, city.lon) else null

    Column(Modifier.fillMaxSize()) {
        Header(
            title = city.name,
            subtitle = "${trip.country} · $visited/$total visitados",
            onBack = onBack,
            colors = colors,
        )

        // ----- Mapa -----
        Box(
            Modifier
                .fillMaxWidth()
                .height(280.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(20.dp))
        ) {
            PlacesMap(
                holder = holder,
                places = shown,
                center = cityCenter,
                hasPerm = hasPerm,
                onPlace = { selectedId = it.id },
                onLongPress = { presetPoint = it; showAdd = true },
                modifier = Modifier.fillMaxSize(),
            )
            SmallFloatingActionButton(
                onClick = { centerOnMe() },
                containerColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp),
            ) { Text("🎯", fontSize = 20.sp) }
            Text(
                "⬇️ Sin conexión",
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Teal,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
                    .clickable { showOffline = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
            if (city.places.isEmpty()) {
                Text(
                    "Mantén presionado el mapa para añadir un punto",
                    fontSize = 12.sp, color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Ink.copy(alpha = 0.7f))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                )
            }
        }

        // ----- Acciones -----
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(onClick = onTickets, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                Text("🎟️ Boletos (${city.tickets.size})", fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = { presetPoint = null; showAdd = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Coral),
            ) { Text("➕ Punto de interés", fontWeight = FontWeight.SemiBold) }
        }

        // ----- Filtros por categoría -----
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("Todos") })
            Category.values().forEach { c ->
                FilterChip(
                    selected = filter == c.name,
                    onClick = { filter = if (filter == c.name) null else c.name },
                    label = { Text("${c.emoji} ${c.label}") },
                )
            }
        }

        if (total > 0) {
            LinearProgressIndicator(
                progress = { visited / total.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(50)),
                color = colors[0],
                trackColor = colors[0].copy(alpha = 0.15f),
            )
        }

        // ----- Lista de puntos -----
        if (shown.isEmpty()) {
            Box(Modifier.weight(1f)) {
                EmptyState(
                    "📍",
                    if (total == 0) "Sin puntos de interés" else "Nada en esta categoría",
                    "Agrega hoteles, restaurantes, museos, sitios y baños",
                )
            }
        } else {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = 12.dp, end = 12.dp, top = 8.dp,
                    bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(shown, key = { it.id }) { p ->
                    PlaceRow(p, onClick = { selectedId = p.id })
                }
            }
        }
    }

    // ----- Hoja inferior del punto seleccionado -----
    val selected = city.places.find { it.id == selectedId }
    if (selected != null) {
        ModalBottomSheet(onDismissRequest = { selectedId = null }, containerColor = Color.White) {
            Column(
                Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryBadge(selected.category, selected.visited)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(selected.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Text(
                            selected.category.label + if (selected.visited) " · ✅ Visitado" else "",
                            fontSize = 13.sp, color = Muted,
                        )
                    }
                }
                if (selected.notes.isNotBlank()) Text(selected.notes, color = Ink)

                Button(
                    onClick = { openRoute(ctx, selected) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("🧭 Cómo llegar", fontWeight = FontWeight.Bold) }

                Button(
                    onClick = { vm.togglePlace(trip.id, city.id, selected.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (selected.visited) Muted else Color(0xFF2E9E6B)),
                ) { Text(if (selected.visited) "↩️ Marcar como pendiente" else "✅ Marcar como visitado", fontWeight = FontWeight.Bold) }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { focusPlace(selected); selectedId = null },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                    ) { Text("🗺️ Ver en mapa") }
                    OutlinedButton(
                        onClick = { vm.deletePlace(trip.id, city.id, selected.id); selectedId = null },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                    ) { Text("🗑️ Eliminar", color = Coral) }
                }
            }
        }
    }

    if (showOffline) {
        OfflineMapDialog(holder = holder, onDismiss = { showOffline = false })
    }

    // ----- Diálogo para agregar punto -----
    if (showAdd) {
        AddPlaceDialog(
            city = city,
            country = trip.country,
            preset = presetPoint,
            myLocation = { holder.loc?.myLocation },
            onSave = { vm.addPlace(trip.id, city.id, it); showAdd = false; presetPoint = null },
            onDismiss = { showAdd = false; presetPoint = null },
        )
    }
}

private fun openRoute(ctx: android.content.Context, p: Place) {
    try {
        // Abre Google Maps (o el navegador) con la ruta desde tu ubicación actual hasta el punto.
        val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${p.lat},${p.lon}&travelmode=walking")
        ctx.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (e: Exception) {
        Toast.makeText(ctx, "No se pudo abrir la ruta", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun CategoryBadge(c: Category, visited: Boolean) {
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (visited) Color(0xFF9AA5A8) else Color(c.color)),
        contentAlignment = Alignment.Center,
    ) { Text(if (visited) "✅" else c.emoji, fontSize = 22.sp) }
}

@Composable
private fun PlaceRow(p: Place, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CategoryBadge(p.category, p.visited)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    p.name, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = if (p.visited) Muted else Ink,
                    textDecoration = if (p.visited) TextDecoration.LineThrough else null,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(p.category.label, fontSize = 13.sp, color = Muted)
            }
            Text(if (p.visited) "Visitado" else "Ver ruta ›", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (p.visited) Color(0xFF2E9E6B) else Teal)
        }
    }
}

// ---------------------------------------------------------------------------
// Mapa (OpenStreetMap con osmdroid)
// ---------------------------------------------------------------------------

@Composable
private fun PlacesMap(
    holder: MapHolder,
    places: List<Place>,
    center: GeoPoint?,
    hasPerm: Boolean,
    onPlace: (Place) -> Unit,
    onLongPress: (GeoPoint) -> Unit,
    modifier: Modifier,
) {
    val ctx = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val mapView = remember {
        MapView(ctx).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            minZoomLevel = 3.0
            controller.setZoom(3.0)
            controller.setCenter(GeoPoint(20.0, 0.0))
            holder.map = this
        }
    }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    mapView.onResume()
                    holder.loc?.enableMyLocation()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    holder.loc?.disableMyLocation()
                    mapView.onPause()
                }
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            holder.loc?.disableMyLocation()
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { map ->
            if (hasPerm && holder.loc == null) {
                holder.loc = MyLocationNewOverlay(GpsMyLocationProvider(ctx), map).also { it.enableMyLocation() }
            }

            map.overlays.clear()
            map.overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean = false
                override fun longPressHelper(p: GeoPoint?): Boolean {
                    if (p != null) onLongPress(p)
                    return true
                }
            }))
            places.forEach { pl ->
                map.overlays.add(Marker(map).apply {
                    position = GeoPoint(pl.lat, pl.lon)
                    title = pl.name
                    snippet = pl.category.label
                    icon = markerIcon(ctx, pl, holder.icons)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    setOnMarkerClickListener { _, _ -> onPlace(pl); true }
                })
            }
            holder.loc?.let { map.overlays.add(it) }

            if (!holder.centered) {
                val target = places.firstOrNull()?.let { GeoPoint(it.lat, it.lon) } ?: center
                if (target != null) {
                    map.controller.setZoom(14.0)
                    map.controller.setCenter(target)
                    holder.centered = true
                }
            }
            map.invalidate()
        },
    )
}

// ---------------------------------------------------------------------------
// Mapas sin conexión
// ---------------------------------------------------------------------------

private const val OFFLINE_MAX_TILES = 3000
private const val OFFLINE_MIN_ZOOM = 11

/**
 * Descarga el área visible del mapa (calles hasta el mayor detalle que quepa en el límite de
 * mosaicos) al almacenamiento del teléfono. Además, todo lo que ves en el mapa con internet
 * queda guardado automáticamente y se puede volver a ver sin conexión.
 */
@Composable
private fun OfflineMapDialog(holder: MapHolder, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val map = holder.map
    val cm = remember { map?.let { CacheManager(it) } }
    val box = remember { map?.boundingBox }
    val plan = remember {
        if (cm == null || box == null) null
        else (16 downTo 13).firstNotNullOfOrNull { z ->
            val n = cm.possibleTilesInArea(box, OFFLINE_MIN_ZOOM, z)
            if (n <= OFFLINE_MAX_TILES) Pair(z, n) else null
        }
    }

    // 0 = confirmar, 1 = descargando, 2 = terminado
    var phase by remember { mutableIntStateOf(0) }
    var done by remember { mutableIntStateOf(0) }
    var totalTiles by remember { mutableIntStateOf(plan?.second ?: 0) }
    var failed by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = { if (phase != 1) onDismiss() },
        title = { Text("⬇️ Mapa sin conexión", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when {
                    plan == null -> Text("El área visible es demasiado grande. Acerca el mapa a la zona donde estarás (por ejemplo el centro de la ciudad) e inténtalo de nuevo.")
                    phase == 0 -> {
                        Text("Se guardará la zona que ves ahora en el mapa: ${plan.second} mosaicos, aprox. ${plan.second * 15 / 1024 + 1} MB. Usa Wi-Fi si puedes.")
                        Text("Lo que veas en el mapa con internet también se guarda solo.", fontSize = 13.sp, color = Muted)
                    }
                    phase == 1 -> {
                        Text("Descargando… $done de $totalTiles")
                        LinearProgressIndicator(
                            progress = { if (totalTiles > 0) done / totalTiles.toFloat() else 0f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    else -> Text(
                        if (failed == 0) "✅ Listo. Esta zona ya se puede ver sin internet."
                        else "Terminó con $failed mosaicos que no se pudieron descargar. Revisa tu conexión e inténtalo de nuevo."
                    )
                }
            }
        },
        confirmButton = {
            when {
                plan != null && phase == 0 -> Button(onClick = {
                    phase = 1
                    cm!!.downloadAreaAsync(ctx, box!!, OFFLINE_MIN_ZOOM, plan.first, object : CacheManager.CacheManagerCallback {
                        override fun onTaskComplete() { phase = 2 }
                        override fun onTaskFailed(errors: Int) { failed = errors; phase = 2 }
                        override fun updateProgress(progress: Int, currentZoomLevel: Int, zoomMin: Int, zoomMax: Int) { done = progress }
                        override fun downloadStarted() {}
                        override fun setPossibleTilesInArea(total: Int) { totalTiles = total }
                    })
                }) { Text("Descargar") }
                phase == 1 -> {}
                else -> Button(onClick = onDismiss) { Text("Cerrar") }
            }
        },
        dismissButton = {
            if (plan != null && phase == 0) TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
    )
}

// ---------------------------------------------------------------------------
// Agregar punto de interés
// ---------------------------------------------------------------------------

@Composable
private fun AddPlaceDialog(
    city: City,
    country: String,
    preset: GeoPoint?,
    myLocation: () -> GeoPoint?,
    onSave: (Place) -> Unit,
    onDismiss: () -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(Category.SIGHT) }
    var point by remember { mutableStateOf(preset) }
    var results by remember { mutableStateOf<List<GeoResult>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }

    FormDialog(
        title = "Nuevo punto de interés",
        enabled = name.isNotBlank() && point != null,
        onConfirm = {
            val pt = point ?: return@FormDialog
            onSave(Place(name = name.trim(), category = category, lat = pt.latitude, lon = pt.longitude, notes = notes.trim()))
        },
        onDismiss = onDismiss,
    ) {
        Field(name, { name = it }, "Nombre (ej. Museo del Prado)")

        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Category.values().forEach { c ->
                FilterChip(selected = category == c, onClick = { category = c }, label = { Text("${c.emoji} ${c.label}") })
            }
        }

        Field(notes, { notes = it }, "Notas (opcional)", singleLine = false)

        Text(
            if (point != null) "📍 Ubicación lista (%.5f, %.5f)".format(point!!.latitude, point!!.longitude)
            else "📍 Falta la ubicación: búscala por nombre o usa tu posición",
            fontSize = 13.sp,
            color = if (point != null) Color(0xFF2E9E6B) else Coral,
            fontWeight = FontWeight.SemiBold,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    searching = true
                    scope.launch {
                        val r = geocode("${name.trim()}, ${city.name}, $country")
                        searching = false
                        results = r
                        if (r.isEmpty()) Toast.makeText(ctx, "No se encontró. Prueba otro nombre o mantén presionado el mapa.", Toast.LENGTH_LONG).show()
                        else point = GeoPoint(r[0].lat, r[0].lon)
                    }
                },
                enabled = name.isNotBlank() && !searching,
                modifier = Modifier.weight(1f),
            ) { Text(if (searching) "Buscando…" else "🔎 Buscar", fontSize = 13.sp) }
            OutlinedButton(
                onClick = {
                    val me = myLocation()
                    if (me != null) point = me
                    else Toast.makeText(ctx, "Aún no tengo tu ubicación (activa el GPS y el permiso)", Toast.LENGTH_LONG).show()
                },
                modifier = Modifier.weight(1f),
            ) { Text("📍 Mi ubicación", fontSize = 13.sp) }
        }

        if (results.size > 1) {
            Text("Otros resultados:", fontSize = 12.sp, color = Muted)
            results.forEach { r ->
                Text(
                    r.name, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    color = if (point?.latitude == r.lat && point?.longitude == r.lon) Teal else Ink,
                    fontWeight = if (point?.latitude == r.lat && point?.longitude == r.lon) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { point = GeoPoint(r.lat, r.lon) }
                        .padding(6.dp),
                )
            }
        }
        Text("Tip: también puedes mantener presionado el mapa para elegir el punto.", fontSize = 12.sp, color = Muted)
    }
}
