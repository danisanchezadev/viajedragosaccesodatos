package org.viajedragos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.arma.*;
import org.viajedragos.catalogo.RepositorioEnemigos;
import org.viajedragos.catalogo.RepositorioTiendaMemoria;
import org.viajedragos.combate.*;
import org.viajedragos.consola.PanelConsola;
import org.viajedragos.modelo.*;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class InterfazConsolaTest {
    private final ByteArrayOutputStream salida = new ByteArrayOutputStream();
    private PrintStream consola() { return new PrintStream(salida, true, StandardCharsets.UTF_8); }
    private String texto() { return salida.toString(StandardCharsets.UTF_8); }

    @Test
    void mainEmiteUnicodeAunqueLaSalidaOriginalUseAscii() {
        PrintStream original = System.out;
        InputStream entradaOriginal = System.in;
        try {
            System.setIn(new ByteArrayInputStream("0\n".getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(salida, true, StandardCharsets.US_ASCII));
            app.main(new String[0]);
        } finally {
            System.setOut(original);
            System.setIn(entradaOriginal);
        }
        comprobarPaneles();
        assertTrue(texto().contains("┌" + "─".repeat(48) + "┐"));
        assertTrue(texto().contains("[0] Salir del juego"));
        assertTrue(texto().contains("Elige una opción > Hasta pronto."));
    }

    private void comprobarPaneles() {
        int abiertos = 0;
        int ancho = 50;
        for (String linea : texto().split("\\R")) {
            if (!linea.matches("^[│┌├└].*")) continue;
            if (linea.startsWith("┌")) {
                ancho = PanelConsola.anchoVisible(linea);
                assertTrue(ancho == 50 || ancho == 102, linea);
            }
            assertEquals(ancho, PanelConsola.anchoVisible(linea), linea);
            if (linea.startsWith("┌")) abiertos++;
            if (linea.startsWith("└")) abiertos--;
            assertTrue(abiertos == 0 || abiertos == 1, linea);
        }
        assertEquals(0, abiertos);
        assertTrue(texto().contains("┌"));
        assertFalse(texto().contains("----------------------------------------"));
    }

    @Test
    void aventuraConTiendaInventarioUsoEquipamientoYRecompensasConservaAcciones() {
        Jugador jugador = new Jugador("Aventurero", "", 6, 2, 3, 2, 2, 1, 100);
        RepositorioEnemigos enemigos = () -> List.of(new DefinicionEnemigo("prueba", "Enemigo", "", 1, 0, 1, 0, 0, 1));
        int[] tiradas = {0};
        int[] botin = {0};
        Random azar = new Random(0) {
            @Override public int nextInt(int limite) {
                assertEquals(100, limite);
                botin[0]++;
                return 99;
            }
        };
        String entrada = "1\n2\n5\n1\n1\n3\n0\n0\n2\n1\n1\n2\n2\n1\n1\n1\n0\n0\n0\n";
        assertFalse(app.jugarAventura(jugador, new Scanner(entrada), consola(), () -> {
            assertTrue(++tiradas[0] <= 6);
            return 4;
        }, azar, enemigos, new Random(0)));
        assertEquals(6, tiradas[0]);
        assertEquals(2, botin[0]);
        assertEquals(3, jugador.getOro());
        assertEquals(9, jugador.getVida());
        assertEquals(7, jugador.getFuerza());
        assertEquals("Espada desgastada", jugador.getEquipoEquipado().get(TipoEquipo.ARMA).getNombre());
        comprobarPaneles();
        for (String titulo : List.of("ENCUENTRO", "TU TURNO", "RECOMPENSAS", "EXPLORACIÓN", "TIENDA", "USAR OBJETO", "INVENTARIO", "CONFIRMAR EQUIPAMIENTO")) {
            assertTrue(texto().contains(titulo), titulo);
        }
        assertTrue(texto().contains("[5] Tienda"));
        assertTrue(texto().contains("[0] Salir del juego"));
        assertFalse(texto().contains("\u001B"));
    }

    @Test
    void nombresLargosNoRompenInventarioComparacionYTienda() {
        Jugador jugador = new Jugador();
        String nombre = "Espada de nombre extremadamente largo para comprobar el ajuste automático";
        jugador.equipar(new Arma(nombre, TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 1), Map.of()));
        jugador.agregarEquipo(new Arma(nombre + " alternativa", TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 3), Map.of()));
        assertTrue(app.consultarInventario(jugador, new Scanner("1\n1\n1\n0\n0\n0\n"), consola(), true));
        assertTrue(app.consultarTienda(jugador, new Scanner("0\n"), consola(), new RepositorioTiendaMemoria()));
        comprobarPaneles();
        assertTrue(texto().contains("\u001B[32m+2\u001B[0m"));
        assertEquals(1, jugador.getInventarioEquipo().size());
        assertEquals(1, jugador.getFuerza());
    }

    @ParameterizedTest
    @ValueSource(strings = {"normal", "critico", "fallo", "contraataque", "huida", "pocion"})
    void panelesDeCombateConDadosControladosMantienenResultadoYDano(String caso) {
        Jugador jugador = new Jugador("Jugador con un nombre muy largo para ajustar columnas", "", 10, 0, 3, 0, 2, 30, 30);
        Enemigo enemigo = new Enemigo("Enemigo con otro nombre muy largo para ajustar columnas", "", 6, 0, 1, 0, 0, 10, 30);
        int[] dados;
        String entrada;
        ResultadoCombate esperado;
        String titulo;
        switch (caso) {
            case "critico" -> { dados = new int[]{1, 1, 1, 1, 6, 1, 4}; entrada = "1\n"; esperado = ResultadoCombate.ENEMIGO_MUERE; titulo = "GOLPE CRÍTICO"; }
            case "fallo" -> { jugador.setVida(5); dados = new int[]{1, 1, 1, 1, 1, 2, 4, 2}; entrada = "1\n"; esperado = ResultadoCombate.JUGADOR_MUERE; titulo = "ATAQUE FALLIDO"; }
            case "contraataque" -> { jugador.setVida(5); dados = new int[]{1, 1, 1, 1, 1, 6}; entrada = "1\n"; esperado = ResultadoCombate.JUGADOR_MUERE; titulo = "CONTRAATAQUE"; }
            case "huida" -> { dados = new int[]{1, 1, 1, 1, 4, 4}; entrada = "3\n"; esperado = ResultadoCombate.JUGADOR_HUYE; titulo = "Has conseguido huir"; }
            case "pocion" -> { jugador.setVida(20); jugador.agregarPocion(TipoPocion.PEQUENA); dados = new int[]{1, 1, 1, 1, 4, 2, 1, 6, 4, 2}; entrada = "2\n1\n1\n"; esperado = ResultadoCombate.ENEMIGO_MUERE; titulo = "USAR OBJETO"; }
            default -> { dados = new int[]{1, 1, 1, 1, 4, 2}; entrada = "1\n"; esperado = ResultadoCombate.ENEMIGO_MUERE; titulo = "RESULTADO DEL ATAQUE"; }
        }
        int[] usadas = {0};
        assertEquals(esperado, new Combate(jugador, enemigo, () -> {
            assertTrue(usadas[0] < dados.length);
            return dados[usadas[0]++];
        }).combatir(new Scanner(entrada), consola()));
        assertEquals(dados.length, usadas[0]);
        comprobarPaneles();
        assertTrue(texto().contains(titulo));
        if (caso.equals("pocion")) {
            assertEquals(20, jugador.getVida());
            assertEquals(0, jugador.getCantidadPociones(TipoPocion.PEQUENA));
        }
    }
}
