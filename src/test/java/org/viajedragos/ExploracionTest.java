package org.viajedragos;

import org.junit.jupiter.api.Test;
import org.viajedragos.catalogo.RepositorioEnemigos;
import org.viajedragos.modelo.DefinicionEnemigo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.arma.Arma;
import org.viajedragos.arma.Atributo;
import org.viajedragos.arma.TipoEquipo;
import org.viajedragos.combate.TiradorDado;
import org.viajedragos.habilidad.implementacion.InstintoSupervivencia;
import org.viajedragos.modelo.Jugador;
import org.viajedragos.modelo.TipoPocion;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class ExploracionTest {
    private RepositorioEnemigos soloLobo() {
        return () -> java.util.List.of(new DefinicionEnemigo("lobo", "Lobo", "", 5, 2, 2, 1, 2, 18));
    }

    private final ByteArrayOutputStream salida = new ByteArrayOutputStream();

    private Jugador jugador(int vida) {
        return new Jugador("Aventurero", "", 6, 2, 3, 2, 2, vida, 24);
    }

    private PrintStream consola() {
        return new PrintStream(salida, true, StandardCharsets.UTF_8);
    }

    private String texto() {
        return org.viajedragos.TextoConsola.contenido(salida.toString(StandardCharsets.UTF_8));
    }

    @Test
    void inventarioVacioNoMuestraObjetosInexistentesNiModificaVida() {
        Jugador j = jugador(13);
        assertTrue(app.consultarInventario(j, new Scanner("0\n"), consola()));
        assertTrue(texto().contains("INVENTARIO"));
        assertTrue(texto().contains("Sin consumibles"));
        assertTrue(texto().contains("Sin equipamiento"));
        assertFalse(texto().contains("Poción"));
        assertEquals(13, j.getVida());
        assertTrue(j.getInventarioPociones().isEmpty());
        assertTrue(j.getEquipoEquipado().isEmpty());
    }

    @Test
    void inventarioMuestraAmbasCantidadesYEquipoRealSinAplicarEfectosDeNuevo() {
        Jugador j = jugador(13);
        j.agregarPocion(TipoPocion.PEQUENA);
        j.agregarPocion(TipoPocion.PEQUENA);
        j.agregarPocion(TipoPocion.GRANDE);
        Arma arma = new Arma("Espada desgastada", TipoEquipo.ARMA,
                Map.of(Atributo.FUERZA, 1, Atributo.AGILIDAD, -1),
                Map.of(new InstintoSupervivencia(), 2));
        assertTrue(j.equipar(arma));
        var inventario = Map.copyOf(j.getInventarioPociones());
        var estados = Map.copyOf(j.getHabilidades());
        var modificadores = Map.copyOf(j.getModificadoresEquipo());
        assertTrue(app.consultarInventario(j, new Scanner("8\n1\n0\n0\n0\n"), consola()));
        assertTrue(app.consultarInventario(j, new Scanner("0\n"), consola()));
        assertTrue(texto().matches("(?s).*Poción pequeña +x2.*Poción grande +x1.*"));
        assertEquals(8, j.getDefinicionPocion(TipoPocion.PEQUENA).recuperacionPS());
        assertEquals(16, j.getDefinicionPocion(TipoPocion.GRANDE).recuperacionPS());
        assertTrue(texto().contains("Arma: Espada desgastada"));
        assertTrue(texto().contains("FUERZA +1"));
        assertTrue(texto().contains("AGILIDAD -1"));
        assertTrue(texto().contains("Instinto de supervivencia: nivel otorgado 2"));
        assertFalse(texto().contains("Armadura: Sin equipo"));
        assertEquals(inventario, j.getInventarioPociones());
        assertEquals(modificadores, j.getModificadoresEquipo());
        assertEquals(estados, j.getHabilidades());
        estados.forEach((h, estado) -> assertSame(estado, j.getHabilidades().get(h)));
        assertEquals(7, j.getFuerza());
        assertEquals(13, j.getVida());
    }

    @ParameterizedTest
    @EnumSource(TipoPocion.class)
    void consultaSoloMuestraLosConsumiblesQueSePoseen(TipoPocion tipo) {
        Jugador j = jugador(13);
        j.agregarPocion(tipo);
        assertTrue(app.consultarInventario(j, new Scanner("0\n"), consola()));
        assertTrue(texto().contains(tipo.getNombre()));
        TipoPocion otra = tipo == TipoPocion.PEQUENA ? TipoPocion.GRANDE : TipoPocion.PEQUENA;
        assertFalse(texto().contains(otra.getNombre()));
    }

    @ParameterizedTest
    @CsvSource({"PEQUENA,1,13,8,21", "GRANDE,2,3,16,19", "PEQUENA,1,22,2,24", "GRANDE,2,22,2,24"})
    void consumoMuestraRecuperacionRealYCantidadRestanteYDevuelveElControl(
            TipoPocion tipo, String opcion, int inicial, int recuperada, int finalVida) {
        Jugador j = jugador(inicial);
        j.agregarPocion(tipo);
        j.agregarPocion(tipo);
        Scanner entrada = new Scanner(opcion + "\nsiguiente\n");
        assertTrue(app.usarObjeto(j, entrada, consola()));
        assertEquals("siguiente", entrada.nextLine(), "No pide otra acción tras el consumo");
        assertTrue(texto().contains("Has utilizado una " + tipo.getNombre()));
        assertTrue(texto().contains("Vida recuperada: +" + recuperada + " PS"));
        assertTrue(texto().contains("Vida actual: " + finalVida + "/24 PS"));
        assertTrue(texto().contains("Unidades restantes de " + tipo.getNombre() + ": 1"));
        assertEquals(finalVida, j.getVida());
        assertEquals(1, j.getCantidadPociones(tipo));
    }

    @Test
    void opcionesDeUsoSonFijasAunqueNoHayaPociones() {
        Jugador j = jugador(13);
        assertTrue(app.usarObjeto(j, new Scanner("1\n2\n0\n"), consola()));
        assertTrue(texto().contains("1. Poción pequeña (x0) +8 PS"));
        assertTrue(texto().contains("2. Poción grande (x0) +16 PS"));
        assertEquals(2, texto().split("No tienes esa poción", -1).length - 1);
        assertFalse(texto().contains("Has utilizado"));
        assertEquals(13, j.getVida());
        assertTrue(j.getInventarioPociones().isEmpty());
    }

    @Test
    void vidaCompletaNoDescuentaNingunaPocion() {
        Jugador j = jugador(24);
        for (TipoPocion tipo : TipoPocion.values()) j.agregarPocion(tipo);
        var antes = Map.copyOf(j.getInventarioPociones());
        assertTrue(app.usarObjeto(j, new Scanner("1\n2\n0\n"), consola()));
        assertTrue(texto().contains("La vida está completa"));
        assertFalse(texto().contains("Has utilizado"));
        assertEquals(antes, j.getInventarioPociones());
        assertEquals(24, j.getVida());
    }

    @Test
    void cancelarYEntradasInvalidasNoCuranNiConsumen() {
        Jugador j = jugador(13);
        j.agregarPocion(TipoPocion.PEQUENA);
        assertTrue(app.usarObjeto(j, new Scanner("x\n\n3\n-1\n0\n"), consola()));
        assertEquals(4, texto().split("Entrada inválida", -1).length - 1);
        assertEquals(13, j.getVida());
        assertEquals(1, j.getCantidadPociones(TipoPocion.PEQUENA));
        assertFalse(texto().contains("Has utilizado"));
    }

    @Test
    void inventarioRechazaAccionesSinModificarEstado() {
        Jugador j = jugador(13);
        j.agregarPocion(TipoPocion.GRANDE);
        assertTrue(app.consultarInventario(j, new Scanner("9\nx\n0\n"), consola()));
        assertEquals(2, texto().split("Entrada inválida", -1).length - 1);
        assertEquals(13, j.getVida());
        assertEquals(1, j.getCantidadPociones(TipoPocion.GRANDE));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void cierreDeEntradaEnSubmenusNoConsume(boolean usar) {
        Jugador j = jugador(13);
        j.agregarPocion(TipoPocion.PEQUENA);
        boolean volver = usar ? app.usarObjeto(j, new Scanner(""), consola())
                : app.consultarInventario(j, new Scanner(""), consola());
        assertFalse(volver);
        assertTrue(texto().contains("Entrada finalizada. Hasta pronto."));
        assertEquals(13, j.getVida());
        assertEquals(1, j.getCantidadPociones(TipoPocion.PEQUENA));
    }

    @Test
    void aventuraConservaCuracionEquipoYPocionesSinTiradasExtraNiRepetirBotin() {
        Jugador j = jugador(24);
        int[] valores = {1, 1, 1, 6, 6, 1, 6, 6, 1, 6, 1, 1, 1, 1, 6, 1, 6};
        int[] usadas = {0};
        TiradorDado dados = () -> {
            assertTrue(usadas[0] < valores.length, "No se permiten dados extra");
            if (usadas[0] == 10) {
                assertEquals(14, j.getVida());
                assertEquals(7, j.getFuerza());
                assertEquals(0, j.getCantidadPociones(TipoPocion.PEQUENA));
                assertEquals(1, j.getCantidadPociones(TipoPocion.GRANDE));
            }
            return valores[usadas[0]++];
        };
        int[] botin = {0};
        Random azar = new Random(0) {
            @Override
            public int nextInt(int bound) {
                assertEquals(100, bound);
                assertTrue(botin[0]++ < 4, "El menú no vuelve a generar botín");
                return 0;
            }
        };
        String entrada = "1\n1\n1\nx\n0\n1\n3\n1\n0\n3\n1\n2\n1\n0\n0\n";
        assertFalse(assertTimeoutPreemptively(Duration.ofSeconds(2), () ->
                app.jugarAventura(j, new Scanner("3\n" + entrada), consola(), dados, azar, soloLobo(), new Random(0),
                org.viajedragos.catalogo.ConfiguracionRepositorios.objetos(),
                null,
                new org.viajedragos.consola.RitmoConsola(org.viajedragos.consola.RitmoConsola.Velocidad.INSTANTANEA),
                TextoConsola.sinBotinEquipamiento(),
                TextoConsola.sinBotinEquipamiento(),
                new org.viajedragos.consola.TiempoAventura(),
                TextoConsola.sinBotinEquipamiento())));
        assertEquals(valores.length, usadas[0]);
        assertEquals(4, botin[0]);
        assertEquals(14, j.getVida(), "El segundo encuentro no recupera vida automáticamente");
        assertEquals(1, j.getCantidadPociones(TipoPocion.PEQUENA));
        assertEquals(2, j.getCantidadPociones(TipoPocion.GRANDE));
        assertEquals(7, j.getFuerza());
        assertTrue(texto().contains("VIAJE A DRAGOS - EXPLORACIÓN"));
        assertTrue(texto().contains("3. Explorar"));
        assertTrue(texto().contains("4. Tienda"));
        assertTrue(texto().contains("Vida recuperada: +8 PS"));
        assertEquals(1, texto().split("Has encontrado una Espada desgastada", -1).length - 1);
        assertEquals(2, texto().split("Botín: Poción pequeña", -1).length - 1);
        assertTrue(texto().indexOf("Botín:") < texto().indexOf("Has encontrado"));
        assertTrue(texto().indexOf("Espada desgastada equipada") < texto().lastIndexOf("EXPLORACIÓN"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1\n", "2\n"})
    void cierreEnSubmenuFinalizaLaSesionSinOtroEncuentro(String opcion) {
        int[] valores = {1, 1, 1, 1, 6, 1, 6};
        int[] usadas = {0};
        assertFalse(assertTimeoutPreemptively(Duration.ofSeconds(2), () ->
                app.jugarAventura(jugador(24), new Scanner("3\n1\n2\n" + opcion), consola(), () -> {
                    assertTrue(usadas[0] < valores.length);
                    return valores[usadas[0]++];
                }, new Random(0), soloLobo(), new Random(0),
                org.viajedragos.catalogo.ConfiguracionRepositorios.objetos(),
                null,
                new org.viajedragos.consola.RitmoConsola(org.viajedragos.consola.RitmoConsola.Velocidad.INSTANTANEA),
                TextoConsola.sinBotinEquipamiento(),
                TextoConsola.sinBotinEquipamiento(),
                new org.viajedragos.consola.TiempoAventura(),
                TextoConsola.sinBotinEquipamiento())));
        assertEquals(valores.length, usadas[0]);
        assertEquals(1, texto().split("Comienza el combate", -1).length - 1);
        assertTrue(texto().endsWith("Entrada finalizada. Hasta pronto." + System.lineSeparator()));
    }

    @Test
    void pocionEnCombateConservaInventarioBotinYConsumoPosteriorEnExploracion() {
        Jugador j = jugador(24);
        int[] tiradas = {1, 1, 1, 6, 6, 1, 6, 6, 1, 6,
                1, 1, 1, 1, 4, 2, 1, 6, 6, 1, 6};
        int[] usadas = {0};
        int[] botin = {0};
        Random azar = new Random(0) {
            @Override
            public int nextInt(int bound) {
                assertEquals(100, bound);
                assertTrue(botin[0]++ < 4);
                return 0;
            }
        };
        assertFalse(assertTimeoutPreemptively(Duration.ofSeconds(2), () -> app.jugarAventura(j,
                new Scanner("3\n1\n1\n3\n2\n1\n1\n2\n1\n2\n0\n0\n1\n3\n2\n0\n0\n"), consola(), () -> {
                    assertTrue(usadas[0] < tiradas.length, "El consumo no tira dados de ataque");
                    if (usadas[0] == 14) {
                        assertEquals(14, j.getVida(), "Se curó al llegar su acción del segundo combate");
                        assertEquals(0, j.getCantidadPociones(TipoPocion.PEQUENA));
                        assertEquals(7, j.getFuerza());
                    }
                    return tiradas[usadas[0]++];
                }, azar, soloLobo(), new Random(0),
                org.viajedragos.catalogo.ConfiguracionRepositorios.objetos(),
                null,
                new org.viajedragos.consola.RitmoConsola(org.viajedragos.consola.RitmoConsola.Velocidad.INSTANTANEA),
                TextoConsola.sinBotinEquipamiento(),
                TextoConsola.sinBotinEquipamiento(),
                new org.viajedragos.consola.TiempoAventura(),
                TextoConsola.sinBotinEquipamiento())));
        assertEquals(tiradas.length, usadas[0]);
        assertEquals(4, botin[0]);
        assertEquals(24, j.getVida());
        assertEquals(1, j.getCantidadPociones(TipoPocion.PEQUENA));
        assertEquals(1, j.getCantidadPociones(TipoPocion.GRANDE));
        assertEquals(7, j.getFuerza());
        assertEquals(1, texto().split("Has encontrado una Espada desgastada", -1).length - 1);
        assertEquals(1, texto().split("Tu acción ha terminado", -1).length - 1);
        assertTrue(texto().contains("Vida final: Aventurero 10/24"));
        assertTrue(texto().contains("Vida recuperada: +14 PS"));
        String inventario = texto().substring(texto().indexOf("INVENTARIO"));
        assertTrue(inventario.matches("(?s).*Poción pequeña +x1.*Poción grande +x2.*"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2\n", "2\nx\n", "2\n0\n"})
    void appTrataCierreEnMenuDeCombateComoInterrupcionSinBotin(String entrada) {
        Jugador j = jugador(20);
        j.agregarPocion(TipoPocion.PEQUENA);
        int[] tiradas = {1, 1, 1, 1};
        int[] usadas = {0};
        Random azar = new Random(0) {
            @Override
            public int nextInt(int bound) {
                fail("No hay botín en un combate interrumpido");
                return 0;
            }
        };
        assertFalse(assertTimeoutPreemptively(Duration.ofSeconds(2), () -> app.jugarAventura(j,
                new Scanner("3\n" + entrada), consola(), () -> {
                    assertTrue(usadas[0] < tiradas.length);
                    return tiradas[usadas[0]++];
                }, azar, soloLobo(), new Random(0),
                org.viajedragos.catalogo.ConfiguracionRepositorios.objetos(),
                null,
                new org.viajedragos.consola.RitmoConsola(org.viajedragos.consola.RitmoConsola.Velocidad.INSTANTANEA),
                TextoConsola.sinBotinEquipamiento(),
                TextoConsola.sinBotinEquipamiento(),
                new org.viajedragos.consola.TiempoAventura(),
                TextoConsola.sinBotinEquipamiento())));
        assertEquals(4, usadas[0]);
        assertEquals(20, j.getVida());
        assertEquals(1, j.getCantidadPociones(TipoPocion.PEQUENA));
        assertTrue(texto().contains("el combate queda sin terminar"));
        assertFalse(texto().contains("Victoria."));
        assertFalse(texto().contains("Derrota."));
        assertEquals(1, texto().split("EXPLORACIÓN", -1).length - 1);
        assertFalse(texto().contains("Has encontrado"));
    }
}
