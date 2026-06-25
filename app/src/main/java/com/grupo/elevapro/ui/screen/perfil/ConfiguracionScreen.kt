package com.grupo.elevapro.ui.screen.perfil

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grupo.elevapro.AppViewModel
import com.grupo.elevapro.ui.components.SectionTitle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

// ── UiState & ViewModel ──────────────────────────────────────────────────────

data class ConfiguracionUiState(
    val notificacionesPush: Boolean = true,
    val sincronizacionAuto: Boolean = true,
    val idioma: String = "es",
)

@HiltViewModel
class ConfiguracionViewModel @Inject constructor() : ViewModel() {

    private val _estado = MutableStateFlow(ConfiguracionUiState())
    val estado: StateFlow<ConfiguracionUiState> = _estado.asStateFlow()

    fun onNotificacionesPush(value: Boolean) { _estado.update { it.copy(notificacionesPush = value) } }
    fun onSincronizacionAuto(value: Boolean) { _estado.update { it.copy(sincronizacionAuto = value) } }
    fun onIdioma(value: String) { _estado.update { it.copy(idioma = value) } }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun ConfiguracionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConfiguracionViewModel = hiltViewModel(),
    appViewModel: AppViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val modoOscuro by appViewModel.modoOscuro.collectAsStateWithLifecycle()
    ConfiguracionContent(
        estado = estado,
        modoOscuro = modoOscuro,
        onBack = onBack,
        onModoOscuro = appViewModel::onModoOscuro,
        onNotificacionesPush = viewModel::onNotificacionesPush,
        onSincronizacionAuto = viewModel::onSincronizacionAuto,
        onIdioma = viewModel::onIdioma,
        modifier = modifier,
    )
}

// ── Content ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfiguracionContent(
    estado: ConfiguracionUiState,
    modoOscuro: Boolean,
    onBack: () -> Unit,
    onModoOscuro: (Boolean) -> Unit,
    onNotificacionesPush: (Boolean) -> Unit,
    onSincronizacionAuto: (Boolean) -> Unit,
    onIdioma: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val idiomaLabel = if (estado.idioma == "es") "Español" else "English"
    var expandidoIdioma by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            SectionTitle("Apariencia y comportamiento")
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                SwitchListItem(
                    icon = Icons.Outlined.DarkMode,
                    label = "Modo oscuro",
                    subtitle = if (modoOscuro) "Activado" else "Desactivado",
                    checked = modoOscuro,
                    onCheckedChange = onModoOscuro,
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SwitchListItem(
                    icon = Icons.Outlined.Notifications,
                    label = "Notificaciones push",
                    subtitle = if (estado.notificacionesPush) "Activadas" else "Desactivadas",
                    checked = estado.notificacionesPush,
                    onCheckedChange = onNotificacionesPush,
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SwitchListItem(
                    icon = Icons.Outlined.Sync,
                    label = "Sincronización automática",
                    subtitle = if (estado.sincronizacionAuto) "Activada" else "Desactivada",
                    checked = estado.sincronizacionAuto,
                    onCheckedChange = onSincronizacionAuto,
                )
            }

            SectionTitle("Idioma")
            ExposedDropdownMenuBox(
                expanded = expandidoIdioma,
                onExpandedChange = { expandidoIdioma = it },
            ) {
                OutlinedTextField(
                    value = idiomaLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Idioma de la aplicación") },
                    leadingIcon = { Icon(Icons.Outlined.Language, contentDescription = "Idioma") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandidoIdioma) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                )
                ExposedDropdownMenu(
                    expanded = expandidoIdioma,
                    onDismissRequest = { expandidoIdioma = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Español") },
                        onClick = { onIdioma("es"); expandidoIdioma = false },
                    )
                    DropdownMenuItem(
                        text = { Text("English") },
                        onClick = { onIdioma("en"); expandidoIdioma = false },
                    )
                }
            }

            SectionTitle("Almacenamiento")
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text("Borrar caché", style = MaterialTheme.typography.titleSmall) },
                    supportingContent = { Text("Libera espacio eliminando archivos temporales") },
                    leadingContent = { Icon(Icons.Outlined.Delete, contentDescription = "Borrar caché") },
                    trailingContent = {
                        FilledTonalButton(onClick = {
                            Toast.makeText(context, "Caché borrada correctamente", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Borrar")
                        }
                    },
                )
            }

            Text(
                text = "Versión: 0.1.0-demo",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun SwitchListItem(
    icon: ImageVector,
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(label, style = MaterialTheme.typography.titleSmall) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = label) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        },
        modifier = modifier,
    )
}
