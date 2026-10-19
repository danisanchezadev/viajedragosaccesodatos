package org.viajedragos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.catalogo.RepositorioEnemigos;
import org.viajedragos.catalogo.RepositorioEnemigosMemoria;
import org.viajedragos.modelo.DefinicionEnemigo;
import org.viajedragos.modelo.Enemigo;
import org.viajedragos.modelo.Jugador;
import org.viajedragos.modelo.Combatiente;
import org.viajedragos.modelo.TipoPocion;
import org.viajedragos.habilidad.Habilidad;
import org.viajedragos.habilidad.EstadoHabilidad;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class CatalogoAventuraTest {
    private final ByteArrayOutputStream salida = new ByteArrayOutputStream();
    private int selecciones;
    private int tiradasBotin;
    private final Random botin = new Random(0) {
        @Override
        public int nextInt(int bound) {
            assertEquals(100, bound, "Seleccionar enemigos no utiliza el azar de botín");
            tiradasBotin++;
            return 0;
        }
    };

    private PrintStream consola() { return new PrintStream(salida, true, StandardCharsets.UTF_8); }
    private String texto() { return org.viajedragos.TextoConsola.contenido(salida.toString(StandardCharsets.UTF_8)); }
    private Jugador jugador() { return new Jugador("Aventurero", "",6,2,3,2,2,48,48); }
    private Random seleccion(int limite, int... indices) {
        return new Random(0) {
            @Override
            public int nextInt(int bound) {
                assertEquals(limite, bound);
                assertTrue(selecciones < indices.length, "Selección adicional inesperada");
                return indices[selecciones++];
            }
        };
    }
    private boolean jugar(Jugador j, RepositorioEnemigos repo, Random azar, String entrada, int... tiradas) {
        int[] usadas = {0};
        boolean volver = assertTimeoutPreemptively(Duration.ofSeconds(3), () -> app.jugarAventura(j,
                new Scanner(entrada), consola(), () -> {
                    assertTrue(usadas[0] < tiradas.length, "Selección no consume dados de combate");
                    return tiradas[usadas[0]++];
                }, botin, repo, azar));
        assertEquals(tiradas.length, usadas[0]);
        return volver;
    }

    @ParameterizedTest
    @ValueSource(ints = {0,1,2,3})
    void cualquieraPuedeSerElPrimerEnemigoYCombatirConElMotorActual(int indice) {
        var repo = new RepositorioEnemigosMemoria();
        var definicion = repo.obtenerTodos().get(indice);
        Jugador j = jugador();
        assertFalse(jugar(j, repo, seleccion(4, indice), "1\n2\n0\n", 1,1,1,1,6,1,6));
        assertEquals(1, selecciones);
        assertEquals(2, tiradasBotin);
        assertEquals(48, j.getVida());
        assertTrue(texto().contains("contra " + definicion.nombre()));
        assertTrue(texto().contains("PS 48/48 PS " + definicion.vidaMaxima() + "/" + definicion.vidaMaxima()));
        assertTrue(texto().contains("Victoria."));
        assertEquals(1, j.getCantidadPociones(TipoPocion.PEQUENA));
        assertTrue(j.getEquipoEquipado().isEmpty(), "La espada no se equipa automáticamente");
    }

    @Test
    void repeticionTrasHuidaCreaInstanciasNuevasYConservaJugadorPremiosYEquipo() {
        Jugador j = jugador();
        List<Combatiente> enemigos = new ArrayList<>();
        j.aprenderHabilidad(new Habilidad("Observación de prueba") {
            @Override
            public void alInicioCombate(Combatiente p, Combatiente o, EstadoHabilidad estado) {
                assertSame(j, p);
                assertEquals(o.getVidaMaxima(), o.getVida());
                assertTrue(o.getHabilidades().isEmpty());
                assertTrue(o.getEquipoEquipado().isEmpty());
                enemigos.add(o);
            }
        },1);
        var original = new ArrayList<>(new RepositorioEnemigosMemoria().obtenerTodos());
        int[] consultas = {0};
        RepositorioEnemigos repo = () -> { consultas[0]++; return original; };
        Random azar = new Random(0) {
            @Override
            public int nextInt(int bound) {
                assertEquals(4, bound);
                original.clear(); // La aventura ya debe tener su instantánea independiente.
                return new int[]{2,2,3}[selecciones++];
            }
        };
        assertFalse(jugar(j, repo, azar, "1\n3\n3\n1\n1\n3\n1\n2\n1\n0\n0\n",
                1,1,1,1,4,2,1,2,1,6,6,1, 1,1,1,1,6,1,6, 1,1,1,1,6,1,6));
        assertEquals(1, consultas[0]);
        assertEquals(3, selecciones);
        assertEquals(List.of("Bandido","Bandido","Esqueleto"), enemigos.stream().map(Combatiente::getNombre).toList());
        assertNotSame(enemigos.get(0), enemigos.get(1));
        assertEquals(6, enemigos.get(0).getVida(), "La instancia abandonada no se reutiliza");
        assertEquals(48, j.getVida());
        assertEquals(7, j.getFuerza());
        assertEquals(4, tiradasBotin);
        assertEquals(2, j.getCantidadPociones(TipoPocion.PEQUENA));
        assertEquals(2, j.getCantidadPociones(TipoPocion.GRANDE));
        assertEquals(1, texto().split("Has encontrado una Espada desgastada", -1).length - 1);
        assertTrue(texto().contains("Has escapado."));
        assertTrue(texto().contains("INVENTARIO"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"vacio","lista nula","entrada nula","duplicado","definicion invalida"})
    void catalogoInvalidoNoIniciaCombateNiConsumeAleatoriedad(String caso) {
        var lobo = new RepositorioEnemigosMemoria().obtenerTodos().getFirst();
        RepositorioEnemigos repo = () -> switch (caso) {
            case "vacio" -> List.of();
            case "lista nula" -> null;
            case "entrada nula" -> Arrays.asList(lobo, null);
            case "duplicado" -> List.of(lobo, lobo);
            default -> List.of(new DefinicionEnemigo("mal","Mal","",1,1,1,1,1,0));
        };
        Jugador j = jugador();
        assertTrue(jugar(j, repo, seleccion(4), ""));
        assertEquals(0, selecciones);
        assertEquals(0, tiradasBotin);
        assertEquals(48, j.getVida());
        assertFalse(texto().contains("Comienza el combate"));
        assertTrue(texto().contains("No se inicia la aventura"));
    }

    @Test
    void repositorioSustituiblePermiteUnCatalogoDistintoSinCambiarSeleccionOMotor() {
        RepositorioEnemigos otro = () -> List.of(new DefinicionEnemigo("prueba","Enemigo de prueba","Descripción",1,0,1,0,0,1));
        assertFalse(jugar(jugador(), otro, seleccion(1,0), "1\n2\n0\n",1,1,1,1,4,2));
        assertTrue(texto().contains("Enemigo de prueba"));
        assertEquals(1, selecciones);
        assertEquals(2, tiradasBotin);
    }

    @Test
    void generadorDeEncuentrosSeConservaEntreAventurasYSeConsultaUnaVezCadaCatalogo() {
        int[] consultas = {0};
        RepositorioEnemigos repo = () -> { consultas[0]++; return new RepositorioEnemigosMemoria().obtenerTodos(); };
        int[] tiradas = {1,1,1,1,6,1,6, 1,1,1,1,6,1,6};
        int[] usadas = {0};
        assertTimeoutPreemptively(Duration.ofSeconds(3), () -> app.ejecutar(
                new Scanner("1\nAventurero\n1\n2\n4\n1\nAventurero\n1\n2\n0\n"), consola(), () -> {
                    assertTrue(usadas[0] < tiradas.length);
                    return tiradas[usadas[0]++];
                },botin,repo,seleccion(4,1,3)));
        assertEquals(tiradas.length, usadas[0]);
        assertEquals(2, selecciones);
        assertEquals(2, consultas[0]);
        assertEquals(4, tiradasBotin);
        assertTrue(texto().contains("contra Goblin"));
        assertTrue(texto().contains("contra Esqueleto"));
        assertEquals(4, texto().split("PS 48/48", -1).length - 1);
    }

    @Test
    void salirSinAventuraNoConsultaRepositorioNiGeneradores() {
        app.ejecutar(new Scanner("0\n"), consola(), () -> { fail("Sin dados"); return 1; },
                botin, () -> { fail("Sin consulta"); return List.of(); }, seleccion(4));
        assertEquals(0, selecciones);
        assertEquals(0, tiradasBotin);
    }

    @Test
    void automaticoFuncionaConTodosLosEnemigosDelRepositorio() {
        for (var d : new RepositorioEnemigosMemoria().obtenerTodos()) {
            var j = jugador();
            while (j.getNivel() < d.nivel()) j.agregarExperiencia((int) j.getExperienciaNecesaria());
            while (j.getPuntosPendientes() > 0) j.asignarPunto(org.viajedragos.arma.Atributo.FUERZA);
            Enemigo e = d.crearEnemigo();
            Random dados = new Random(1234);
            assertEquals(org.viajedragos.combate.ResultadoCombate.ENEMIGO_MUERE,
                    new org.viajedragos.combate.Combate(j,e, () -> dados.nextInt(1,7)).combatir());
            assertTrue(j.getInventarioPociones().isEmpty());
        }
    }
}
