@file:OptIn(ExperimentalMaterial3Api::class)

package com.viajero.app.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.viajero.app.data.Attachment
import com.viajero.app.data.TripViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun Header(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    colors: List<Color> = listOf(Teal, TealDark),
    actions: @Composable RowScope.() -> Unit = {},
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(Brush.linearGradient(colors))
    ) {
        Row(
            Modifier
                .statusBarsPadding()
                .padding(start = 8.dp, end = 12.dp, top = 12.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                }
            } else {
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(subtitle, color = Color.White.copy(alpha = 0.88f), fontSize = 14.sp)
                }
            }
            actions()
        }
    }
}

@Composable
fun HeaderButton(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.22f),
        contentColor = Color.White,
    ) {
        Text(text, Modifier.padding(horizontal = 14.dp, vertical = 8.dp), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun EmptyState(emoji: String, title: String, hint: String) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(emoji, fontSize = 64.sp)
        Spacer(Modifier.padding(6.dp))
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink, textAlign = TextAlign.Center)
        Text(hint, fontSize = 15.sp, color = Muted, textAlign = TextAlign.Center)
    }
}

@Composable
fun FormDialog(
    title: String,
    confirmText: String = "Guardar",
    enabled: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content,
            )
        },
        confirmButton = { Button(onClick = onConfirm, enabled = enabled) { Text(confirmText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
    )
}

@Composable
fun ConfirmDialog(text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Eliminar?", fontWeight = FontWeight.Bold) },
        text = { Text(text) },
        confirmButton = {
            Button(onClick = onConfirm, colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Coral)) {
                Text("Eliminar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
    )
}

@Composable
fun Field(value: String, onChange: (String) -> Unit, label: String, singleLine: Boolean = true) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
    )
}

/** Botón para adjuntar un PDF o imagen (boleto) y mostrar el archivo elegido. */
@Composable
fun AttachmentField(vm: TripViewModel, current: Attachment?, onChange: (Attachment?) -> Unit) {
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val att = withContext(Dispatchers.IO) { vm.copyAttachment(uri) }
                onChange(att)
            }
        }
    }
    if (current == null) {
        OutlinedButton(
            onClick = { launcher.launch(arrayOf("application/pdf", "image/*")) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("📎 Adjuntar boleto (PDF o imagen)") }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("📎 ${current.name}", Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, color = Ink)
            TextButton(onClick = { onChange(null) }) { Text("Quitar") }
        }
    }
}

fun openAttachment(ctx: Context, vm: TripViewModel, a: Attachment) {
    try {
        val file = vm.attachmentFile(a)
        if (!file.exists()) {
            Toast.makeText(ctx, "No se encontró el archivo", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        val i = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, a.mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(i)
    } catch (e: Exception) {
        Toast.makeText(ctx, "No hay una app para abrir este archivo", Toast.LENGTH_SHORT).show()
    }
}
