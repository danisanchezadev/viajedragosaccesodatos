package org.viajedragos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.arma.*;
import org.viajedragos.catalogo.*;
import org.viajedragos.consola.*;
import org.viajedragos.habilidad.*;
import org.viajedragos.habilidad.implementacion.*;
import org.viajedragos.modelo.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MenuPersonajeTest {
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private PrintStream salida() { return new PrintStream(bytes, true, StandardCharsets.UTF_8); }
    private String bruto() { return bytes.toString(StandardCharsets.UTF_8); }
    private String plano() { return bruto().replaceAll("\u001b\\[[0-9;]*m", ""); }
    private String texto() { return TextoConsola.contenido(plano()).replaceAll("\\s+", " "); }
    private Jugador jugador() { return new Jugador("Arthos", "Prueba", 6, 2, 3, 2, 2, 24, 48); }
    private Equipo pieza(String nombre, int fuerza) {
        return new Equipo(null, nombre, TipoEquipo.ARMA, Map.of(Atributo.FUERZA, fuerza), Map.of(new Caparazon(), 2), 3);
    }
    private void ancho(int valor, Runnable tarea) {
        String anterior = System.getProperty("viajedragos.columnas");
        try { System.setProperty("viajedragos.columnas", String.valueOf(valor)); tarea.run(); }
        finally { if (anterior == null) System.clearProperty("viajedragos.columnas"); else System.setProperty("viajedragos.columnas", anterior); }
    }
    private void general(Jugador j, boolean ansi) {
        ancho(102, () -> VistaMenuPersonaje.general(j, salida(), new TiempoAventura(() -> 0L), ansi));
        bordes(102);
    }
    private void bordes(int esperado) {
        boolean hayPanel = false;
        for (String linea : plano().lines().toList()) if (linea.matches("^[│┌├└].*")) {
            hayPanel = true; assertEquals(esperado, PanelConsola.anchoVisible(linea), linea);
        }
        assertTrue(hayPanel);
    }
    private boolean menu(Jugador j, String entrada) {
        boolean[] resultado = {false};
        ancho(102, () -> resultado[0] = assertTimeoutPreemptively(Duration.ofSeconds(3),
                () -> app.consultarPersonaje(j, new Scanner(entrada), salida(), new TiempoAventura(() -> 0L), false)));
        TextoConsola.comprobarPaneles(bruto());
        return resultado[0];
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void generalVacioTieneDatosRealesTresColumnasYDosFilasDeAcciones(boolean ansi) {
        Jugador j = jugador(); j.agregarExperiencia(10); general(j, ansi);
        assertTrue(texto().contains("Arthos")); assertTrue(texto().contains("NIVEL 1"));
        assertTrue(texto().contains("ORO 15")); assertTrue(texto().contains("TIEMPO 00:00:00"));
        assertTrue(texto().contains("VIDA 24/48 [█████░░░░░]")); assertTrue(texto().contains("EXP 10/20 [█████░░░░░]"));
        assertEquals(3, plano().split("Sin objetos", -1).length - 1);
        assertFalse(texto().contains("Base:")); assertFalse(texto().contains("Equipo:"));
        String cabecera = plano().lines().filter(l -> l.contains("CONSUMIBLES")).findFirst().orElseThrow();
        assertEquals("CONSUMIBLES", cabecera.substring(3, 33).strip());
        assertEquals("PERGAMINOS", cabecera.substring(35, 65).strip());
        assertEquals("EQUIPAMIENTO", cabecera.substring(67, 99).strip());
        assertTrue(plano().lines().anyMatch(l -> l.contains("[1] Estadísticas") && l.contains("[4] Equipar")));
        assertTrue(plano().lines().anyMatch(l -> l.contains("[5] Desequipar") && l.contains("[0] Volver")));
        assertEquals(ansi, bruto().contains("\u001b[44m"));
        if (ansi) bruto().lines().filter(l -> l.contains("│") || l.contains("─")).forEach(l -> assertTrue(l.endsWith("\u001b[0m")));
    }

    @Test void categoriasPobladasConCantidadesYReferenciasIndependientes() {
        Jugador j = jugador(); for (int i = 0; i < 12; i++) j.agregarPocion(TipoPocion.PEQUENA);
        j.agregarPergamino(new Pergamino(Atributo.FUERZA));
        j.agregarPergaminoHabilidad(new PergaminoHabilidad(HabilidadDisponible.RABIA));
        j.agregarPergaminoHabilidad(new PergaminoHabilidad(HabilidadDisponible.CAPARAZON)); j.agregarPergaminoMaestria();
        Equipo a = pieza("Espada", 2), b = pieza("Espada", 3); j.equipar(a); j.agregarEquipo(b);
        general(j, false);
        assertTrue(texto().contains("Poción pequeña x12")); assertTrue(texto().contains("Fuerza x1"));
        assertTrue(texto().contains("Habilidad: Rabia x1")); assertTrue(texto().contains("Habilidad: Caparazón x1"));
        assertTrue(texto().contains("Maestría x1")); assertTrue(texto().contains("[E] Espada x1"));
        assertEquals(2, texto().split("Espada x1", -1).length - 1);
        String fila = plano().lines().filter(l -> l.contains("Poción pequeña")).findFirst().orElseThrow();
        assertTrue(fila.substring(3, 33).endsWith("x12"));
        assertTrue(fila.substring(67, 99).endsWith("x1"));
        assertSame(a, j.getEquipoEquipado().get(TipoEquipo.ARMA)); assertSame(b, j.getInventarioEquipo().getFirst());
        assertEquals(8, j.getFuerza()); assertTrue(texto().contains("FUERZA 8"));
    }

    @Test void cincoObjetosPorCategoriaYContadorDeOmitidosSinLimitarElInventarioReal() {
        Jugador j = jugador();
        for (int i = 1; i <= 8; i++) j.agregarEquipo(pieza("Pieza " + i, i));
        for (Atributo a : Atributo.values()) j.agregarPergamino(new Pergamino(a));
        general(j, false);
        assertTrue(texto().contains("Pieza 5 x1")); assertFalse(texto().contains("Pieza 6"));
        assertTrue(texto().contains("+3 objetos más")); assertTrue(texto().contains("+1 objetos más"));
        assertEquals(8, j.getInventarioEquipo().size()); assertEquals(6, j.getPergaminos().size());
    }

    @ParameterizedTest @ValueSource(ints = {80, 102})
    void textosLargosYNumerosGrandesMantienenBordesYCantidades(int columnas) throws Exception {
        Jugador j = jugador(); j.setNombre("Á".repeat(30));
        String largo = "Equipo de nombre deliberadamente largo para la vista general";
        j.agregarEquipo(pieza(largo, 2));
        var campo = Jugador.class.getDeclaredField("pergaminosMaestria"); campo.setAccessible(true); campo.setInt(j, Integer.MAX_VALUE);
        j.setVidaMaxima(Integer.MAX_VALUE); j.setFuerza(Integer.MAX_VALUE);
        ancho(columnas, () -> VistaMenuPersonaje.general(j, salida(), new TiempoAventura(() -> 0), false));
        bordes(columnas == 80 ? 50 : 102);
        assertTrue(texto().contains("2147483647")); assertTrue(texto().contains("…"));
        assertEquals(largo, j.getInventarioEquipo().getFirst().getNombre());
        assertEquals(Integer.MAX_VALUE, j.getCantidadPergaminosMaestria());
    }

    @Test void vistaCompactaEsConfigurableYSoloAfectaAlMenuNuevo() {
        Jugador j = jugador(); ancho(80, () -> VistaMenuPersonaje.general(j, salida(), new TiempoAventura(() -> 0), false));
        bordes(50); assertTrue(texto().contains("CONSUMIBLES")); assertTrue(texto().contains("EQUIPAMIENTO"));
        bytes.reset(); ancho(102, () -> { var panel = new PanelConsola(salida(), "TRADICIONAL"); panel.cerrar(); }); bordes(50);
    }

    @Test void estadisticasDesglosaBaseEquipoEfectivoYAdvierteTemporalesSinLimpiarlos() {
        Jugador j = new Jugador("Arthos", "", 6, 2, 3, 2, 2, 1, 100);
        var h = new InstintoSupervivencia(); j.aprenderHabilidad(h, 1); j.curarse(8);
        j.equipar(pieza("Espada", 2)); var estado = j.getHabilidades().get(h);
        ancho(102, () -> VistaMenuPersonaje.estadisticas(j, salida(), false)); bordes(102);
        assertTrue(texto().contains("FUERZA | Efectivo: 11"));
        assertTrue(texto().contains("Base: 9 | Equipo: +2")); assertTrue(texto().contains("Efectos temporales activos"));
        assertTrue(texto().contains("VIDA MÁXIMA | Efectivo: 100"));
        assertEquals(11, j.getFuerza()); assertEquals(9, j.getVida()); assertSame(estado, j.getHabilidades().get(h));
        assertTrue(estado.isEfectoTemporalActivo());
    }

    @Test void habilidadesAmpliasNoDuplicanLasDosProcedencias() {
        Jugador j = jugador(); var h = new Caparazon(); j.aprenderHabilidad(h, 2);
        j.equipar(new Equipo(null, "Arma", TipoEquipo.ARMA, Map.of(), Map.of(h, 2, new Rabia(), 3), 0));
        ancho(102, () -> VistaMenuPersonaje.habilidades(j, salida(), false)); bordes(102);
        assertEquals(1, texto().split("Caparazón", -1).length - 1);
        assertTrue(texto().contains("Permanente: 2 | Equipo: +2 | Total: 4"));
        assertTrue(texto().contains("Rabia | Nivel de equipo: 3")); assertTrue(texto().contains("Fuente: Arma (+3)"));
        assertEquals(2, j.getHabilidades().size());
    }

    @Test void navegarPorTodasLasVistasNoCambiaElPersonajeNiPierdeLaSiguienteEntrada() {
        Jugador j = jugador(); var pieza = pieza("Arma", 2); j.agregarEquipo(pieza);
        j.agregarPocion(TipoPocion.PEQUENA); j.agregarPergaminoMaestria();
        Scanner entrada = new Scanner("1\n0\n2\n8\n1\n0\n0\n0\n6\n0\n0\npendiente\n");
        ancho(102, () -> assertTrue(app.consultarPersonaje(j, entrada, salida(), new TiempoAventura(() -> 0), false)));
        assertEquals("pendiente", entrada.nextLine()); TextoConsola.comprobarPaneles(bruto());
        assertTrue(texto().contains("ESTADÍSTICAS")); assertTrue(texto().contains("INVENTARIO"));
        assertTrue(texto().contains("Mejoras aplicadas: 0")); assertTrue(texto().contains("HABILIDADES APRENDIDAS"));
        assertEquals(24, j.getVida()); assertEquals(15, j.getOro()); assertEquals(6, j.getFuerza());
        assertSame(pieza, j.getInventarioEquipo().getFirst()); assertEquals(1, j.getCantidadPergaminosMaestria());
    }

    @Test void consumirEquiparDesequiparYMejorarSeReflejaEnLaVistaGeneral() {
        Jugador j = jugador(); var pieza = pieza("Arma", 2); j.agregarEquipo(pieza); j.agregarPocion(TipoPocion.PEQUENA);
        j.agregarPergamino(new Pergamino(Atributo.FUERZA));
        assertTrue(menu(j, "3\n1\n4\n1\n1\n1\n5\n1\n1\n2\n4\n1\n1\n1\n0\n0\n"));
        assertEquals(32, j.getVida()); assertEquals(0, j.getCantidadPociones(TipoPocion.PEQUENA));
        assertTrue(j.getEquipoEquipado().isEmpty()); assertSame(pieza, j.getInventarioEquipo().getFirst());
        assertEquals(3, pieza.getBonificaciones().get(Atributo.FUERZA)); assertEquals(2, pieza.getMejorasDisponibles());
        assertTrue(texto().contains("[E] Arma x1")); assertTrue(texto().contains("FUERZA 8"));
        assertTrue(texto().contains("VIDA 32/48"));
    }

    @Test void pergaminosDeHabilidadYMaestriaSiguenAccesiblesDesdeInventario() {
        Jugador j = jugador(); j.agregarPergaminoHabilidad(new PergaminoHabilidad(HabilidadDisponible.CAPARAZON)); j.agregarPergaminoMaestria();
        assertTrue(menu(j, "2\n5\n1\n1\n6\n1\n1\n0\n0\n"));
        assertEquals(2, j.getHabilidades().get(new Caparazon()).getNivelPermanente());
        assertEquals(0, j.getCantidadPergaminosMaestria()); assertTrue(j.getPergaminosHabilidad().isEmpty());
    }

    @Test void inventarioYPiezasSeRecorrenPorPaginasConNombresCompletos() {
        Jugador j = jugador();
        for (int i = 1; i <= 19; i++) j.agregarEquipo(pieza("Pieza de nombre largo número " + i + " que debe mostrarse completa", i));
        ancho(102, () -> assertTrue(app.consultarInventario(j, new Scanner("9\n9\n10\n10\n8\n9\n9\n3\n0\n0\n0\n"), salida())));
        TextoConsola.comprobarPaneles(bruto());
        assertTrue(texto().contains("Página 3/3"));
        assertTrue(texto().contains("Pieza de nombre largo número 19 que debe mostrarse completa"));
        assertTrue(texto().contains("FUERZA +19")); assertEquals(19, j.getInventarioEquipo().size()); assertEquals(6, j.getFuerza());
    }

    @ParameterizedTest @ValueSource(strings = {"", "1\n", "2\n", "6\n", "4\n", "5\n", "3\n"})
    void eofEnCadaVistaOAccionNoModificaNada(String entrada) {
        Jugador j = jugador(); var pieza = pieza("Arma", 2); j.agregarEquipo(pieza); j.agregarPocion(TipoPocion.PEQUENA);
        assertFalse(menu(j, entrada)); assertEquals(24, j.getVida()); assertEquals(6, j.getFuerza());
        assertEquals(1, j.getCantidadPociones(TipoPocion.PEQUENA)); assertSame(pieza, j.getInventarioEquipo().getFirst());
    }

    @ParameterizedTest @ValueSource(strings = {"7\nx\n0\n", "3\n0\n0\n", "4\n0\n0\n", "5\n0\n0\n"})
    void cancelarOValidarMantieneRecursos(String entrada) {
        Jugador j = jugador(); j.agregarEquipo(pieza("Arma", 2)); j.agregarPocion(TipoPocion.PEQUENA);
        assertTrue(menu(j, entrada)); assertEquals(24, j.getVida()); assertEquals(1, j.getCantidadPociones(TipoPocion.PEQUENA));
        assertTrue(j.getEquipoEquipado().isEmpty());
    }

    @Test void tiempoIncluyeCombatePausasYMenusPeroExcluyeLoAnteriorALaAventura() {
        long[] reloj = {500_000_000_000L}; var tiempo = new TiempoAventura(() -> reloj[0]);
        Jugador j = jugador(); j.setFuerza(20); int[] dados = {0}, esperas = {0};
        var ritmo = new RitmoConsola(RitmoConsola.Velocidad.NORMAL, ms -> { reloj[0] += 1_000_000_000L; esperas[0]++; });
        Random sinBotin = new Random(0) { public int nextInt(int n) { return n - 1; } };
        ancho(102, () -> assertFalse(app.jugarAventura(j, new Scanner("1\n2\n1\n1\n0\n6\n0\n0\n0\n"), salida(),
                () -> { dados[0]++; reloj[0] += 1_000_000_000L; return 4; }, sinBotin,
                () -> List.of(new DefinicionEnemigo("prueba", "Prueba", "", 1, 0, 1, 0, 0, 1)), new Random(0),
                new RepositorioObjetosMemoria(), null, ritmo, sinBotin, sinBotin, tiempo)));
        assertEquals(6, dados[0]); assertEquals(6, esperas[0]);
        assertTrue(texto().contains("TIEMPO 00:00:12")); assertFalse(texto().contains("00:08:"));
        assertTrue(texto().contains("1. Menú de personaje")); assertFalse(texto().contains("6. Estado del personaje"));
    }
}
