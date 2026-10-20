package org.viajedragos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.arma.Atributo;
import org.viajedragos.catalogo.*;
import org.viajedragos.consola.PanelConsola;
import org.viajedragos.habilidad.*;
import org.viajedragos.modelo.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EncuentrosPorNivelTest {
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private PrintStream salida() { return new PrintStream(bytes, true, StandardCharsets.UTF_8); }
    private Jugador jugador(int nivel) {
        Jugador j = new Jugador("Prueba", "", 6, 2, 3, 2, 2, 48, 48);
        while (j.getNivel() < nivel) j.agregarExperiencia((int) j.getExperienciaNecesaria());
        while (j.getPuntosPendientes() > 0) assertTrue(j.asignarPunto(Atributo.VELOCIDAD));
        return j;
    }
    private Random sinBotin() {
        return new Random(0) { public int nextInt(int limite) { assertEquals(100, limite); return 99; } };
    }
    private void comprobarSeleccion(RepositorioEnemigos repo, int nivel, List<DefinicionEnemigo> elegibles) {
        for (int indice = 0; indice < elegibles.size(); indice++) {
            bytes.reset();
            int seleccion = indice;
            int[] llamadas = {0, 0};
            Random azar = new Random(0) {
                @Override public int nextInt(int limite) {
                    assertEquals(elegibles.size(), limite);
                    assertEquals(1, ++llamadas[0]);
                    return seleccion;
                }
            };
            Jugador j = jugador(nivel);
            int dadosEsperados = j.getIniciativa() == elegibles.get(indice).iniciativa() ? 6 : 4;
            assertFalse(assertTimeoutPreemptively(Duration.ofSeconds(2), () -> app.jugarAventura(j,
                    new Scanner("3\n"), salida(), () -> { assertTrue(++llamadas[1] <= dadosEsperados); return llamadas[1] % 2 == 1 ? 6 : 1; },
                    sinBotin(), repo, azar,
                org.viajedragos.catalogo.ConfiguracionRepositorios.objetos(),
                null,
                new org.viajedragos.consola.RitmoConsola(org.viajedragos.consola.RitmoConsola.Velocidad.INSTANTANEA),
                TextoConsola.sinBotinEquipamiento(),
                TextoConsola.sinBotinEquipamiento(),
                new org.viajedragos.consola.TiempoAventura(),
                TextoConsola.sinBotinEquipamiento())));
            assertEquals(1, llamadas[0]);
            assertEquals(dadosEsperados, llamadas[1], "Solo se tiran dados de iniciativa y orden, incluido el desempate inicial");
            var esperado = elegibles.get(indice);
            String texto = TextoConsola.contenido(bytes.toString(StandardCharsets.UTF_8));
            assertTrue(texto.contains(esperado.nombre() + " — Nivel " + esperado.nivel()));
            assertEquals(nivel, j.getNivel());
            assertEquals(0, j.getExperiencia());
            assertEquals(15, j.getOro());
            comprobarPaneles();
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6})
    void nivelDelJugadorNoExcluyeEnemigosDeLaPlantaInicial(int nivel) {
        var repo = new RepositorioEnemigosMemoria();
        var esperados = repo.obtenerTodos().stream().filter(d -> d.planta() == 1 && !d.guardian()).toList();
        assertEquals(5, esperados.size());
        assertEquals(5, esperados.stream().filter(d -> d.nivel() == 1).count());
        comprobarSeleccion(repo, nivel, esperados);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 5})
    void repositorioAlternativoConOrdenNivelesEIdentificadoresPropios(int nivel) {
        var alto = new DefinicionEnemigo("alto_externo", "Alto externo", "", 1, 0, 1, 0, 0, 1, 25, 5);
        var bajo = new DefinicionEnemigo("bajo_externo", "Bajo externo", "", 1, 0, 1, 0, 0, 1, 5, 1);
        var medio = new DefinicionEnemigo("medio_externo", "Medio externo", "", 1, 0, 1, 0, 0, 1, 15, 3);
        RepositorioEnemigos repo = () -> List.of(alto, bajo, medio);
        List<DefinicionEnemigo> esperados = List.of(alto, bajo, medio);
        comprobarSeleccion(repo, nivel, esperados);
    }

    @Test
    void subirDeNivelNoRecargaNiCambiaLosElegiblesDeLaMismaPlanta() {
        var inicial = new DefinicionEnemigo("inicial", "Inicial externo", "", 1, 0, 1, 0, 0, 1, 50, 1);
        var avanzado = new DefinicionEnemigo("avanzado", "Avanzado externo", "", 1, 0, 1, 0, 0, 1, 15, 3);
        var bloqueado = new DefinicionEnemigo("bloqueado", "Bloqueado externo", "", 1, 0, 1, 0, 0, 1, 20, 4);
        int[] lecturas = {0}, selecciones = {0}, dados = {0};
        RepositorioEnemigos repo = () -> { lecturas[0]++; return List.of(inicial, avanzado, bloqueado); };
        Random azar = new Random(0) {
            public int nextInt(int limite) {
                assertEquals(3, limite);
                return selecciones[0]++;
            }
        };
        Jugador j = jugador(1);
        assertFalse(app.jugarAventura(j, new Scanner("3\n1\n5\n5\n5\n5\n2\n3\n1\n2\n0\n"), salida(),
                () -> { assertTrue(++dados[0] <= 12); return 4; }, sinBotin(), repo, azar,
                org.viajedragos.catalogo.ConfiguracionRepositorios.objetos(),
                null,
                new org.viajedragos.consola.RitmoConsola(org.viajedragos.consola.RitmoConsola.Velocidad.INSTANTANEA),
                TextoConsola.sinBotinEquipamiento(),
                TextoConsola.sinBotinEquipamiento(),
                new org.viajedragos.consola.TiempoAventura(),
                TextoConsola.sinBotinEquipamiento()));
        assertEquals(1, lecturas[0]);
        assertEquals(2, selecciones[0]);
        assertEquals(12, dados[0]);
        assertEquals(3, j.getNivel());
        assertEquals(15, j.getExperiencia());
        assertEquals(25, j.getOro());
        assertEquals(0, j.getPuntosPendientes());
        String texto = TextoConsola.contenido(bytes.toString(StandardCharsets.UTF_8));
        assertTrue(texto.contains("Inicial externo — Nivel 1"));
        assertTrue(texto.contains("Avanzado externo — Nivel 3"));
        assertFalse(texto.contains("Bloqueado externo"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void vacioOSinElegiblesNoConsumeEntradaDadosAzarNiConcedeRecompensas(boolean vacio) {
        RepositorioEnemigos repo = () -> vacio ? List.of()
                : List.of(new DefinicionEnemigo("alto", "Alto", "", 1, 0, 1, 0, 0, 1, 15, 3, 2, false));
        Scanner entrada = new Scanner(vacio ? "pendiente\n" : "3\n0\npendiente\n");
        Random prohibido = new Random(0) {
            public int nextInt(int limite) { fail("No hay tiradas sin enemigos elegibles"); return 0; }
        };
        Jugador j = jugador(1);
        assertEquals(vacio, app.jugarAventura(j, entrada, salida(), () -> { fail("No hay combate"); return 1; },
                prohibido, repo, prohibido,
                org.viajedragos.catalogo.ConfiguracionRepositorios.objetos(),
                null,
                new org.viajedragos.consola.RitmoConsola(org.viajedragos.consola.RitmoConsola.Velocidad.INSTANTANEA),
                TextoConsola.sinBotinEquipamiento(),
                TextoConsola.sinBotinEquipamiento(),
                new org.viajedragos.consola.TiempoAventura(),
                TextoConsola.sinBotinEquipamiento()));
        assertEquals("pendiente", entrada.nextLine());
        assertEquals(15, j.getOro());
        assertEquals(0, j.getExperiencia());
        assertEquals(48, j.getVida());
        assertTrue(j.getInventarioEquipo().isEmpty());
        assertTrue(j.getInventarioPociones().isEmpty());
        assertFalse(bytes.toString(StandardCharsets.UTF_8).contains("Comienza el combate"));
        assertTrue(bytes.toString(StandardCharsets.UTF_8).contains(vacio ? "No hay enemigos disponibles" : "No hay enemigos disponibles para ese encuentro"));
    }

    @ParameterizedTest
    @ValueSource(ints = {4, 5, 6})
    void variantesConcedenLaExpDeSusDatosConOroYOfertasExistentes(int indice) {
        var dato = new RepositorioEnemigosMemoria().obtenerTodos().get(indice);
        var definicion = new DefinicionEnemigo(dato.id(), dato.nombre(), dato.descripcion(), dato.fuerza(),
                dato.resistencia(), dato.iniciativa(), dato.agilidad(), dato.velocidad(), dato.vidaMaxima(), dato.experiencia(), dato.nivel());
        Jugador j = jugador(definicion.nivel());
        // Observador de prueba para finalizar sin acoplar la entrega de recompensas a dados concretos.
        j.aprenderHabilidad(new Habilidad("Victoria de prueba") {
            public void alInicioCombate(Combatiente p, Combatiente o, EstadoHabilidad e) { o.setVida(0); }
        }, 1);
        assertFalse(app.jugarAventura(j, new Scanner("3\n2\n0\n"), salida(),
                () -> { fail("Muerto antes de los turnos"); return 1; }, sinBotin(),
                () -> List.of(definicion), new Random(0),
                org.viajedragos.catalogo.ConfiguracionRepositorios.objetos(),
                null,
                new org.viajedragos.consola.RitmoConsola(org.viajedragos.consola.RitmoConsola.Velocidad.INSTANTANEA),
                TextoConsola.sinBotinEquipamiento(),
                TextoConsola.sinBotinEquipamiento(),
                new org.viajedragos.consola.TiempoAventura(),
                TextoConsola.sinBotinEquipamiento()));
        assertEquals(definicion.experiencia(), j.getExperiencia());
        assertEquals(20, j.getOro());
        assertTrue(bytes.toString(StandardCharsets.UTF_8).contains("EXP obtenida: +" + definicion.experiencia()));
    }

    private void comprobarPaneles() {
        assertTrue(bytes.toString(StandardCharsets.UTF_8).contains("┌"));
        bytes.toString(StandardCharsets.UTF_8).lines()
                .filter(l -> l.startsWith("┌") || l.startsWith("├") || l.startsWith("└") || l.startsWith("│"))
                .forEach(l -> assertEquals(50, PanelConsola.anchoVisible(l), l));
    }
}
