package com.grupo.elevapro.ui.screen.facturacion

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.grupo.elevapro.data.model.domain.Cliente
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DatePickerField(
    label: String,
    valor: String,
    onFecha: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var mostrar by remember { mutableStateOf(false) }
    val dpState = rememberDatePickerState()

    if (mostrar) {
        DatePickerDialog(
            onDismissRequest = { mostrar = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let { millis ->
                        onFecha(epochToDisplay(millis))
                    }
                    mostrar = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrar = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = dpState)
        }
    }

    OutlinedTextField(
        value = valor,
        onValueChange = {},
        readOnly = true,
        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
        leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
        modifier = modifier,
        interactionSource = remember { MutableInteractionSource() }
            .also { source ->
                LaunchedEffect(source) {
                    source.interactions.collect { interaction ->
                        if (interaction is PressInteraction.Release) {
                            mostrar = true
                        }
                    }
                }
            },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ClienteDropdown(
    clientes: List<Cliente>,
    seleccionadoNombre: String,
    onSeleccion: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expandido by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expandido, onExpandedChange = { expandido = it }, modifier = modifier) {
        OutlinedTextField(
            value = seleccionadoNombre,
            onValueChange = {},
            readOnly = true,
            label = { Text("Cliente *") },
            leadingIcon = { Icon(Icons.Outlined.People, contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            clientes.forEach { cliente ->
                DropdownMenuItem(
                    text = { Text(cliente.nombre) },
                    onClick = { onSeleccion(cliente.id, cliente.nombre); expandido = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TipoDropdown(
    seleccionado: String,
    onSeleccion: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tipos = listOf("A", "B", "C")
    var expandido by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expandido, onExpandedChange = { expandido = it }, modifier = modifier) {
        OutlinedTextField(
            value = if (seleccionado.isNotBlank()) "Factura $seleccionado" else "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Tipo de factura *") },
            leadingIcon = { Icon(Icons.Outlined.Receipt, contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            tipos.forEach { tipo ->
                DropdownMenuItem(
                    text = { Text("Factura $tipo") },
                    onClick = { onSeleccion(tipo); expandido = false },
                )
            }
        }
    }
}

@Composable
internal fun FilaImporte(
    label: String,
    valor: String,
    negrita: Boolean = false,
    colorValor: Color = Color.Unspecified,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = if (negrita) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = valor,
            style = if (negrita) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            else MaterialTheme.typography.bodyMedium,
            color = if (colorValor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else colorValor,
        )
    }
}

internal fun formatearMonto(monto: Double): String =
    NumberFormat.getCurrencyInstance(Locale("es", "AR")).format(monto)

internal fun epochToDisplay(millis: Long): String =
    SimpleDateFormat("dd MMM yyyy", Locale("es", "AR")).format(Date(millis))
