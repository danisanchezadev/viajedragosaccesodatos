package org.viajedragos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.arma.*;
import org.viajedragos.catalogo.*;
import org.viajedragos.consola.*;
import org.viajedragos.habilidad.*;
import org.viajedragos.modelo.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class BotinPergaminosTest {
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private PrintStream salida() { return new PrintStream(bytes, true, StandardCharsets.UTF_8); }
    private Jugador jugador() { return new Jugador("Prueba", "", 20, 2, 3, 2, 2, 48, 48); }
    private RepositorioEnemigos enemigos() {
        return () -> List.of(new DefinicionEnemigo("prueba", "Prueba", "", 1, 0, 1, 0, 0, 1));
    }
    private RitmoConsola instantaneo() { return new RitmoConsola(RitmoConsola.Velocidad.INSTANTANEA); }

    @Test
    void enumeracionExactaDaDiezPorCientoYLosSeisAtributosEquiprobables() {
        int[] cantidades = new int[6]; int exitos = 0;
        try (var silenciar = new PrintStream(OutputStream.nullOutputStream())) {
            for (int tirada = 0; tirada < 100; tirada++) {
                for (int atributo = 0; atributo < 6; atributo++) {
                    final int valor = tirada, elegido = atributo; int[] usos = {0};
                    Random azar = new Random(0) {
                        public int nextInt(int limite) {
                            assertEquals(usos[0] == 0 ? 100 : 6, limite);
                            return usos[0]++ == 0 ? valor : elegido;
                        }
                    };
                    Jugador j = jugador();
                    app.obtenerPergamino(j, azar, silenciar, instantaneo());
                    assertEquals(tirada < 10 ? 2 : 1, usos[0]);
                    if (tirada < 10) {
                        exitos++;
                        cantidades[atributo] += j.getCantidadPergaminos(Atributo.values()[atributo]);
                        assertEquals(1, j.getPergaminos().values().stream().mapToInt(Integer::intValue).sum());
                    } else assertTrue(j.getPergaminos().isEmpty());
                    assertTrue(j.getInventarioPociones().isEmpty()); assertEquals(15, j.getOro());
                }
            }
        }
        assertEquals(60, exitos); // 60 / 600 = 10 %.
        assertArrayEquals(new int[]{10, 10, 10, 10, 10, 10}, cantidades);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 9, 10, 99})
    void limitesMensajeYUnaPausaSoloSiSeObtiene(int tirada) {
        int[] usos = {0}, esperas = {0};
        Random azar = new Random(0) { public int nextInt(int limite) { return usos[0]++ == 0 ? tirada : 5; } };
        Jugador j = jugador();
        app.obtenerPergamino(j, azar, salida(), new RitmoConsola(RitmoConsola.Velocidad.NORMAL, ms -> esperas[0]++));
        assertEquals(tirada < 10 ? 1 : 0, j.getCantidadPergaminos(Atributo.VIDA));
        assertEquals(tirada < 10 ? 1 : 0, esperas[0]);
        assertEquals(tirada < 10, bytes.toString(StandardCharsets.UTF_8).contains("Pergamino de vida máxima"));
        assertEquals(tirada < 10 ? 2 : 1, usos[0]);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void independienteDeLosCuatroResultadosDePocionesYOtrasRecompensas(int resultado) {
        Jugador j = jugador(); int[] pociones = {0}, pergaminos = {0}, dados = {0};
        Random azarPociones = new Random(0) {
            public int nextInt(int limite) {
                assertEquals(100, limite); assertTrue(pociones[0] < 2);
                return pociones[0]++ == 0 ? ((resultado & 1) != 0 ? 0 : 99) : ((resultado & 2) != 0 ? 0 : 99);
            }
        };
        Random azarPergaminos = new Random(0) {
            public int nextInt(int limite) { assertEquals(pergaminos[0] == 0 ? 100 : 6, limite); return pergaminos[0]++ == 0 ? 0 : 1; }
        };
        assertFalse(app.jugarAventura(j, new Scanner("1\n2\n0\n"), salida(), () -> { assertTrue(++dados[0] <= 6); return 4; },
                azarPociones, enemigos(), new Random(0), new RepositorioObjetosMemoria(), null, instantaneo(), azarPergaminos));
        assertEquals(6, dados[0]); assertEquals(2, pociones[0]); assertEquals(2, pergaminos[0]);
        assertEquals(resultado & 1, j.getCantidadPociones(TipoPocion.PEQUENA));
        assertEquals((resultado & 2) == 0 ? 0 : 1, j.getCantidadPociones(TipoPocion.GRANDE));
        assertEquals(1, j.getCantidadPergaminos(Atributo.RESISTENCIA));
        assertEquals(20, j.getOro()); assertEquals(5, j.getExperiencia());
        assertTrue(bytes.toString(StandardCharsets.UTF_8).contains("Has encontrado una Espada desgastada"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"derrota", "simultanea", "huida", "incompleto"})
    void noConsultaAzarSinVictoria(String caso) {
        Jugador j = jugador();
        if (caso.equals("derrota") || caso.equals("simultanea")) j.aprenderHabilidad(new Habilidad("Final de prueba") {
            public void alInicioCombate(Combatiente p, Combatiente o, EstadoHabilidad e) {
                p.setVida(0); if (caso.equals("simultanea")) o.setVida(0);
            }
        }, 1);
        Random prohibido = new Random(0) { public int nextInt(int limite) { fail("No hay botín"); return 0; } };
        app.jugarAventura(j, new Scanner(caso.equals("huida") ? "3\n0\n" : ""), salida(), () -> 4,
                prohibido, enemigos(), new Random(0), new RepositorioObjetosMemoria(), null, instantaneo(), prohibido);
        assertTrue(j.getPergaminos().isEmpty()); assertEquals(15, j.getOro()); assertEquals(0, j.getExperiencia());
        assertFalse(bytes.toString(StandardCharsets.UTF_8).contains("Has obtenido un Pergamino"));
    }

    @Test
    void generadorSeConservaEntreAventurasYSoloSeConsultaPorVictorias() {
        int[] llamadas = {0};
        Random pergaminos = new Random(0) {
            public int nextInt(int limite) {
                assertEquals(llamadas[0] < 2 ? 100 : 6, limite);
                return new int[]{99, 0, 5}[llamadas[0]++];
            }
        };
        Random sinPociones = new Random(0) { public int nextInt(int limite) { return 99; } };
        app.ejecutar(new Scanner("1\nAventurero\n1\n2\n4\n1\nAventurero\n1\n2\n0\n"), salida(), () -> 4,
                sinPociones, enemigos(), new Random(0), new RepositorioObjetosMemoria(), null, instantaneo(), pergaminos);
        assertEquals(3, llamadas[0]);
        assertEquals(1, bytes.toString(StandardCharsets.UTF_8).split("Has obtenido un Pergamino de vida máxima", -1).length - 1);
    }

    @Test
    void recompensaDeArmaGeneraPuntosUnaSolaVezAunqueSeRepitanEntradasInvalidas() {
        Jugador j = jugador(); int[] llamadas = {0};
        var d = new RepositorioObjetosMemoria().obtenerArmas().getFirst();
        Random azar = new Random(0) { public int nextInt(int limite) { assertEquals(4, limite); llamadas[0]++; return 3; } };
        assertTrue(app.ofrecerEspada(j, new Scanner("x\n9\n1\n"), salida(), d, instantaneo(), azar));
        assertEquals(1, llamadas[0]);
        assertEquals(3, j.getEquipoEquipado().get(TipoEquipo.ARMA).getMejorasDisponibles());
        j.desequiparAlInventario(TipoEquipo.ARMA); j.equiparDesdeInventario(TipoEquipo.ARMA, 0);
        assertEquals(1, llamadas[0]);
    }

    @Test
    void segundaRecompensaTambienGeneraUnEjemplarSoloUnaVez() {
        Jugador j = jugador(); int[] usos = {0};
        Random azar = new Random(0) { public int nextInt(int limite) { assertEquals(4, limite); usos[0]++; return 2; } };
        var d = new RepositorioObjetosMemoria().obtenerArmas().getLast();
        assertTrue(app.ofrecerArmaGuardada(j, new Scanner("x\n9\n1\n"), salida(), d, instantaneo(), azar));
        assertEquals(1, usos[0]); assertEquals(2, j.getInventarioEquipo().getFirst().getMejorasDisponibles());
        assertEquals(d.id(), j.getInventarioEquipo().getFirst().getIdDefinicion());
    }

    @Test
    void victoriaHaceLaComprobacionAunqueLaEntradaTermineEnElRepartoDeNivel() {
        Jugador j = jugador(); int[] usos = {0};
        Random azar = new Random(0) { public int nextInt(int limite) { usos[0]++; return 0; } };
        Random sinPociones = new Random(0) { public int nextInt(int limite) { return 99; } };
        assertFalse(app.jugarAventura(j, new Scanner("1\n"), salida(), () -> 4, sinPociones,
                () -> List.of(new DefinicionEnemigo("prueba", "Prueba", "", 1, 0, 1, 0, 0, 1, 20)),
                new Random(0), new RepositorioObjetosMemoria(), null, instantaneo(), azar));
        assertEquals(2, usos[0]); assertEquals(1, j.getCantidadPergaminos(Atributo.FUERZA));
        assertEquals(2, j.getNivel()); assertEquals(2, j.getPuntosPendientes());
    }
}
