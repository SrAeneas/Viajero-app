@file:OptIn(ExperimentalMaterial3Api::class)

package com.viajero.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viajero.app.data.Attachment
import com.viajero.app.data.City
import com.viajero.app.data.Ticket
import com.viajero.app.data.Trip
import com.viajero.app.data.TripViewModel

/** Boletos de museos, pases de transporte, tours, etc. de una ciudad. */
@Composable
fun TicketsScreen(vm: TripViewModel, trip: Trip, city: City, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val colors = gradientFor(trip.id)
    var showAdd by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Ticket?>(null) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Header(
                title = "🎟️ Boletos y pases",
                subtitle = "${city.name} · ${trip.country}",
                onBack = onBack,
                colors = colors,
            )
            if (city.tickets.isEmpty()) {
                EmptyState("🎟️", "Aún no hay boletos", "Guarda entradas a museos, pases de transporte o tours de ${city.name}")
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, top = 16.dp,
                        bottom = 96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(city.tickets, key = { it.id }) { t ->
                        TicketCard(
                            t, colors[0],
                            onOpen = { t.attachment?.let { openAttachment(ctx, vm, it) } },
                            onDelete = { toDelete = t },
                        )
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
            Text("Agregar boleto", fontWeight = FontWeight.Bold)
        }
    }

    if (showAdd) {
        var title by remember { mutableStateOf("") }
        var date by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        var attachment by remember { mutableStateOf<Attachment?>(null) }
        FormDialog(
            title = "Nuevo boleto o pase",
            enabled = title.isNotBlank(),
            onConfirm = {
                vm.addTicket(trip.id, city.id, Ticket(title = title.trim(), date = date.trim(), notes = notes.trim(), attachment = attachment))
                showAdd = false
            },
            onDismiss = { showAdd = false },
        ) {
            Field(title, { title = it }, "Nombre (ej. Museo del Louvre)")
            Field(date, { date = it }, "Fecha y hora")
            Field(notes, { notes = it }, "Código / notas", singleLine = false)
            AttachmentField(vm, attachment) { attachment = it }
        }
    }

    toDelete?.let { t ->
        ConfirmDialog(
            text = "Se borrará “${t.title}”.",
            onConfirm = { vm.deleteTicket(trip.id, city.id, t.id); toDelete = null },
            onDismiss = { toDelete = null },
        )
    }
}

@Composable
private fun TicketCard(t: Ticket, accent: Color, onOpen: () -> Unit, onDelete: () -> Unit) {
    Card(
        onClick = onOpen,
        enabled = t.attachment != null,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White, disabledContainerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) { Text("🎟️", fontSize = 24.sp) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(t.title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
                if (t.date.isNotBlank()) Text("📅 ${t.date}", fontSize = 13.sp, color = Muted)
                if (t.notes.isNotBlank()) Text(t.notes, fontSize = 13.sp, color = Muted)
                t.attachment?.let {
                    Text("📎 Toca para abrir ${it.name}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = accent, modifier = Modifier.padding(top = 6.dp))
                }
            }
            Text("🗑️", fontSize = 20.sp, modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onDelete)
                .padding(8.dp))
        }
    }
}
