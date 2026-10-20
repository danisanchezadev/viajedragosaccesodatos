package org.viajedragos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.catalogo.*;
import org.viajedragos.consola.*;
import org.viajedragos.habilidad.*;
import org.viajedragos.modelo.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PlantasConsolaTest {
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private PrintStream salida() { return new PrintStream(bytes, true, StandardCharsets.UTF_8); }
    private String texto() { return TextoConsola.contenido(bytes.toString(StandardCharsets.UTF_8)).replaceAll("\\s+", " "); }
    private Random ninguno() { return new Random(0) { public int nextInt(int limite) { return limite - 1; } }; }
    private Jugador jugador() { return new Jugador("Prueba", "", 6, 2, 3, 2, 100, 48, 48); }
    private RepositorioEnemigos catalogo() {
        var datos = new RepositorioEnemigosMemoria().obtenerTodos().stream()
                .map(d -> new DefinicionEnemigo(d.id(), d.nombre(), d.descripcion(), 1, 0, 1, 0, 0, 1, 1, d.nivel(), d.planta(), d.guardian())).toList();
        return () -> datos;
    }
    private boolean jugar(Jugador j, String entradas, RepositorioEnemigos repo, Random encuentros, int[] dados) {
        boolean resultado = app.jugarAventura(j, new Scanner(entradas), salida(), () -> { dados[0]++; return 4; },
                ninguno(), repo, encuentros, new RepositorioObjetosMemoria(), null,
                new RitmoConsola(RitmoConsola.Velocidad.NORMAL, ms -> {}), ninguno(), ninguno(),
                new org.viajedragos.consola.TiempoAventura(),
                TextoConsola.sinBotinEquipamiento());
        TextoConsola.comprobarPaneles(bytes.toString(StandardCharsets.UTF_8));
        return resultado;
    }

    @Test void exploracionInicialTieneNumerosExactosGuardarNoCambiaNiCombate() {
        Jugador j = jugador(); int[] dados = {0};
        Random prohibido = new Random(0) { public int nextInt(int n) { fail("No se ha elegido Explorar"); return 0; } };
        assertFalse(jugar(j, "5\n4\n0\n1\n2\n7\n0\n0\n0\n0\n", catalogo(), prohibido, dados));
        String s = texto();
        for (String opcion : List.of("1. Menú de personaje", "2. Cambiar de planta", "3. Explorar", "4. Tienda", "5. Guardar", "0. Salir del juego"))
            assertTrue(s.contains(opcion));
        assertFalse(s.contains("2. Usar objeto")); assertFalse(s.contains("Terminar aventura"));
        assertTrue(s.contains("Planta 1 - El vestíbulo"));
        assertTrue(s.contains("Funcionalidad no implementada: el sistema de persistencia de partidas todavía no está disponible."));
        assertTrue(s.contains("INVENTARIO")); assertTrue(s.contains("USAR OBJETO"));
        assertTrue(s.contains("TIENDA")); assertEquals(0, dados[0]);
        assertEquals(48, j.getVida()); assertEquals(15, j.getOro()); assertEquals(0, j.getExperiencia());
        assertTrue(j.getInventarioPociones().isEmpty()); assertTrue(j.getInventarioEquipo().isEmpty());
    }

    @ParameterizedTest @ValueSource(strings = {"", "2\n", "2\nx\n0\n", "2\n1\n2\n0\n0\n"})
    void eofCancelacionEIntentosBloqueadosNoCreanCombate(String entrada) {
        int[] dados = {0}; assertFalse(jugar(jugador(), entrada, catalogo(), ninguno(), dados));
        assertEquals(0, dados[0]); assertFalse(texto().contains("Comienza el combate"));
        assertTrue(texto().contains("Planta 1 - El vestíbulo"));
        assertTrue(texto().contains("Victorias necesarias: 0/3"));
    }

    @Test void recorrerCincoPlantasConUnSoloJugadorPremiaGuardianUnaVezYRegresaAEntrenar() {
        Jugador j = jugador(); var participantes = new ArrayList<Combatiente>();
        j.aprenderHabilidad(new Habilidad("Resolución controlada de prueba") {
            public void alInicioCombate(Combatiente p, Combatiente o, EstadoHabilidad e) {
                assertSame(j, p); participantes.add(o); o.setVida(0);
            }
        }, 1);
        int[] selecciones = {0};
        Random encuentros = new Random(0) {
            public int nextInt(int limite) {
                int i = selecciones[0]++;
                assertEquals(i == 3 ? 4 : 5, limite, "El Capitán no entra en el sorteo");
                return i >= 4 && i <= 6 ? i - 4 : 0;
            }
        };
        String entrada = "3\n2\n3\n2\n3\n2\n2\n4\n1\n0\n0\n" // Tres victorias, descenso a 2 y consulta de tienda.
                + "3\n6\n6\n2\n2\n4\n1\n0\n0\n" // Soldado, Capitán, repetición inválida y descenso a 3.
                + "3\n3\n3\n2\n2\n4\n1\n0\n0\n" // Tres especies y descenso a 4.
                + "3\n3\n3\n2\n2\n4\n1\n0\n0\n" // Tres fragmentos y descenso a 5.
                + "3\n2\n2\n0\n2\n1\n2\n1\n2\n1\n2\n1\n3\n0\n";
        int[] dados = {0}; assertFalse(jugar(j, entrada, catalogo(), encuentros, dados));
        assertEquals(12, selecciones[0]); assertEquals(13, participantes.size());
        assertEquals(1, participantes.stream().filter(e -> e.getNombre().equals("Capitán de barracones")).count());
        assertEquals(List.of("Momia", "Araña de cripta", "Espectro"), participantes.subList(5, 8).stream().map(Combatiente::getNombre).toList());
        assertTrue(((Enemigo) participantes.get(3)).getNivel() > j.getNivel(), "No se filtra por nivel del jugador");
        assertNotSame(participantes.getFirst(), participantes.getLast()); assertEquals(0, dados[0]);
        for (Planta p : Planta.values()) assertTrue(texto().contains("Planta " + p.numero() + " - " + p.nombre()));
        for (int numero = 2; numero <= 5; numero++)
            assertEquals(1, texto().split("Planta " + numero + " disponible:", -1).length - 1);
        for (String resumen : List.of("Guardián: pendiente", "Guardián: derrotado",
                "Especies diferentes derrotadas: 2/3", "Fragmentos del sello: 2/3", "Última planta disponible"))
            assertTrue(texto().contains(resumen));
        assertTrue(texto().contains("Todavía no se han desarrollado más plantas"));
        assertTrue(texto().contains("Espada de hierro - 20 monedas"));
        assertTrue(texto().contains("Espada de acero - 35 monedas"));
        assertTrue(texto().contains("Espada de la forja - 50 monedas"));
        assertEquals(13, texto().split("EXP obtenida: \\+1", -1).length - 1);
        String[] premios=texto().split("RECOMPENSAS");
        int[] cantidades={5,5,5,8,8,15,15,15,24,24,24,35,5};
        for(int i=0;i<cantidades.length;i++) assertTrue(premios[i+1].contains("Oro obtenido: +"+cantidades[i]+" monedas"));
        assertEquals(203, j.getOro()); assertEquals(13, j.getExperiencia());
        assertEquals(1, texto().split("Has encontrado una Espada desgastada", -1).length - 1);
        assertEquals(1, texto().split("Has encontrado una Espada de hierro", -1).length - 1);
        assertTrue(j.getInventarioEquipo().isEmpty()); assertTrue(j.getInventarioPociones().isEmpty());
    }

    @Test void huidaNoCuentaVictoriaNiOfreceEspadaYDespuesSePuedeExplorar() {
        Jugador j = jugador(); int[] encuentros = {0};
        j.aprenderHabilidad(new Habilidad("Preparación controlada de prueba") {
            public void alInicioCombate(Combatiente p, Combatiente o, EstadoHabilidad e) { if (++encuentros[0] > 1) o.setVida(0); }
        }, 1);
        int[] dados = {0};
        assertFalse(jugar(j, "3\n3\n2\n2\n0\n3\n2\n2\n2\n0\n0\n", catalogo(), ninguno(), dados));
        assertEquals(2, encuentros[0]); assertEquals(6, dados[0]);
        assertTrue(texto().contains("Victorias necesarias: 0/3"));
        assertTrue(texto().contains("Victorias necesarias: 1/3"));
        assertFalse(texto().contains("Planta 2 disponible:"));
        assertTrue(texto().contains("Has escapado.")); assertTrue(texto().contains("0/3 victorias"));
        assertTrue(texto().contains("1/3 victorias")); assertFalse(texto().contains("Planta 2 -"));
        assertEquals(20, j.getOro()); assertEquals(1, j.getExperiencia());
        assertEquals(1, texto().split("Has encontrado una Espada desgastada", -1).length - 1);
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void derrotaOMuerteSimultaneaTerminaSinRecompensas(boolean simultanea) {
        Jugador j = jugador();
        j.aprenderHabilidad(new Habilidad("Final controlado de prueba") {
            public void alInicioCombate(Combatiente p, Combatiente o, EstadoHabilidad e) { p.setVida(0); if (simultanea) o.setVida(0); }
        }, 1);
        int[] dados = {0}; assertTrue(jugar(j, "3\n", catalogo(), ninguno(), dados));
        assertEquals(0, dados[0]); assertEquals(15, j.getOro()); assertEquals(0, j.getExperiencia());
        assertFalse(texto().contains("RECOMPENSAS")); assertFalse(texto().contains("Has encontrado"));
        assertFalse(texto().contains("disponible:"));
        assertEquals(1, texto().split("EXPLORACIÓN", -1).length - 1);
    }

    @Test void dosAventurasDelMismoJugadorNoCompartenDesbloqueos() {
        Jugador j = jugador();
        j.aprenderHabilidad(new Habilidad("Victoria controlada de prueba") {
            public void alInicioCombate(Combatiente p, Combatiente o, EstadoHabilidad e) { o.setVida(0); }
        }, 1);
        assertFalse(jugar(j, "3\n2\n3\n2\n3\n2\n2\n0\n", catalogo(), ninguno(), new int[]{0}));
        assertTrue(texto().contains("Planta 2 - Los barracones")); bytes.reset();
        assertFalse(jugar(j, "2\n2\n0\n0\n", catalogo(), ninguno(), new int[]{0}));
        assertFalse(texto().contains("Planta 2 - Los barracones")); assertTrue(texto().contains("0/3 victorias"));
    }

    @Test void desbloqueoSeAnunciaUnaVezSinDescenderAunqueSeSigaCombatiendo() {
        Jugador j = jugador();
        j.aprenderHabilidad(new Habilidad("Victoria controlada de prueba") {
            public void alInicioCombate(Combatiente p, Combatiente o, EstadoHabilidad e) { o.setVida(0); }
        }, 1);
        assertFalse(jugar(j, "3\n2\n3\n2\n3\n3\n0\n", catalogo(), ninguno(), new int[]{0}));
        assertEquals(1, texto().split("Planta 2 disponible: Los barracones", -1).length - 1);
        assertFalse(texto().contains("Planta 2 - Los barracones"));
        assertTrue(texto().contains("Victorias necesarias: 3/3"));
        assertEquals(4, j.getExperiencia()); assertEquals(35, j.getOro());
    }

    @Test void consultarProgresoYNavegarNoIntroducePausas() {
        var ritmo = new RitmoConsola(RitmoConsola.Velocidad.LENTA, ms -> fail("No hay acontecimientos narrativos"));
        assertFalse(app.jugarAventura(jugador(), new Scanner("5\n2\n0\n0\n"), salida(), () -> { fail(); return 1; },
                ninguno(), catalogo(), ninguno(), new RepositorioObjetosMemoria(), null, ritmo, ninguno(), ninguno(),
                new TiempoAventura(() -> 0), TextoConsola.sinBotinEquipamiento()));
        assertTrue(texto().contains("Victorias necesarias: 0/3"));
        TextoConsola.comprobarPaneles(bytes.toString(StandardCharsets.UTF_8));
    }
}
