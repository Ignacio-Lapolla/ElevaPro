package com.grupo.elevapro.ui.screen.clientes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.grupo.elevapro.data.model.domain.Cliente
import com.grupo.elevapro.data.repository.ClienteRepository
import com.grupo.elevapro.data.repository.FakeMockData
import com.grupo.elevapro.ui.components.ElevaProTextField
import com.grupo.elevapro.ui.components.ElevaProTopAppBar
import com.grupo.elevapro.ui.components.FilledPrimaryButton
import com.grupo.elevapro.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class AgregarClienteFormState(
    val nombre: String = "",
    val telefono: String = "",
    val email: String = "",
    val direccion: String = "",
    val cuit: String = "",
    val supervisorId: String? = null,
    val supervisorNombre: String = "",
    val notas: String = "",
    val guardando: Boolean = false,
    val error: String? = null,
) {
    val isFormValid: Boolean get() = nombre.isNotBlank() && telefono.isNotBlank()
}

sealed interface AgregarClienteUiState {
    data class Formulario(val form: AgregarClienteFormState = AgregarClienteFormState()) : AgregarClienteUiState
    data object Guardado : AgregarClienteUiState
}

@HiltViewModel
class AgregarClienteViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ClienteRepository,
) : ViewModel() {

    private val clienteId: String? = savedStateHandle[Screen.AgregarCliente.ARG_ID]
    val esEdicion: Boolean get() = clienteId != null

    private val _estado = MutableStateFlow<AgregarClienteUiState>(AgregarClienteUiState.Formulario())
    val estado: StateFlow<AgregarClienteUiState> = _estado.asStateFlow()

    val supervisores = FakeMockData.supervisores

    init {
        if (clienteId != null) {
            viewModelScope.launch {
                val cliente = repository.obtenerPorId(clienteId)
                if (cliente != null) {
                    val supervisorNombre = cliente.supervisorId
                        ?.let { repository.obtenerSupervisorNombre(it) } ?: ""
                    _estado.value = AgregarClienteUiState.Formulario(
                        AgregarClienteFormState(
                            nombre = cliente.nombre,
                            telefono = cliente.telefono,
                            email = cliente.email,
                            direccion = cliente.direccion,
                            cuit = cliente.cuit,
                            supervisorId = cliente.supervisorId,
                            supervisorNombre = supervisorNombre,
                            notas = cliente.notas ?: "",
                        )
                    )
                }
            }
        }
    }

    fun onNombre(v: String) = updateForm { copy(nombre = v) }
    fun onTelefono(v: String) = updateForm { copy(telefono = v) }
    fun onEmail(v: String) = updateForm { copy(email = v) }
    fun onDireccion(v: String) = updateForm { copy(direccion = v) }
    fun onCuit(v: String) = updateForm { copy(cuit = v.filter { it.isDigit() }.take(11)) }
    fun onSupervisor(id: String?, nombre: String) = updateForm { copy(supervisorId = id, supervisorNombre = nombre) }
    fun onNotas(v: String) = updateForm { copy(notas = v) }

    fun guardar() {
        val formulario = (_estado.value as? AgregarClienteUiState.Formulario)?.form ?: return
        if (!formulario.isFormValid) return
        viewModelScope.launch {
            updateForm { copy(guardando = true, error = null) }
            try {
                val cliente = Cliente(
                    id = clienteId ?: UUID.randomUUID().toString(),
                    nombre = formulario.nombre.trim(),
                    direccion = formulario.direccion.trim(),
                    telefono = formulario.telefono.trim(),
                    email = formulario.email.trim(),
                    cuit = formulario.cuit,
                    notas = formulario.notas.trim().ifBlank { null },
                    supervisorId = formulario.supervisorId,
                )
                if (esEdicion) repository.actualizar(cliente) else repository.agregar(cliente)
                _estado.value = AgregarClienteUiState.Guardado
            } catch (e: Exception) {
                updateForm { copy(guardando = false, error = "No se pudo guardar el cliente") }
            }
        }
    }

    private fun updateForm(block: AgregarClienteFormState.() -> AgregarClienteFormState) {
        _estado.update { current ->
            if (current is AgregarClienteUiState.Formulario) {
                AgregarClienteUiState.Formulario(current.form.block())
            } else current
        }
    }
}

@Composable
fun AgregarClienteScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AgregarClienteViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val guardado = estado is AgregarClienteUiState.Guardado

    LaunchedEffect(guardado) {
        if (guardado) onBack()
    }

    val form = (estado as? AgregarClienteUiState.Formulario)?.form ?: AgregarClienteFormState()
    val titulo = if (viewModel.esEdicion) "Editar cliente" else "Nuevo cliente"
    val textoBoton = if (viewModel.esEdicion) "Guardar cambios" else "Agregar cliente"

    AgregarClienteContent(
        form = form,
        titulo = titulo,
        textoBoton = textoBoton,
        supervisores = viewModel.supervisores,
        onBack = onBack,
        onNombre = viewModel::onNombre,
        onTelefono = viewModel::onTelefono,
        onEmail = viewModel::onEmail,
        onDireccion = viewModel::onDireccion,
        onCuit = viewModel::onCuit,
        onSupervisor = viewModel::onSupervisor,
        onNotas = viewModel::onNotas,
        onGuardar = viewModel::guardar,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgregarClienteContent(
    form: AgregarClienteFormState,
    titulo: String,
    textoBoton: String,
    supervisores: List<com.grupo.elevapro.data.model.domain.Supervisor>,
    onBack: () -> Unit,
    onNombre: (String) -> Unit,
    onTelefono: (String) -> Unit,
    onEmail: (String) -> Unit,
    onDireccion: (String) -> Unit,
    onCuit: (String) -> Unit,
    onSupervisor: (String?, String) -> Unit,
    onNotas: (String) -> Unit,
    onGuardar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            ElevaProTopAppBar(
                titulo = titulo,
                onBack = onBack,
                acciones = {
                    IconButton(onClick = onGuardar, enabled = form.isFormValid && !form.guardando) {
                        Icon(Icons.Outlined.Check, contentDescription = "Guardar")
                    }
                },
            )
        },
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ElevaProTextField(
                value = form.nombre,
                onValueChange = onNombre,
                label = "Nombre del consorcio / cliente *",
                leadingIcon = Icons.Outlined.People,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )

            ElevaProTextField(
                value = form.telefono,
                onValueChange = onTelefono,
                label = "Teléfono *",
                leadingIcon = Icons.Outlined.Phone,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            )

            ElevaProTextField(
                value = form.email,
                onValueChange = onEmail,
                label = "E-mail (opcional)",
                leadingIcon = Icons.Outlined.Email,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )

            ElevaProTextField(
                value = form.direccion,
                onValueChange = onDireccion,
                label = "Dirección (opcional)",
                leadingIcon = Icons.Outlined.LocationOn,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )

            ElevaProTextField(
                value = form.cuit,
                onValueChange = onCuit,
                label = "NIF / CUIT (opcional)",
                leadingIcon = Icons.Outlined.Key,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = CuitVisualTransformation,
            )

            SupervisorDropdown(
                supervisores = supervisores,
                seleccionadoNombre = form.supervisorNombre,
                onSeleccion = onSupervisor,
                modifier = Modifier.fillMaxWidth(),
            )

            ElevaProTextField(
                value = form.notas,
                onValueChange = onNotas,
                label = "Notas (opcional)",
                leadingIcon = Icons.Outlined.Notes,
                singleLine = false,
                modifier = Modifier.fillMaxWidth(),
            )

            if (form.error != null) {
                Text(
                    text = form.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            FilledPrimaryButton(
                text = if (form.guardando) "Guardando…" else textoBoton,
                onClick = onGuardar,
                enabled = form.isFormValid && !form.guardando,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupervisorDropdown(
    supervisores: List<com.grupo.elevapro.data.model.domain.Supervisor>,
    seleccionadoNombre: String,
    onSeleccion: (String?, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expandido by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { expandido = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = seleccionadoNombre,
            onValueChange = {},
            readOnly = true,
            label = { Text("Supervisor (opcional)") },
            leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false },
        ) {
            DropdownMenuItem(
                text = { Text("Sin supervisor") },
                onClick = {
                    onSeleccion(null, "")
                    expandido = false
                },
            )
            supervisores.forEach { supervisor ->
                DropdownMenuItem(
                    text = { Text(supervisor.nombre) },
                    onClick = {
                        onSeleccion(supervisor.id, supervisor.nombre)
                        expandido = false
                    },
                )
            }
        }
    }
}

// ─── Previews ───────────────────────────────────────────────────────────────

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "AgregarCliente – formulario vacío")
@Composable
private fun AgregarClienteVacioPreview() {
    com.grupo.elevapro.ui.theme.ElevaProTheme {
        AgregarClienteContent(
            form = AgregarClienteFormState(),
            titulo = "Nuevo cliente",
            textoBoton = "Agregar cliente",
            supervisores = com.grupo.elevapro.data.repository.FakeMockData.supervisores,
            onBack = {},
            onNombre = {},
            onTelefono = {},
            onEmail = {},
            onDireccion = {},
            onCuit = {},
            onSupervisor = { _, _ -> },
            onNotas = {},
            onGuardar = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "AgregarCliente – edición")
@Composable
private fun AgregarClienteEdicionPreview() {
    com.grupo.elevapro.ui.theme.ElevaProTheme {
        AgregarClienteContent(
            form = AgregarClienteFormState(
                nombre = "Cons. Prop. Lafinur 3060",
                telefono = "+54 11 4801-1234",
                email = "admin@lafinur3060.com.ar",
                direccion = "Lafinur 3060",
                cuit = "30654321891",
                supervisorNombre = "Carlos Méndez",
                notas = "Ascensor modelo 2010, revisión semestral.",
            ),
            titulo = "Editar cliente",
            textoBoton = "Guardar cambios",
            supervisores = com.grupo.elevapro.data.repository.FakeMockData.supervisores,
            onBack = {},
            onNombre = {},
            onTelefono = {},
            onEmail = {},
            onDireccion = {},
            onCuit = {},
            onSupervisor = { _, _ -> },
            onNotas = {},
            onGuardar = {},
        )
    }
}

private object CuitVisualTransformation : VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): TransformedText {
        val digits = text.text
        val out = buildString {
            digits.forEachIndexed { i, c ->
                append(c)
                if (i == 1 || i == 9) append('-')
            }
        }
        val offsetMap = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = when {
                offset <= 2 -> offset
                offset <= 10 -> offset + 1
                offset <= 11 -> offset + 2
                else -> out.length
            }
            override fun transformedToOriginal(offset: Int): Int = when {
                offset <= 2 -> offset
                offset <= 11 -> offset - 1
                offset <= 13 -> offset - 2
                else -> digits.length
            }
        }
        return TransformedText(androidx.compose.ui.text.AnnotatedString(out), offsetMap)
    }
}
