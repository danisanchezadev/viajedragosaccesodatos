package org.viajedragos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.arma.Atributo;
import org.viajedragos.catalogo.*;
import org.viajedragos.consola.*;
import org.viajedragos.habilidad.*;
import org.viajedragos.modelo.*;
import java.io.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class BotinHabilidadesTest {
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private PrintStream salida() { return new PrintStream(bytes); }
    private Jugador jugador() { return new Jugador("Prueba", "", 20, 2, 3, 2, 2, 48, 48); }
    private RitmoConsola ritmo() { return new RitmoConsola(RitmoConsola.Velocidad.INSTANTANEA); }
    private RepositorioEnemigos enemigos() {
        // Repositorio alternativo: el botín no depende de los identificadores del catálogo en memoria.
        return () -> List.of(new DefinicionEnemigo("externo", "Prueba", "", 1, 0, 1, 0, 0, 1));
    }
    private Random ninguno() { return new Random(0) { public int nextInt(int limite) { return limite - 1; } }; }
    private static class Tiradas extends Random {
        final int[] valores; final List<Integer> limites = new ArrayList<>(); int usados;
        Tiradas(int... valores) { this.valores = valores; }
        @Override public int nextInt(int limite) {
            limites.add(limite); assertTrue(usados < valores.length, "Tirada inesperada");
            int valor = valores[usados++]; assertTrue(valor >= 0 && valor < limite); return valor;
        }
        void agotadas() { assertEquals(valores.length, usados); }
    }

    @Test void cincoPorCientoExactoYSeleccionUniformeSinExcluirLasConocidas() {
        int[] cantidades = new int[HabilidadDisponible.values().length]; int exitos = 0;
        for (int tirada = 0; tirada < 100; tirada++) {
            for (var tipo : HabilidadDisponible.values()) {
                Jugador j = jugador(); j.aprenderHabilidad(tipo.crearHabilidad(), 2);
                var azar = tirada < 5 ? new Tiradas(tirada, tipo.ordinal(), 99) : new Tiradas(tirada, 99);
                app.obtenerPergaminosHabilidades(j, azar, salida(), ritmo()); azar.agotadas();
                assertEquals(tirada < 5 ? List.of(100, 13, 100) : List.of(100, 100), azar.limites);
                if (tirada < 5) {
                    exitos++; cantidades[tipo.ordinal()] += j.getCantidadPergaminosHabilidad(tipo);
                    assertEquals(1, j.getPergaminosHabilidad().values().stream().mapToInt(Integer::intValue).sum());
                } else assertTrue(j.getPergaminosHabilidad().isEmpty());
                assertEquals(0, j.getCantidadPergaminosMaestria());
                assertEquals(2, j.getHabilidades().get(tipo.crearHabilidad()).getNivel());
            }
        }
        assertEquals(65, exitos); // 65 / 1300 = 5 %; exactamente cinco por habilidad.
        for (int cantidad : cantidades) assertEquals(5, cantidad);
    }

    @Test void dosPorCientoExactoParaMaestria() {
        int exitos = 0;
        for (int tirada = 0; tirada < 100; tirada++) {
            Jugador j = jugador(); var azar = new Tiradas(99, tirada);
            app.obtenerPergaminosHabilidades(j, azar, salida(), ritmo()); azar.agotadas();
            assertEquals(tirada < 2 ? 1 : 0, j.getCantidadPergaminosMaestria());
            exitos += j.getCantidadPergaminosMaestria(); assertTrue(j.getPergaminosHabilidad().isEmpty());
        }
        assertEquals(2, exitos);
    }

    @ParameterizedTest @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7})
    void lasOchoCombinacionesSonIndependientesSinCambiarOtrosGeneradores(int combinacion) {
        Jugador j = jugador(); boolean atributo = (combinacion & 1) != 0;
        boolean habilidad = (combinacion & 2) != 0, maestria = (combinacion & 4) != 0;
        var atributos = atributo ? new Tiradas(9, 5) : new Tiradas(10);
        var habilidades = habilidad ? new Tiradas(4, 8, maestria ? 1 : 2) : new Tiradas(5, maestria ? 1 : 2);
        var pociones = new Tiradas(0, 0); int[] dados = {0};
        assertFalse(app.jugarAventura(j, new Scanner("1\n2\n0\n"), salida(), () -> { dados[0]++; return 4; },
                pociones, enemigos(), new Random(0), new RepositorioObjetosMemoria(), null, ritmo(), atributos, habilidades));
        atributos.agotadas(); habilidades.agotadas(); pociones.agotadas(); assertEquals(6, dados[0]);
        assertEquals(atributo ? 1 : 0, j.getCantidadPergaminos(Atributo.VIDA));
        assertEquals(habilidad ? 1 : 0, j.getCantidadPergaminosHabilidad(HabilidadDisponible.INSTINTO_SUPERVIVENCIA));
        assertEquals(maestria ? 1 : 0, j.getCantidadPergaminosMaestria());
        assertEquals(1, j.getCantidadPociones(TipoPocion.PEQUENA)); assertEquals(1, j.getCantidadPociones(TipoPocion.GRANDE));
        assertEquals(20, j.getOro()); assertEquals(5, j.getExperiencia()); assertTrue(j.getHabilidades().isEmpty());
        assertTrue(bytes.toString().contains("Has encontrado una Espada desgastada"));
    }

    @ParameterizedTest @ValueSource(strings = {"derrota", "simultanea", "huida", "incompleto"})
    void noSeTiraNingunaRecompensaSinVictoria(String caso) {
        Jugador j = jugador();
        if (caso.equals("derrota") || caso.equals("simultanea")) j.aprenderHabilidad(new Habilidad("Prueba") {
            @Override public void alInicioCombate(Combatiente p, Combatiente o, EstadoHabilidad e) {
                p.setVida(0); if (caso.equals("simultanea")) o.setVida(0);
            }
        }, 1);
        Random prohibido = new Random(0) { public int nextInt(int limite) { fail("No debe haber recompensa"); return 0; } };
        app.jugarAventura(j, new Scanner(caso.equals("huida") ? "3\n0\n" : ""), salida(), () -> 4,
                prohibido, enemigos(), new Random(0), new RepositorioObjetosMemoria(), null, ritmo(), prohibido, prohibido);
        assertTrue(j.getPergaminosHabilidad().isEmpty()); assertEquals(0, j.getCantidadPergaminosMaestria());
        assertEquals(15, j.getOro()); assertEquals(0, j.getExperiencia());
    }

    @Test void seAcumulanEntreEncuentrosConUnSoloGeneradorYMaximoUnaUnidadPorTipo() {
        Jugador j = jugador(); var nuevos = new Tiradas(0, 0, 0, 0, 0, 0);
        assertFalse(app.jugarAventura(j, new Scanner("1\n2\n3\n1\n2\n0\n"), salida(), () -> 4,
                ninguno(), enemigos(), new Random(0), new RepositorioObjetosMemoria(), null, ritmo(), ninguno(), nuevos));
        nuevos.agotadas(); assertEquals(2, j.getCantidadPergaminosHabilidad(HabilidadDisponible.CAPARAZON));
        assertEquals(2, j.getCantidadPergaminosMaestria());
        assertEquals(25, j.getOro()); assertEquals(10, j.getExperiencia());
    }

    @Test void mismoGeneradorEnAventurasNuevasEInventariosIndependientes() {
        var nuevos = new Tiradas(99, 99, 0, 0, 0);
        app.ejecutar(new Scanner("1\nAventurero\n1\n2\n4\n1\nAventurero\n1\n2\n0\n"), salida(), () -> 4,
                ninguno(), enemigos(), new Random(0), new RepositorioObjetosMemoria(), null, ritmo(), ninguno(), nuevos);
        nuevos.agotadas(); String texto = bytes.toString();
        assertEquals(1, texto.split("Has obtenido un Pergamino de habilidad:", -1).length - 1);
        assertEquals(1, texto.split("Has obtenido un Pergamino de maestría", -1).length - 1);
    }

    @Test void victoriaConSubidaRecompensaAntesDeTerminarEntradaYNoAprendeAutomaticamente() {
        Jugador j = jugador(); var nuevos = new Tiradas(0, 1, 0);
        app.jugarAventura(j, new Scanner("1\n"), salida(), () -> 4, ninguno(),
                () -> List.of(new DefinicionEnemigo("prueba", "Prueba", "", 1, 0, 1, 0, 0, 1, 20)),
                new Random(0), new RepositorioObjetosMemoria(), null, ritmo(), ninguno(), nuevos);
        nuevos.agotadas(); assertEquals(2, j.getNivel()); assertEquals(2, j.getPuntosPendientes());
        assertEquals(1, j.getCantidadPergaminosHabilidad(HabilidadDisponible.CASTIGO_AL_TORPE));
        assertEquals(1, j.getCantidadPergaminosMaestria()); assertTrue(j.getHabilidades().isEmpty());
    }

    @ParameterizedTest @ValueSource(ints = {0, 1, 2, 3})
    void soloPausaYAnunciaRecompensasRealmenteObtenidas(int resultado) {
        Jugador j = jugador(); boolean habilidad = (resultado & 1) != 0, maestria = (resultado & 2) != 0;
        var azar = habilidad ? new Tiradas(0, 0, maestria ? 0 : 99) : new Tiradas(99, maestria ? 0 : 99);
        int[] esperas = {0};
        app.obtenerPergaminosHabilidades(j, azar, salida(), new RitmoConsola(RitmoConsola.Velocidad.NORMAL, ms -> esperas[0]++));
        assertEquals((habilidad ? 1 : 0) + (maestria ? 1 : 0), esperas[0]);
        assertEquals(habilidad, bytes.toString().contains("Pergamino de habilidad: Caparazón"));
        assertEquals(maestria, bytes.toString().contains("Pergamino de maestría")); azar.agotadas();
    }
}
