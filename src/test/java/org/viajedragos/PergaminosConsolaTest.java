package org.viajedragos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.arma.*;
import org.viajedragos.catalogo.*;
import org.viajedragos.consola.*;
import org.viajedragos.modelo.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PergaminosConsolaTest {
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private PrintStream salida() { return new PrintStream(bytes, true, StandardCharsets.UTF_8); }
    private String texto() { return TextoConsola.contenido(bytes.toString(StandardCharsets.UTF_8)).replaceAll("\\s+", " "); }
    private Jugador jugador() { return new Jugador("Prueba", "", 6, 2, 3, 2, 2, 20, 30); }
    private Equipo pieza(int puntos) {
        return new DefinicionEquipo("anillo", "Anillo igual", TipoEquipo.ANILLO, Map.of(Atributo.AGILIDAD, 1)).crearEquipo(puntos);
    }
    private boolean inventario(Jugador j, String entrada) {
        return assertTimeoutPreemptively(Duration.ofSeconds(2), () -> app.consultarInventario(j, new Scanner(entrada), salida()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void confirmaGuardadaOEquipadaYMuestraAntesDespuesYPuntos(boolean equipada) {
        Jugador j = jugador(); Equipo pieza = pieza(3);
        if (equipada) j.equipar(pieza); else j.agregarEquipo(pieza);
        j.agregarPergamino(new Pergamino(Atributo.FUERZA)); j.agregarPergamino(new Pergamino(Atributo.FUERZA));
        assertTrue(inventario(j, "4\n1\n1\n1\n0\n"));
        assertEquals(1, j.getCantidadPergaminos(Atributo.FUERZA));
        assertEquals(2, pieza.getMejorasDisponibles()); assertEquals(1, pieza.getBonificaciones().get(Atributo.FUERZA));
        assertEquals(equipada ? 7 : 6, j.getFuerza());
        assertTrue(texto().contains("Pergamino de fuerza x2"));
        assertTrue(texto().contains("FUERZA: 0 -> 1"));
        assertTrue(texto().contains("Mejoras disponibles: 3 -> 2"));
        assertTrue(texto().contains("MEJORA APLICADA"));
        assertTrue(texto().contains(equipada ? "equipada" : "guardada"));
        comprobarPaneles();
    }

    @Test
    void dosEjemplaresIgualesYUnoAgotadoPermitenSeleccionarElCorrecto() {
        Jugador j = jugador(); Equipo cero = pieza(0), tres = pieza(3);
        j.agregarEquipo(cero); j.agregarEquipo(tres); j.agregarPergamino(new Pergamino(Atributo.FUERZA));
        assertTrue(inventario(j, "4\n1\n1\n2\n1\n0\n"));
        assertFalse(cero.getBonificaciones().containsKey(Atributo.FUERZA));
        assertEquals(1, tres.getBonificaciones().get(Atributo.FUERZA));
        assertEquals(0, cero.getMejorasDisponibles()); assertEquals(2, tres.getMejorasDisponibles());
        assertEquals(List.of(cero, tres), j.getInventarioEquipo());
        assertTrue(texto().contains("Esta pieza no tiene mejoras disponibles"));
        assertTrue(j.getPergaminos().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"4\n0\n0\n", "4\n1\n0\n0\n0\n", "4\n1\n1\n0\n0\n0\n0\n"})
    void cerosRetrocedenUnNivelYCancelarNoModificaNada(String entrada) {
        Jugador j = jugador(); Equipo pieza = pieza(3); j.agregarEquipo(pieza);
        j.agregarPergamino(new Pergamino(Atributo.VIDA));
        assertTrue(inventario(j, entrada));
        assertEquals(3, pieza.getMejorasDisponibles()); assertEquals(0, pieza.getMejorasConsumidas());
        assertEquals(1, j.getCantidadPergaminos(Atributo.VIDA));
        assertEquals(30, j.getVidaMaxima()); assertEquals(20, j.getVida());
        assertFalse(texto().contains("MEJORA APLICADA"));
    }

    @Test
    void entradasInvalidasNoConsumenPergaminos() {
        Jugador j = jugador(); Equipo pieza = pieza(3); j.agregarEquipo(pieza);
        j.agregarPergamino(new Pergamino(Atributo.FUERZA));
        assertTrue(inventario(j, "x\n4\nx\n9\n1\nx\n9\n1\nx\n9\n0\n0\n0\n0\n"));
        assertEquals(7, texto().split("Entrada inválida", -1).length - 1);
        assertEquals(3, pieza.getMejorasDisponibles()); assertEquals(1, j.getCantidadPergaminos(Atributo.FUERZA));
        comprobarPaneles();
    }

    @ParameterizedTest
    @ValueSource(strings = {"4\n", "4\n1\n", "4\n1\n1\n", "4\n1\n1\nx\n"})
    void finDeEntradaNoConfirmaNiGasta(String entrada) {
        Jugador j = jugador(); Equipo pieza = pieza(3); j.equipar(pieza);
        j.agregarPergamino(new Pergamino(Atributo.VIDA));
        assertFalse(inventario(j, entrada));
        assertEquals(3, pieza.getMejorasDisponibles()); assertEquals(1, j.getCantidadPergaminos(Atributo.VIDA));
        assertEquals(30, j.getVidaMaxima());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void sinPergaminosOSinEquipoPermiteRegresar(boolean conPergamino) {
        Jugador j = jugador();
        if (conPergamino) j.agregarPergamino(new Pergamino(Atributo.VIDA));
        assertTrue(inventario(j, conPergamino ? "4\n1\n0\n0\n0\n" : "4\n0\n0\n"));
        assertTrue(texto().contains(conPergamino ? "No tienes equipamiento" : "No tienes pergaminos disponibles"));
    }

    @Test
    void falloDeMejoraSeInformaYConservaInventario() {
        Jugador j = jugador();
        Equipo pieza = new DefinicionEquipo("max", "Máxima", TipoEquipo.ARMA, Map.of(Atributo.FUERZA, Integer.MAX_VALUE)).crearEquipo(1);
        j.agregarEquipo(pieza); j.agregarPergamino(new Pergamino(Atributo.FUERZA));
        assertTrue(inventario(j, "4\n1\n1\n1\n0\n0\n0\n"));
        assertEquals(1, pieza.getMejorasDisponibles()); assertEquals(1, j.getCantidadPergaminos(Atributo.FUERZA));
        assertTrue(texto().contains("Conservas el pergamino y el estado anterior"));
    }

    @Test
    void pergaminosNoSeOfrecenEnTiendaNiSePuedenVender() {
        Jugador j = jugador(); j.agregarPergamino(new Pergamino(Atributo.FUERZA));
        assertTrue(app.consultarTienda(j, new Scanner("2\n0\n0\n"), salida(), new RepositorioTiendaMemoria()));
        assertTrue(texto().contains("No tienes objetos vendibles"));
        assertEquals(1, j.getCantidadPergaminos(Atributo.FUERZA)); assertEquals(15, j.getOro());
        assertEquals(24, new RepositorioTiendaMemoria().obtenerTodos().size());
        assertFalse(texto().contains("Pergamino de fuerza"));
    }

    @Test
    void aventuraObtienePergaminoYLoUsaSinPausasEnLosMenusNiDadosAdicionales() {
        Jugador j = new Jugador("Prueba", "", 20, 2, 3, 2, 2, 48, 48);
        Equipo pieza = pieza(3); j.agregarEquipo(pieza);
        int[] dados = {0}, azar = {0}; var esperas = new ArrayList<String>();
        var ritmo = new RitmoConsola(RitmoConsola.Velocidad.NORMAL, ms -> esperas.add(bytes.toString(StandardCharsets.UTF_8)));
        var pergaminos = new Random(0) { public int nextInt(int limite) { assertEquals(azar[0] == 0 ? 100 : 6, limite); azar[0]++; return 0; } };
        var objetos = new RepositorioObjetosMemoria();
        assertFalse(app.jugarAventura(j, new Scanner("3\n1\n2\n1\n2\n4\n1\n1\n1\n0\n0\n0\n"), salida(),
                () -> { assertTrue(++dados[0] <= 6); return 4; }, new Random(0) { public int nextInt(int limite) { return 99; } },
                () -> List.of(new DefinicionEnemigo("prueba", "Prueba", "", 1, 0, 1, 0, 0, 1)), new Random(0),
                objetos, null, ritmo, pergaminos, new Random(0) { public int nextInt(int limite) { return 99; } },
                new org.viajedragos.consola.TiempoAventura(),
                TextoConsola.sinBotinEquipamiento()));
        assertEquals(6, dados[0]); assertEquals(2, azar[0]); assertEquals(7, esperas.size());
        assertTrue(esperas.stream().noneMatch(s -> s.contains("UTILIZAR PERGAMINOS")));
        assertEquals(2, pieza.getMejorasDisponibles()); assertEquals(1, pieza.getBonificaciones().get(Atributo.FUERZA));
        assertTrue(j.getPergaminos().isEmpty());
        comprobarPaneles();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void tiendaVendeLaPiezaMejoradaAlPrecioDeSuDefinicion(boolean equipada) {
        Jugador j = jugador();
        var d = new RepositorioObjetosMemoria().obtenerEquipamiento().get(4);
        Equipo pieza = d.crearEquipo(3); j.agregarEquipo(pieza);
        j.agregarPergamino(new Pergamino(Atributo.FUERZA));
        assertTrue(j.usarPergamino(Atributo.FUERZA, pieza));
        if (equipada) j.equiparDesdeInventario(TipoEquipo.ANILLO, 0);
        assertTrue(app.consultarTienda(j, new Scanner("2\n1\n1\n0\n0\n"), salida(), new RepositorioTiendaMemoria()));
        assertEquals(20, j.getOro()); assertEquals(6, j.getFuerza());
        assertTrue(j.getInventarioEquipo().isEmpty()); assertTrue(j.getEquipoEquipado().isEmpty());
        assertTrue(texto().contains("Has vendido Anillo sencillo por 5 monedas"));
    }

    private void comprobarPaneles() {
        TextoConsola.comprobarPaneles(bytes.toString(StandardCharsets.UTF_8));
    }
}
