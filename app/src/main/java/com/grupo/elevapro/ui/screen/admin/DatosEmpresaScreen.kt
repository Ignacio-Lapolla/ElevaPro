package com.grupo.elevapro.ui.screen.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.grupo.elevapro.data.model.domain.CondicionIva
import com.grupo.elevapro.data.model.domain.Empresa
import com.grupo.elevapro.data.repository.EmpresaRepository
import com.grupo.elevapro.ui.components.ElevaProTextField
import com.grupo.elevapro.ui.components.ElevaProTopAppBar
import com.grupo.elevapro.ui.components.FilledPrimaryButton
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private fun CondicionIva.label(): String = when (this) {
    CondicionIva.RESPONSABLE_INSCRIPTO -> "Responsable Inscripto"
    CondicionIva.MONOTRIBUTISTA        -> "Monotributista"
    CondicionIva.EXENTO                -> "Exento"
}

private object CuitVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val sb = StringBuilder()
        text.text.forEachIndexed { i, c ->
            if (i == 2 || i == 10) sb.append('-')
            sb.append(c)
        }
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = when {
                offset < 2  -> offset
                offset < 10 -> offset + 1
                else        -> (offset + 2).coerceAtMost(sb.length)
            }
            override fun transformedToOriginal(offset: Int): Int = when {
                offset <= 2  -> offset
                offset <= 11 -> (offset - 1).coerceAtMost(text.text.length)
                else         -> (offset - 2).coerceAtMost(text.text.length)
            }
        }
        return TransformedText(AnnotatedString(sb.toString()), mapping)
    }
}

sealed interface DatosEmpresaUiState {
    data object Loading : DatosEmpresaUiState
    data class Success(val empresa: Empresa) : DatosEmpresaUiState
    data class Error(val mensaje: String) : DatosEmpresaUiState
}

@HiltViewModel
class DatosEmpresaViewModel @Inject constructor(
    private val empresaRepository: EmpresaRepository,
) : ViewModel() {

    val estado: StateFlow<DatosEmpresaUiState> = empresaRepository.observar()
        .map { DatosEmpresaUiState.Success(it) as DatosEmpresaUiState }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DatosEmpresaUiState.Loading)

    private val _guardadoExitoso = MutableStateFlow(false)
    val guardadoExitoso: StateFlow<Boolean> = _guardadoExitoso.asStateFlow()

    fun guardar(
        razonSocial: String,
        cuitDigits: String,
        direccion: String,
        telefono: String,
        email: String,
        condicionIva: CondicionIva,
        certNombre: String?,
    ) {
        val empresaActual = (estado.value as? DatosEmpresaUiState.Success)?.empresa ?: return
        val cuitFormateado = buildString {
            cuitDigits.forEachIndexed { i, c ->
                if (i == 2 || i == 10) append('-')
                append(c)
            }
        }
        viewModelScope.launch {
            empresaRepository.actualizar(
                empresaActual.copy(
                    razonSocial           = razonSocial.trim(),
                    cuit                  = cuitFormateado,
                    direccion             = direccion.trim(),
                    telefono              = telefono.trim(),
                    email                 = email.trim(),
                    condicionIva          = condicionIva,
                    certificadoAfipNombre = certNombre,
                )
            )
            _guardadoExitoso.value = true
        }
    }

    fun onGuardadoHandled() { _guardadoExitoso.value = false }
}

@Composable
fun DatosEmpresaScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DatosEmpresaViewModel = hiltViewModel(),
) {
    val estado          by viewModel.estado.collectAsStateWithLifecycle()
    val guardadoExitoso by viewModel.guardadoExitoso.collectAsStateWithLifecycle()
    DatosEmpresaContent(
        estado            = estado,
        guardadoExitoso   = guardadoExitoso,
        onGuardadoHandled = viewModel::onGuardadoHandled,
        onGuardar         = viewModel::guardar,
        onBack            = onBack,
        modifier          = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatosEmpresaContent(
    estado: DatosEmpresaUiState,
    guardadoExitoso: Boolean,
    onGuardadoHandled: () -> Unit,
    onGuardar: (String, String, String, String, String, CondicionIva, String?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(guardadoExitoso) {
        if (guardadoExitoso) {
            snackbarHostState.showSnackbar("Cambios guardados")
            onGuardadoHandled()
        }
    }

    Scaffold(
        topBar = { ElevaProTopAppBar(titulo = "Datos de la empresa", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { padding ->
        when (val s = estado) {
            DatosEmpresaUiState.Loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            is DatosEmpresaUiState.Error -> Text(
                text = s.mensaje,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(padding).padding(16.dp),
            )

            is DatosEmpresaUiState.Success -> DatosEmpresaForm(
                empresa   = s.empresa,
                onGuardar = onGuardar,
                modifier  = Modifier.fillMaxSize().padding(padding),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatosEmpresaForm(
    empresa: Empresa,
    onGuardar: (String, String, String, String, String, CondicionIva, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var razonSocial  by remember { mutableStateOf(empresa.razonSocial) }
    var cuit         by remember { mutableStateOf(empresa.cuit.filter { it.isDigit() }) }
    var direccion    by remember { mutableStateOf(empresa.direccion) }
    var telefono     by remember { mutableStateOf(empresa.telefono) }
    var email        by remember { mutableStateOf(empresa.email) }
    var condicionIva by remember { mutableStateOf(empresa.condicionIva) }
    var certNombre   by remember { mutableStateOf(empresa.certificadoAfipNombre) }
    var dropdownOpen by remember { mutableStateOf(false) }

    LaunchedEffect(empresa) {
        razonSocial  = empresa.razonSocial
        cuit         = empresa.cuit.filter { it.isDigit() }
        direccion    = empresa.direccion
        telefono     = empresa.telefono
        email        = empresa.email
        condicionIva = empresa.condicionIva
        certNombre   = empresa.certificadoAfipNombre
    }

    val puedeGuardar = razonSocial.isNotBlank() && cuit.length == 11 && email.isNotBlank()

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ElevaProTextField(value = razonSocial, onValueChange = { razonSocial = it }, label = "Razón social")

        OutlinedTextField(
            value = cuit,
            onValueChange = { cuit = it.filter { c -> c.isDigit() }.take(11) },
            label = { Text("CUIT") },
            visualTransformation = CuitVisualTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        ElevaProTextField(value = direccion, onValueChange = { direccion = it }, label = "Dirección")
        ElevaProTextField(
            value = telefono, onValueChange = { telefono = it }, label = "Teléfono",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        )
        ElevaProTextField(
            value = email, onValueChange = { email = it }, label = "Email",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )

        ExposedDropdownMenuBox(
            expanded = dropdownOpen,
            onExpandedChange = { dropdownOpen = !dropdownOpen },
        ) {
            OutlinedTextField(
                value = condicionIva.label(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Condición IVA") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownOpen) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = dropdownOpen,
                onDismissRequest = { dropdownOpen = false },
            ) {
                CondicionIva.entries.forEach { opcion ->
                    DropdownMenuItem(
                        text = { Text(opcion.label()) },
                        onClick = {
                            condicionIva = opcion
                            dropdownOpen = false
                        },
                    )
                }
            }
        }

        HorizontalDivider()

        Text("Certificado AFIP", style = MaterialTheme.typography.titleMedium)

        if (certNombre == null) {
            OutlinedButton(
                onClick = { certNombre = "certificado.pfx" },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Subir certificado") }
        } else {
            OutlinedButton(
                onClick = { certNombre = null },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("$certNombre ✓  (toca para quitar)") }
        }

        FilledPrimaryButton(
            text = "Guardar cambios",
            onClick = { onGuardar(razonSocial, cuit, direccion, telefono, email, condicionIva, certNombre) },
            enabled = puedeGuardar,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
