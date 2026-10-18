package org.viajedragos.consola;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class PanelConsolaTest {
    private final ByteArrayOutputStream salida = new ByteArrayOutputStream();
    private PanelConsola panel(String titulo) {
        return new PanelConsola(new PrintStream(salida, true, StandardCharsets.UTF_8), titulo);
    }
    private String texto() { return salida.toString(StandardCharsets.UTF_8); }

    private void comprobarBordes() {
        for (String linea : texto().split("\\R")) {
            if (linea.isEmpty()) continue;
            assertEquals(50, PanelConsola.anchoVisible(linea), linea);
            assertTrue(linea.endsWith("│") || linea.endsWith("┐") || linea.endsWith("┤") || linea.endsWith("┘"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"VIAJE A DRAGOS", "TU TURNO", "ÁRBOL", "Título muy largo que se divide automáticamente en varias líneas centradas"})
    void titulosCentradosYBordesAlineados(String titulo) {
        PanelConsola panel = panel(titulo);
        panel.espacio();
        panel.cerrar();
        comprobarBordes();
        for (String linea : texto().lines().filter(l -> l.startsWith("│") && !l.isBlank()).toList()) {
            String interior = linea.substring(1, linea.length() - 1);
            if (interior.isBlank()) continue;
            int izquierda = interior.length() - interior.stripLeading().length();
            int derecha = interior.length() - interior.stripTrailing().length();
            assertTrue(Math.abs(izquierda - derecha) <= 1, linea);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"Un nombre largo con palabras que deben ajustarse sin desbordar los bordes del panel",
            "ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ",
            "Línea uno\nLínea dos", "Árbol y pingüino", ""})
    void lineasLargasSeDividenSinPerderTexto(String contenido) {
        PanelConsola panel = panel("MENSAJE");
        panel.linea(contenido);
        panel.cerrar();
        comprobarBordes();
        String recuperado = texto().lines().filter(l -> l.startsWith("│  "))
                .filter(l -> !l.contains("MENSAJE"))
                .map(l -> l.substring(3, l.length() - 3)).reduce("", String::concat);
        assertEquals(contenido.replaceAll("\\s", ""), recuperado.replaceAll("\\s", ""));
    }

    @Test
    void columnasAjustanNombresYLargosYConservanTodosLosDatos() {
        PanelConsola panel = panel("COMBATIENTES");
        panel.columnas("Aventurero de nombre extremadamente largo", "Lobo de nombre igualmente largo");
        panel.columnas("PS 2147483647/2147483647", "PS 1/12");
        panel.columnas(PanelConsola.barraVida(44, 48), PanelConsola.barraVida(4, 12));
        panel.cerrar();
        comprobarBordes();
        assertTrue(texto().contains("extremadamente largo"));
        assertTrue(texto().contains("[█████████░]"));
        assertTrue(texto().contains("[███░░░░░░░]"));
    }

    @ParameterizedTest
    @CsvSource({"0,48,0", "24,48,5", "44,48,9", "48,48,10", "99,48,10", "-1,48,0", "5,0,0",
            "5,-1,0", "2147483647,2147483647,10"})
    void barrasSonProporcionalesYTienenLongitudFija(int vida, int maxima, int llenas) {
        assertEquals("[" + "█".repeat(llenas) + "░".repeat(10 - llenas) + "]", PanelConsola.barraVida(vida, maxima));
        assertEquals(12, PanelConsola.barraVida(vida, maxima).length());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void ansiNoAlteraAnchuraNiColoreaBordes(boolean cerrarColor) {
        PanelConsola panel = panel("ANSI");
        panel.linea("\u001B[32m" + "Texto coloreado muy largo ".repeat(6) + (cerrarColor ? "\u001B[0m" : ""));
        panel.columnas(new int[]{11, 8, 8, 11}, "FUERZA", "1", "3", "\u001B[32m+2\u001B[0m");
        panel.cerrar();
        comprobarBordes();
        for (String linea : texto().lines().filter(l -> l.contains("\u001B")).toList()) {
            assertTrue(linea.substring(linea.lastIndexOf("\u001B")).startsWith("\u001B[0m"), linea);
        }
    }

    @Test
    void opcionesNumeradasNoSeConviertenEnEntradaNiCambianLaConsolaGlobal() {
        PanelConsola panel = panel("ACCIONES");
        panel.opcion(1, "Atacar");
        panel.opcion(2, "Usar objeto");
        panel.opcion(3, "Huir");
        panel.opcion(0, "Volver");
        panel.cerrar();
        comprobarBordes();
        assertTrue(texto().contains("[1] Atacar"));
        assertTrue(texto().contains("[2] Usar objeto"));
        assertTrue(texto().contains("[3] Huir"));
        assertTrue(texto().contains("[0] Volver"));
        assertFalse(texto().contains("\u001B"));
    }

    @Test
    void rechazaColumnasQueNoCaben() {
        PanelConsola panel = panel("DATOS");
        assertThrows(IllegalArgumentException.class, () -> panel.columnas(new int[]{45}, "largo"));
        assertThrows(IllegalArgumentException.class, () -> panel.columnas(new int[]{0}, "cero"));
        assertThrows(IllegalArgumentException.class, () -> panel.columnas(new int[]{1, 1}, "solo una"));
    }
}
