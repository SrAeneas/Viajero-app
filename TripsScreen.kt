@file:OptIn(ExperimentalMaterial3Api::class)

package com.viajero.app.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viajero.app.data.Trip
import com.viajero.app.data.TripViewModel

@Composable
fun TripsScreen(vm: TripViewModel, onOpen: (String) -> Unit) {
    val ctx = LocalContext.current
    var showAdd by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Trip?>(null) }

    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val t = vm.importUri(uri)
            Toast.makeText(
                ctx,
                if (t != null) "Viaje importado: ${t.country}" else "El archivo no es un viaje válido",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Header(
                title = "Mis viajes",
                subtitle = "Tu próxima aventura empieza aquí ✈️",
            ) {
                HeaderButton("📥 Importar") { importer.launch(arrayOf("*/*")) }
            }

            if (vm.trips.isEmpty()) {
                EmptyState("🌍", "Aún no tienes viajes", "Toca “Nuevo viaje” o importa uno que te hayan compartido")
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, top = 16.dp,
                        bottom = 96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(vm.trips, key = { it.id }) { trip ->
                        TripCard(trip, onClick = { onOpen(trip.id) }, onDelete = { toDelete = trip })
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showAdd = true },
            containerColor = Coral,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Nuevo viaje", fontWeight = FontWeight.Bold)
        }
    }

    if (showAdd) {
        var country by remember { mutableStateOf("") }
        FormDialog(
            title = "Nuevo viaje",
            confirmText = "Crear",
            enabled = country.isNotBlank(),
            onConfirm = { vm.addTrip(country); showAdd = false },
            onDismiss = { showAdd = false },
        ) {
            Text("¿Qué país vas a visitar?", color = Muted)
            Field(country, { country = it }, "País (ej. Italia)")
        }
    }

    toDelete?.let { t ->
        ConfirmDialog(
            text = "Se borrará el viaje a ${t.country} con todas sus ciudades, lugares y boletos.",
            onConfirm = { vm.deleteTrip(t.id); toDelete = null },
            onDismiss = { toDelete = null },
        )
    }
}

@Composable
private fun TripCard(trip: Trip, onClick: () -> Unit, onDelete: () -> Unit) {
    val places = trip.cities.sumOf { it.places.size }
    val visited = trip.cities.sumOf { c -> c.places.count { it.visited } }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(gradientFor(trip.id)))
            .clickable(onClick = onClick)
    ) {
        Text("🌍", fontSize = 90.sp, modifier = Modifier
            .align(Alignment.CenterEnd)
            .padding(end = 14.dp), color = Color.White.copy(alpha = 0.35f))
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    trip.country, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f),
                )
                Text("🗑️", fontSize = 20.sp, modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onDelete)
                    .padding(6.dp))
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("🏙️ ${trip.cities.size} ciudades")
                Pill("🚆 ${trip.transports.size}")
                Pill("✅ $visited/$places")
            }
        }
    }
}

@Composable
fun Pill(text: String) {
    Text(
        text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.25f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}
