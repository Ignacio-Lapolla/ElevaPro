package com.grupo.elevapro

import com.grupo.elevapro.data.model.domain.Permiso
import com.grupo.elevapro.data.model.domain.Rol
import com.grupo.elevapro.data.model.domain.permisosDefault
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PermisosRepositoryTest {

    @Test
    fun `inicializarSiVacio siembra los defaults del rol`() = runTest {
        val repo = FakePermisosRepository()
        repo.inicializarSiVacio("u1", Rol.OPERATIVO)

        val permisos = repo.observarPermisos("u1").first()
        assertEquals(Rol.OPERATIVO.permisosDefault, permisos)
    }

    @Test
    fun `inicializarSiVacio no sobreescribe si ya hay permisos`() = runTest {
        val repo = FakePermisosRepository()
        val permisosCustom = setOf(Permiso.VER_ORDENES)
        repo.actualizar("u1", permisosCustom)

        repo.inicializarSiVacio("u1", Rol.ADMINISTRADOR)  // no debería sobreescribir

        val permisos = repo.observarPermisos("u1").first()
        assertEquals(permisosCustom, permisos)
    }

    @Test
    fun `actualizar reemplaza permisos completamente`() = runTest {
        val repo = FakePermisosRepository()
        repo.actualizar("u1", setOf(Permiso.VER_ORDENES, Permiso.CREAR_ORDENES))
        repo.actualizar("u1", setOf(Permiso.VER_CLIENTES))

        val permisos = repo.observarPermisos("u1").first()
        assertEquals(setOf(Permiso.VER_CLIENTES), permisos)
    }

    @Test
    fun `usuario sin permisos emite set vacio`() = runTest {
        val repo = FakePermisosRepository()
        val permisos = repo.observarPermisos("no-existe").first()
        assertTrue(permisos.isEmpty())
    }

    @Test
    fun `permisos de admin incluyen todos los permisos`() = runTest {
        val repo = FakePermisosRepository()
        repo.inicializarSiVacio("admin1", Rol.ADMINISTRADOR)

        val permisos = repo.observarPermisos("admin1").first()
        assertTrue(Permiso.GESTIONAR_USUARIOS in permisos)
        assertTrue(Permiso.VER_FACTURAS in permisos)
        assertTrue(Permiso.GENERAR_FACTURAS in permisos)
    }
}
