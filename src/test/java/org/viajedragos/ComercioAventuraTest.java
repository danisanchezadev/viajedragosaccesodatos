package org.viajedragos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.arma.Atributo;
import org.viajedragos.arma.TipoEquipo;
import org.viajedragos.catalogo.*;
import org.viajedragos.combate.TiradorDado;
import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.Habilidad;
import org.viajedragos.modelo.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class ComercioAventuraTest {
    private final ByteArrayOutputStream salida = new ByteArrayOutputStream();
    private PrintStream consola() { return new PrintStream(salida, true, StandardCharsets.UTF_8); }
    private String texto() { return org.viajedragos.TextoConsola.contenido(salida.toString(StandardCharsets.UTF_8)); }
    private Jugador jugador() { return new Jugador("Aventurero", "", 6, 2, 3, 2, 2, 1, 100); }
    private RepositorioEnemigos enemigos() {
        return () -> List.of(new DefinicionEnemigo("prueba", "Enemigo", "", 1, 0, 1, 0, 0, 1));
    }
    private Random sinBotin() {
        return new Random(0) {
            int usos;
            @Override public int nextInt(int limite) {
                assertEquals(100, limite);
                assertTrue(++usos <= 4, "No hay botín adicional por entrar en tiendas");
                return 99;
            }
        };
    }
    private TiradorDado dados(int cantidad) {
        return new TiradorDado() {
            int usados;
            @Override public int tirarDado() {
                assertTrue(++usados <= cantidad, "El comercio no consume dados");
                return 4;
            }
        };
    }
    private boolean aventura(Jugador jugador, String entrada, int tiradas) {
        return assertTimeoutPreemptively(Duration.ofSeconds(3), () -> app.jugarAventura(jugador,
                new Scanner(entrada), consola(), dados(tiradas), sinBotin(), enemigos(), new Random(0)));
    }

    @Test
    void victoriaConcedeCincoYLaTiendaMuestraElSaldoYLosCuatroProductos() {
        Jugador jugador = jugador();
        assertFalse(aventura(jugador, "1\n2\n5\n1\n0\n0\n0\n", 6));
        assertEquals(20, jugador.getOro());
        assertTrue(texto().contains("Oro obtenido: +5 monedas Oro total: 20 monedas"));
        assertTrue(texto().contains("5. Tienda"));
        assertTrue(texto().contains("Oro disponible: 20 monedas"));
        assertTrue(texto().contains("1. Poción pequeña - 5 monedas"));
        assertTrue(texto().contains("2. Poción grande - 10 monedas"));
        assertTrue(texto().contains("3. Espada desgastada - 12 monedas"));
        assertTrue(texto().contains("4. Espada de hierro - 20 monedas"));
        assertTrue(texto().contains("4. Terminar aventura"));
        assertTrue(texto().contains("0. Salir del juego"));
        assertTrue(jugador.getInventarioEquipo().isEmpty());
        assertTrue(jugador.getInventarioPociones().isEmpty());
    }

    @Test
    void comprarConsumirEquiparYContinuarConElMismoPersonaje() {
        Jugador jugador = jugador();
        int[] inicios = {0};
        jugador.aprenderHabilidad(new Habilidad("Observación de continuidad") {
            @Override public void alInicioCombate(Combatiente p, Combatiente o, EstadoHabilidad estado) {
                if (++inicios[0] == 2) {
                    assertSame(jugador, p);
                    assertEquals(9, p.getVida());
                    assertEquals(3, jugador.getOro());
                    assertEquals(7, p.getFuerza());
                    assertEquals(0, jugador.getCantidadPociones(TipoPocion.PEQUENA));
                }
            }
        }, 1);
        String entrada = "1\n2\n5\n1\n1\n3\n0\n0\n2\n1\n1\n2\n1\n1\n1\n0\n3\n1\n2\n0\n";
        assertFalse(aventura(jugador, entrada, 12));
        assertEquals(2, inicios[0]);
        assertEquals(8, jugador.getOro());
        assertEquals(9, jugador.getVida());
        assertEquals(7, jugador.getFuerza());
        assertEquals("Espada desgastada", jugador.getEquipoEquipado().get(TipoEquipo.ARMA).getNombre());
        assertTrue(jugador.getInventarioEquipo().isEmpty());
        assertTrue(texto().contains("Vida recuperada: +8 PS"));
        assertEquals(2, texto().split("Has comprado", -1).length - 1);
        assertEquals(1, texto().split("Has encontrado una Espada desgastada", -1).length - 1);
        assertEquals(1, texto().split("Has encontrado una Espada de hierro", -1).length - 1);
    }

    @Test
    void huirNoConcedeOroNiArmasNiBotinYPermiteIrALaTienda() {
        Jugador jugador = jugador();
        Random prohibido = new Random(0) {
            @Override public int nextInt(int limite) { fail("No hay botín por huida"); return 0; }
        };
        assertFalse(app.jugarAventura(jugador, new Scanner("3\n5\n0\n0\n"), consola(), dados(6),
                prohibido, enemigos(), new Random(0)));
        assertEquals(15, jugador.getOro());
        assertTrue(texto().contains("Oro disponible: 15 monedas"));
        assertFalse(texto().contains("Oro obtenido: +5"));
        assertFalse(texto().contains("Has encontrado una Espada"));
        assertTrue(jugador.getInventarioPociones().isEmpty());
        assertTrue(jugador.getInventarioEquipo().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void derrotaYMuerteSimultaneaNoConcedenOro(boolean simultanea) {
        Jugador jugador = jugador();
        jugador.aprenderHabilidad(new Habilidad("Fin de prueba") {
            @Override public void alInicioCombate(Combatiente p, Combatiente o, EstadoHabilidad estado) {
                p.setVida(0);
                if (simultanea) o.setVida(0);
            }
        }, 1);
        assertTrue(aventura(jugador, "", 0));
        assertEquals(15, jugador.getOro());
        assertFalse(texto().contains("Oro obtenido: +5"));
        assertTrue(jugador.getInventarioPociones().isEmpty());
        assertTrue(jugador.getInventarioEquipo().isEmpty());
    }

    @Test
    void volverAlMenuInicialYNuevaAventuraReiniciaSaldoYPertenencias() {
        app.ejecutar(new Scanner("1\nAventurero\n1\n2\n5\n1\n1\n0\n0\n4\n1\nAventurero\n1\n2\n5\n0\n0\n"), consola(),
                dados(12), sinBotin(), enemigos(), new Random(0));
        assertEquals(2, texto().split("Oro obtenido: \\+5 monedas Oro total: 20 monedas", -1).length - 1);
        assertEquals(1, texto().split("Has comprado", -1).length - 1);
        assertEquals(2, texto().split("Has encontrado una Espada desgastada", -1).length - 1);
        assertTrue(texto().endsWith("Hasta pronto." + System.lineSeparator()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0\n", "texto\n-1\n5\n99999999999999999999999\n\n0\n"})
    void cancelarOEntradasInvalidasNoCompranNiCambianElSaldo(String entrada) {
        Jugador jugador = jugador();
        assertTrue(assertTimeoutPreemptively(Duration.ofSeconds(2), () -> app.consultarTienda(
                jugador, new Scanner(entrada), consola(), new RepositorioTiendaMemoria())));
        assertEquals(15, jugador.getOro());
        assertTrue(jugador.getInventarioPociones().isEmpty());
        assertTrue(jugador.getInventarioEquipo().isEmpty());
        assertFalse(texto().contains("Has comprado"));
        if (!entrada.equals("0\n")) assertEquals(5, texto().split("Entrada inválida", -1).length - 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "1\n1\n"})
    void finDeEntradaEnLaTiendaNoRepiteNiCancelaComprasYaCompletadas(String entrada) {
        Jugador jugador = jugador();
        assertFalse(app.consultarTienda(jugador, new Scanner(entrada), consola(), new RepositorioTiendaMemoria()));
        assertTrue(texto().contains("Entrada finalizada. Hasta pronto."));
        assertEquals(entrada.isEmpty() ? 15 : 10, jugador.getOro());
        assertEquals(entrada.isEmpty() ? 0 : 1, jugador.getCantidadPociones(TipoPocion.PEQUENA));
    }

    @Test
    void compraConSaldoInsuficienteSeInformaYSigueEnLaTienda() {
        Jugador jugador = jugador();
        assertTrue(app.consultarTienda(jugador, new Scanner("1\n4\n0\n0\n"), consola(), new RepositorioTiendaMemoria()));
        assertTrue(texto().contains("No tienes suficiente oro"));
        assertEquals(15, jugador.getOro());
        assertTrue(jugador.getInventarioEquipo().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void catalogoComercialInvalidoNoCompraNiConsumeEntrada(int caso) {
        Jugador jugador = jugador();
        ProductoTienda producto = new RepositorioTiendaMemoria().obtenerTodos().getFirst();
        RepositorioTienda repositorio = switch (caso) {
            case 0 -> () -> null;
            case 1 -> () -> Arrays.asList(producto, null);
            case 2 -> () -> List.of(producto, producto);
            default -> () -> { throw new IllegalStateException("Sin catálogo"); };
        };
        Scanner entrada = new Scanner("pendiente\n");
        assertTrue(app.consultarTienda(jugador, entrada, consola(), repositorio));
        assertEquals("pendiente", entrada.nextLine());
        assertEquals(15, jugador.getOro());
        assertTrue(texto().contains("El catálogo de la tienda no es válido"));
    }

    @Test
    void catalogoVacioPermiteVolver() {
        assertTrue(app.consultarTienda(jugador(), new Scanner("0\n"), consola(), List::of));
        assertTrue(texto().contains("No hay productos disponibles"));
    }

    @Test
    void repositorioComercialAlternativoSeUsaSinCambiarLasFirmasPrevias() {
        var objetos = new RepositorioObjetosMemoria();
        ProductoTienda oferta = new ProductoTienda(objetos.obtenerArmas().get(1), 7);
        RepositorioTienda alternativo = () -> List.of(oferta);
        app.ejecutar(new Scanner("1\nAventurero\n1\n2\n5\n1\n1\n0\n0\n1\n0\n0\n"), consola(), dados(6), sinBotin(),
                enemigos(), new Random(0), objetos, alternativo);
        assertTrue(texto().contains("1. Espada de hierro - 7 monedas"));
        assertTrue(texto().contains("Oro disponible: 13 monedas"));
        assertTrue(texto().contains("EQUIPAMIENTO GUARDADO"));
        assertTrue(texto().contains("FUERZA +2"));
        assertFalse(texto().contains("2. Poción grande - 10 monedas"));
    }

    @Test
    void catalogosSeCarganUnaVezYLaTiendaReutilizaSusDefiniciones() {
        int[] lecturas = {0, 0};
        var pequena = new DefinicionPocion("p", TipoPocion.PEQUENA, "Pequeña del repositorio", 11);
        var grande = new DefinicionPocion("g", TipoPocion.GRANDE, "Grande del repositorio", 21);
        var objetos = new RepositorioObjetos() {
            @Override public List<DefinicionArma> obtenerArmas() {
                assertEquals(0, lecturas[0]++);
                return List.of(new DefinicionArma(DefinicionArma.ID_ESPADA_DESGASTADA, "Desgastada alternativa", Map.of(Atributo.FUERZA, 3)),
                        new DefinicionArma(DefinicionArma.ID_ESPADA_HIERRO, "Hierro alternativo", Map.of(Atributo.FUERZA, 4)));
            }
            @Override public List<DefinicionPocion> obtenerPociones() {
                assertEquals(0, lecturas[1]++);
                return List.of(pequena, grande);
            }
        };
        Jugador jugador = jugador();
        assertFalse(app.jugarAventura(jugador, new Scanner("1\n2\n5\n1\n1\n0\n0\n2\n1\n5\n0\n0\n"), consola(),
                dados(6), sinBotin(), enemigos(), new Random(0), objetos));
        assertEquals(1, lecturas[0]);
        assertEquals(1, lecturas[1]);
        assertSame(pequena, jugador.getDefinicionPocion(TipoPocion.PEQUENA));
        assertEquals(12, jugador.getVida());
        assertEquals(15, jugador.getOro());
        assertTrue(texto().contains("1. Pequeña del repositorio - 5 monedas"));
        assertTrue(texto().contains("Vida recuperada: +11 PS"));
    }
}
