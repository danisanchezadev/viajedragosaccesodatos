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
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EstadoPersonajeTest {
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private PrintStream salida() { return new PrintStream(bytes, true, StandardCharsets.UTF_8); }
    private String texto() { return TextoConsola.contenido(bytes.toString(StandardCharsets.UTF_8)).replaceAll("\\s+", " "); }
    private Jugador jugador() { return new Jugador("María del Río", "Exploradora", 6, 2, 3, 2, 2, 20, 48); }
    private Equipo pieza(String nombre, TipoEquipo tipo, Map<Atributo, Integer> bonos, Map<Habilidad, Integer> habilidades) {
        return new Equipo(null, nombre, tipo, bonos, habilidades, 3);
    }
    private void mostrar(Jugador j) {
        bytes.reset(); assertTrue(app.consultarEstado(j, new Scanner("0\n"), salida()));
        bytes.toString(StandardCharsets.UTF_8).lines().filter(l -> l.startsWith("│") || l.startsWith("┌") || l.startsWith("├") || l.startsWith("└"))
                .forEach(l -> assertEquals(50, PanelConsola.anchoVisible(l), l));
    }
    private void atributo(String nombre, int base, int equipo, int total) {
        assertTrue(texto().contains(nombre + " Base: " + base + " | Equipo: "
                + String.format(Locale.ROOT, "%+d", equipo) + " | Total: " + total), texto());
    }

    @Test void estadoBasicoMuestraTodosLosDatosYSeisAtributosSinConfundirVida() {
        Jugador j = jugador(); mostrar(j);
        assertTrue(texto().contains("Nombre: María del Río")); assertTrue(texto().contains("Descripción: Exploradora"));
        assertTrue(texto().contains("Nivel: 1 | EXP: 0/20")); assertTrue(texto().contains("Puntos pendientes: 0"));
        assertTrue(texto().contains("Vida actual/máxima: 20/48 PS")); assertTrue(texto().contains("Oro: 15 monedas"));
        atributo("FUERZA", 6, 0, 6); atributo("RESISTENCIA", 2, 0, 2); atributo("INICIATIVA", 3, 0, 3);
        atributo("AGILIDAD", 2, 0, 2); atributo("VELOCIDAD", 2, 0, 2); atributo("VIDA MÁXIMA", 48, 0, 48);
        assertTrue(texto().contains("No tienes habilidades aprendidas."));
        assertTrue(texto().contains("No tienes habilidades exclusivas de equipo."));
        assertTrue(bytes.toString(StandardCharsets.UTF_8).contains("[0] Volver a exploración"));
        assertFalse(bytes.toString(StandardCharsets.UTF_8).contains("\u001b"));
    }

    @Test void experienciaRepartoBonosAcumuladosYPergaminosUsanLasFuentesDelModelo() {
        Jugador j = jugador(); j.agregarExperiencia(25);
        assertTrue(j.asignarPunto(Atributo.FUERZA)); assertTrue(j.asignarPunto(Atributo.FUERZA));
        var arma = pieza("Espada", TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 2), Map.of());
        j.equipar(arma);
        j.equipar(pieza("Guantes", TipoEquipo.GUANTES, Map.of(Atributo.FUERZA, 1), Map.of()));
        j.equipar(pieza("Anillo", TipoEquipo.ANILLO, Map.of(Atributo.INICIATIVA, 1), Map.of()));
        j.equipar(pieza("Botas", TipoEquipo.BOTAS, Map.of(Atributo.AGILIDAD, -1), Map.of()));
        j.equipar(pieza("Casco", TipoEquipo.CASCO, Map.of(Atributo.RESISTENCIA, -5), Map.of()));
        j.equipar(pieza("Armadura", TipoEquipo.ARMADURA, Map.of(Atributo.VIDA, 10), Map.of()));
        j.equipar(pieza("Amuleto", TipoEquipo.AMULETO, Map.of(Atributo.VIDA, -2), Map.of()));
        j.agregarPergamino(new Pergamino(Atributo.FUERZA)); assertTrue(j.usarPergamino(Atributo.FUERZA, arma));
        mostrar(j);
        atributo("FUERZA", 9, 4, 13); atributo("RESISTENCIA", 3, -5, 0); atributo("INICIATIVA", 4, 1, 5);
        atributo("AGILIDAD", 3, -1, 2); atributo("VELOCIDAD", 3, 0, 3); atributo("VIDA MÁXIMA", 54, 8, 62);
        assertTrue(texto().contains("Nivel: 2 | EXP: 5/30"));
        assertTrue(texto().contains("Vida actual/máxima: 26/62 PS"));
        assertTrue(texto().contains("Total limitado al mínimo de cero."));
    }

    @Test void muestraLosPuntosPendientesSinAsignarlos() {
        Jugador j = jugador(); j.agregarExperiencia(20); mostrar(j);
        assertTrue(texto().contains("Puntos pendientes: 2")); assertEquals(2, j.getPuntosPendientes());
        atributo("FUERZA", 7, 0, 7);
    }

    @Test void actualizaDespuesDeEquiparSustituirYDesequiparSinPerderLaBase() {
        Jugador j = jugador(); j.aprenderHabilidad(new Caparazon(), 2);
        var anterior = pieza("Anterior", TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 1), Map.of(new Caparazon(), 2));
        var nueva = pieza("Nueva", TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 4), Map.of(new Caparazon(), 3));
        j.agregarEquipo(anterior); j.agregarEquipo(nueva);
        assertTrue(j.equiparDesdeInventario(TipoEquipo.ARMA, 0)); mostrar(j);
        atributo("FUERZA", 6, 1, 7); assertTrue(texto().contains("Permanente: 2 | Equipo: +2 | Total: 4"));
        assertTrue(j.equiparDesdeInventario(TipoEquipo.ARMA, 0)); mostrar(j);
        atributo("FUERZA", 6, 4, 10); assertTrue(texto().contains("Permanente: 2 | Equipo: +3 | Total: 5"));
        assertTrue(j.desequiparAlInventario(TipoEquipo.ARMA)); mostrar(j);
        atributo("FUERZA", 6, 0, 6); assertTrue(texto().contains("Permanente: 2 | Equipo: +0 | Total: 2"));
        var e = j.getHabilidades().get(new Caparazon());
        assertTrue(e.isAprendida()); assertEquals(2, e.getNivelPermanente()); assertEquals(0, e.getNivelEquipo());
        assertEquals(2, j.getInventarioEquipo().size());
    }

    @Test void separaAprendidasDeEquipoYNoDuplicaHabilidadConVariasFuentes() {
        Jugador j = jugador(); var caparazon = new Caparazon(); var instinto = new InstintoSupervivencia();
        j.aprenderHabilidad(caparazon, 2);
        j.equipar(pieza("Espada", TipoEquipo.ARMA, Map.of(), Map.of(caparazon, 2, instinto, 1)));
        j.equipar(pieza("Anillo", TipoEquipo.ANILLO, Map.of(), Map.of(caparazon, 2, instinto, 3)));
        mostrar(j);
        assertEquals(1, texto().split("Caparazón", -1).length - 1);
        assertTrue(texto().contains("Caparazón Permanente: 2 | Equipo: +4 | Total: 6"));
        assertEquals(1, texto().split("Instinto de supervivencia", -1).length - 1);
        assertTrue(texto().contains("Nivel de equipo: 4"));
        assertTrue(texto().contains("Fuente: Espada (+1)")); assertTrue(texto().contains("Fuente: Anillo (+3)"));
        assertTrue(texto().indexOf("Caparazón") < texto().indexOf("HABILIDADES OTORGADAS POR EQUIPO"));
        assertTrue(j.desequiparAlInventario(TipoEquipo.ARMA)); assertTrue(j.desequiparAlInventario(TipoEquipo.ANILLO));
        mostrar(j); assertFalse(texto().contains("Instinto de supervivencia"));
        assertTrue(texto().contains("Permanente: 2 | Equipo: +0 | Total: 2"));
    }

    @Test void habilidadAprendidaPorPergaminoSeMuestraConElNivelPermanenteReal() {
        Jugador j = jugador(); j.equipar(pieza("Espada", TipoEquipo.ARMA, Map.of(), Map.of(new Caparazon(), 2)));
        j.agregarPergaminoMaestria(); assertTrue(j.usarPergaminoMaestria(HabilidadDisponible.CAPARAZON));
        mostrar(j); assertTrue(texto().contains("Permanente: 1 | Equipo: +2 | Total: 3"));
        assertTrue(j.desequiparAlInventario(TipoEquipo.ARMA)); mostrar(j);
        assertTrue(texto().contains("Permanente: 1 | Equipo: +0 | Total: 1"));
    }

    @Test void consultarNoDisparaCuracionNiLimpiezaNiCambiaInstanciasORecursos() {
        Jugador j = jugador(); j.aprenderHabilidad(new InstintoSupervivencia(), 2);
        j.setVida(1); j.curarse(8); j.agregarExperiencia(1); j.agregarPocion(TipoPocion.PEQUENA);
        j.agregarPergamino(new Pergamino(Atributo.VIDA)); j.agregarPergaminoMaestria();
        var arma = pieza("Espada", TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 2), Map.of(new InstintoSupervivencia(), 3));
        j.equipar(arma); j.agregarEquipo(pieza("Guardada", TipoEquipo.CASCO, Map.of(), Map.of()));
        var atributos = new EnumMap<Atributo, Integer>(Atributo.class);
        for (Atributo a : Atributo.values()) if (a != Atributo.VIDA) atributos.put(a, j.getAtributoBase(a));
        var mods = Map.copyOf(j.getModificadoresEquipo()); var ranuras = Map.copyOf(j.getEquipoEquipado());
        var guardadas = List.copyOf(j.getInventarioEquipo()); var estado = j.getHabilidades().get(new InstintoSupervivencia());
        for (int i = 0; i < 3; i++) mostrar(j);
        atributos.forEach((a, valor) -> assertEquals(valor, j.getAtributoBase(a)));
        assertEquals(mods, j.getModificadoresEquipo()); assertEquals(ranuras, j.getEquipoEquipado());
        assertEquals(guardadas, j.getInventarioEquipo()); assertSame(estado, j.getHabilidades().get(new InstintoSupervivencia()));
        assertTrue(estado.isEfectoTemporalActivo()); assertEquals(2, estado.getNivelPermanente());
        assertEquals(3, estado.getNivelEquipo()); assertEquals(5, estado.getNivel());
        assertEquals(9, j.getVida()); assertEquals(48, j.getVidaMaxima()); assertEquals(15, j.getOro());
        assertEquals(1, j.getExperiencia()); assertEquals(1, j.getCantidadPociones(TipoPocion.PEQUENA));
        assertEquals(1, j.getCantidadPergaminos(Atributo.VIDA)); assertEquals(1, j.getCantidadPergaminosMaestria());
        assertEquals(3, arma.getMejorasDisponibles()); assertTrue(texto().contains("Efectos temporales activos"));
    }

    @ParameterizedTest @ValueSource(strings = {"", "x\n", "1\n"})
    void eofSaleSinModificarAunqueHayaEntradasInvalidas(String entrada) {
        Jugador j = jugador(); assertFalse(app.consultarEstado(j, new Scanner(entrada), salida()));
        assertEquals(20, j.getVida()); assertEquals(15, j.getOro()); assertEquals(6, j.getFuerza());
        assertTrue(texto().contains("Entrada finalizada"));
    }

    @Test void entradasInvalidasPermitenVolverSinConsumirLaSiguienteOpcion() {
        var entrada = new Scanner("x\n1\n-1\n0\npendiente\n");
        assertTrue(app.consultarEstado(jugador(), entrada, salida())); assertEquals("pendiente", entrada.nextLine());
        assertEquals(3, texto().split("Entrada inválida", -1).length - 1);
    }

    @Test void nombresLargosDescripcionAusenteYNivelesGrandesNoDesbordanElPanel() {
        Jugador j = jugador(); j.setNombre("a".repeat(30)); j.setDescripcion("");
        j.aprenderHabilidad(new Caparazon(), Integer.MAX_VALUE);
        mostrar(j); assertFalse(texto().contains("Descripción:"));
        assertTrue(texto().contains("Permanente: 2147483647"));
    }
}
