package org.viajedragos.modelo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.arma.Arma;
import org.viajedragos.arma.Atributo;
import org.viajedragos.arma.Equipo;
import org.viajedragos.arma.TipoEquipo;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ComparacionEquipoTest {
    private Equipo pieza(Map<Atributo,Integer> bonificaciones) {
        return new Arma("Prueba", TipoEquipo.ARMA, bonificaciones, Map.of());
    }

    @ParameterizedTest
    @CsvSource({"1,3,+2", "3,1,-2", "3,3,0", "-4,-1,+3", "-1,-4,-3", "0,0,0"})
    void diferenciaEsNuevaMenosActual(int actual, int nuevo, String diferencia) {
        var salida = new ByteArrayOutputStream();
        pieza(Map.of(Atributo.FUERZA, actual)).compararCon(pieza(Map.of(Atributo.FUERZA, nuevo)),
                new PrintStream(salida, true, StandardCharsets.UTF_8), false);
        String[] columnas = salida.toString(StandardCharsets.UTF_8).lines()
                .filter(l -> l.startsWith("│  FUERZA")).map(l -> l.substring(3, l.length() - 3))
                .findFirst().orElseThrow().trim().split("\\s+");
        assertArrayEquals(new String[]{"FUERZA", actual == 0 ? "--" : "" + actual,
                nuevo == 0 ? "--" : "" + nuevo, diferencia}, columnas);
        assertFalse(salida.toString(StandardCharsets.UTF_8).contains("\u001B"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false,true})
    void unionOrdenadaYColoresSoloEnDiferencias(boolean ansi) {
        Equipo actual = pieza(Map.of(Atributo.FUERZA, 1, Atributo.RESISTENCIA, 4, Atributo.INICIATIVA, 2));
        Equipo nueva = pieza(Map.of(Atributo.FUERZA, 3, Atributo.AGILIDAD, -2, Atributo.INICIATIVA, 2));
        var salida = new ByteArrayOutputStream();
        actual.compararCon(nueva, new PrintStream(salida, true, StandardCharsets.UTF_8), ansi);
        String texto = salida.toString(StandardCharsets.UTF_8);
        List<String> filas = texto.lines().filter(l -> l.startsWith("│  "))
                .map(l -> l.substring(3, l.length() - 3))
                .filter(l -> l.matches("^(FUERZA|RESISTENCIA|AGILIDAD|INICIATIVA)\\s+.*")).toList();
        assertEquals(4, filas.size());
        List<Atributo> esperados = List.of(Atributo.FUERZA, Atributo.RESISTENCIA, Atributo.AGILIDAD, Atributo.INICIATIVA);
        for (int i = 0; i < filas.size(); i++) {
            String fila = filas.get(i);
            assertTrue(fila.startsWith(esperados.get(i).name()));
            assertFalse(fila.substring(0, 33).contains("\u001B"), "Atributo, ACTUAL y NUEVO sin colores");
        }
        String limpio = texto.replaceAll("\u001B\\[[0-9;]*m", "");
        assertTrue(limpio.lines().anyMatch(l -> l.matches("│\\s+RESISTENCIA\\s+4\\s+--\\s+-4\\s+│")));
        assertTrue(limpio.lines().anyMatch(l -> l.matches("│\\s+AGILIDAD\\s+--\\s+-2\\s+-2\\s+│")));
        if (ansi) {
            assertTrue(texto.contains("\u001B[32m+2\u001B[0m"));
            assertTrue(texto.contains("\u001B[31m-4\u001B[0m"));
            assertTrue(texto.contains("\u001B[31m-2\u001B[0m"));
            assertEquals(3, texto.split("\u001B\\[0m", -1).length - 1);
            assertFalse(filas.getLast().contains("\u001B"), "Diferencia cero sin color");
        } else assertFalse(texto.contains("\u001B"));
        var sinColores = new ByteArrayOutputStream();
        actual.compararCon(nueva, new PrintStream(sinColores, true, StandardCharsets.UTF_8), false);
        assertEquals(sinColores.toString(StandardCharsets.UTF_8), limpio, "Mismos valores y alineación");
    }

    @Test
    void mapasVaciosNoInventanFilasNiCodigosDeColor() {
        var salida = new ByteArrayOutputStream();
        pieza(Map.of()).compararCon(pieza(Map.of()), new PrintStream(salida), true);
        assertEquals(7, salida.toString(StandardCharsets.UTF_8).lines().count());
        assertFalse(salida.toString().contains("\u001B"));
    }
}
