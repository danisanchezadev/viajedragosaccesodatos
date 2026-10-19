package org.viajedragos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.arma.*;
import org.viajedragos.consola.*;
import org.viajedragos.habilidad.*;
import org.viajedragos.habilidad.implementacion.*;
import org.viajedragos.modelo.Jugador;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class HabilidadesMenuPersonajeTest {
    private Jugador jugador() { return new Jugador("Arthos", "", 6, 2, 3, 2, 2, 24, 48); }

    private String mostrar(Jugador j, boolean ansi, int columnas) {
        String anterior = System.getProperty("viajedragos.columnas");
        var bytes = new ByteArrayOutputStream();
        try {
            System.setProperty("viajedragos.columnas", String.valueOf(columnas));
            VistaMenuPersonaje.general(j, new PrintStream(bytes, true, StandardCharsets.UTF_8), new TiempoAventura(() -> 0), ansi);
        } finally {
            if (anterior == null) System.clearProperty("viajedragos.columnas");
            else System.setProperty("viajedragos.columnas", anterior);
        }
        String salida = bytes.toString(StandardCharsets.UTF_8);
        TextoConsola.comprobarPaneles(salida);
        assertEquals(ansi, salida.contains("\u001b["));
        if (ansi) salida.lines().filter(l -> !l.isBlank()).forEach(l -> assertTrue(l.endsWith("\u001b[0m")));
        return salida.replaceAll("\u001b\\[[0-9;]*m", "");
    }

    private String seccion(String salida) {
        int titulo = salida.indexOf("HABILIDADES");
        int inicio = salida.indexOf('\n', titulo) + 1;
        return salida.substring(inicio, salida.indexOf("├", inicio));
    }

    private String texto(String salida) { return TextoConsola.contenido(salida).replaceAll("\\s+", " "); }

    private Equipo pieza(TipoEquipo tipo, Map<Habilidad, Integer> habilidades) {
        return new Equipo(null, tipo.getNombre(), tipo, Map.of(Atributo.FUERZA, 1), habilidades, 0);
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void vacioEntreAtributosEInventarioConservaLasTresColumnas(boolean ansi) {
        String salida = mostrar(jugador(), ansi, 102);
        assertTrue(seccion(salida).contains("Sin habilidades"));
        assertTrue(salida.indexOf("ATRIBUTOS") < salida.indexOf("HABILIDADES"));
        assertTrue(salida.indexOf("HABILIDADES") < salida.indexOf("CONSUMIBLES"));
        String fila = salida.lines().filter(l -> l.contains("CONSUMIBLES")).findFirst().orElseThrow();
        assertEquals("CONSUMIBLES", fila.substring(3, 33).strip());
        assertEquals("PERGAMINOS", fila.substring(35, 65).strip());
        assertEquals("EQUIPAMIENTO", fila.substring(67, 99).strip());
        assertTrue(salida.contains("[6] Habilidades"));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void tresProcedenciasNivelEfectivoYUnaSolaEntrada(boolean ansi) {
        Jugador j = jugador();
        j.aprenderHabilidad(new Caparazon(), 2); j.aprenderHabilidad(new Rabia(), 3);
        j.equipar(pieza(TipoEquipo.ARMA, Map.of(new Caparazon(), 3, new Impetu(), 2)));
        String resumen = texto(seccion(mostrar(j, ansi, 102)));
        assertTrue(resumen.contains("[A+E] Caparazón Nv.5"));
        assertTrue(resumen.contains("[A] Rabia Nv.3"));
        assertTrue(resumen.contains("[E] Ímpetu Nv.2"));
        assertEquals(1, resumen.split("Caparazón", -1).length - 1);
        assertEquals(2, j.getHabilidades().get(new Caparazon()).getNivelPermanente());
        assertEquals(3, j.getHabilidades().get(new Caparazon()).getNivelEquipo());
    }

    @Test void equiparVariasFuentesYRetirarlasRefrescaOrigenYNivelSinDuplicados() {
        Jugador j = jugador(); var h = new Caparazon(); j.aprenderHabilidad(h, 1);
        assertTrue(seccion(mostrar(j, false, 102)).contains("[A] Caparazón Nv.1"));
        j.equipar(pieza(TipoEquipo.ARMA, Map.of(h, 2, new Rabia(), 1)));
        j.equipar(pieza(TipoEquipo.CASCO, Map.of(new Caparazon(), 3)));
        String resumen = seccion(mostrar(j, false, 102));
        assertTrue(resumen.contains("[A+E] Caparazón Nv.6"));
        assertEquals(1, resumen.split("Caparazón", -1).length - 1);
        assertTrue(resumen.contains("[E] Rabia Nv.1"));
        assertTrue(j.desequipar(TipoEquipo.ARMA));
        resumen = seccion(mostrar(j, false, 102));
        assertFalse(resumen.contains("Rabia")); assertTrue(resumen.contains("[A+E] Caparazón Nv.4"));
        assertTrue(j.desequipar(TipoEquipo.CASCO));
        assertTrue(seccion(mostrar(j, false, 102)).contains("[A] Caparazón Nv.1"));
    }

    @Test void ultimaFuenteExclusivaDesapareceYNoSeMuestranEntradasInactivas() {
        Jugador j = jugador(); j.equipar(pieza(TipoEquipo.ARMA, Map.of(new Rabia(), 2)));
        assertTrue(seccion(mostrar(j, false, 102)).contains("[E] Rabia Nv.2"));
        assertTrue(j.desequipar(TipoEquipo.ARMA));
        j.getHabilidades().put(new Caparazon(), new EstadoHabilidad());
        assertTrue(seccion(mostrar(j, false, 102)).contains("Sin habilidades"));
        assertFalse(seccion(mostrar(j, false, 102)).contains("Caparazón"));
    }

    @Test void seisNombresCortosEnTresColumnasYDosFilas() {
        Jugador j = jugador();
        for (Habilidad h : List.of(new Caparazon(), new Rabia(), new Impetu(), new GolpeCertero(), new GuardiaFerrea(), new FuriaContenida()))
            j.aprenderHabilidad(h, 1);
        String resumen = seccion(mostrar(j, false, 102));
        var filas = resumen.lines().filter(l -> l.contains("Nv.")).toList();
        assertEquals(2, filas.size());
        for (String fila : filas) {
            assertEquals(3, fila.split("Nv.1", -1).length - 1);
            assertTrue(fila.substring(3, 33).startsWith("[A]"));
            assertTrue(fila.substring(35, 65).startsWith("[A]"));
            assertTrue(fila.substring(67, 99).startsWith("[A]"));
        }
        assertFalse(resumen.contains("habilidades más"));
    }

    @Test void masDeSeisConservaAccesoATodasEnLaVistaDetallada() {
        Jugador j = jugador();
        for (var h : HabilidadDisponible.values()) j.aprenderHabilidad(h.crearHabilidad(), 2);
        var nombres = j.getHabilidades().keySet().stream().map(Habilidad::getNombre).sorted().toList();
        String resumen = seccion(mostrar(j, false, 102));
        assertEquals(6, resumen.split("Nv.2", -1).length - 1);
        assertTrue(resumen.contains("+7 habilidades más"));
        for (int i = 0; i < nombres.size(); i++) assertEquals(i < 6, resumen.contains(nombres.get(i)));
        var bytes = new ByteArrayOutputStream();
        VistaMenuPersonaje.habilidades(j, new PrintStream(bytes, true, StandardCharsets.UTF_8), false);
        String detalle = texto(bytes.toString(StandardCharsets.UTF_8));
        for (String nombre : nombres) assertTrue(detalle.contains(nombre));
        assertTrue(detalle.contains("0. Volver al menú de personaje"));
    }

    @ParameterizedTest @ValueSource(ints = {80, 102})
    void nombreLargoYNivelGrandeSeConservanCompletosSinDesbordar(int columnas) {
        Jugador j = jugador();
        String nombre = "Nombre de habilidad de prueba muy largo para comprobar el ajuste completo del texto sin abreviar ni perder palabras";
        Habilidad h = new Habilidad(nombre) { };
        j.aprenderHabilidad(h, Integer.MAX_VALUE);
        String resumen = texto(seccion(mostrar(j, false, columnas)));
        assertTrue(resumen.contains("[A] " + nombre + " Nv.2147483647"));
        assertFalse(resumen.contains("…"));
    }

    @Test void nombreRealLargoUtilizaDosColumnasSinTruncarse() {
        Jugador j = jugador(); j.aprenderHabilidad(new InstintoSupervivencia(), 3); j.aprenderHabilidad(new Rabia(), 1);
        String resumen = seccion(mostrar(j, false, 102));
        assertTrue(resumen.lines().anyMatch(l -> l.contains("[A] Instinto de supervivencia Nv.3") && l.contains("[A] Rabia Nv.1")));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void consultasNoModificanAtributosInstanciasNivelesNiEstadoTemporal(boolean ansi) {
        Jugador j = new Jugador("Arthos", "", 6, 2, 3, 2, 2, 1, 100);
        var h = new InstintoSupervivencia(); j.aprenderHabilidad(h, 2);
        var pieza = pieza(TipoEquipo.ARMA, Map.of(h, 1)); j.equipar(pieza); j.curarse(8);
        EstadoHabilidad estado = j.getHabilidades().get(h); var copia = new EstadoHabilidad(estado);
        var atributos = Arrays.stream(Atributo.values()).map(a -> a == Atributo.VIDA ? j.getVidaMaxima() : j.getAtributo(a)).toList();
        int vida = j.getVida(), oro = j.getOro();
        for (int i = 0; i < 3; i++) assertTrue(seccion(mostrar(j, ansi, 102)).contains("[A+E] Instinto de supervivencia Nv.3"));
        assertSame(estado, j.getHabilidades().get(h)); assertEquals(1, j.getHabilidades().size());
        assertEquals(copia.getNivel(), estado.getNivel()); assertEquals(copia.getNivelPermanente(), estado.getNivelPermanente());
        assertEquals(copia.getVecesEquipada(), estado.getVecesEquipada());
        assertEquals(copia.isEfectoTemporalActivo(), estado.isEfectoTemporalActivo());
        assertEquals(copia.getAcumulacionesTemporales(), estado.getAcumulacionesTemporales());
        assertEquals(atributos, Arrays.stream(Atributo.values()).map(a -> a == Atributo.VIDA ? j.getVidaMaxima() : j.getAtributo(a)).toList());
        assertEquals(vida, j.getVida()); assertEquals(oro, j.getOro());
        assertSame(pieza, j.getEquipoEquipado().get(TipoEquipo.ARMA)); assertTrue(j.getInventarioEquipo().isEmpty());
    }
}
