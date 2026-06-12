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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.grupo.elevapro.data.model.domain.Rol
import com.grupo.elevapro.data.model.domain.Usuario
import com.grupo.elevapro.data.repository.AuthRepository
import com.grupo.elevapro.ui.theme.Primary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Login state machine ────────────────────────────────────────────────────

sealed interface EstadoLogin {
    data object Idle : EstadoLogin
    data object Cargando : EstadoLogin
    data object Exito : EstadoLogin
    data class Error(val mensaje: String) : EstadoLogin
}

data class LoginUiState(
    val empresa: String = "",
    val email: String = "",
    val password: String = "",
    val mostrarPassword: Boolean = false,
    val estadoLogin: EstadoLogin = EstadoLogin.Idle,
    val perfilDetectado: Usuario? = null,
) {
    val puedeIngresar: Boolean
        get() = empresa.isNotBlank() && email.isNotBlank() && password.isNotBlank() &&
                estadoLogin !is EstadoLogin.Cargando
}

private fun LoginUiState.dismissError() =
    if (estadoLogin is EstadoLogin.Error) copy(estadoLogin = EstadoLogin.Idle) else this

sealed interface LoginEvent {
    data class EmpresaChange(val value: String) : LoginEvent
    data class EmailChange(val value: String) : LoginEvent
    data class PasswordChange(val value: String) : LoginEvent
    data object TogglePassword : LoginEvent
    data object Ingresar : LoginEvent
}

// ── ViewModel ──────────────────────────────────────────────────────────────

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(LoginUiState())
    val estado: StateFlow<LoginUiState> = _estado.asStateFlow()

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.EmpresaChange  -> _estado.update { it.copy(empresa = event.value).dismissError() }
            is LoginEvent.EmailChange    -> _estado.update {
                it.copy(
                    email = event.value,
                    perfilDetectado = authRepository.buscarPerfilPorEmail(event.value),
                ).dismissError()
            }
            is LoginEvent.PasswordChange -> _estado.update { it.copy(password = event.value).dismissError() }
            LoginEvent.TogglePassword    -> _estado.update { it.copy(mostrarPassword = !it.mostrarPassword) }
            LoginEvent.Ingresar          -> ingresar()
        }
    }

    private fun ingresar() {
        val s = _estado.value
        if (!s.puedeIngresar) return
        viewModelScope.launch {
            _estado.update { it.copy(estadoLogin = EstadoLogin.Cargando) }
            authRepository.login(s.empresa, s.email, s.password)
                .onSuccess { _estado.update { it.copy(estadoLogin = EstadoLogin.Exito) } }
                .onFailure { e -> _estado.update { it.copy(estadoLogin = EstadoLogin.Error(e.message ?: "Error")) } }
        }
    }
}

// ── Screen ─────────────────────────────────────────────────────────────────

@Composable
fun LoginScreen(
    onLoginOk: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(estado.estadoLogin) { if (estado.estadoLogin is EstadoLogin.Exito) onLoginOk() }
    LoginContent(estado = estado, onEvent = viewModel::onEvent, modifier = modifier)
}

@Composable
private fun LoginContent(
    estado: LoginUiState,
    onEvent: (LoginEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Primary)
            .verticalScroll(scroll),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HeroLogin()
            TarjetaFormulario(estado = estado, onEvent = onEvent)
        }
    }
}

@Composable
private fun HeroLogin(modifier: Modifier = Modifier) {
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
                    .size(88.dp)
                    .background(Color.White.copy(alpha = 0.20f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Apartment,
                    contentDescription = "Logo de ElevaPro",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "ElevaPro",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "GESTIÓN DE MANTENIMIENTO",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.85f),
                letterSpacing = 2.sp,
            )
        }
    }
}

@Composable
private fun TarjetaFormulario(
    estado: LoginUiState,
    onEvent: (LoginEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Iniciar sesión",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Ingresá tus datos para continuar",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FilledFormField(
                value = estado.empresa,
                onValueChange = { onEvent(LoginEvent.EmpresaChange(it)) },
                label = "Número de empresa",
                leadingIcon = Icons.Outlined.Tag,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            FilledFormField(
                value = estado.email,
                onValueChange = { onEvent(LoginEvent.EmailChange(it)) },
                label = "Correo electrónico",
                leadingIcon = Icons.Outlined.MailOutline,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )
            FilledFormField(
                value = estado.password,
                onValueChange = { onEvent(LoginEvent.PasswordChange(it)) },
                label = "Contraseña",
                leadingIcon = Icons.Outlined.Lock,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (estado.mostrarPassword) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { onEvent(LoginEvent.TogglePassword) }) {
                        Icon(
                            imageVector = if (estado.mostrarPassword) Icons.Outlined.VisibilityOff
                                          else Icons.Outlined.Visibility,
                            contentDescription = if (estado.mostrarPassword) "Ocultar contraseña"
                                                 else "Mostrar contraseña",
                        )
                    }
                },
            )

            if (estado.perfilDetectado != null) {
                PerfilDetectadoPill(perfil = estado.perfilDetectado)
            }

            if (estado.estadoLogin is EstadoLogin.Error) {
                Text(
                    text = estado.estadoLogin.mensaje,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = { onEvent(LoginEvent.Ingresar) },
                enabled = estado.puedeIngresar,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = Color.White,
                    disabledContainerColor = MaterialTheme.colorScheme.outlineVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Text(
                    text = if (estado.estadoLogin is EstadoLogin.Cargando) "Ingresando…"
                           else "Iniciar sesión",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            DividerOConTexto()

            OutlinedButton(
                onClick = { /* TODO: forgot password */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    text = "Olvidé mi contraseña",
                    color = Primary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(4.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "¿Problemas para ingresar?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { /* TODO: contactar */ }) {
                    Text(
                        text = "Contactar al administrador",
                        color = Primary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun FilledFormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(leadingIcon, contentDescription = null) },
        trailingIcon = trailingIcon,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            focusedIndicatorColor = Primary,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
private fun PerfilDetectadoPill(perfil: Usuario, modifier: Modifier = Modifier) {
    val esAdmin = perfil.rol == Rol.ADMINISTRADOR
    val bg = if (esAdmin) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFE3EBFC)
    val iconBg = if (esAdmin) Color(0xFF37474F) else Primary
    Surface(
        color = bg,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconBg, RoundedCornerShape(50)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (esAdmin) Icons.Outlined.AdminPanelSettings else Icons.Outlined.Person,
                    contentDescription = "Rol detectado",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = perfil.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (esAdmin) "Administrador" else "Operativo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DividerOConTexto(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text("ó", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}
