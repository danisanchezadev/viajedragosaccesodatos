package org.viajedragos.modelo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.catalogo.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PlantasTest {
    @Test void resumenCompactoReutilizaProgresoYSeConservaAlRegresar() {
        var p = new ProgresoAventura();
        assertEquals("Victorias necesarias: 0/3", p.resumenProgreso());
        p.registrarVictoria(1, enemigo(1, "rata", false));
        p.registrarVictoria(2, enemigo(1, "rata", false));
        assertEquals("Victorias necesarias: 2/3", p.resumenProgreso()); assertFalse(p.bajar());
        p.registrarVictoria(3, enemigo(1, "rata", false));
        assertEquals(1, p.plantaActual().numero()); assertEquals("Victorias necesarias: 3/3", p.resumenProgreso());
        assertTrue(p.bajar()); assertEquals("Guardián: pendiente", p.resumenProgreso());
        p.registrarVictoria(4, enemigo(2, "soldado", false)); assertEquals("Guardián: pendiente", p.resumenProgreso());
        p.registrarVictoria(5, enemigo(2, "capitan", true)); assertEquals("Guardián: derrotado", p.resumenProgreso());
        assertTrue(p.bajar()); assertEquals("Especies diferentes derrotadas: 0/3", p.resumenProgreso());
        p.registrarVictoria(6, enemigo(3, "momia", false)); p.registrarVictoria(7, enemigo(3, "momia", false));
        assertEquals("Especies diferentes derrotadas: 1/3", p.resumenProgreso());
        p.registrarVictoria(8, enemigo(3, "arana", false)); assertEquals("Especies diferentes derrotadas: 2/3", p.resumenProgreso());
        p.registrarVictoria(9, enemigo(3, "espectro", false)); assertTrue(p.bajar());
        assertEquals("Fragmentos del sello: 0/3", p.resumenProgreso());
        for (int i = 1; i <= 3; i++) {
            p.registrarVictoria(9 + i, enemigo(4, "golem", false));
            assertEquals("Fragmentos del sello: " + i + "/3", p.resumenProgreso());
        }
        assertTrue(p.bajar()); assertEquals("Última planta disponible", p.resumenProgreso());
        assertFalse(p.bajar());
        assertTrue(p.subir()); assertEquals("Fragmentos del sello: 3/3", p.resumenProgreso());
        assertTrue(p.subir()); assertEquals("Especies diferentes derrotadas: 3/3", p.resumenProgreso());
        assertTrue(p.subir()); assertEquals("Guardián: derrotado", p.resumenProgreso());
        assertTrue(p.subir()); assertEquals("Victorias necesarias: 3/3", p.resumenProgreso());
        p.registrarVictoria(13, enemigo(1, "rata", false));
        assertEquals(4, p.victorias(1)); assertEquals("Victorias necesarias: 3/3", p.resumenProgreso());
    }
    private DefinicionEnemigo enemigo(int planta, String id, boolean guardian) {
        return new DefinicionEnemigo(id, "Nombre compartido", "", 1, 0, 1, 0, 0, 1, 1, 1, planta, guardian);
    }
    private void desbloquearSegunda(ProgresoAventura p) {
        for (int i = 1; i <= 3; i++) assertTrue(p.registrarVictoria(i, enemigo(1, "repetido", false)));
        assertTrue(p.bajar());
    }
    private void desbloquearTercera(ProgresoAventura p) {
        desbloquearSegunda(p);
        assertTrue(p.registrarVictoria(4, enemigo(2, "capitan_externo", true)));
        assertTrue(p.bajar());
    }
    private void desbloquearCuarta(ProgresoAventura p) {
        desbloquearTercera(p);
        for (int i = 5; i <= 7; i++) p.registrarVictoria(i, enemigo(3, "especie_" + i, false));
        assertTrue(p.bajar());
    }

    @Test void cincoUbicacionesConIdentidadDescripcionYOrdenEstables() {
        assertEquals(List.of("El vestíbulo", "Los barracones", "Las criptas", "La forja olvidada", "El abismo"),
                Arrays.stream(Planta.values()).map(Planta::nombre).toList());
        assertEquals(5, Arrays.stream(Planta.values()).map(Planta::id).distinct().count());
        for (var p : Planta.values()) {
            assertSame(p, Planta.porNumero(p.numero())); assertFalse(p.descripcion().isBlank());
        }
    }

    @ParameterizedTest @ValueSource(ints = {-1, 0, 6, Integer.MAX_VALUE})
    void plantasInvalidasSeRechazan(int numero) {
        assertThrows(IllegalArgumentException.class, () -> Planta.porNumero(numero));
        assertThrows(IllegalArgumentException.class, () -> enemigo(numero, "id", false));
    }

    @Test void constructoresAnterioresAsignanPlantaUnoYNoGuardian() {
        var d = new DefinicionEnemigo("id", "Nombre", "", 1, 0, 1, 0, 0, 1, 23, 17);
        assertEquals(1, d.planta()); assertFalse(d.guardian());
        assertEquals(17, d.crearEnemigo().getNivel()); assertEquals(23, d.crearEnemigo().getExperiencia());
        assertThrows(IllegalArgumentException.class, () -> enemigo(3, "guardian", true));
    }

    @Test void tresVictoriasInclusoRepetidasDesbloqueanSoloLaSegunda() {
        var p = new ProgresoAventura();
        assertSame(Planta.VESTIBULO, p.plantaActual()); assertTrue(p.desbloqueada(1));
        assertFalse(p.subir()); assertFalse(p.bajar());
        p.registrarVictoria(1, enemigo(1, "rata", false)); p.registrarVictoria(2, enemigo(1, "rata", false));
        assertFalse(p.bajar()); assertFalse(p.desbloqueada(2));
        assertFalse(p.registrarVictoria(2, enemigo(1, "rata", false)));
        assertEquals(2, p.victorias(1));
        p.registrarVictoria(3, enemigo(1, "rata", false)); assertTrue(p.bajar());
        assertSame(Planta.BARRACONES, p.plantaActual()); assertFalse(p.bajar());
        assertEquals(Set.of("rata"), p.especiesDerrotadas(1));
        assertThrows(UnsupportedOperationException.class, () -> p.especiesDerrotadas(1).clear());
        assertTrue(p.subir()); assertTrue(p.bajar()); assertFalse(p.desbloqueada(3));
    }

    @Test void soloLaVictoriaDelGuardianDesbloqueaLaTercera() {
        var p = new ProgresoAventura(); desbloquearSegunda(p);
        for (int i = 4; i < 10; i++) p.registrarVictoria(i, enemigo(2, "soldado", false));
        assertFalse(p.bajar()); assertFalse(p.guardianDerrotado());
        p.registrarVictoria(10, enemigo(2, "guardian_otro_id", true));
        assertTrue(p.guardianDerrotado()); assertTrue(p.bajar());
        assertTrue(p.subir()); assertTrue(p.bajar()); assertEquals(7, p.victorias(2));
    }

    @Test void tresEspeciesSeDistinguenPorIdNoPorNombre() {
        var p = new ProgresoAventura(); desbloquearTercera(p);
        for (int i = 5; i <= 8; i++) p.registrarVictoria(i, enemigo(3, "momia", false));
        assertFalse(p.bajar()); assertEquals(1, p.especiesDerrotadas(3).size());
        p.registrarVictoria(9, enemigo(3, "espectro", false)); assertFalse(p.bajar());
        p.registrarVictoria(10, enemigo(3, "arana", false)); assertTrue(p.bajar());
        assertEquals(Set.of("momia", "espectro", "arana"), p.especiesDerrotadas(3));
    }

    @Test void fragmentosSeAcotanYTodaLaNavegacionEsAdyacente() {
        var p = new ProgresoAventura(); desbloquearCuarta(p);
        for (int i = 8; i <= 9; i++) { p.registrarVictoria(i, enemigo(4, "golem", false)); assertFalse(p.bajar()); }
        p.registrarVictoria(10, enemigo(4, "golem", false)); assertTrue(p.desbloqueada(5));
        p.registrarVictoria(11, enemigo(4, "golem", false)); assertEquals(3, p.fragmentos());
        assertTrue(p.bajar()); assertSame(Planta.ABISMO, p.plantaActual()); assertFalse(p.bajar());
        assertEquals("Todavía no se han desarrollado más plantas", p.requisitoDescenso());
        for (int numero = 4; numero >= 1; numero--) { assertTrue(p.subir()); assertEquals(numero, p.plantaActual().numero()); }
        for (int numero = 2; numero <= 5; numero++) { assertTrue(p.bajar()); assertEquals(numero, p.plantaActual().numero()); }
    }

    @Test void progresoIndependienteYVictoriasAjenasNoAlteranElEstado() {
        var p = new ProgresoAventura(); desbloquearCuarta(p);
        var otro = new ProgresoAventura();
        assertFalse(otro.desbloqueada(2)); assertEquals(0, otro.victorias(1));
        assertFalse(otro.guardianDerrotado()); assertEquals(0, otro.fragmentos());
        assertTrue(otro.especiesDerrotadas(3).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> otro.registrarVictoria(1, enemigo(2, "otro", false)));
        assertThrows(IllegalArgumentException.class, () -> otro.registrarVictoria(0, enemigo(1, "otro", false)));
        assertEquals(0, otro.victorias(1));
    }

    @ParameterizedTest @ValueSource(ints = {1, 2, 3, 4, 5})
    void catalogoDeCincoEspeciesPorPlantaSinHabilidadesNiEscalado(int planta) {
        var catalogo = new RepositorioEnemigosMemoria().obtenerTodos();
        var enemigos = catalogo.stream().filter(d -> d.planta() == planta).toList();
        assertEquals(5, enemigos.size());
        for (var d : enemigos) {
            var e = d.crearEnemigo(); assertEquals(d.fuerza(), e.getFuerza());
            assertEquals(d.vidaMaxima(), e.getVida()); assertTrue(e.getHabilidades().isEmpty());
            assertTrue(e.getEquipoEquipado().isEmpty()); assertNotSame(e, d.crearEnemigo());
            var jugador = new Jugador("Prueba", "", 6, 2, 3, 2, 2, 48, 48);
            while (jugador.getNivel() < d.nivel()) jugador.agregarExperiencia((int) jugador.getExperienciaNecesaria());
            while (jugador.getPuntosPendientes() > 0) jugador.asignarPunto(org.viajedragos.arma.Atributo.FUERZA);
            int[] dados = {0};
            assertEquals(org.viajedragos.combate.ResultadoCombate.ENEMIGO_MUERE,
                    assertTimeoutPreemptively(java.time.Duration.ofSeconds(2), () ->
                            new org.viajedragos.combate.Combate(jugador, e, () -> dados[0]++ % 2 == 0 ? 6 : 1).combatir()));
        }
        if (planta > 1) {
            var anteriores = catalogo.stream().filter(d -> d.planta() == planta - 1).toList();
            assertTrue(enemigos.stream().mapToInt(DefinicionEnemigo::fuerza).average().orElseThrow()
                    > anteriores.stream().mapToInt(DefinicionEnemigo::fuerza).average().orElseThrow());
            assertTrue(enemigos.stream().mapToInt(DefinicionEnemigo::vidaMaxima).average().orElseThrow()
                    > anteriores.stream().mapToInt(DefinicionEnemigo::vidaMaxima).average().orElseThrow());
        }
        assertEquals(1, catalogo.stream().filter(DefinicionEnemigo::guardian).count());
        var guardian = catalogo.stream().filter(DefinicionEnemigo::guardian).findFirst().orElseThrow();
        assertEquals("capitan_barracones", guardian.id()); assertEquals(2, guardian.planta());
        assertFalse(catalogo.stream().filter(d -> d.id().equals("guardian_abismo")).findFirst().orElseThrow().guardian());
    }
}
