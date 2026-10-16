package org.viajedragos.modelo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class InventarioPocionesTest {
    private Jugador jugador() {
        return new Jugador("Prueba", "", 6, 2, 3, 2, 2, 3, 24);
    }

    @Test
    void ambosConstructoresComienzanConInventarioVacio() {
        for (Jugador j : new Jugador[]{new Jugador(), jugador()}) {
            assertTrue(j.getInventarioPociones().isEmpty());
            assertEquals(0, j.getCantidadPociones(TipoPocion.PEQUENA));
            assertEquals(0, j.getCantidadPociones(TipoPocion.GRANDE));
        }
    }

    @ParameterizedTest
    @EnumSource(TipoPocion.class)
    void cadaIncorporacionSumaUnaUnidadSoloAlTipoCorrespondiente(TipoPocion tipo) {
        Jugador j = jugador();
        j.agregarPocion(tipo);
        assertEquals(1, j.getCantidadPociones(tipo));
        j.agregarPocion(tipo);
        assertEquals(2, j.getCantidadPociones(tipo));
        TipoPocion otra = tipo == TipoPocion.PEQUENA ? TipoPocion.GRANDE : TipoPocion.PEQUENA;
        assertEquals(0, j.getCantidadPociones(otra));
    }

    @Test
    void obtenerPocionesNoCuraNiModificaAtributosEquipoOHabilidades() {
        Jugador j = jugador();
        j.agregarPocion(TipoPocion.PEQUENA);
        j.agregarPocion(TipoPocion.GRANDE);
        assertEquals(3, j.getVida());
        assertEquals(24, j.getVidaMaxima());
        assertEquals(6, j.getFuerza());
        assertEquals(6, j.getAtributoBase(org.viajedragos.arma.Atributo.FUERZA));
        assertEquals(2, j.getResistencia());
        assertEquals(3, j.getIniciativa());
        assertEquals(2, j.getAgilidad());
        assertEquals(2, j.getVelocidad());
        assertTrue(j.getEquipoEquipado().isEmpty());
        assertTrue(j.getHabilidades().isEmpty());
    }

    @Test
    void inventarioNoPermiteAlterarCantidadesDesdeFuera() {
        Jugador j = jugador();
        j.agregarPocion(TipoPocion.PEQUENA);
        var inventario = j.getInventarioPociones();
        assertThrows(UnsupportedOperationException.class, () -> inventario.put(TipoPocion.GRANDE, 20));
        assertThrows(UnsupportedOperationException.class, () -> inventario.remove(TipoPocion.PEQUENA));
        assertThrows(UnsupportedOperationException.class, inventario::clear);
        assertEquals(1, j.getCantidadPociones(TipoPocion.PEQUENA));
        assertEquals(0, j.getCantidadPociones(TipoPocion.GRANDE));
    }

    @Test
    void cadaJugadorTieneSuPropioInventario() {
        Jugador primero = jugador();
        Jugador segundo = jugador();
        primero.agregarPocion(TipoPocion.GRANDE);
        assertEquals(1, primero.getCantidadPociones(TipoPocion.GRANDE));
        assertTrue(segundo.getInventarioPociones().isEmpty());
    }

    @Test
    void recuperacionFuturaEsUnDatoDeOchoODieciseisPS() {
        assertEquals("Poción pequeña", TipoPocion.PEQUENA.getNombre());
        assertEquals(8, TipoPocion.PEQUENA.getRecuperacionPS());
        assertEquals("Poción grande", TipoPocion.GRANDE.getNombre());
        assertEquals(16, TipoPocion.GRANDE.getRecuperacionPS());
    }

    @Test
    void tipoNuloSeRechazaSinAlterarInventario() {
        Jugador j = jugador();
        assertThrows(NullPointerException.class, () -> j.agregarPocion(null));
        assertThrows(NullPointerException.class, () -> j.getCantidadPociones(null));
        assertTrue(j.getInventarioPociones().isEmpty());
    }
}
