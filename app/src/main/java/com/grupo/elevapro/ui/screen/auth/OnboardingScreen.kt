package com.grupo.elevapro.ui.screen.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grupo.elevapro.ui.theme.Primary
import com.grupo.elevapro.ui.theme.Tertiary
import kotlinx.coroutines.launch

private data class Feature(
    val icon: ImageVector,
    val texto: String,
    val tintBg: Color,
    val tintFg: Color,
)

private sealed interface OnboardingPagina {
    val heroIcon: ImageVector
    val heroTitulo: String
    data class Texto(
        override val heroIcon: ImageVector,
        override val heroTitulo: String,
        val cardTitulo: String,
        val cardDescripcion: String,
    ) : OnboardingPagina

    data class Features(
        override val heroIcon: ImageVector,
        override val heroTitulo: String,
        val cardTitulo: String,
        val features: List<Feature>,
    ) : OnboardingPagina

    data class Final(
        override val heroIcon: ImageVector,
        override val heroTitulo: String,
        val cardTitulo: String,
        val cardDescripcion: String,
    ) : OnboardingPagina
}

private val paginas: List<OnboardingPagina> = listOf(
    OnboardingPagina.Texto(
        heroIcon = Icons.Outlined.Apartment,
        heroTitulo = "ElevaPro",
        cardTitulo = "Bienvenido a ElevaPro",
        cardDescripcion = "La herramienta que necesitás para gestionar el mantenimiento de ascensores desde cualquier lugar.",
    ),
    OnboardingPagina.Features(
        heroIcon = Icons.Outlined.Folder,
        heroTitulo = "Todo en un solo lugar",
        cardTitulo = "Gestiona cada aspecto de tu operación desde el celular",
        features = listOf(
            Feature(Icons.Outlined.Edit, "Órdenes de trabajo con firma digital", Color(0xFFE3EBFC), Color(0xFF1565C0)),
            Feature(Icons.Outlined.People, "Clientes, stakeholders y consorcios organizados", Color(0xFFFFE8D6), Color(0xFFFF6D00)),
            Feature(Icons.Outlined.Link, "Integración con facturas electrónicas de ARCA", Color(0xFFD6F0DA), Color(0xFF2E7D32)),
            Feature(Icons.Outlined.Image, "Saque fotos del trabajo desde la app", Color(0xFFFFF1D6), Color(0xFFFFA000)),
        ),
    ),
    OnboardingPagina.Final(
        heroIcon = Icons.Outlined.Wifi,
        heroTitulo = "Todo en un solo lugar",
        cardTitulo = "Siempre disponible",
        cardDescripcion = "ElevaPro funciona incluso sin internet. Tus órdenes y datos se sincronizan automáticamente cuando recuperás la señal.",
    ),
)

@Composable
fun OnboardingScreen(
    onFin: () -> Unit,
    onIniciarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { paginas.size })
    val scope = rememberCoroutineScope()
    val esUltima = pagerState.currentPage == paginas.lastIndex

    Box(modifier = modifier.fillMaxSize().background(Primary)) {
        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) { index ->
                PaginaOnboarding(pagina = paginas[index])
            }

            BotoneraOnboarding(
                esUltima = esUltima,
                paginaActual = pagerState.currentPage,
                totalPaginas = paginas.size,
                onSiguiente = {
                    if (esUltima) onFin()
                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                onSaltar = onFin,
                onIniciarSesion = onIniciarSesion,
            )
        }
    }
}

@Composable
private fun PaginaOnboarding(pagina: OnboardingPagina, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        HeroAzul(icon = pagina.heroIcon, titulo = pagina.heroTitulo, mostrarSubtitulo = pagina is OnboardingPagina.Texto)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            when (pagina) {
                is OnboardingPagina.Texto -> ContenidoTexto(pagina.cardTitulo, pagina.cardDescripcion)
                is OnboardingPagina.Features -> ContenidoFeatures(pagina.cardTitulo, pagina.features)
                is OnboardingPagina.Final -> ContenidoTexto(pagina.cardTitulo, pagina.cardDescripcion)
            }
        }
    }
}

@Composable
private fun HeroAzul(
    icon: ImageVector,
    titulo: String,
    mostrarSubtitulo: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 240.dp)
            .background(Primary)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(Color.White.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = titulo,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = titulo,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            if (mostrarSubtitulo) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "GESTIÓN DE MANTENIMIENTO",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    letterSpacing = 2.sp,
                )
            }
        }
    }
}

@Composable
private fun ContenidoTexto(titulo: String, descripcion: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = descripcion,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ContenidoFeatures(titulo: String, features: List<Feature>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        features.forEach { feature ->
            FeatureItem(feature)
        }
    }
}

@Composable
private fun FeatureItem(feature: Feature, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(feature.tintBg, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = feature.icon,
                contentDescription = feature.texto,
                tint = feature.tintFg,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = feature.texto,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun BotoneraOnboarding(
    esUltima: Boolean,
    paginaActual: Int,
    totalPaginas: Int,
    onSiguiente: () -> Unit,
    onSaltar: () -> Unit,
    onIniciarSesion: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PagerDots(count = totalPaginas, current = paginaActual)
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onSiguiente,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (esUltima) Tertiary else Primary,
                    contentColor = if (esUltima) Color.Black else Color.White,
                ),
            ) {
                Text(
                    text = if (esUltima) "Comenzar ahora!" else "Siguiente",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(8.dp))
            if (esUltima) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("¿Ya tenés cuenta? ", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onIniciarSesion, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                        Text(
                            text = "Iniciá sesión",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Primary,
                        )
                    }
                }
            } else {
                TextButton(onClick = onSaltar) {
                    Text("Saltar", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Text(
                text = "v1.0.0 · ElevaPro S.A.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun PagerDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { i ->
            val ancho = if (i == current) 28.dp else 8.dp
            val color = if (i == current) Primary else MaterialTheme.colorScheme.outlineVariant
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(ancho)
                    .background(color, RoundedCornerShape(50)),
            )
        }
    }
}

