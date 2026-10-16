package org.viajedragos.modelo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.viajedragos.arma.Arma;
import org.viajedragos.arma.Atributo;
import org.viajedragos.arma.TipoEquipo;
import org.viajedragos.combate.Combate;
import org.viajedragos.combate.ResultadoCombate;
import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.Habilidad;
import org.viajedragos.habilidad.implementacion.InstintoSupervivencia;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConsumoPocionesTest {
    private Jugador jugador(int vida) {
        return new Jugador("Prueba", "", 6, 2, 3, 2, 2, vida, 24);
    }

    @ParameterizedTest
    @CsvSource({"PEQUENA,3,8,11", "GRANDE,3,16,19", "PEQUENA,22,2,24", "GRANDE,22,2,24"})
    void recuperaLaCantidadRealYDescuentaSoloUnaUnidad(TipoPocion tipo, int vida, int recuperacion, int finalVida) {
        Jugador j = jugador(vida);
        j.agregarPocion(tipo);
        j.agregarPocion(tipo);
        assertEquals(recuperacion, j.usarPocion(tipo));
        assertEquals(finalVida, j.getVida());
        assertEquals(24, j.getVidaMaxima());
        assertEquals(1, j.getCantidadPociones(tipo));
    }

    @ParameterizedTest
    @EnumSource(TipoPocion.class)
    void ultimaUnidadDesapareceDelMapaSinAfectarAlOtroTipo(TipoPocion tipo) {
        Jugador j = jugador(3);
        for (TipoPocion pocion : TipoPocion.values()) j.agregarPocion(pocion);
        var vista = j.getInventarioPociones();
        assertEquals(tipo.getRecuperacionPS(), j.usarPocion(tipo));
        assertEquals(0, j.getCantidadPociones(tipo));
        assertFalse(vista.containsKey(tipo));
        TipoPocion otra = tipo == TipoPocion.PEQUENA ? TipoPocion.GRANDE : TipoPocion.PEQUENA;
        assertEquals(1, j.getCantidadPociones(otra));
        int vida = j.getVida();
        assertEquals(0, j.usarPocion(tipo));
        assertEquals(vida, j.getVida());
    }

    @ParameterizedTest
    @EnumSource(TipoPocion.class)
    void vidaCompletaNoConsumeNiDisparaCuracion(TipoPocion tipo) {
        Jugador j = jugador(24);
        j.agregarPocion(tipo);
        j.aprenderHabilidad(new Habilidad("No debe activarse") {
            @Override
            public void alCurarse(Combatiente c, EstadoHabilidad estado) {
                fail("No se debe invocar curarse con vida completa");
            }
        }, 1);
        assertEquals(0, j.usarPocion(tipo));
        assertEquals(24, j.getVida());
        assertEquals(1, j.getCantidadPociones(tipo));
    }

    @ParameterizedTest
    @EnumSource(TipoPocion.class)
    void pocionInexistenteNoCuraNiDisparaEventos(TipoPocion tipo) {
        Jugador j = jugador(3);
        j.aprenderHabilidad(new Habilidad("No debe activarse") {
            @Override
            public void alCurarse(Combatiente c, EstadoHabilidad estado) {
                fail("No se debe invocar curarse sin una poción");
            }
        }, 1);
        assertEquals(0, j.usarPocion(tipo));
        assertEquals(3, j.getVida());
        assertTrue(j.getInventarioPociones().isEmpty());
    }

    @Test
    void tipoNuloNoConsumeNiCura() {
        Jugador j = jugador(3);
        j.agregarPocion(TipoPocion.PEQUENA);
        assertThrows(NullPointerException.class, () -> j.usarPocion(null));
        assertEquals(3, j.getVida());
        assertEquals(1, j.getCantidadPociones(TipoPocion.PEQUENA));
    }

    @Test
    void curacionDisparaUnaVezSoloHabilidadesActivasConSuEstado() {
        Jugador j = jugador(22);
        int[] llamadas = {0};
        EstadoHabilidad[] esperado = {null};
        Habilidad activa = new Habilidad("Registro") {
            @Override
            public void alCurarse(Combatiente c, EstadoHabilidad estado) {
                assertSame(j, c);
                assertSame(esperado[0], estado);
                assertEquals(3, estado.getNivel());
                assertEquals(24, c.getVida());
                llamadas[0]++;
            }
        };
        j.aprenderHabilidad(activa, 3);
        esperado[0] = j.getHabilidades().get(activa);
        Habilidad inactiva = new Habilidad("Inactiva") {
            @Override
            public void alCurarse(Combatiente c, EstadoHabilidad estado) {
                fail("Una habilidad inactiva no debe reaccionar");
            }
        };
        // Clases anónimas distintas para que sean claves distintas.
        j.getHabilidades().put(inactiva, new EstadoHabilidad());
        j.agregarPocion(TipoPocion.GRANDE);
        assertEquals(2, j.usarPocion(TipoPocion.GRANDE));
        assertEquals(1, llamadas[0]);
        assertSame(esperado[0], j.getHabilidades().get(activa));
        assertEquals(3, esperado[0].getNivel());
    }

    @Test
    void instintoRetiraSuBonificacionAlCurarsePorEncimaDelUmbralSinRetirarEquipo() {
        Jugador j = jugador(5);
        InstintoSupervivencia instinto = new InstintoSupervivencia();
        j.aprenderHabilidad(instinto, 1);
        var estado = j.getHabilidades().get(instinto);
        Arma arma = new Arma("Prueba", TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 1), Map.of());
        assertTrue(j.equipar(arma));
        instinto.alInicioCombate(j, new Enemigo(), estado);
        assertEquals(10, j.getFuerza());
        j.agregarPocion(TipoPocion.PEQUENA);
        assertEquals(8, j.usarPocion(TipoPocion.PEQUENA));
        assertEquals(13, j.getVida());
        assertEquals(6, j.getAtributoBase(Atributo.FUERZA));
        assertEquals(7, j.getFuerza());
        assertSame(arma, j.getEquipoEquipado().get(TipoEquipo.ARMA));
        assertSame(estado, j.getHabilidades().get(instinto));
        instinto.alFinalCombate(j, new Enemigo(), estado);
        assertEquals(7, j.getFuerza(), "No se retira dos veces el efecto");
    }

    @Test
    void instintoPuedeActivarseAlCurarseConVidaBajaYElSiguienteCombateLoRestaura() {
        Jugador j = jugador(3);
        j.setVidaMaxima(100);
        InstintoSupervivencia instinto = new InstintoSupervivencia();
        j.aprenderHabilidad(instinto, 1);
        j.agregarPocion(TipoPocion.PEQUENA);
        assertEquals(8, j.usarPocion(TipoPocion.PEQUENA));
        assertEquals(9, j.getFuerza(), "Se conserva alCurarse aun fuera de combate");
        int[] dados = {1, 1, 1, 1, 6, 1, 6};
        int[] usadas = {0};
        Enemigo lobo = new Enemigo("Lobo", "", 5, 2, 2, 1, 2, 18, 18);
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, new Combate(j, lobo, () -> {
            assertTrue(usadas[0] < dados.length);
            return dados[usadas[0]++];
        }).combatir());
        assertEquals(dados.length, usadas[0]);
        assertEquals(6, j.getFuerza());
        assertEquals(11, j.getVida());
    }
}
