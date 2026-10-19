package org.viajedragos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.arma.*;
import org.viajedragos.consola.*;
import org.viajedragos.habilidad.*;
import org.viajedragos.modelo.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PergaminosHabilidadesConsolaTest {
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private PrintStream salida() { return new PrintStream(bytes, true, StandardCharsets.UTF_8); }
    private String texto() { return TextoConsola.contenido(bytes.toString(StandardCharsets.UTF_8)).replaceAll("\\s+", " "); }
    private Jugador jugador() { return new Jugador("Prueba", "", 6, 2, 3, 2, 2, 20, 30); }
    private boolean inventario(Jugador j, String entrada) {
        boolean resultado = assertTimeoutPreemptively(Duration.ofSeconds(2),
                () -> app.consultarInventario(j, new Scanner(entrada), salida()));
        assertTrue(bytes.toString(StandardCharsets.UTF_8).contains("┌"));
        bytes.toString(StandardCharsets.UTF_8).lines()
                .filter(l -> l.startsWith("┌") || l.startsWith("│") || l.startsWith("├") || l.startsWith("└"))
                .forEach(l -> assertEquals(50, PanelConsola.anchoVisible(l), l));
        assertFalse(bytes.toString(StandardCharsets.UTF_8).contains("\u001b["));
        return resultado;
    }
    private void dar(Jugador j) {
        j.agregarPergaminoHabilidad(new PergaminoHabilidad(HabilidadDisponible.CAPARAZON));
        j.agregarPergaminoMaestria();
    }

    @ParameterizedTest @CsvSource({"false,false", "true,false", "false,true", "true,true"})
    void confirmaNivelesPermanentesYEquipoSinConfundirlos(boolean aprendida, boolean equipada) {
        Jugador j = jugador(); var h = HabilidadDisponible.CAPARAZON.crearHabilidad(); dar(j);
        if (aprendida) j.aprenderHabilidad(h, 2);
        if (equipada) j.equipar(new Equipo(null, "Arma", TipoEquipo.ARMA, Map.of(), Map.of(h, 2), 0));
        assertTrue(inventario(j, "5\n1\n1\n0\n"));
        int permanente = aprendida ? 2 : 0, equipo = equipada ? 2 : 0;
        assertTrue(texto().contains("Aprendida permanentemente: " + (aprendida ? "Sí" : "No")));
        assertTrue(texto().contains("Nivel permanente: " + permanente + " -> " + (permanente + 1)));
        if (equipada) {
            assertTrue(texto().contains("Contribución de equipo: 2"));
            assertTrue(texto().contains("Nivel efectivo: " + (permanente + equipo) + " -> " + (permanente + equipo + 1)));
        }
        assertTrue(texto().contains("HABILIDAD APRENDIDA O MEJORADA"));
        var e = j.getHabilidades().get(h);
        assertEquals(permanente + 1, e.getNivelPermanente()); assertEquals(permanente + equipo + 1, e.getNivel());
        assertEquals(0, j.getCantidadPergaminosHabilidad(HabilidadDisponible.CAPARAZON));
        assertEquals(1, j.getCantidadPergaminosMaestria());
    }

    @ParameterizedTest @ValueSource(ints = {1, 8, 13})
    void maestriaOfreceTodoElCatalogoYAprendeLaEleccionSinEquipo(int elegida) {
        Jugador j = jugador(); dar(j);
        assertTrue(inventario(j, "6\n" + elegida + "\n1\n0\n"));
        var tipo = HabilidadDisponible.values()[elegida - 1];
        for (var h : HabilidadDisponible.values()) assertTrue(texto().contains(h.getNombre()));
        assertEquals(1, j.getHabilidades().size());
        assertEquals(1, j.getHabilidades().get(tipo.crearHabilidad()).getNivelPermanente());
        assertEquals(0, j.getCantidadPergaminosMaestria());
        assertEquals(1, j.getCantidadPergaminosHabilidad(HabilidadDisponible.CAPARAZON));
    }

    @ParameterizedTest @ValueSource(strings = {"5\n0\n0\n", "6\n0\n0\n", "5\n1\n0\n0\n0\n", "6\n1\n0\n0\n0\n"})
    void cancelarSeleccionOConfirmacionNoConsumeYRetrocedeUnNivel(String entrada) {
        Jugador j = jugador(); dar(j);
        assertTrue(inventario(j, entrada));
        assertEquals(1, j.getCantidadPergaminosHabilidad(HabilidadDisponible.CAPARAZON));
        assertEquals(1, j.getCantidadPergaminosMaestria()); assertTrue(j.getHabilidades().isEmpty());
        assertFalse(texto().contains("Pergamino consumido: 1"));
    }

    @ParameterizedTest @ValueSource(strings = {"5\n", "6\n", "5\n1\n", "6\n1\n"})
    void finDeEntradaEnSeleccionOConfirmacionNoModifica(String entrada) {
        Jugador j = jugador(); dar(j); assertFalse(inventario(j, entrada));
        assertEquals(1, j.getCantidadPergaminosHabilidad(HabilidadDisponible.CAPARAZON));
        assertEquals(1, j.getCantidadPergaminosMaestria()); assertTrue(j.getHabilidades().isEmpty());
    }

    @ParameterizedTest @ValueSource(ints = {5, 6})
    void validaOpcionesSinConsumirHastaConfirmarYConsumeSoloUna(int opcion) {
        Jugador j = jugador(); dar(j); dar(j);
        assertTrue(inventario(j, opcion + "\nx\n-1\n99\n1\nx\n2\n1\n0\n"));
        assertEquals(5, texto().split("Entrada inválida", -1).length - 1);
        assertEquals(1, j.getHabilidades().get(HabilidadDisponible.CAPARAZON.crearHabilidad()).getNivelPermanente());
        assertEquals(opcion == 5 ? 1 : 2, j.getCantidadPergaminosHabilidad(HabilidadDisponible.CAPARAZON));
        assertEquals(opcion == 6 ? 1 : 2, j.getCantidadPergaminosMaestria());
        assertEquals(6, j.getFuerza()); assertEquals(20, j.getVida()); assertEquals(30, j.getVidaMaxima());
    }

    @Test void menuVacioConservaOpcionesPreviasYRegresaSinBucles() {
        Jugador j = jugador();
        assertTrue(inventario(j, "5\n1\n0\n6\n1\n0\n0\n"));
        assertTrue(texto().contains("No tienes pergaminos disponibles."));
        String original = bytes.toString(StandardCharsets.UTF_8);
        assertTrue(original.contains("[4] Utilizar pergaminos"));
        assertTrue(original.contains("[5] Utilizar pergamino de habilidad"));
        assertTrue(original.contains("[6] Utilizar pergamino de maestría"));
        assertTrue(j.getHabilidades().isEmpty());
    }

    @Test void consultaRepetidaNoCambiaContenidoYConservaLosTresTipos() {
        Jugador j = jugador(); dar(j); j.agregarPergamino(new Pergamino(Atributo.VIDA));
        var esperado = Map.copyOf(j.getPergaminosHabilidad());
        assertTrue(inventario(j, "5\n0\n6\n0\n0\n"));
        assertTrue(inventario(j, "0\n"));
        assertEquals(esperado, j.getPergaminosHabilidad());
        assertEquals(1, j.getCantidadPergaminos(Atributo.VIDA)); assertEquals(1, j.getCantidadPergaminosMaestria());
        assertTrue(texto().contains("Pergamino de habilidad: Caparazón x1"));
        assertTrue(texto().contains("Pergamino de maestría x1"));
        assertTrue(texto().contains("Pergamino de vida máxima x1"));
    }

    @Test void nivelMaximoMuestraFalloSinPerderElPergaminoNiElEstado() {
        Jugador j = jugador(); dar(j); var h = HabilidadDisponible.CAPARAZON.crearHabilidad();
        j.aprenderHabilidad(h, Integer.MAX_VALUE);
        assertTrue(inventario(j, "5\n1\n1\n0\n0\n"));
        assertTrue(texto().contains("Conservas el pergamino y el estado anterior"));
        assertEquals(Integer.MAX_VALUE, j.getHabilidades().get(h).getNivelPermanente());
        assertEquals(1, j.getCantidadPergaminosHabilidad(HabilidadDisponible.CAPARAZON));
    }
}
