package org.viajedragos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.catalogo.*;
import org.viajedragos.combate.*;
import org.viajedragos.consola.RitmoConsola;
import org.viajedragos.consola.RitmoConsola.Velocidad;
import org.viajedragos.modelo.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class RitmoAventuraTest {
    private final ByteArrayOutputStream salida = new ByteArrayOutputStream();
    private PrintStream consola() { return new PrintStream(salida, true, StandardCharsets.UTF_8); }
    private String texto() { return salida.toString(StandardCharsets.UTF_8); }
    private RepositorioEnemigos enemigos() {
        return () -> List.of(new DefinicionEnemigo("prueba", "Enemigo", "", 1, 0, 1, 0, 0, 1));
    }
    private Random sinBotin() {
        return new Random(0) {
            @Override public int nextInt(int limite) { assertEquals(100, limite); return 99; }
        };
    }
    private RitmoConsola registrar(List<String> momentos) {
        return new RitmoConsola(Velocidad.NORMAL, ms -> {
            assertEquals(700, ms);
            momentos.add(texto());
        });
    }

    @Test
    void mainEmpiezaEnNormalYPermiteCambiarSinEsperarEnLosMenus() {
        InputStream entradaAnterior = System.in;
        PrintStream salidaAnterior = System.out;
        try {
            System.setIn(new ByteArrayInputStream("2\n1\n0\n".getBytes(StandardCharsets.UTF_8)));
            System.setOut(consola());
            app.main(new String[0]);
        } finally {
            System.setIn(entradaAnterior);
            System.setOut(salidaAnterior);
        }
        assertTrue(texto().contains("Velocidad: Normal"));
        assertTrue(texto().contains("Velocidad: Instantánea"));
        assertTrue(texto().contains("[2] Velocidad de narración"));
        assertTrue(texto().contains("[1] Instantánea (0 ms)"));
        assertTrue(texto().contains("[4] Lenta (1200 ms)"));
    }

    @ParameterizedTest
    @CsvSource({"1,INSTANTANEA", "2,RAPIDA", "3,NORMAL", "4,LENTA"})
    void elegirVelocidadNoEsperaNiConsumeOtraEntrada(String opcion, Velocidad esperada) {
        RitmoConsola ritmo = new RitmoConsola(Velocidad.NORMAL, ms -> fail("Configurar no debe esperar"));
        Scanner entrada = new Scanner(opcion + "\npendiente\n");
        assertTrue(app.configurarVelocidad(entrada, consola(), ritmo));
        assertEquals(esperada, ritmo.getVelocidad());
        assertEquals("pendiente", entrada.nextLine());
        assertTrue(texto().contains("Velocidad de narración: " + esperada.getNombre()));
        comprobarBordes();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0\n", "x\n-1\n5\n0\n", "", "x\n"})
    void cancelarInvalidosYFinDeEntradaConservanVelocidadYSinPausas(String entrada) {
        RitmoConsola ritmo = new RitmoConsola(Velocidad.RAPIDA, ms -> fail("El menú no debe esperar"));
        boolean completa = app.configurarVelocidad(new Scanner(entrada), consola(), ritmo);
        assertEquals(entrada.endsWith("0\n"), completa);
        assertEquals(Velocidad.RAPIDA, ritmo.getVelocidad());
    }

    @Test
    void pausasOcurrenTrasPanelesCompletosYNoDuranteMenusOValidaciones() {
        List<String> momentos = new ArrayList<>();
        Jugador jugador = new Jugador("Aventurero", "", 6, 2, 3, 2, 2, 48, 48);
        int[] tiradas = {0};
        String entrada = "x\n2\n0\n1\nx\n2\nx\n5\nx\n0\n1\nx\n0\n2\nx\n0\n0\n";
        assertFalse(app.jugarAventura(jugador, new Scanner(entrada), consola(), () -> {
            assertTrue(++tiradas[0] <= 6);
            return 4;
        }, sinBotin(), enemigos(), new Random(0), new RepositorioObjetosMemoria(), null, registrar(momentos)));
        assertEquals(6, tiradas[0]);
        assertEquals(6, momentos.size());
        assertTrue(momentos.get(0).contains("ENCUENTRO 1"));
        assertFalse(momentos.get(0).contains("TURNO 1"));
        assertTrue(momentos.get(1).contains("Turno 1. Orden"));
        assertFalse(momentos.get(1).contains("TU TURNO"));
        assertTrue(momentos.get(2).contains("RESULTADO DEL ATAQUE"));
        assertFalse(momentos.get(2).contains("FIN DEL ENCUENTRO"));
        assertTrue(momentos.get(3).contains("Victoria."));
        assertFalse(momentos.get(3).contains("RECOMPENSAS"));
        assertTrue(momentos.get(4).contains("Oro total: 20"));
        assertFalse(momentos.get(4).contains("Has encontrado"));
        assertTrue(momentos.get(5).contains("BOTÍN - ARMA ENCONTRADA"));
        for (int indice : new int[]{0, 1, 2, 4, 5}) {
            assertTrue(momentos.get(indice).stripTrailing().endsWith("┘"), "Nunca se espera dentro de un panel");
        }
        assertEquals(20, jugador.getOro());
        assertTrue(jugador.getEquipoEquipado().isEmpty());
        comprobarBordes();
    }

    @Test
    void ataqueEnemigoEsperaAntesDeMostrarResultadoYDespuesSinCambiarDados() {
        Jugador jugador = new Jugador("Jugador", "", 10, 0, 3, 0, 0, 30, 30);
        Enemigo enemigo = new Enemigo("Enemigo", "", 6, 0, 1, 0, 0, 10, 30);
        int[] dados = {1, 1, 1, 6, 4, 2, 4, 2};
        int[] usadas = {0};
        List<String> momentos = new ArrayList<>();
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, new Combate(jugador, enemigo,
                () -> dados[usadas[0]++], registrar(momentos)).combatir(new Scanner("1\n"), consola()));
        assertEquals(dados.length, usadas[0]);
        assertEquals(4, momentos.size());
        assertTrue(momentos.get(1).contains("ACCIÓN DEL ENEMIGO"));
        assertFalse(momentos.get(1).contains("RESULTADO DEL ATAQUE"));
        assertTrue(TextoConsola.contenido(momentos.get(2)).contains("Enemigo golpea a Jugador"));
        assertFalse(momentos.get(2).contains("TU TURNO"));
        assertTrue(TextoConsola.contenido(momentos.get(3)).contains("Jugador golpea a Enemigo"));
        assertEquals(22, jugador.getVida());
        comprobarBordes();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void falloYContraataqueTambienPausanTrasMostrarElPanel(boolean contraataque) {
        Jugador jugador = new Jugador("Jugador", "", 10, 0, 3, 0, 0, 5, 30);
        Enemigo enemigo = new Enemigo("Enemigo", "", 6, 0, 1, 0, 0, 10, 30);
        int[] dados = contraataque ? new int[]{1, 1, 1, 1, 1, 6} : new int[]{1, 1, 1, 1, 1, 2, 4, 2};
        int[] usadas = {0};
        List<String> momentos = new ArrayList<>();
        assertEquals(ResultadoCombate.JUGADOR_MUERE, new Combate(jugador, enemigo,
                () -> dados[usadas[0]++], registrar(momentos)).combatir(new Scanner("1\n"), consola()));
        assertEquals(dados.length, usadas[0]);
        assertEquals(contraataque ? 2 : 4, momentos.size());
        assertTrue(momentos.get(1).contains("ATAQUE FALLIDO"));
        assertTrue(momentos.get(1).stripTrailing().endsWith("┘"));
        assertEquals(contraataque, momentos.get(1).contains("CONTRAATAQUE"));
    }

    @Test
    void instantaneoMantieneTextoDadosYResultadosSinLlamarALaEspera() {
        Jugador jugador = new Jugador("Jugador", "", 6, 2, 3, 2, 2, 48, 48);
        RitmoConsola ritmo = new RitmoConsola(Velocidad.INSTANTANEA, ms -> fail("Instantáneo no espera"));
        assertFalse(app.jugarAventura(jugador, new Scanner("1\n2\n0\n"), consola(), () -> 4,
                sinBotin(), enemigos(), new Random(0), new RepositorioObjetosMemoria(), null, ritmo));
        String instantaneo = texto();
        salida.reset();
        Jugador otro = new Jugador("Jugador", "", 6, 2, 3, 2, 2, 48, 48);
        assertFalse(app.jugarAventura(otro, new Scanner("1\n2\n0\n"), consola(), () -> 4,
                sinBotin(), enemigos(), new Random(0), new RepositorioObjetosMemoria(), null,
                new RitmoConsola(Velocidad.NORMAL, ms -> {})));
        assertEquals(instantaneo, texto());
        assertEquals(jugador.getVida(), otro.getVida());
        assertEquals(jugador.getOro(), otro.getOro());
    }

    @Test
    void automaticoNuncaEsperaAunqueRecibaRitmoNormal() {
        RitmoConsola ritmo = new RitmoConsola(Velocidad.NORMAL, ms -> fail("Automático no espera"));
        Jugador jugador = new Jugador("Jugador", "", 6, 2, 3, 2, 2, 48, 48);
        Enemigo enemigo = enemigos().obtenerTodos().getFirst().crearEnemigo();
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, new Combate(jugador, enemigo, () -> 4, ritmo).combatir());
    }

    @Test
    void velocidadSeMantieneEntreAventurasYSePuedeCambiarEnElMenuPrincipal() {
        List<Long> esperas = new ArrayList<>();
        RitmoConsola ritmo = new RitmoConsola(Velocidad.NORMAL, esperas::add);
        int[] encuentros = {0};
        Random azarEncuentros = new Random(0) {
            @Override public int nextInt(int limite) { encuentros[0]++; return 0; }
        };
        app.ejecutar(new Scanner("2\n2\n1\n1\n2\n4\n1\n1\n2\n4\n2\n1\n1\n1\n2\n0\n"),
                consola(), () -> 4, sinBotin(), enemigos(), azarEncuentros, new RepositorioObjetosMemoria(), null, ritmo);
        assertEquals(3, encuentros[0]);
        assertEquals(12, esperas.size(), "Seis pausas por cada una de las dos primeras aventuras");
        assertTrue(esperas.stream().allMatch(ms -> ms == 250));
        assertEquals(Velocidad.INSTANTANEA, ritmo.getVelocidad());
        assertTrue(texto().contains("Velocidad: Rápida"));
        assertTrue(texto().contains("Velocidad: Instantánea"));
    }

    @Test
    void segundaVictoriaPausaTambienAlEncontrarLaEspadaDeHierro() {
        Jugador jugador = new Jugador("Jugador", "", 6, 2, 3, 2, 2, 48, 48);
        List<String> momentos = new ArrayList<>();
        assertFalse(app.jugarAventura(jugador, new Scanner("1\n2\n3\n1\n2\n0\n"), consola(), () -> 4,
                sinBotin(), enemigos(), new Random(0), new RepositorioObjetosMemoria(), null, registrar(momentos)));
        assertEquals(12, momentos.size());
        assertTrue(momentos.getLast().contains("Has encontrado una Espada de hierro"));
        assertTrue(momentos.getLast().stripTrailing().endsWith("┘"));
        assertEquals(25, jugador.getOro());
        assertTrue(jugador.getInventarioEquipo().isEmpty());
    }

    @Test
    void derrotaPausaTrasElAnuncioSinPresentarRecompensas() {
        Jugador jugador = new Jugador("Jugador", "", 6, 2, 3, 2, 2, 48, 48);
        RepositorioEnemigos fuerte = () -> List.of(new DefinicionEnemigo("fuerte", "Enemigo", "", 100, 0, 4, 0, 0, 100));
        List<String> momentos = new ArrayList<>();
        int[] tiradas = {0};
        assertTrue(app.jugarAventura(jugador, new Scanner(""), consola(), () -> { tiradas[0]++; return 4; },
                sinBotin(), fuerte, new Random(0), new RepositorioObjetosMemoria(), null, registrar(momentos)));
        assertEquals(6, tiradas[0]);
        assertEquals(5, momentos.size());
        assertTrue(momentos.getLast().contains("Derrota."));
        assertFalse(texto().contains("RECOMPENSAS"));
        assertEquals(15, jugador.getOro());
        assertEquals(0, jugador.getVida());
    }

    @Test
    void botinPausaSoloPorCadaObjetoRealmenteEncontrado() {
        Jugador jugador = new Jugador();
        List<String> momentos = new ArrayList<>();
        Random ambos = new Random(0) {
            @Override public int nextInt(int limite) { return 0; }
        };
        app.obtenerBotin(jugador, ambos, consola(), registrar(momentos));
        assertEquals(2, momentos.size());
        assertFalse(momentos.getFirst().contains("Poción grande"));
        assertTrue(momentos.getLast().contains("Poción grande"));
        assertEquals(1, jugador.getCantidadPociones(TipoPocion.PEQUENA));
        assertEquals(1, jugador.getCantidadPociones(TipoPocion.GRANDE));
        app.obtenerBotin(jugador, sinBotin(), consola(), registrar(momentos));
        assertEquals(2, momentos.size());
    }

    private void comprobarBordes() {
        for (String linea : texto().split("\\R")) {
            if (linea.matches("^[│┌├└].*")) assertEquals(50, org.viajedragos.consola.PanelConsola.anchoVisible(linea), linea);
        }
    }
}
