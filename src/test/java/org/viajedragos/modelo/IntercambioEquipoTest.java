package org.viajedragos.modelo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.arma.*;
import org.viajedragos.habilidad.implementacion.InstintoSupervivencia;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IntercambioEquipoTest {
    private Jugador jugador() { return new Jugador("Prueba", "",6,2,3,2,2,13,24); }
    private Equipo pieza(String nombre, TipoEquipo tipo, int fuerza) {
        return new Arma(nombre, tipo, Map.of(Atributo.FUERZA, fuerza), Map.of());
    }

    @Test
    void ranuraVaciaEquipaLaMismaInstanciaYRetiraSoloSuEntrada() {
        Jugador j = jugador();
        Equipo nueva = pieza("Nueva", TipoEquipo.ARMA, 2);
        j.agregarEquipo(nueva);
        j.agregarEquipo(nueva);
        j.agregarPocion(TipoPocion.PEQUENA);
        assertTrue(j.equiparDesdeInventario(TipoEquipo.ARMA, 1));
        assertSame(nueva, j.getEquipoEquipado().get(TipoEquipo.ARMA));
        assertEquals(List.of(nueva), j.getInventarioEquipo());
        assertEquals(8, j.getFuerza());
        assertEquals(6, j.getAtributoBase(Atributo.FUERZA));
        assertEquals(13, j.getVida());
        assertEquals(1, j.getCantidadPociones(TipoPocion.PEQUENA));
    }

    @Test
    void intercambioConservaIdentidadesYSePuedeRevertirSinAcumular() {
        Jugador j = jugador();
        Equipo vieja = pieza("Vieja", TipoEquipo.ARMA, 1);
        Equipo nueva = pieza("Nueva", TipoEquipo.ARMA, 2);
        Equipo otra = pieza("Otra", TipoEquipo.BOTAS, -1);
        j.equipar(vieja);
        j.equipar(otra);
        j.agregarEquipo(nueva);
        for (int i = 0; i < 20; i++) {
            assertTrue(j.equiparDesdeInventario(TipoEquipo.ARMA, 0));
            Equipo equipada = i % 2 == 0 ? nueva : vieja;
            Equipo guardada = i % 2 == 0 ? vieja : nueva;
            assertSame(equipada, j.getEquipoEquipado().get(TipoEquipo.ARMA));
            assertEquals(1, j.getInventarioEquipo().size());
            assertSame(guardada, j.getInventarioEquipo().getFirst());
            assertSame(otra, j.getEquipoEquipado().get(TipoEquipo.BOTAS));
            assertEquals(i % 2 == 0 ? 7 : 6, j.getFuerza());
        }
    }

    @Test
    void habilidadesAprendidasYDeOtrasPiezasMantienenSusContribuciones() {
        Jugador j = jugador();
        var habilidad = new InstintoSupervivencia();
        j.aprenderHabilidad(habilidad, 2);
        Equipo vieja = new Arma("Vieja", TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 1), Map.of(habilidad, 3));
        Equipo nueva = new Arma("Nueva", TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 2), Map.of(habilidad, 4));
        Equipo botas = new Arma("Botas", TipoEquipo.BOTAS, Map.of(), Map.of(habilidad, 1));
        j.equipar(vieja);
        j.equipar(botas);
        var estado = j.getHabilidades().get(habilidad);
        j.agregarEquipo(nueva);
        assertTrue(j.equiparDesdeInventario(TipoEquipo.ARMA, 0));
        assertSame(estado, j.getHabilidades().get(habilidad));
        assertTrue(estado.isAprendida());
        assertEquals(7, estado.getNivel());
        assertEquals(2, estado.getVecesEquipada());
        assertEquals(8, j.getFuerza());
        assertTrue(j.equiparDesdeInventario(TipoEquipo.ARMA, 0));
        assertEquals(6, estado.getNivel());
        assertEquals(2, estado.getVecesEquipada());
        assertEquals(7, j.getFuerza());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1,0,1,999})
    void rechazaIndiceInvalidoOIncompatibilidadSinEfectos(int indice) {
        Jugador j = jugador();
        Equipo botas = pieza("Botas", TipoEquipo.BOTAS, 2);
        j.agregarEquipo(botas);
        assertFalse(j.equiparDesdeInventario(TipoEquipo.ARMA, indice));
        assertEquals(List.of(botas), j.getInventarioEquipo());
        assertTrue(j.getEquipoEquipado().isEmpty());
        assertEquals(6, j.getFuerza());
    }

    @ParameterizedTest
    @ValueSource(ints = {0,1,2,3,4,5})
    void fallosRestauranRanurasAtributosYEstadosOriginales(int fallo) {
        var habilidad = new InstintoSupervivencia();
        boolean[] simular = {false};
        Equipo nueva = new Arma("Nueva", TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 4), Map.of(habilidad, 4)) {
            @Override public void aplicarEfectos(Combatiente c) {
                super.aplicarEfectos(c);
                if (fallo == 0) throw new IllegalStateException("Fallo tras aplicar");
            }
        };
        Equipo vieja = new Arma("Vieja", TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 1), Map.of(habilidad, 3)) {
            @Override public void retirarEfectos(Combatiente c) {
                if (fallo == 5) throw new IllegalStateException("Fallo antes de retirar");
                super.retirarEfectos(c);
                if (fallo == 1) throw new IllegalStateException("Fallo tras retirar");
            }
        };
        Jugador j = new Jugador("Prueba", "",6,2,3,2,2,13,24) {
            @Override public boolean equipar(Equipo equipo) {
                if (simular[0] && fallo == 2) return false;
                boolean resultado = super.equipar(equipo);
                return simular[0] && fallo == 4 ? false : resultado;
            }
            @Override public boolean desequipar(TipoEquipo tipo) {
                boolean resultado = super.desequipar(tipo);
                return simular[0] && fallo == 3 ? false : resultado;
            }
        };
        j.aprenderHabilidad(habilidad, 2);
        j.equipar(vieja);
        Equipo botas = pieza("Botas", TipoEquipo.BOTAS, -2);
        j.equipar(botas);
        j.agregarEquipo(nueva);
        j.agregarEquipo(nueva);
        var mapa = j.getHabilidades();
        var estado = mapa.get(habilidad);
        simular[0] = true;
        assertFalse(j.equiparDesdeInventario(TipoEquipo.ARMA, 0));
        assertSame(vieja, j.getEquipoEquipado().get(TipoEquipo.ARMA));
        assertSame(botas, j.getEquipoEquipado().get(TipoEquipo.BOTAS));
        assertEquals(List.of(nueva,nueva), j.getInventarioEquipo());
        assertSame(mapa, j.getHabilidades());
        assertSame(estado, mapa.get(habilidad));
        assertEquals(5, estado.getNivel());
        assertEquals(1, estado.getVecesEquipada());
        assertTrue(estado.isAprendida());
        assertEquals(5, j.getFuerza());
        assertEquals(6, j.getAtributoBase(Atributo.FUERZA));
        assertEquals(13, j.getVida());
        assertEquals(24, j.getVidaMaxima());
    }

    @Test
    void falloDePiezaQueIntroduceUnaHabilidadNoDejaLaEntradaNueva() {
        Jugador j = jugador();
        Equipo nueva = new Arma("Falla", TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 2),
                Map.of(new InstintoSupervivencia(), 1)) {
            @Override public void aplicarEfectos(Combatiente c) {
                super.aplicarEfectos(c);
                throw new IllegalStateException("Fallo");
            }
        };
        j.agregarEquipo(nueva);
        assertFalse(j.equiparDesdeInventario(TipoEquipo.ARMA, 0));
        assertTrue(j.getHabilidades().isEmpty());
        assertTrue(j.getEquipoEquipado().isEmpty());
        assertEquals(List.of(nueva), j.getInventarioEquipo());
        assertEquals(6, j.getFuerza());
    }

    @Test
    void falloRestauraEstadoDeHabilidadQueHabiaDesaparecidoAlRetirarLaPieza() {
        Jugador j = jugador();
        var habilidad = new InstintoSupervivencia();
        Equipo vieja = new Arma("Vieja",TipoEquipo.ARMA,Map.of(Atributo.FUERZA,1),Map.of(habilidad,2));
        j.equipar(vieja);
        var estado = j.getHabilidades().get(habilidad);
        Equipo nueva = new Arma("Nueva",TipoEquipo.ARMA,Map.of(Atributo.FUERZA,2),Map.of(habilidad,3)) {
            @Override public void aplicarEfectos(Combatiente c) {
                super.aplicarEfectos(c);
                throw new IllegalStateException("Fallo tras crear otro estado");
            }
        };
        j.agregarEquipo(nueva);
        assertFalse(j.equiparDesdeInventario(TipoEquipo.ARMA,0));
        assertSame(estado,j.getHabilidades().get(habilidad));
        assertEquals(1,estado.getVecesEquipada());
        assertEquals(2,estado.getNivel());
        assertFalse(estado.isAprendida());
        assertSame(vieja,j.getEquipoEquipado().get(TipoEquipo.ARMA));
        assertEquals(List.of(nueva),j.getInventarioEquipo());
        assertEquals(7,j.getFuerza());
    }
}
