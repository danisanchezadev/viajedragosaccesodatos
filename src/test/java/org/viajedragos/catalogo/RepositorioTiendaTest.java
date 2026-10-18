package org.viajedragos.catalogo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.arma.Atributo;
import org.viajedragos.modelo.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RepositorioTiendaTest {
    @Test
    void cuatroProductosConIdentificadoresUnicosPreciosYDefinicionesReutilizadas() {
        var objetos = new RepositorioObjetosMemoria();
        var tienda = new RepositorioTiendaMemoria(objetos);
        List<ProductoTienda> productos = tienda.obtenerTodos();
        assertEquals(10, productos.size());
        assertEquals(10, productos.stream().map(ProductoTienda::id).distinct().count());
        assertEquals(List.of(5, 10, 12, 20, 8, 8, 10, 8, 10, 10), productos.stream().map(ProductoTienda::precio).toList());
        assertSame(objetos.obtenerPociones().get(0), productos.get(0).pocion());
        assertSame(objetos.obtenerPociones().get(1), productos.get(1).pocion());
        assertSame(objetos.obtenerArmas().get(0), productos.get(2).arma());
        assertSame(objetos.obtenerArmas().get(1), productos.get(3).arma());
        for (ProductoTienda producto : productos) {
            assertSame(producto, tienda.buscarPorId(producto.id()).orElseThrow());
            assertFalse(producto.nombre().isBlank());
        }
        assertTrue(tienda.buscarPorId("no_existe").isEmpty());
        assertTrue(tienda.buscarPorId(null).isEmpty());
        assertThrows(UnsupportedOperationException.class, productos::clear);
    }

    @Test
    void definicionesAlternativasConservanNombresMagnitudesEIdentificadores() {
        var pequena = new DefinicionPocion("p", TipoPocion.PEQUENA, "Pequeña alternativa", 11);
        var grande = new DefinicionPocion("g", TipoPocion.GRANDE, "Grande alternativa", 21);
        var espada = new DefinicionArma(DefinicionArma.ID_ESPADA_DESGASTADA, "Espada alternativa", Map.of(Atributo.FUERZA, 7));
        var hierro = new DefinicionArma(DefinicionArma.ID_ESPADA_HIERRO, "Hierro alternativo", Map.of(Atributo.FUERZA, 9));
        var armas = new ArrayList<>(List.of(hierro, espada));
        var pociones = new ArrayList<>(List.of(grande, pequena));
        var tienda = new RepositorioTiendaMemoria(armas, pociones);
        armas.clear();
        pociones.clear();
        assertSame(pequena, tienda.buscarPorId("p").orElseThrow().pocion());
        assertSame(grande, tienda.buscarPorId("g").orElseThrow().pocion());
        assertSame(espada, tienda.obtenerTodos().get(2).arma());
        assertSame(hierro, tienda.obtenerTodos().get(3).arma());
        assertEquals("Pequeña alternativa", tienda.obtenerTodos().getFirst().nombre());
        assertEquals(7, tienda.obtenerTodos().get(2).arma().crearArma().getBonificaciones().get(Atributo.FUERZA));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void rechazaPreciosInvalidos(int precio) {
        var objetos = new RepositorioObjetosMemoria();
        assertThrows(IllegalArgumentException.class, () -> new ProductoTienda(objetos.obtenerArmas().getFirst(), precio));
        assertThrows(IllegalArgumentException.class, () -> new ProductoTienda(objetos.obtenerPociones().getFirst(), precio));
    }

    @Test
    void productoExigeExactamenteUnaDefinicion() {
        var objetos = new RepositorioObjetosMemoria();
        assertThrows(IllegalArgumentException.class, () -> new ProductoTienda(null, null, 5));
        assertThrows(IllegalArgumentException.class, () -> new ProductoTienda(
                objetos.obtenerArmas().getFirst(), objetos.obtenerPociones().getFirst(), 5));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8})
    void catalogosInvalidosOIncompletosNoCreanTienda(int caso) {
        var objetos = new RepositorioObjetosMemoria();
        List<DefinicionArma> armas = objetos.obtenerArmas();
        List<DefinicionPocion> pociones = objetos.obtenerPociones();
        switch (caso) {
            case 0 -> armas = null;
            case 1 -> pociones = null;
            case 2 -> armas = List.of(armas.getFirst());
            case 3 -> pociones = List.of(pociones.getFirst());
            case 4 -> armas = List.of(armas.getFirst(), armas.getFirst());
            case 5 -> pociones = List.of(pociones.getFirst(), pociones.getFirst(), pociones.get(1));
            case 6 -> pociones = Arrays.asList(pociones.getFirst(), null);
            case 7 -> pociones = List.of(pociones.getFirst(), new DefinicionPocion("otra", TipoPocion.PEQUENA, "Otra", 8));
            case 8 -> pociones = List.of(pociones.getFirst(), new DefinicionPocion(
                    DefinicionArma.ID_ESPADA_DESGASTADA, TipoPocion.GRANDE, "Otra", 16));
        }
        var armasFinales = armas;
        var pocionesFinales = pociones;
        assertThrows(RuntimeException.class, () -> new RepositorioTiendaMemoria(armasFinales, pocionesFinales));
    }

    @Test
    void interfazPermiteBuscarEnOtroRepositorioSinDependenciasDeMemoria() {
        ProductoTienda producto = new RepositorioTiendaMemoria().obtenerTodos().getFirst();
        RepositorioTienda alternativo = () -> List.of(producto);
        assertSame(producto, alternativo.buscarPorId(producto.id()).orElseThrow());
        assertTrue(alternativo.buscarPorId("ausente").isEmpty());
    }
}
