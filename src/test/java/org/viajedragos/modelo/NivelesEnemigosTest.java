package org.viajedragos.modelo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.catalogo.RepositorioEnemigosMemoria;
import static org.junit.jupiter.api.Assertions.*;

class NivelesEnemigosTest {
    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void definicionYCombatienteRechazanNivelesInvalidos(int nivel) {
        assertThrows(IllegalArgumentException.class,
                () -> new DefinicionEnemigo("id", "Nombre", "", 1, 0, 1, 0, 0, 1, 5, nivel));
        assertThrows(IllegalArgumentException.class,
                () -> new Enemigo("Nombre", "", 1, 0, 1, 0, 0, 1, 1, 5, nivel));
    }

    @Test
    void constructoresAnterioresConservanNivelUnoSinCambiarExp() {
        assertEquals(1, new DefinicionEnemigo("id", "Nombre", "", 1, 0, 1, 0, 0, 1).nivel());
        assertEquals(1, new DefinicionEnemigo("id", "Nombre", "", 1, 0, 1, 0, 0, 1, 93).nivel());
        assertEquals(1, new Enemigo().getNivel());
        assertEquals(1, new Enemigo("Nombre", "", 1, 0, 1, 0, 0, 1, 1).getNivel());
        var e = new Enemigo("Nombre", "", 1, 0, 1, 0, 0, 1, 1, 93);
        assertEquals(1, e.getNivel());
        assertEquals(93, e.getExperiencia());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6})
    void crearEnemigoTransmiteNivelExpYEstadisticasSinEscalarlas(int indice) {
        var d = new RepositorioEnemigosMemoria().obtenerTodos().get(indice);
        var e = d.crearEnemigo();
        assertEquals(indice < 5 ? 1 : 3, e.getNivel());
        assertEquals(d.nivel(), e.getNivel());
        assertEquals(d.experiencia(), e.getExperiencia());
        assertEquals(d.fuerza(), e.getFuerza());
        assertEquals(d.resistencia(), e.getResistencia());
        assertEquals(d.iniciativa(), e.getIniciativa());
        assertEquals(d.agilidad(), e.getAgilidad());
        assertEquals(d.velocidad(), e.getVelocidad());
        assertEquals(d.vidaMaxima(), e.getVida());
        assertEquals(d.vidaMaxima(), e.getVidaMaxima());
        assertNotSame(e, d.crearEnemigo());
        assertTrue(e.getEquipoEquipado().isEmpty());
        assertTrue(e.getHabilidades().isEmpty());
    }

    @Test
    void nivelAltoNoImplicaEstadisticasGeneradas() {
        var e = new DefinicionEnemigo("externo", "Externo", "", 0, 1, 2, 3, 4, 5, 6, 99).crearEnemigo();
        assertEquals(99, e.getNivel());
        assertEquals(0, e.getFuerza());
        assertEquals(1, e.getResistencia());
        assertEquals(2, e.getIniciativa());
        assertEquals(3, e.getAgilidad());
        assertEquals(4, e.getVelocidad());
        assertEquals(5, e.getVidaMaxima());
        assertEquals(6, e.getExperiencia());
    }
}
