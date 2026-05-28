package com.grupo.elevapro.ui.screen.perfil

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.grupo.elevapro.ui.components.SectionTitle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Datos estáticos ───────────────────────────────────────────────────────────

private data class FaqItem(val pregunta: String, val respuesta: String)

private val faqs = listOf(
    FaqItem(
        "¿Cómo registro una nueva orden de trabajo?",
        "Desde la pantalla de Órdenes, tocá el botón '+' (FAB) en la esquina inferior derecha. Completá los datos del cliente, el tipo de servicio y la plantilla correspondiente. Guardá para crear la orden.",
    ),
    FaqItem(
        "¿Cómo firmo una orden de conformidad?",
        "Abrí el detalle de la orden y tocá 'Solicitar firma'. El cliente podrá firmar directamente en la pantalla. Una vez firmada, la orden queda marcada como completada y puede facturarse.",
    ),
    FaqItem(
        "¿Puedo usar la app sin conexión a internet?",
        "En la versión demo, los datos son locales y no requieren conexión. En producción (H2), la app sincroniza al recuperar la conexión y permite operar en modo offline con los datos cacheados.",
    ),
    FaqItem(
        "¿Cómo genero una factura para una orden firmada?",
        "Desde la sección de Facturación, tocá 'Nueva factura' y seleccioná la orden firmada correspondiente. Revisá los datos y confirmá para enviarla a ARCA.",
    ),
    FaqItem(
        "¿Qué diferencia hay entre el rol Operativo y Administrador?",
        "El rol Operativo puede ver y gestionar órdenes, clientes y artículos propios. El Administrador tiene acceso completo: gestión de usuarios, roles, supervisores, plantillas, datos de empresa y toda la facturación.",
    ),
)

// ── Eventos one-shot ──────────────────────────────────────────────────────────

private sealed interface AyudaEvent {
    data object ContactarEmail : AyudaEvent
    data object ContactarWhatsApp : AyudaEvent
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class AyudaSoporteViewModel @Inject constructor() : ViewModel() {

    private val _eventos = MutableSharedFlow<AyudaEvent>()
    val eventos: SharedFlow<AyudaEvent> = _eventos.asSharedFlow()

    fun onContactarEmail() { viewModelScope.launch { _eventos.emit(AyudaEvent.ContactarEmail) } }
    fun onContactarWhatsApp() { viewModelScope.launch { _eventos.emit(AyudaEvent.ContactarWhatsApp) } }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun AyudaSoporteScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AyudaSoporteViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.eventos.collect { evento ->
            when (evento) {
                AyudaEvent.ContactarEmail -> try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("mailto:soporte@elevapro.com.ar")))
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, "No se encontró cliente de email", Toast.LENGTH_SHORT).show()
                }
                AyudaEvent.ContactarWhatsApp -> try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/5491155551234")))
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    AyudaSoporteContent(
        onBack = onBack,
        onContactarEmail = viewModel::onContactarEmail,
        onContactarWhatsApp = viewModel::onContactarWhatsApp,
        modifier = modifier,
    )
}

// ── Content ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AyudaSoporteContent(
    onBack: () -> Unit,
    onContactarEmail: () -> Unit,
    onContactarWhatsApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expandidoIndex by remember { mutableIntStateOf(-1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ayuda y Soporte", style = MaterialTheme.typography.titleLarge) },
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

            SectionTitle("Preguntas frecuentes")
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                faqs.forEachIndexed { index, faq ->
                    FaqAccordion(
                        item = faq,
                        expandido = expandidoIndex == index,
                        onToggle = { expandidoIndex = if (expandidoIndex == index) -1 else index },
                    )
                    if (index < faqs.size - 1) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }

            SectionTitle("Contactar soporte")
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FilledTonalButton(
                        onClick = onContactarEmail,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Outlined.Email, contentDescription = "Enviar email de soporte", modifier = Modifier.padding(end = 6.dp))
                        Text("Email")
                    }
                    FilledTonalButton(
                        onClick = onContactarWhatsApp,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Outlined.Chat, contentDescription = "Contactar por WhatsApp", modifier = Modifier.padding(end = 6.dp))
                        Text("WhatsApp")
                    }
                }
            }

            SectionTitle("Acerca de")
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Info, contentDescription = "Información de versión", tint = MaterialTheme.colorScheme.primary)
                        Text("ElevaPro", style = MaterialTheme.typography.titleMedium)
                    }
                    AcercaDeRow("Versión", "0.1.0-demo")
                    AcercaDeRow("Plataforma", "Android · Jetpack Compose")
                    AcercaDeRow("Empresa", "Ascensores Rápidos S.A.")
                    AcercaDeRow("Año", "2026")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Desarrollado por el equipo ElevaPro para la gestión integral de servicios de ascensores.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Componentes ───────────────────────────────────────────────────────────────

@Composable
private fun FaqAccordion(
    item: FaqItem,
    expandido: Boolean,
    onToggle: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.pregunta,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
            )
            Icon(
                imageVector = if (expandido) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = if (expandido) "Colapsar" else "Expandir",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AnimatedVisibility(visible = expandido) {
            Text(
                text = item.respuesta,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).padding(bottom = 14.dp),
            )
        }
    }
}

@Composable
private fun AcercaDeRow(clave: String, valor: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(clave, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.labelLarge)
    }
}
