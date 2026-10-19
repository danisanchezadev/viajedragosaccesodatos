package org.viajedragos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.catalogo.*;
import org.viajedragos.consola.*;
import org.viajedragos.modelo.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class NombrePersonajeTest {
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private PrintStream salida() { return new PrintStream(bytes, true, StandardCharsets.UTF_8); }
    private String texto() { return TextoConsola.contenido(bytes.toString(StandardCharsets.UTF_8)).replaceAll("\\s+", " "); }
    private Random sinBotin() { return new Random(0) { public int nextInt(int limite) { return limite - 1; } }; }
    private void ejecutar(String entrada, int encuentros) {
        int[] tiradas = {0}, pausas = {0};
        app.ejecutar(new Scanner(entrada), salida(), () -> { tiradas[0]++; return 4; }, sinBotin(),
                () -> { assertTrue(encuentros > 0, "Cancelar o EOF no consulta el catálogo");
                    return List.of(new DefinicionEnemigo("prueba", "Prueba", "", 1, 0, 1, 0, 0, 1)); },
                new Random(0), new RepositorioObjetosMemoria(), null,
                new RitmoConsola(RitmoConsola.Velocidad.NORMAL, ms -> pausas[0]++), sinBotin(), sinBotin());
        assertEquals(encuentros * 6, tiradas[0]);
        // No hay esperas por nombre/estado: seis hitos narrativos por victoria.
        assertEquals(encuentros * 6, pausas[0]);
        comprobarPaneles();
    }
    private void comprobarPaneles() {
        bytes.toString(StandardCharsets.UTF_8).lines().filter(l -> l.startsWith("│") || l.startsWith("┌") || l.startsWith("├") || l.startsWith("└"))
                .forEach(l -> assertEquals(50, PanelConsola.anchoVisible(l), l));
    }

    @ParameterizedTest @ValueSource(strings = {"Ana", "  María del Río  ", "É", "Álvaro-27", "\u00a0Lucía\u00a0"})
    void nombreValidoSeRecortaYNoConsumeLaSiguienteEntrada(String nombre) {
        var entrada = new Scanner(nombre + "\npendiente\n");
        assertEquals(nombre.replaceAll("(?U)^\\s+|\\s+$", ""), app.solicitarNombre(entrada, salida()));
        assertEquals("pendiente", entrada.nextLine()); comprobarPaneles();
        String original = bytes.toString(StandardCharsets.UTF_8);
        assertTrue(original.indexOf("┘") < original.indexOf("Nombre >"));
    }

    @ParameterizedTest @ValueSource(strings = {"", "   ", "\u00a0", "con\ttabulador", "\tAna", "Ana\t", "A\bB", "A\u001b[31m", "A\u0000B", "A\u007fB", "A\u202eB"})
    void entradasInvalidasPermitenRepetirSinInyectarControles(String invalida) {
        assertEquals("Válido", app.solicitarNombre(new Scanner(invalida + "\nVálido\n"), salida()));
        assertEquals(1, texto().split("Nombre inválido", -1).length - 1);
        assertFalse(bytes.toString(StandardCharsets.UTF_8).contains("\u001b"));
    }

    @Test void limitesDeLongitudSonInclusivosYNoSeTruncaUnNombreLargo() {
        String treinta = "Á".repeat(30);
        assertEquals(treinta, app.solicitarNombre(new Scanner("a".repeat(31) + "\n" + treinta + "\n"), salida()));
        assertEquals(1, texto().split("Nombre inválido", -1).length - 1); comprobarPaneles();
    }

    @Test void cancelarVuelveAlMenuInicialSinConsultarRepositoriosNiDados() {
        ejecutar("1\n0\n0\n", 0);
        assertEquals(2, texto().split("1. Nueva aventura", -1).length - 1);
        assertFalse(texto().contains("ENCUENTRO 1"));
    }

    @ParameterizedTest @ValueSource(strings = {"1\n", "1\n   \n"})
    void eofEnNombreNoCreaAventura(String entrada) {
        ejecutar(entrada, 0); assertTrue(texto().contains("Entrada finalizada"));
        assertFalse(texto().contains("ENCUENTRO 1"));
    }

    @Test void nombreElegidoSeConservaEnCombateExploracionYConsultaRepetida() {
        ejecutar("1\n  Lucía del Río  \n1\n2\n6\n0\n6\n0\n3\n1\n2\n0\n", 2);
        assertTrue(texto().contains("Lucía del Río golpea a Prueba"));
        assertTrue(texto().contains("Vida final: Lucía del Río"));
        assertEquals(2, texto().split("Nombre: Lucía del Río", -1).length - 1);
        assertTrue(texto().contains("6. Estado del personaje"));
        assertFalse(texto().contains("Aventurero"));
    }

    @Test void unaAventuraNuevaPuedeTenerOtroNombreSinCambiarElRitmoDeLaSesion() {
        ejecutar("1\nAna\n1\n2\n4\n1\nLuis\n1\n2\n0\n", 2);
        assertTrue(texto().contains("Ana golpea a Prueba"));
        assertTrue(texto().contains("Luis golpea a Prueba"));
        assertEquals(2, texto().split("NOMBRE DEL PERSONAJE", -1).length - 1);
    }
}
