package org.viajedragos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.arma.*;
import org.viajedragos.catalogo.*;
import org.viajedragos.combate.*;
import org.viajedragos.consola.*;
import org.viajedragos.habilidad.*;
import org.viajedragos.modelo.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ProgresionAventuraTest {
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private PrintStream salida() { return new PrintStream(bytes, true, StandardCharsets.UTF_8); }
    private String texto() { return TextoConsola.contenido(bytes.toString(StandardCharsets.UTF_8)).replaceAll("\\s+", " "); }
    private Jugador jugador() { return new Jugador("Prueba", "", 20, 2, 3, 2, 2, 48, 48); }
    private RepositorioEnemigos enemigos(int experiencia) {
        return () -> List.of(new DefinicionEnemigo("prueba", "Enemigo de prueba", "", 1, 0, 1, 0, 0, 1, experiencia));
    }
    private boolean jugar(Jugador j, String entrada, int exp, int dadosEsperados) {
        int[] dados = {0};
        boolean resultado = assertTimeoutPreemptively(Duration.ofSeconds(3), () -> app.jugarAventura(j,
                new Scanner((j.getPuntosPendientes() > 0 ? "" : "3\n") + entrada), salida(), () -> { assertTrue(++dados[0] <= dadosEsperados); return 4; },
                new Random(0) { public int nextInt(int limite) { assertEquals(100, limite); return 0; } },
                enemigos(exp), new Random(0),
                org.viajedragos.catalogo.ConfiguracionRepositorios.objetos(),
                null,
                new org.viajedragos.consola.RitmoConsola(org.viajedragos.consola.RitmoConsola.Velocidad.INSTANTANEA),
                TextoConsola.sinBotinEquipamiento(),
                TextoConsola.sinBotinEquipamiento(),
                new org.viajedragos.consola.TiempoAventura(),
                TextoConsola.sinBotinEquipamiento()));
        assertEquals(dadosEsperados, dados[0]);
        return resultado;
    }

    @Test
    void victoriaConcedeExpSinSustituirOroBotinNiOfertaDeEspada() {
        Jugador j = jugador();
        assertFalse(jugar(j, "1\n1\n0\n", 7, 6));
        assertEquals(1, j.getNivel());
        assertEquals(7, j.getExperiencia());
        assertEquals(20, j.getOro());
        assertEquals(1, j.getCantidadPociones(TipoPocion.PEQUENA));
        assertEquals(1, j.getCantidadPociones(TipoPocion.GRANDE));
        assertEquals("Espada desgastada", j.getEquipoEquipado().get(TipoEquipo.ARMA).getNombre());
        assertTrue(texto().contains("EXP obtenida: +7"));
        assertTrue(texto().contains("Nivel: 1 | EXP: 7/20"));
        assertFalse(texto().contains("REPARTIR PUNTOS"));
    }

    @Test
    void dosVictoriasAcumulanExpYConservanTodasLasRecompensasAnteriores() {
        Jugador j = jugador();
        assertFalse(jugar(j, "1\n2\n3\n1\n1\n1\n2\n0\n", 10, 12));
        assertEquals(2, j.getNivel());
        assertEquals(0, j.getExperiencia());
        assertEquals(0, j.getPuntosPendientes());
        assertEquals(23, j.getFuerza());
        assertEquals(54, j.getVida());
        assertEquals(54, j.getVidaMaxima());
        assertEquals(25, j.getOro());
        assertEquals(2, j.getCantidadPociones(TipoPocion.PEQUENA));
        assertEquals(2, j.getCantidadPociones(TipoPocion.GRANDE));
        assertEquals(1, texto().split("Has encontrado una Espada desgastada", -1).length - 1);
        assertEquals(1, texto().split("Has encontrado una Espada de hierro", -1).length - 1);
    }

    @Test
    void recompensaExternaPermiteSubidasMultiplesYRepartirTodosLosPuntos() {
        Jugador j = jugador();
        assertFalse(jugar(j, "1\n1\n1\n2\n3\n4\n5\n2\n0\n", 97, 6));
        assertEquals(4, j.getNivel());
        assertEquals(7, j.getExperiencia());
        assertEquals(50, j.getExperienciaNecesaria());
        assertEquals(0, j.getPuntosPendientes());
        assertEquals(25, j.getFuerza());
        assertEquals(6, j.getResistencia());
        assertEquals(6, j.getAgilidad());
        assertEquals(7, j.getIniciativa());
        assertEquals(6, j.getVelocidad());
        assertEquals(66, j.getVida());
        assertEquals(66, j.getVidaMaxima());
        assertTrue(texto().contains("Nivel alcanzado: 2"));
        assertTrue(texto().contains("Nivel alcanzado: 3"));
        assertTrue(texto().contains("Nivel alcanzado: 4"));
        assertTrue(texto().contains("Vida máxima y actual: +18 PS"));
        assertTrue(texto().contains("Puntos adicionales disponibles: 6"));
        assertTrue(texto().contains("ATRIBUTOS TRAS LA SUBIDA"));
        comprobarPaneles();
    }

    @ParameterizedTest
    @ValueSource(strings = {"derrota", "simultanea", "huida", "incompleto"})
    void sinVictoriaNoHayExperiencia(String caso) {
        Jugador j = jugador();
        j.agregarExperiencia(7);
        if (caso.equals("derrota") || caso.equals("simultanea")) {
            j.aprenderHabilidad(new Habilidad("Muerte de prueba") {
                @Override public void alInicioCombate(Combatiente p, Combatiente o, EstadoHabilidad e) {
                    p.setVida(0);
                    if (caso.equals("simultanea")) o.setVida(0);
                }
            }, 1);
        }
        boolean resultado = jugar(j, caso.equals("huida") ? "3\n0\n" : "", 20,
                caso.equals("huida") ? 6 : caso.equals("incompleto") ? 4 : 0);
        assertEquals(caso.equals("derrota") || caso.equals("simultanea"), resultado);
        assertEquals(1, j.getNivel());
        assertEquals(7, j.getExperiencia());
        assertEquals(0, j.getPuntosPendientes());
        assertEquals(15, j.getOro());
        assertFalse(texto().contains("EXP obtenida"));
        assertTrue(j.getInventarioPociones().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1\n1\n", "1\n2\n"})
    void repartoMismoAtributoODosDistintos(String entrada) {
        Jugador j = jugador();
        j.agregarExperiencia(20);
        assertTrue(app.repartirPuntos(j, new Scanner(entrada), salida()));
        assertEquals(0, j.getPuntosPendientes());
        assertEquals(entrada.equals("1\n1\n") ? 23 : 22, j.getFuerza());
        assertEquals(entrada.equals("1\n1\n") ? 3 : 4, j.getResistencia());
        assertTrue(texto().contains("Puntos pendientes: 0"));
    }

    @Test
    void invalidezIncluidoCeroNoPermiteSalirNiGastaPuntos() {
        Jugador j = jugador();
        j.agregarExperiencia(20);
        Scanner entrada = new Scanner("x\n0\n-1\n6\n999999999999999\n\n1\n2\nrestante\n");
        assertTrue(app.repartirPuntos(j, entrada, salida()));
        assertEquals("restante", entrada.nextLine());
        assertEquals(0, j.getPuntosPendientes());
        assertEquals(22, j.getFuerza());
        assertEquals(4, j.getResistencia());
        assertEquals(6, texto().split("Entrada inválida", -1).length - 1);
        assertFalse(texto().contains("[0]"));
        comprobarPaneles();
    }

    @Test
    void cerrarEntradaConservaPuntosYReanudarNoPermiteCombatirAntesDeAsignarlos() {
        Jugador j = jugador();
        assertFalse(jugar(j, "1\n1\n", 20, 6));
        assertEquals(2, j.getNivel());
        assertEquals(1, j.getPuntosPendientes());
        assertEquals(22, j.getFuerza());
        assertFalse(jugar(j, "", 20, 0));
        assertEquals(1, j.getPuntosPendientes());
        assertFalse(jugar(j, "2\n3\n", 20, 4)); // Reparte el restante y llega al turno, sin acción.
        assertEquals(0, j.getPuntosPendientes());
        assertEquals(22, j.getFuerza());
        assertEquals(4, j.getResistencia());
        assertEquals(0, j.getExperiencia());
    }

    @Test
    void nivelYExpSeMuestranEnElEstadoSinGastarPuntos() {
        Jugador j = jugador();
        j.agregarExperiencia(25);
        assertTrue(app.consultarEstado(j, new Scanner("0\n"), salida()));
        assertTrue(texto().contains("Nivel: 2"));
        assertTrue(texto().contains("EXPERIENCIA 5/30"));
        assertTrue(texto().contains("Puntos pendientes: 2"));
        assertEquals(2, j.getPuntosPendientes());
        comprobarPaneles();
    }

    @Test
    void repartoYEstadoNoIntroducenEsperasNarrativas() {
        Jugador j = jugador();
        var esperas = new ArrayList<String>();
        var ritmo = new RitmoConsola(RitmoConsola.Velocidad.NORMAL, ms -> {
            assertEquals(700, ms);
            esperas.add(bytes.toString(StandardCharsets.UTF_8));
        });
        var objetos = new RepositorioObjetosMemoria();
        assertFalse(app.jugarAventura(j, new Scanner("3\n1\nx\n0\n1\n2\n2\n0\n"), salida(), () -> 4,
                new Random(0) { public int nextInt(int limite) { return 99; } }, enemigos(20), new Random(0),
                objetos, null, ritmo, new Random(0) { public int nextInt(int limite) { return 99; } },
                new Random(0) { public int nextInt(int limite) { return 99; } },
                new org.viajedragos.consola.TiempoAventura(),
                TextoConsola.sinBotinEquipamiento()));
        assertEquals(7, esperas.size()); // Encuentro, turno, ataque, victoria, oro/EXP, subida y espada.
        assertTrue(esperas.get(5).contains("SUBIDA DE NIVEL"));
        assertTrue(esperas.get(5).stripTrailing().endsWith("┘"), "Panel de subida cerrado antes de esperar");
        assertEquals(0, esperas.stream().filter(t -> t.contains("REPARTIR PUNTOS") && !t.contains("Has encontrado una")).count());
        assertEquals(0, j.getPuntosPendientes());
    }

    @Test
    void nuevaAventuraEmpiezaSinProgresionAnterior() {
        RepositorioEnemigos repo = enemigos(20);
        TextoConsola.ejecutarSesiones(new Scanner("1\nAventurero\n3\n1\n1\n1\n2\n0\n1\nAventurero\n3\n1\n1\n1\n2\n0\n"), entradaSesion -> app.ejecutar(entradaSesion, salida(), () -> 4,
                new Random(0), repo, new Random(0),
                org.viajedragos.catalogo.ConfiguracionRepositorios.objetos(),
                null,
                new org.viajedragos.consola.RitmoConsola(org.viajedragos.consola.RitmoConsola.Velocidad.INSTANTANEA),
                TextoConsola.sinBotinEquipamiento(),
                TextoConsola.sinBotinEquipamiento(),
                TextoConsola.sinBotinEquipamiento()));
        assertEquals(2, texto().split("Nivel alcanzado: 2", -1).length - 1);
        assertFalse(texto().contains("Nivel alcanzado: 3"));
    }

    @Test
    void motorAutomaticoNoDuplicaRecompensasDeLaAventura() {
        Jugador j = jugador();
        assertEquals(ResultadoCombate.ENEMIGO_MUERE,
                new Combate(j, enemigos(20).obtenerTodos().getFirst().crearEnemigo(), () -> 4).combatir());
        assertEquals(0, j.getExperiencia());
        assertEquals(1, j.getNivel());
        assertEquals(15, j.getOro());
    }

    private void comprobarPaneles() {
        assertTrue(bytes.toString(StandardCharsets.UTF_8).contains("┌"));
        bytes.toString(StandardCharsets.UTF_8).lines()
                .filter(l -> l.startsWith("┌") || l.startsWith("├") || l.startsWith("└") || l.startsWith("│"))
                .forEach(l -> assertEquals(50, PanelConsola.anchoVisible(l), l));
    }
}
