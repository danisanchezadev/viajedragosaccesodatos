package org.viajedragos;

import org.junit.jupiter.api.Test;
import org.viajedragos.combate.TiradorDado;
import org.viajedragos.combate.ResultadoCombate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Scanner;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class AppTest {
    private String ejecutar(String entrada, int... valores) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        int[] usadas = {0};
        TiradorDado dados = () -> {
            assertTrue(usadas[0] < valores.length, "Tirada inesperada");
            return valores[usadas[0]++];
        };
        assertTimeoutPreemptively(Duration.ofSeconds(2), () ->
                app.ejecutar(new Scanner(entrada), new PrintStream(salida, true, StandardCharsets.UTF_8), dados, new Random(0)));
        assertEquals(valores.length, usadas[0]);
        return salida.toString(StandardCharsets.UTF_8);
    }

    @Test
    void iniciarAtacarGanarYSalir() {
        String texto = ejecutar("1\n1\n2\n0\n", 1, 1, 1, 1, 6, 1, 6);
        assertTrue(texto.contains("=== Viaje a Dragos ==="));
        assertTrue(texto.contains("1. Atacar"));
        assertTrue(texto.contains("Aventurero: 24/24 de vida | Lobo: 18/18 de vida"));
        assertTrue(texto.contains("¡Golpe crítico!"));
        assertTrue(texto.contains("Aventurero golpea a Lobo y causa 18 puntos de daño"));
        assertTrue(texto.contains("Vida final: Aventurero 24/24 | Lobo 0/18"));
        assertTrue(texto.contains("Victoria."));
        assertTrue(texto.endsWith("Hasta pronto." + System.lineSeparator()));
    }

    @Test
    void entradasInvalidasNoConsumenDadosNiCancelanLaPartida() {
        String texto = ejecutar("x\n\n1\n0\natacar\n\n 1 \nx\n2\n0\n", 1, 1, 1, 1, 6, 1, 6);
        assertEquals(6, texto.split("Entrada inválida", -1).length - 1);
        assertTrue(texto.contains("Victoria."));
    }

    @Test
    void nuevaAventuraCreaPersonajesConTodaSuVida() {
        // En cada combate el enemigo golpea primero y deja al jugador a 6 de vida.
        String texto = ejecutar("1\n1\n2\n2\n1\n1\n2\n0\n",
                1, 1, 1, 6, 6, 1, 6, 6, 1, 6,
                1, 1, 1, 6, 6, 1, 6, 6, 1, 6);
        assertEquals(2, texto.split("Comienza el combate", -1).length - 1);
        assertEquals(2, texto.split("Aventurero: 24/24 de vida", -1).length - 1);
        assertEquals(2, texto.split("Vida final: Aventurero 6/24", -1).length - 1);
        assertEquals(2, texto.split("Victoria\\.", -1).length - 1);
        assertTrue(texto.contains("1. Nueva aventura"));
        assertTrue(texto.contains("1. Continuar al siguiente encuentro"));
    }

    @Test
    void derrotaMuestraResultadoSinPedirAccionAlJugadorMuerto() {
        String texto = ejecutar("1\n1\n0\n", 1, 1, 1, 6, 6, 1, 6, 1, 6, 6, 1, 4, 2);
        assertTrue(texto.contains("Derrota."));
        assertTrue(texto.contains("Lobo contraataca a Aventurero y causa 5 puntos de daño"));
        assertEquals(1, texto.split("1. Atacar", -1).length - 1);
    }

    @Test
    void salirAntesDeCombatirNoTiraDados() {
        String texto = ejecutar("0\n");
        assertFalse(texto.contains("Comienza el combate"));
        assertTrue(texto.contains("Hasta pronto."));
    }

    @Test
    void cierreDeEntradaEnMenuSaleSinIniciarCombate() {
        assertTrue(ejecutar("").contains("Entrada finalizada"));
    }

    @Test
    void cierreDeEntradaDuranteCombateNoInventaResultado() {
        String texto = ejecutar("1\n", 1, 1, 1, 1);
        assertTrue(texto.contains("combate queda sin terminar"));
        assertFalse(texto.contains("Victoria."));
        assertFalse(texto.contains("Derrota."));
    }

    @Test
    void cierreDeEntradaDespuesDeCombateNoReinicia() {
        String texto = ejecutar("1\n1\n2\n", 1, 1, 1, 1, 6, 1, 6);
        assertTrue(texto.contains("Victoria."));
        assertTrue(texto.contains("Entrada finalizada. Hasta pronto."));
    }

    @Test
    void resultadoDeMuerteSimultaneaTieneMensajeExplicito() {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        app.mostrarResultado(ResultadoCombate.AMBOS_MUEREN,
                new PrintStream(salida, true, StandardCharsets.UTF_8));
        assertTrue(salida.toString(StandardCharsets.UTF_8).startsWith("Ambos han muerto."));
        assertTrue(salida.toString(StandardCharsets.UTF_8).contains(ResultadoCombate.AMBOS_MUEREN.getDescripcion()));
    }

    @Test
    void mainUsaLaEntradaYSalidaDeConsola() {
        InputStream anteriorEntrada = System.in;
        PrintStream anteriorSalida = System.out;
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream("0\n".getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(salida, true, StandardCharsets.UTF_8));
            app.main(new String[0]);
        } finally {
            System.setIn(anteriorEntrada);
            System.setOut(anteriorSalida);
        }
        assertTrue(salida.toString(StandardCharsets.UTF_8).contains("Viaje a Dragos"));
        assertTrue(salida.toString(StandardCharsets.UTF_8).contains("Hasta pronto."));
    }
}
