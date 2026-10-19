package org.viajedragos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.arma.*;
import org.viajedragos.habilidad.*;
import org.viajedragos.habilidad.implementacion.*;
import org.viajedragos.modelo.*;
import org.viajedragos.consola.PanelConsola;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class InventarioPresentacionTest {
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private PrintStream salida() { return new PrintStream(bytes, true, StandardCharsets.UTF_8); }
    private String texto() { return TextoConsola.contenido(bytes.toString(StandardCharsets.UTF_8)).replaceAll("\\s+", " "); }
    private Jugador jugador() { return new Jugador("Prueba", "", 6, 2, 3, 2, 2, 20, 48); }
    private Equipo pieza(String nombre, TipoEquipo tipo, int fuerza) {
        return new Equipo(null, nombre, tipo, Map.of(Atributo.FUERZA, fuerza), Map.of(), 3);
    }
    private boolean inventario(Jugador j, String entrada) { return inventario(j, entrada, false); }
    private boolean inventario(Jugador j, String entrada, boolean ansi) {
        boolean resultado = assertTimeoutPreemptively(Duration.ofSeconds(2),
                () -> app.consultarInventario(j, new Scanner(entrada), salida(), ansi));
        bytes.toString(StandardCharsets.UTF_8).lines().filter(l -> l.startsWith("│") || l.startsWith("┌") || l.startsWith("├") || l.startsWith("└"))
                .forEach(l -> assertEquals(50, PanelConsola.anchoVisible(l), l));
        return resultado;
    }
    private List<String> pantallas() {
        var pantallas = new ArrayList<String>(); StringBuilder actual = null;
        for (String linea : bytes.toString(StandardCharsets.UTF_8).lines().toList()) {
            if (linea.matches("│\\s+INVENTARIO\\s+│")) actual = new StringBuilder();
            if (actual == null) continue;
            actual.append(linea).append('\n');
            if (linea.startsWith("└")) { pantallas.add(actual.toString()); actual = null; }
        }
        return pantallas;
    }

    @Test void vacioTieneExactamenteCuatroSeccionesSinCantidadesFicticias() {
        Jugador j = jugador(); assertTrue(inventario(j, "0\n"));
        String p = pantallas().getFirst();
        assertTrue(p.indexOf("CONSUMIBLES") < p.indexOf("PERGAMINOS"));
        assertTrue(p.indexOf("PERGAMINOS") < p.indexOf("EQUIPAMIENTO"));
        assertTrue(p.indexOf("EQUIPAMIENTO") < p.indexOf("ACCIONES"));
        assertTrue(p.contains("Sin consumibles")); assertTrue(p.contains("Sin pergaminos")); assertTrue(p.contains("Sin equipamiento"));
        assertFalse(p.contains("x0")); assertFalse(p.contains("Poción")); assertFalse(p.contains("Sin equipo\n"));
        assertFalse(p.contains("Nivel:")); assertFalse(p.contains("Vida:"));
        for (int numero = 0; numero <= 8; numero++) assertEquals(1, p.split("\\[" + numero + "]", -1).length - 1);
        assertTrue(p.lines().anyMatch(l -> l.contains("[1] Comparar equipo") && l.contains("[2] Equipar objeto")));
        assertTrue(j.getInventarioPociones().isEmpty()); assertTrue(j.getInventarioEquipo().isEmpty()); assertEquals(20, j.getVida());
    }

    @Test void consumiblesCortosYCantidadesSePresentanEnLaMismaFila() {
        Jugador j = jugador(); for (int i = 0; i < 4; i++) j.agregarPocion(TipoPocion.PEQUENA);
        j.agregarPocion(TipoPocion.GRANDE); assertTrue(inventario(j, "0\n"));
        String fila = pantallas().getFirst().lines().filter(l -> l.contains("Poción pequeña")).findFirst().orElseThrow();
        assertEquals("Poción pequeña x4", fila.substring(3, 24).strip());
        assertEquals("Poción grande x1", fila.substring(26, 47).strip());
        assertTrue(texto().contains("Sin pergaminos")); assertTrue(texto().contains("Sin equipamiento"));
    }

    @Test void pergaminosSeAgrupanPorAtributoYContenidoSinMezclarse() {
        Jugador j = jugador();
        for (int i = 0; i < 2; i++) { j.agregarPergamino(new Pergamino(Atributo.FUERZA)); j.agregarPergaminoHabilidad(new PergaminoHabilidad(HabilidadDisponible.CAPARAZON)); }
        j.agregarPergamino(new Pergamino(Atributo.RESISTENCIA));
        j.agregarPergaminoHabilidad(new PergaminoHabilidad(HabilidadDisponible.RABIA));
        j.agregarPergaminoMaestria(); assertTrue(inventario(j, "0\n"));
        String p = pantallas().getFirst();
        assertTrue(p.contains("Fuerza x2")); assertTrue(p.contains("Resistencia x1"));
        assertEquals(1, p.split("Habilidad: Caparazón x2", -1).length - 1);
        assertEquals(1, p.split("Habilidad: Rabia x1", -1).length - 1);
        assertTrue(p.contains("Maestría x1")); assertFalse(p.contains("x0"));
        assertTrue(j.getHabilidades().isEmpty()); assertEquals(2, j.getCantidadPergaminos(Atributo.FUERZA));
    }

    @Test void todasLasRanurasYLasPiezasRepetidasSeMuestranIndividualmente() {
        Jugador j = jugador();
        for (TipoEquipo tipo : TipoEquipo.values()) j.equipar(pieza(tipo.getNombre(), tipo, 1));
        Equipo a = pieza("Repetida", TipoEquipo.ARMA, 2), b = pieza("Repetida", TipoEquipo.ARMA, 4);
        j.agregarEquipo(a); j.agregarEquipo(b); assertTrue(inventario(j, "9\n0\n"));
        String p = String.join("", pantallas());
        assertEquals(7, p.split("\\[E]", -1).length - 1);
        assertEquals(2, p.split("Repetida", -1).length - 1); assertFalse(p.contains("Repetida x2"));
        assertSame(a, j.getInventarioEquipo().getFirst()); assertSame(b, j.getInventarioEquipo().getLast());
        assertEquals(13, j.getFuerza()); assertEquals(2, a.getBonificaciones().get(Atributo.FUERZA));
        assertEquals(4, b.getBonificaciones().get(Atributo.FUERZA));
    }

    @Test void inspeccionPermiteDistinguirInstanciasDelMismoNombreSinEquipar() {
        Jugador j = jugador(); Equipo a = pieza("Igual", TipoEquipo.ARMA, 2), b = pieza("Igual", TipoEquipo.ARMA, 4);
        j.agregarEquipo(a); j.agregarEquipo(b);
        assertTrue(inventario(j, "8\n1\n0\n2\n0\n0\n0\n"));
        assertEquals(2, texto().split("DETALLE DE EQUIPAMIENTO", -1).length - 1);
        assertTrue(texto().contains("FUERZA +2")); assertTrue(texto().contains("FUERZA +4"));
        assertEquals(List.of(a, b), j.getInventarioEquipo()); assertTrue(j.getEquipoEquipado().isEmpty()); assertEquals(6, j.getFuerza());
    }

    @Test void nombresDeRepositorioLargosYConAnsiUsanAnchoCompletoSinTruncar() {
        Jugador j = jugador(); String largo = "Poción alternativa con un nombre bastante largo que necesita varias líneas";
        j.configurarPociones(List.of(new DefinicionPocion("pequena", TipoPocion.PEQUENA, largo, 10),
                new DefinicionPocion("grande", TipoPocion.GRANDE, "Grande alternativa", 16)));
        j.agregarPocion(TipoPocion.PEQUENA); j.agregarPocion(TipoPocion.GRANDE);
        j.agregarEquipo(pieza("\u001b[32mEquipo con nombre muy largo que comprueba el ajuste y el reset\u001b[0m", TipoEquipo.CASCO, 1));
        assertTrue(inventario(j, "0\n", true)); assertTrue(texto().contains(largo + " x1"));
        assertTrue(texto().contains("Grande alternativa x1"));
        var filas = pantallas().getFirst().lines().toList();
        assertTrue(filas.stream().filter(l -> l.contains("Poción alternativa")).allMatch(l -> l.contains("bastante")));
        filas.stream().filter(l -> l.contains("\u001b[")).forEach(l -> assertTrue(l.lastIndexOf("\u001b[0m") > l.lastIndexOf("\u001b[32m")));
    }

    @Test void consumirDesdeInventarioActualizaCantidadYRespetaAlCurarse() {
        Jugador j = new Jugador("Prueba", "", 6, 2, 3, 2, 2, 1, 100);
        var h = new InstintoSupervivencia(); j.aprenderHabilidad(h, 2);
        j.agregarPocion(TipoPocion.PEQUENA); j.agregarPocion(TipoPocion.PEQUENA);
        assertTrue(inventario(j, "7\n1\n0\n"));
        assertTrue(pantallas().getFirst().contains("Poción pequeña x2"));
        assertTrue(pantallas().getLast().contains("Poción pequeña x1"));
        assertEquals(9, j.getVida()); assertEquals(9, j.getFuerza()); assertTrue(j.getHabilidades().get(h).isEfectoTemporalActivo());
    }

    @Test void equiparYDesequiparRefrescaLaMarcaSinPerderLaPieza() {
        Jugador j = jugador(); var pieza = pieza("Elegida", TipoEquipo.ARMA, 2); j.agregarEquipo(pieza);
        assertTrue(inventario(j, "2\n1\n1\n1\n3\n1\n1\n0\n"));
        assertEquals(3, pantallas().size()); assertFalse(pantallas().getFirst().contains("[E] Elegida"));
        assertTrue(pantallas().get(1).contains("[E] Elegida")); assertFalse(pantallas().getLast().contains("[E] Elegida"));
        assertSame(pieza, j.getInventarioEquipo().getFirst()); assertTrue(j.getEquipoEquipado().isEmpty()); assertEquals(6, j.getFuerza());
    }

    @Test void losTresConsumosDePergaminosActualizanElContenidoSinReabrir() {
        Jugador j = jugador(); var pieza = pieza("Arma", TipoEquipo.ARMA, 0); j.agregarEquipo(pieza);
        j.agregarPergamino(new Pergamino(Atributo.FUERZA));
        j.agregarPergaminoHabilidad(new PergaminoHabilidad(HabilidadDisponible.CAPARAZON)); j.agregarPergaminoMaestria();
        assertTrue(inventario(j, "4\n1\n1\n1\n5\n1\n1\n6\n1\n1\n0\n"));
        assertEquals(4, pantallas().size()); assertTrue(pantallas().getLast().contains("Sin pergaminos"));
        assertFalse(pantallas().get(1).contains("Fuerza x1"));
        assertFalse(pantallas().get(2).contains("Habilidad: Caparazón"));
        assertEquals(1, pieza.getBonificaciones().get(Atributo.FUERZA)); assertEquals(2, pieza.getMejorasDisponibles());
        assertEquals(2, j.getHabilidades().get(new Caparazon()).getNivelPermanente());
    }

    @ParameterizedTest @ValueSource(strings = {"7\n0\n0\n", "8\n0\n0\n", "8\n1\n0\n0\n0\n", "9\nx\n0\n"})
    void cancelarOValidarNoModificaObjetosNiAtributos(String entrada) {
        Jugador j = jugador(); var pieza = pieza("Equipo", TipoEquipo.ARMA, 2); j.agregarEquipo(pieza); j.agregarPocion(TipoPocion.PEQUENA);
        assertTrue(inventario(j, entrada)); assertEquals(List.of(pieza), j.getInventarioEquipo());
        assertEquals(1, j.getCantidadPociones(TipoPocion.PEQUENA)); assertEquals(20, j.getVida()); assertEquals(6, j.getFuerza());
    }

    @ParameterizedTest @ValueSource(strings = {"", "7\n", "8\n", "8\n1\n"})
    void eofEnCadaNuevoNivelNoModificaEstado(String entrada) {
        Jugador j = jugador(); var pieza = pieza("Equipo", TipoEquipo.ARMA, 2); j.agregarEquipo(pieza); j.agregarPocion(TipoPocion.PEQUENA);
        assertFalse(inventario(j, entrada)); assertEquals(List.of(pieza), j.getInventarioEquipo());
        assertEquals(1, j.getCantidadPociones(TipoPocion.PEQUENA)); assertEquals(20, j.getVida()); assertEquals(6, j.getFuerza());
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void compararConservaAnsiYRefrescaSinCambiarInstancias(boolean ansi) {
        Jugador j = jugador(); var actual = pieza("Actual", TipoEquipo.ARMA, 1); var nueva = pieza("Nueva", TipoEquipo.ARMA, 3);
        j.equipar(actual); j.agregarEquipo(nueva);
        assertTrue(inventario(j, "1\n1\n1\n0\n0\n0\n", ansi));
        assertEquals(ansi, bytes.toString(StandardCharsets.UTF_8).contains("\u001b[32m+2\u001b[0m"));
        assertTrue(pantallas().getFirst().equals(pantallas().getLast()));
        assertSame(actual, j.getEquipoEquipado().get(TipoEquipo.ARMA)); assertSame(nueva, j.getInventarioEquipo().getFirst());
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void opcionesDePanelUsanDosColumnasOSuAnchuraCompletaSiNoCaben(boolean largo) {
        var panel = new PanelConsola(salida(), "OPCIONES");
        String derecha = largo ? "Una opción con texto largo que requiere todo el ancho" : "Equipar";
        panel.opcion(1, "Comparar", 2, derecha); panel.cerrar();
        var filas = bytes.toString(StandardCharsets.UTF_8).lines().filter(l -> l.startsWith("│")).toList();
        assertEquals(!largo, filas.stream().anyMatch(l -> l.contains("[1] Comparar") && l.contains("[2] Equipar")));
        assertTrue(texto().contains("2. " + derecha));
        bytes.toString(StandardCharsets.UTF_8).lines().filter(l -> l.startsWith("│") || l.startsWith("┌") || l.startsWith("├") || l.startsWith("└"))
                .forEach(l -> assertEquals(50, PanelConsola.anchoVisible(l), l));
    }
}
