package org.viajedragos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.arma.*;
import org.viajedragos.catalogo.*;
import org.viajedragos.consola.PanelConsola;
import org.viajedragos.modelo.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class VentaConsolaTest {
    private final RepositorioTienda tienda = new RepositorioTiendaMemoria();
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private String texto() { return TextoConsola.contenido(bytes.toString(StandardCharsets.UTF_8)); }
    private boolean entrar(Jugador j, String entrada) {
        return assertTimeoutPreemptively(Duration.ofSeconds(2), () -> app.consultarTienda(j,
                new Scanner(entrada), new PrintStream(bytes, true, StandardCharsets.UTF_8), tienda));
    }
    private Equipo espada() { return tienda.buscarPorId("espada_desgastada").orElseThrow().arma().crearArma(); }

    @Test
    void saldoVisibleEnTiendaCompraVentaYTrasAmbasOperaciones() {
        Jugador j = new Jugador();
        assertTrue(entrar(j, "1\n1\n0\n2\n1\n1\n0\n0\n"));
        assertEquals(12, j.getOro());
        assertEquals(0, j.getCantidadPociones(TipoPocion.PEQUENA));
        String t = texto().replaceAll("\\s+", " ");
        assertTrue(t.contains("1. Comprar objetos 2. Vender objetos 0. Volver a exploración"));
        assertTrue(t.contains("COMPRAR OBJETOS Oro disponible: 15 monedas"));
        assertTrue(t.contains("COMPRAR OBJETOS Oro disponible: 10 monedas"));
        assertTrue(t.contains("VENDER OBJETOS Oro disponible: 10 monedas"));
        assertTrue(t.contains("VENDER OBJETOS Oro disponible: 12 monedas"));
        assertTrue(t.contains("TIENDA Oro disponible: 12 monedas"));
        assertTrue(t.contains("Has vendido Poción pequeña por 2 monedas. Oro disponible: 12 monedas."));
        // Los paneles completos conservan el ancho común y no incluyen ANSI.
        assertTrue(bytes.toString(StandardCharsets.UTF_8).contains("┌"));
        bytes.toString(StandardCharsets.UTF_8).lines().filter(l -> l.startsWith("│") || l.startsWith("┌")
                || l.startsWith("├") || l.startsWith("└"))
                .forEach(l -> assertEquals(50, PanelConsola.anchoVisible(l), l));
        assertFalse(bytes.toString(StandardCharsets.UTF_8).contains("\u001B"));
    }

    @Test
    void consumiblesSeVendenIndividualmenteYSeActualizaLaCantidad() {
        Jugador j = new Jugador();
        j.agregarPocion(TipoPocion.GRANDE);
        j.agregarPocion(TipoPocion.GRANDE);
        assertTrue(entrar(j, "2\n1\n1\n1\n1\n0\n0\n"));
        assertEquals(25, j.getOro());
        assertEquals(0, j.getCantidadPociones(TipoPocion.GRANDE));
        assertTrue(texto().contains("Cantidad: 2 - Venta: 5 monedas"));
        assertTrue(texto().contains("Cantidad: 1 - Venta: 5 monedas"));
        assertTrue(texto().contains("No tienes objetos vendibles."));
        assertEquals(2, texto().split("Has vendido", -1).length - 1);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void ventaDeArmaGuardadaOEquipadaSeConfirma(boolean equipada) {
        Jugador j = new Jugador("Prueba", "", 6, 2, 3, 2, 2, 24, 24);
        Equipo arma = espada();
        if (equipada) j.equipar(arma); else j.agregarEquipo(arma);
        assertTrue(entrar(j, "2\n1\n1\n0\n0\n"));
        assertEquals(21, j.getOro());
        assertEquals(6, j.getFuerza());
        assertTrue(j.getInventarioEquipo().isEmpty());
        assertTrue(j.getEquipoEquipado().isEmpty());
        assertEquals(equipada, texto().contains("(equipada)"));
        assertEquals(equipada, texto().contains("Desequipar y vender"));
        assertEquals(equipada, texto().contains("se desequipará y perderás sus efectos"));
        assertTrue(texto().contains("Oro disponible: 21 monedas"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2\n0\n0\n", "2\n1\n0\n0\n", "9\nx\n-1\n2\nx\n8\n1\nx\n9\n0\n0\n"})
    void cancelacionYValidacionesNoCambianInventarioNiSaldo(String entrada) {
        Jugador j = new Jugador();
        var arma = espada();
        j.equipar(arma);
        assertTrue(entrar(j, entrada));
        assertSame(arma, j.getEquipoEquipado().get(TipoEquipo.ARMA));
        assertEquals(15, j.getOro());
        assertEquals(1, j.getFuerza());
        assertFalse(texto().contains("Has vendido"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "2\n", "2\n1\n", "2\n1\nx\n"})
    void finDeEntradaNoVendeNiSeBloquea(String entrada) {
        Jugador j = new Jugador();
        j.agregarPocion(TipoPocion.PEQUENA);
        assertFalse(entrar(j, entrada));
        assertEquals(15, j.getOro());
        assertEquals(1, j.getCantidadPociones(TipoPocion.PEQUENA));
    }

    @Test
    void objetosDesconocidosNoAparecenComoVendibles() {
        Jugador j = new Jugador();
        var desconocida = new Arma("Desconocida", TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 5), Map.of());
        j.agregarEquipo(desconocida);
        assertTrue(entrar(j, "2\n1\n0\n0\n"));
        assertTrue(texto().contains("No tienes objetos vendibles."));
        assertTrue(texto().contains("Entrada inválida"));
        assertFalse(texto().contains("Desconocida"));
        assertEquals(List.of(desconocida), j.getInventarioEquipo());
    }

    @Test
    void venderLaUltimaUnidadNoPermiteVenderlaDeNuevo() {
        Jugador j = new Jugador();
        j.agregarPocion(TipoPocion.PEQUENA);
        assertTrue(entrar(j, "2\n1\n1\n1\n0\n0\n"));
        assertEquals(17, j.getOro());
        assertEquals(1, texto().split("Has vendido", -1).length - 1);
        assertTrue(texto().contains("Entrada inválida"));
    }

    @Test
    void ventaFallidaInformaYConservaElSaldoVisible() {
        Jugador j = new Jugador();
        j.agregarPocion(TipoPocion.PEQUENA);
        j.agregarOro(Integer.MAX_VALUE - 15);
        assertTrue(entrar(j, "2\n1\n1\n0\n0\n"));
        assertEquals(Integer.MAX_VALUE, j.getOro());
        assertEquals(1, j.getCantidadPociones(TipoPocion.PEQUENA));
        assertTrue(texto().contains("No se ha podido vender"));
        assertTrue(texto().contains("Oro disponible: " + j.getOro() + " monedas"));
    }
}
