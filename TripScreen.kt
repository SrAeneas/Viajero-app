@file:OptIn(ExperimentalMaterial3Api::class)

package com.viajero.app.ui

import android.content.Intent
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.viajero.app.data.Attachment
import com.viajero.app.data.City
import com.viajero.app.data.Transport
import com.viajero.app.data.TransportType
import com.viajero.app.data.Trip
import com.viajero.app.data.TripViewModel
import com.viajero.app.data.geocode
import kotlinx.coroutines.launch

@Composable
fun TripScreen(vm: TripViewModel, trip: Trip, onBack: () -> Unit, onOpenCity: (String) -> Unit) {
    val ctx = LocalContext.current
    val colors = gradientFor(trip.id)
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showAddCity by remember { mutableStateOf(false) }
    var showAddTransport by remember { mutableStateOf(false) }
    var cityToDelete by remember { mutableStateOf<City?>(null) }
    var transportToDelete by remember { mutableStateOf<Transport?>(null) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Header(
                title = trip.country,
                subtitle = "${trip.cities.size} ciudades · ${trip.transports.size} traslados",
                onBack = onBack,
                colors = colors,
            ) {
                HeaderButton("📤 Exportar") { shareTrip(ctx, vm, trip) }
            }
            TabRow(selectedTabIndex = tab, containerColor = Color.Transparent, contentColor = colors[0]) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("🏙️ Ciudades", fontWeight = FontWeight.SemiBold) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("🎫 Traslados", fontWeight = FontWeight.SemiBold) })
            }

            val listPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 16.dp,
                bottom = 96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
            )

            if (tab == 0) {
                if (trip.cities.isEmpty()) {
                    EmptyState("🏙️", "Agrega tu primera ciudad", "Dentro de cada ciudad guardarás hoteles, restaurantes, museos y más")
                } else {
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = listPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(trip.cities, key = { it.id }) { city ->
                            CityCard(city, colors[0], onClick = { onOpenCity(city.id) }, onDelete = { cityToDelete = city })
                        }
                    }
                }
            } else {
                if (trip.transports.isEmpty()) {
                    EmptyState("✈️", "Sin traslados todavía", "Guarda aquí tus boletos de avión, tren o camión")
                } else {
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = listPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(trip.transports, key = { it.id }) { t ->
                            TransportCard(t, colors[0], onOpenFile = { openAttachment(ctx, vm, it) }, onDelete = { transportToDelete = t })
                        }
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { if (tab == 0) showAddCity = true else showAddTransport = true },
            containerColor = Coral,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (tab == 0) "Agregar ciudad" else "Agregar traslado", fontWeight = FontWeight.Bold)
        }
    }

    if (showAddCity) {
        AddCityDialog(
            country = trip.country,
            onAdd = { name, lat, lon -> vm.addCity(trip.id, name, lat, lon); showAddCity = false },
            onDismiss = { showAddCity = false },
        )
    }
    if (showAddTransport) {
        AddTransportDialog(
            vm = vm,
            onSave = { vm.addTransport(trip.id, it); showAddTransport = false },
            onDismiss = { showAddTransport = false },
        )
    }
    cityToDelete?.let { c ->
        ConfirmDialog(
            text = "Se borrará ${c.name} con sus puntos de interés y boletos.",
            onConfirm = { vm.deleteCity(trip.id, c.id); cityToDelete = null },
            onDismiss = { cityToDelete = null },
        )
    }
    transportToDelete?.let { t ->
        ConfirmDialog(
            text = "Se borrará el traslado “${t.title.ifBlank { t.type.label }}”.",
            onConfirm = { vm.deleteTransport(trip.id, t.id); transportToDelete = null },
            onDismiss = { transportToDelete = null },
        )
    }
}

private fun shareTrip(ctx: android.content.Context, vm: TripViewModel, trip: Trip) {
    val file = vm.exportTrip(trip.id) ?: return
    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
    val i = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Mi viaje a ${trip.country}")
        putExtra(Intent.EXTRA_TEXT, "Abre este archivo con Viajero para cargar el viaje a ${trip.country}.")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    ctx.startActivity(Intent.createChooser(i, "Compartir viaje"))
}

@Composable
private fun CityCard(city: City, accent: Color, onClick: () -> Unit, onDelete: () -> Unit) {
    val total = city.places.size
    val visited = city.places.count { it.visited }
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) { Text("🏙️", fontSize = 24.sp) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(city.name, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text("$total lugares · ${city.tickets.size} boletos", fontSize = 13.sp, color = Muted)
                }
                Text("🗑️", fontSize = 20.sp, modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onDelete)
                    .padding(8.dp))
            }
            if (total > 0) {
                Spacer(Modifier.padding(5.dp))
                LinearProgressIndicator(
                    progress = { visited / total.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50)),
                    color = accent,
                    trackColor = accent.copy(alpha = 0.15f),
                )
                Text("Visitados $visited de $total", fontSize = 12.sp, color = Muted, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun TransportCard(t: Transport, accent: Color, onOpenFile: (Attachment) -> Unit, onDelete: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) { Text(t.type.emoji, fontSize = 24.sp) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                val route = listOf(t.from, t.to).filter { it.isNotBlank() }.joinToString("  →  ")
                Text(route.ifBlank { t.title.ifBlank { t.type.label } }, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
                if (route.isNotBlank() && t.title.isNotBlank()) Text(t.title, fontSize = 14.sp, color = Ink)
                if (t.date.isNotBlank()) Text("📅 ${t.date}", fontSize = 13.sp, color = Muted)
                if (t.code.isNotBlank()) Text("🔖 ${t.code}", fontSize = 13.sp, color = Muted)
                if (t.notes.isNotBlank()) Text(t.notes, fontSize = 13.sp, color = Muted)
                t.attachment?.let { a ->
                    Text(
                        "📎 Ver boleto", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = accent,
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable { onOpenFile(a) }
                            .padding(vertical = 4.dp, horizontal = 2.dp),
                    )
                }
            }
            Text("🗑️", fontSize = 20.sp, modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onDelete)
                .padding(8.dp))
        }
    }
}

@Composable
private fun AddCityDialog(country: String, onAdd: (String, Double?, Double?) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    FormDialog(
        title = "Nueva ciudad",
        confirmText = if (loading) "Buscando…" else "Agregar",
        enabled = name.isNotBlank() && !loading,
        onConfirm = {
            loading = true
            scope.launch {
                val r = geocode("${name.trim()}, $country").firstOrNull()
                onAdd(name.trim(), r?.lat, r?.lon)
            }
        },
        onDismiss = onDismiss,
    ) {
        Field(name, { name = it }, "Ciudad (ej. Roma)")
        Text("Buscaremos su ubicación para centrar el mapa.", fontSize = 13.sp, color = Muted)
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
    }
}

@Composable
private fun AddTransportDialog(vm: TripViewModel, onSave: (Transport) -> Unit, onDismiss: () -> Unit) {
    var type by remember { mutableStateOf(TransportType.FLIGHT) }
    var title by remember { mutableStateOf("") }
    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var attachment by remember { mutableStateOf<Attachment?>(null) }

    FormDialog(
        title = "Nuevo traslado",
        enabled = from.isNotBlank() || to.isNotBlank() || title.isNotBlank(),
        onConfirm = {
            onSave(Transport(type = type, title = title.trim(), from = from.trim(), to = to.trim(),
                date = date.trim(), code = code.trim(), notes = notes.trim(), attachment = attachment))
        },
        onDismiss = onDismiss,
    ) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TransportType.values().forEach { t ->
                FilterChip(selected = type == t, onClick = { type = t }, label = { Text("${t.emoji} ${t.label}") })
            }
        }
        Field(from, { from = it }, "Origen")
        Field(to, { to = it }, "Destino")
        Field(title, { title = it }, "Aerolínea / empresa")
        Field(date, { date = it }, "Fecha y hora (ej. 12/03 10:30)")
        Field(code, { code = it }, "Código de reservación / asiento")
        Field(notes, { notes = it }, "Notas", singleLine = false)
        AttachmentField(vm, attachment) { attachment = it }
    }
}
