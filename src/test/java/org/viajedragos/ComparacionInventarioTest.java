package org.viajedragos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.arma.Arma;
import org.viajedragos.arma.Atributo;
import org.viajedragos.arma.Equipo;
import org.viajedragos.arma.TipoEquipo;
import org.viajedragos.habilidad.implementacion.InstintoSupervivencia;
import org.viajedragos.modelo.Jugador;
import org.viajedragos.modelo.TipoPocion;
import org.viajedragos.catalogo.RepositorioEnemigos;
import org.viajedragos.modelo.DefinicionEnemigo;
import org.viajedragos.modelo.Combatiente;
import org.viajedragos.habilidad.Habilidad;
import org.viajedragos.habilidad.EstadoHabilidad;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ComparacionInventarioTest {
    private final ByteArrayOutputStream salida = new ByteArrayOutputStream();
    private PrintStream consola() { return new PrintStream(salida, true, StandardCharsets.UTF_8); }
    private String texto() { return salida.toString(StandardCharsets.UTF_8); }
    private Jugador jugador() { return new Jugador("Prueba", "",6,2,3,2,2,13,24); }
    private Equipo pieza(String nombre, TipoEquipo tipo, int fuerza) {
        return new Arma(nombre, tipo, Map.of(Atributo.FUERZA, fuerza), Map.of());
    }
    private Jugador preparado() {
        Jugador j = jugador();
        assertTrue(j.equipar(pieza("Equipada", TipoEquipo.ARMA, 1)));
        j.agregarEquipo(pieza("Guardada", TipoEquipo.ARMA, 3));
        return j;
    }

    @ParameterizedTest
    @ValueSource(booleans = {false,true})
    void navegarCompararYVolverNoModificaNada(boolean ansi) {
        Jugador j = jugador();
        var habilidad = new InstintoSupervivencia();
        j.aprenderHabilidad(habilidad, 2);
        var actual = new Arma("Equipada", TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 1), Map.of(habilidad, 3));
        assertTrue(j.equipar(actual));
        var guardada = new Arma("Guardada", TipoEquipo.ARMA,
                Map.of(Atributo.FUERZA, 3, Atributo.AGILIDAD, -1), Map.of(habilidad, 4));
        j.agregarEquipo(guardada);
        j.agregarEquipo(guardada);
        j.agregarEquipo(pieza("Botas incompatibles", TipoEquipo.BOTAS, 5));
        j.agregarPocion(TipoPocion.PEQUENA);
        var equipo = Map.copyOf(j.getEquipoEquipado());
        var modificadores = Map.copyOf(j.getModificadoresEquipo());
        var inventario = List.copyOf(j.getInventarioEquipo());
        var pociones = Map.copyOf(j.getInventarioPociones());
        var estado = j.getHabilidades().get(habilidad);
        int nivel = estado.getNivel();
        int veces = estado.getVecesEquipada();
        boolean aprendida = estado.isAprendida();
        Scanner entrada = new Scanner("1\n1\n1\n2\n0\n0\n0\nsiguiente\n");
        assertTrue(app.consultarInventario(j, entrada, consola(), ansi));
        assertEquals("siguiente", entrada.nextLine());
        assertEquals(2, texto().split("Actual: Equipada \\| Nuevo: Guardada", -1).length - 1);
        assertTrue(texto().contains("2. Guardada"));
        assertTrue(texto().contains("EQUIPAMIENTO GUARDADO"));
        assertTrue(texto().contains("Instinto de supervivencia: nivel otorgado 4"));
        assertEquals(ansi, texto().contains("\u001B[32m+2\u001B[0m"));
        assertEquals(equipo, j.getEquipoEquipado());
        assertEquals(modificadores, j.getModificadoresEquipo());
        assertEquals(inventario, j.getInventarioEquipo());
        assertSame(guardada, j.getInventarioEquipo().getFirst());
        assertEquals(pociones, j.getInventarioPociones());
        assertEquals(1, j.getHabilidades().size());
        assertSame(estado, j.getHabilidades().get(habilidad));
        assertEquals(nivel, estado.getNivel());
        assertEquals(veces, estado.getVecesEquipada());
        assertEquals(aprendida, estado.isAprendida());
        assertEquals(7, j.getFuerza());
        assertEquals(6, j.getAtributoBase(Atributo.FUERZA));
        assertEquals(2, j.getAgilidad());
        assertEquals(13, j.getVida());
        assertEquals(24, j.getVidaMaxima());
    }

    @Test
    void muestraSoloPiezasEquipadasYFiltraLasGuardadasPorRanura() {
        Jugador j = preparado();
        assertTrue(j.equipar(pieza("Casco equipado", TipoEquipo.CASCO, 2)));
        j.agregarEquipo(pieza("Botas guardadas", TipoEquipo.BOTAS, 5));
        j.agregarEquipo(pieza("Casco guardado", TipoEquipo.CASCO, 4));
        assertTrue(app.consultarInventario(j, new Scanner("1\n2\n2\n1\n0\n0\n0\n"), consola()));
        String seleccion = texto().substring(texto().indexOf("COMPARAR - PIEZA EQUIPADA"));
        assertTrue(seleccion.contains("1. Arma: Equipada"));
        assertTrue(seleccion.contains("2. Casco: Casco equipado"));
        assertFalse(seleccion.contains("Botas guardadas"));
        assertFalse(seleccion.contains("1. Guardada"));
        assertTrue(seleccion.contains("1. Casco guardado"));
        assertTrue(seleccion.contains("Actual: Casco equipado | Nuevo: Casco guardado"));
        assertEquals(1, texto().split("Entrada inválida", -1).length - 1);
        assertFalse(texto().contains("\u001B"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0,1,2})
    void vaciosEIncompatiblesPermitenRegresar(int escenario) {
        Jugador j = jugador();
        String entrada;
        if (escenario == 0) {
            j.agregarEquipo(pieza("Guardada", TipoEquipo.ARMA, 3));
            entrada = "1\n0\n";
        } else {
            j.equipar(pieza("Equipada", TipoEquipo.ARMA, 1));
            if (escenario == 2) j.agregarEquipo(pieza("Botas", TipoEquipo.BOTAS, 3));
            entrada = escenario == 1 ? "1\n0\n" : "1\n1\n0\n0\n";
        }
        assertTrue(app.consultarInventario(j, new Scanner(entrada), consola()));
        assertTrue(texto().contains(switch (escenario) {
            case 0 -> "No tienes equipo equipado para comparar";
            case 1 -> "No tienes equipamiento guardado para comparar";
            default -> "No tienes piezas guardadas compatibles";
        }));
        assertFalse(texto().contains("DIFERENCIA"));
    }

    @Test
    void inventarioCompletamenteVacio() {
        Jugador j = jugador();
        assertTrue(app.consultarInventario(j, new Scanner("1\n0\n"), consola()));
        assertTrue(texto().contains("No tienes consumibles"));
        assertTrue(texto().contains("No tienes equipo equipado"));
        assertTrue(texto().contains("No tienes equipamiento guardado"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1\n0\n0\n", "1\n1\n0\n0\n0\n"})
    void cancelarEnCadaSeleccionNoCompara(String entrada) {
        assertTrue(app.consultarInventario(preparado(), new Scanner(entrada), consola()));
        assertFalse(texto().contains("DIFERENCIA"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "1\n", "1\n1\n", "1\n1\n1\n", "1\n1\n0\n", "1\n0\n"})
    void cierreDeEntradaEnTodosLosNivelesInterrumpeSinCambios(String entrada) {
        Jugador j = preparado();
        var equipo = Map.copyOf(j.getEquipoEquipado());
        var inventario = List.copyOf(j.getInventarioEquipo());
        assertFalse(app.consultarInventario(j, new Scanner(entrada), consola()));
        assertTrue(texto().contains("Entrada finalizada. Hasta pronto."));
        assertEquals(equipo, j.getEquipoEquipado());
        assertEquals(inventario, j.getInventarioEquipo());
        assertEquals(7, j.getFuerza());
    }

    @Test
    void entradasInvalidasEnCadaMenuNoCambianLaSeleccionNiElEstado() {
        Jugador j = preparado();
        String invalidas = "x\n\n-1\n99\n9999999999999999999999999\n";
        assertTrue(app.consultarInventario(j, new Scanner(invalidas + "1\n" + invalidas
                + "1\n" + invalidas + "1\n0\n0\n0\n"), consola()));
        assertEquals(15, texto().split("Entrada inválida", -1).length - 1);
        assertEquals(1, texto().split("DIFERENCIA", -1).length - 1);
        assertEquals(7, j.getFuerza());
        assertEquals(1, j.getInventarioEquipo().size());
    }

    @Test
    void menuUsaElComparadorExistenteEnLaDireccionCorrecta() {
        Jugador j = jugador();
        Equipo nuevo = pieza("Guardada", TipoEquipo.ARMA, 3);
        int[] llamadas = {0};
        Equipo equipada = new Arma("Equipada", TipoEquipo.ARMA, Map.of(), Map.of()) {
            @Override public void compararCon(Equipo otro, PrintStream salida, boolean ansi) {
                assertSame(nuevo, otro);
                assertTrue(ansi);
                llamadas[0]++;
                super.compararCon(otro, salida, ansi);
            }
        };
        j.equipar(equipada);
        j.agregarEquipo(nuevo);
        assertTrue(app.consultarInventario(j, new Scanner("1\n1\n1\n0\n0\n0\n"), consola(), true));
        assertEquals(1, llamadas[0]);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false,true})
    void aventuraSoloActivaAnsiConPermisoExplicitoEnSystemOut(boolean salidaEstandar) {
        Jugador j = preparado();
        String propiedadAnterior = System.getProperty("viajedragos.ansi");
        PrintStream anterior = System.out;
        PrintStream captura = consola();
        try {
            System.setProperty("viajedragos.ansi", "true");
            if (salidaEstandar) System.setOut(captura);
            int[] tiradas = {1,1,1,1,6,1,6};
            int[] usadas = {0};
            RepositorioEnemigos repo = () -> List.of(new DefinicionEnemigo("lobo", "Lobo", "",5,2,2,1,2,18));
            assertFalse(app.jugarAventura(j, new Scanner("1\n2\n1\n1\n1\n1\n0\n0\n0\n0\n"), captura,
                    () -> tiradas[usadas[0]++], new Random(0), repo, new Random(0)));
            assertEquals(tiradas.length, usadas[0]);
            assertTrue(texto().contains("DIFERENCIA"));
            assertEquals(salidaEstandar, texto().contains("\u001B[32m+2\u001B[0m"));
            if (!salidaEstandar) assertFalse(texto().contains("\u001B"));
        } finally {
            System.setOut(anterior);
            if (propiedadAnterior == null) System.clearProperty("viajedragos.ansi");
            else System.setProperty("viajedragos.ansi", propiedadAnterior);
        }
    }

    @Test
    void equipoGuardadoContinuaEntreEncuentrosSinCambiarLaRecompensa() {
        Jugador j = jugador();
        Equipo guardada = pieza("Reservada", TipoEquipo.ARMA, 3);
        j.agregarEquipo(guardada);
        int[] encuentros = {0};
        j.aprenderHabilidad(new Habilidad("Observar continuidad") {
            @Override public void alInicioCombate(Combatiente p, Combatiente o, EstadoHabilidad estado) {
                assertSame(j, p);
                assertEquals(1, j.getInventarioEquipo().size());
                assertSame(guardada, j.getInventarioEquipo().getFirst());
                assertEquals(encuentros[0]++ == 0 ? 6 : 7, j.getFuerza());
            }
        }, 1);
        RepositorioEnemigos repo = () -> List.of(new DefinicionEnemigo("lobo", "Lobo", "",5,2,2,1,2,18));
        int[] tiradas = {1,1,1,1,6,1,6, 1,1,1,1,6,1,6};
        int[] usadas = {0};
        assertFalse(app.jugarAventura(j, new Scanner("1\n1\n3\n1\n2\n0\n"), consola(),
                () -> tiradas[usadas[0]++], new Random(0), repo, new Random(0)));
        assertEquals(tiradas.length, usadas[0]);
        assertEquals(2, encuentros[0]);
        assertEquals(1, j.getInventarioEquipo().size());
        assertSame(guardada, j.getInventarioEquipo().getFirst());
        assertEquals("Espada desgastada", j.getEquipoEquipado().get(TipoEquipo.ARMA).getNombre());
        assertEquals(1, texto().split("Has encontrado una Espada desgastada", -1).length - 1);
    }
}
