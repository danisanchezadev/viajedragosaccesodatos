package org.viajedragos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.arma.*;
import org.viajedragos.catalogo.*;
import org.viajedragos.consola.*;
import org.viajedragos.modelo.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EquipoGeneralConsolaTest {
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private PrintStream salida() { return new PrintStream(bytes, true, StandardCharsets.UTF_8); }
    private String texto() { return TextoConsola.contenido(bytes.toString(StandardCharsets.UTF_8)); }
    private Jugador jugador() { return new Jugador("Prueba", "", 6, 2, 3, 2, 2, 20, 30); }

    @ParameterizedTest
    @ValueSource(ints = {4, 5, 6, 7, 8, 9})
    void compraEquipaDesequipaYVendeCadaCategoriaNueva(int indice) {
        Jugador j = jugador(); j.agregarOro(100);
        var tienda = new RepositorioTiendaMemoria();
        var producto = tienda.obtenerTodos().get(indice);
        assertTrue(app.consultarTienda(j, new Scanner("1\n" + (indice + 1) + "\n0\n0\n"), salida(), tienda));
        assertEquals(115 - producto.precio(), j.getOro());
        Equipo equipo = j.getInventarioEquipo().getFirst();
        assertEquals(producto.id(), equipo.getIdDefinicion());
        assertTrue(app.consultarInventario(j, new Scanner("2\n" + (equipo.getTipoEquipo().ordinal() + 1) + "\n1\n1\n0\n"), salida()));
        assertSame(equipo, j.getEquipoEquipado().get(equipo.getTipoEquipo()));
        assertTrue(j.getInventarioEquipo().isEmpty());
        assertEquals(20, j.getVida());
        assertTrue(app.consultarInventario(j, new Scanner("3\n1\n1\n0\n"), salida()));
        assertEquals(List.of(equipo), j.getInventarioEquipo());
        assertTrue(j.getEquipoEquipado().isEmpty());
        assertTrue(app.consultarTienda(j, new Scanner("2\n1\n1\n0\n0\n"), salida(), tienda));
        assertEquals(115 - producto.precio() + producto.precioVenta(), j.getOro());
        assertTrue(j.getInventarioEquipo().isEmpty());
        assertEquals(30, j.getVidaMaxima());
        assertEquals(6, j.getFuerza()); assertEquals(2, j.getResistencia());
        assertTrue(texto().contains("CONFIRMAR DESEQUIPADO"));
        assertTrue(texto().contains("Has vendido " + producto.nombre()));
        if (indice == 9) assertTrue(texto().contains("Vida máxima +2"));
        comprobarPaneles();
    }

    @ParameterizedTest
    @ValueSource(ints = {4, 5, 6, 7, 8, 9})
    void sePuedeVenderDirectamenteCadaPiezaEquipada(int indice) {
        Jugador j = jugador();
        var tienda = new RepositorioTiendaMemoria();
        var producto = tienda.obtenerTodos().get(indice);
        Equipo equipo = producto.equipo().crearEquipo();
        assertTrue(j.equipar(equipo));
        assertTrue(app.consultarTienda(j, new Scanner("2\n1\n1\n0\n0\n"), salida(), tienda));
        assertTrue(j.getEquipoEquipado().isEmpty());
        assertTrue(j.getInventarioEquipo().isEmpty());
        assertEquals(15 + producto.precioVenta(), j.getOro());
        assertEquals(30, j.getVidaMaxima());
        assertEquals(6, j.getFuerza());
        assertTrue(texto().contains("(equipada)"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"3\n0\n0\n", "3\n1\n0\n0\n0\n", "3\nx\n9\n0\n0\n"})
    void volverCancelarEInvalidosNoRetiranLaPieza(String entrada) {
        Jugador j = jugador();
        Equipo pieza = new RepositorioObjetosMemoria().obtenerEquipamiento().getLast().crearEquipo();
        j.equipar(pieza);
        assertTrue(assertTimeoutPreemptively(Duration.ofSeconds(2), () -> app.consultarInventario(j, new Scanner(entrada), salida())));
        assertSame(pieza, j.getEquipoEquipado().get(TipoEquipo.AMULETO));
        assertEquals(32, j.getVidaMaxima()); assertEquals(20, j.getVida());
        assertTrue(j.getInventarioEquipo().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"3\n", "3\n1\n", "3\n1\nx\n"})
    void finDeEntradaNoConfirmaDesequipado(String entrada) {
        Jugador j = jugador();
        Equipo pieza = new RepositorioObjetosMemoria().obtenerEquipamiento().getLast().crearEquipo();
        j.equipar(pieza);
        assertFalse(app.consultarInventario(j, new Scanner(entrada), salida()));
        assertSame(pieza, j.getEquipoEquipado().get(TipoEquipo.AMULETO));
        assertEquals(32, j.getVidaMaxima());
    }

    @Test
    void ejemplaresIgualesSeSeleccionanPorSuIndiceYSeConservaElOtro() {
        Jugador j = jugador();
        var d = new RepositorioObjetosMemoria().obtenerEquipamiento().get(4);
        Equipo uno = d.crearEquipo(), dos = d.crearEquipo();
        j.agregarEquipo(uno); j.agregarEquipo(dos);
        assertTrue(app.consultarInventario(j, new Scanner("2\n6\n2\n1\n0\n"), salida()));
        assertSame(dos, j.getEquipoEquipado().get(TipoEquipo.ANILLO));
        assertEquals(List.of(uno), j.getInventarioEquipo());
        assertTrue(texto().contains("1. Anillo sencillo"));
        assertTrue(texto().contains("2. Anillo sencillo"));
        assertTrue(app.consultarInventario(j, new Scanner("3\n1\n1\n0\n"), salida()));
        assertEquals(List.of(uno, dos), j.getInventarioEquipo());
    }

    @Test
    void repositorioAlternativoOfrecePiezaMultibonificacionSinPausasEnMenus() {
        var base = new RepositorioObjetosMemoria();
        var d = new DefinicionEquipo("amuleto_externo", "Amuleto externo", TipoEquipo.AMULETO,
                Map.of(Atributo.VIDA, 5, Atributo.RESISTENCIA, 3));
        int[] lecturas = {0, 0, 0}, dados = {0}, esperas = {0};
        RepositorioObjetos objetos = new RepositorioObjetos() {
            public List<DefinicionArma> obtenerArmas() { assertEquals(0, lecturas[0]++); return base.obtenerArmas(); }
            public List<DefinicionPocion> obtenerPociones() { assertEquals(0, lecturas[1]++); return base.obtenerPociones(); }
            public List<DefinicionEquipo> obtenerEquipamiento() { assertEquals(0, lecturas[2]++); return List.of(d); }
        };
        RepositorioTienda tienda = () -> List.of(new ProductoTienda(d, 13));
        RepositorioEnemigos enemigos = () -> List.of(new DefinicionEnemigo("prueba", "Prueba", "", 1, 0, 1, 0, 0, 1));
        Jugador j = new Jugador("Prueba", "", 20, 2, 3, 2, 2, 20, 30);
        var ritmo = new RitmoConsola(RitmoConsola.Velocidad.NORMAL, ms -> esperas[0]++);
        String entrada = "1\n2\n5\n1\n1\n0\n0\n1\n2\n7\n1\n1\n0\n5\n2\n1\n1\n0\n0\n0\n";
        assertFalse(app.jugarAventura(j, new Scanner(entrada), salida(), () -> { assertTrue(++dados[0] <= 6); return 4; },
                new Random(0) { public int nextInt(int limite) { return 99; } }, enemigos, new Random(0), objetos, tienda, ritmo,
                new Random(0) { public int nextInt(int limite) { return 99; } }));
        assertArrayEquals(new int[]{1, 1, 1}, lecturas);
        assertEquals(6, dados[0]); assertEquals(6, esperas[0]);
        assertEquals(13, j.getOro()); assertEquals(20, j.getVida()); assertEquals(30, j.getVidaMaxima());
        assertTrue(j.getEquipoEquipado().isEmpty()); assertTrue(j.getInventarioEquipo().isEmpty());
        assertTrue(texto().contains("Has comprado Amuleto externo"));
        assertTrue(texto().contains("Has vendido Amuleto externo"));
        comprobarPaneles();
    }

    @Test
    void sinEquipoSePuedeVolverSinErrores() {
        assertTrue(app.consultarInventario(jugador(), new Scanner("3\n0\n0\n"), salida()));
        assertTrue(texto().contains("No tienes equipo equipado"));
        assertTrue(texto().contains("Amuleto: Sin equipo"));
    }

    private void comprobarPaneles() {
        assertTrue(bytes.toString(StandardCharsets.UTF_8).contains("┌"));
        bytes.toString(StandardCharsets.UTF_8).lines()
                .filter(l -> l.startsWith("┌") || l.startsWith("├") || l.startsWith("└") || l.startsWith("│"))
                .forEach(l -> assertEquals(50, PanelConsola.anchoVisible(l), l));
    }
}
