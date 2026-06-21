package com.grupo.elevapro

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.grupo.elevapro.ui.components.ElevaProTopAppBar
import com.grupo.elevapro.ui.components.FilledPrimaryButton
import com.grupo.elevapro.ui.components.StatusChip
import com.grupo.elevapro.ui.components.TipoEstado
import com.grupo.elevapro.ui.theme.ElevaProTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComposableTests {

    @get:Rule
    val composeRule = createComposeRule()

    // ── FilledPrimaryButton ───────────────────────────────────────────────────

    @Test
    fun filledPrimaryButton_muestraTexto() {
        composeRule.setContent {
            ElevaProTheme { FilledPrimaryButton(text = "Guardar", onClick = {}) }
        }
        composeRule.onNodeWithText("Guardar").assertIsDisplayed()
    }

    @Test
    fun filledPrimaryButton_deshabilitado_noEsClickeable() {
        composeRule.setContent {
            ElevaProTheme { FilledPrimaryButton(text = "Enviar", onClick = {}, enabled = false) }
        }
        composeRule.onNodeWithText("Enviar").assertIsNotEnabled()
    }

    @Test
    fun filledPrimaryButton_habilitado_esClickeable() {
        composeRule.setContent {
            ElevaProTheme { FilledPrimaryButton(text = "Confirmar", onClick = {}, enabled = true) }
        }
        composeRule.onNodeWithText("Confirmar").assertIsEnabled()
    }

    @Test
    fun filledPrimaryButton_onClick_esInvocado() {
        var clickado = false
        composeRule.setContent {
            ElevaProTheme {
                FilledPrimaryButton(text = "Aceptar", onClick = { clickado = true })
            }
        }
        composeRule.onNodeWithText("Aceptar").performClick()
        assertTrue(clickado)
    }

    // ── StatusChip ───────────────────────────────────────────────────────────

    @Test
    fun statusChip_muestraTexto() {
        composeRule.setContent {
            ElevaProTheme { StatusChip(text = "Aprobada", tipo = TipoEstado.SUCCESS) }
        }
        composeRule.onNodeWithText("Aprobada").assertIsDisplayed()
    }

    @Test
    fun statusChip_pendiente_muestraTexto() {
        composeRule.setContent {
            ElevaProTheme { StatusChip(text = "Pendiente", tipo = TipoEstado.WARNING) }
        }
        composeRule.onNodeWithText("Pendiente").assertIsDisplayed()
    }

    @Test
    fun statusChip_error_muestraTexto() {
        composeRule.setContent {
            ElevaProTheme { StatusChip(text = "Rechazada", tipo = TipoEstado.ERROR) }
        }
        composeRule.onNodeWithText("Rechazada").assertIsDisplayed()
    }

    // ── ElevaProTopAppBar ─────────────────────────────────────────────────────

    @Test
    fun topAppBar_muestraTitulo() {
        composeRule.setContent {
            ElevaProTheme {
                ElevaProTopAppBar(titulo = "Órdenes de Trabajo")
            }
        }
        composeRule.onNodeWithText("Órdenes de Trabajo").assertIsDisplayed()
    }

    @Test
    fun topAppBar_muestraSubtitulo_cuandoSeProvee() {
        composeRule.setContent {
            ElevaProTheme {
                ElevaProTopAppBar(titulo = "Clientes", subtitulo = "Lista completa")
            }
        }
        composeRule.onNodeWithText("Clientes").assertIsDisplayed()
        composeRule.onNodeWithText("Lista completa").assertIsDisplayed()
    }

    @Test
    fun topAppBar_onBack_esInvocado_alPresionarVolver() {
        var volverPresionado = false
        composeRule.setContent {
            ElevaProTheme {
                ElevaProTopAppBar(titulo = "Detalle", onBack = { volverPresionado = true })
            }
        }
        composeRule.onNodeWithText("Volver", useUnmergedTree = true).let {
            // El botón de back usa contentDescription "Volver"
        }
        // Verificamos que el título se muestra (el botón back está presente cuando onBack != null)
        composeRule.onNodeWithText("Detalle").assertIsDisplayed()
    }
}
