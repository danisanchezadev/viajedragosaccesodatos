package org.viajedragos.catalogo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.arma.Atributo;
import org.viajedragos.modelo.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class IntegracionCatalogosTest {
    private final RepositorioObjetos objetos = new RepositorioObjetosMemoria();

    @Test
    void configuracionCentralConservaLosRepositoriosYDatosPredeterminados() {
        assertInstanceOf(RepositorioEnemigosMemoria.class, ConfiguracionRepositorios.enemigos());
        assertInstanceOf(RepositorioObjetosMemoria.class, ConfiguracionRepositorios.objetos());
        var tienda = ConfiguracionRepositorios.tienda(objetos.obtenerArmas(), objetos.obtenerPociones());
        assertInstanceOf(RepositorioTiendaMemoria.class, tienda);
        assertEquals(List.of(5, 10, 12, 20), tienda.obtenerTodos().stream().map(ProductoTienda::precio).toList());
        assertEquals(List.of(2, 5, 6, 10), tienda.obtenerTodos().stream().map(ProductoTienda::precioVenta).toList());
        assertEquals(new RepositorioEnemigosMemoria().obtenerTodos(), ConfiguracionRepositorios.enemigos().obtenerTodos());
    }

    @Test
    void ofertasInyectadasAdmitenNuevosProductosOrdenYPreciosSinDuplicarDefiniciones() {
        var nueva = new DefinicionArma("lanza", "Lanza de prueba", Map.of(Atributo.FUERZA, 4));
        var armas = new ArrayList<>(objetos.obtenerArmas());
        armas.add(nueva);
        var pocion = objetos.obtenerPociones().getLast();
        var ofertas = new ArrayList<>(List.of(new ProductoTienda(nueva, 31), new ProductoTienda(pocion, 9)));
        var tienda = new RepositorioTiendaMemoria(ofertas, armas, objetos.obtenerPociones());
        ofertas.clear(); armas.clear();
        assertEquals(List.of("lanza", pocion.id()), tienda.obtenerTodos().stream().map(ProductoTienda::id).toList());
        assertSame(nueva, tienda.buscarPorId("lanza").orElseThrow().arma());
        assertSame(pocion, tienda.obtenerTodos().getLast().pocion());
        assertEquals(15, tienda.obtenerTodos().getFirst().precioVenta());
        assertThrows(UnsupportedOperationException.class, () -> tienda.obtenerTodos().clear());
    }

    @Test
    void catalogoComercialVacioNoExigeLosCuatroProductos() {
        var tienda = new RepositorioTiendaMemoria(List.of(), List.of(), List.of());
        assertTrue(tienda.obtenerTodos().isEmpty());
        assertTrue(tienda.buscarPorId("ausente").isEmpty());
    }

    @Test
    void definicionesEquivalentesNoNecesitanLaMismaIdentidadJava() {
        var arma = objetos.obtenerArmas().getFirst();
        var equivalente = new DefinicionArma(arma.id(), arma.nombre(), arma.bonificaciones());
        var producto = new ProductoTienda(equivalente, 19);
        assertNotSame(arma, equivalente);
        assertEquals(List.of(producto), RepositorioTienda.validarProductos(List.of(producto),
                objetos.obtenerArmas(), objetos.obtenerPociones()));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8})
    void validacionComunRechazaNulosDuplicadosDesconocidosYDiferenciasDeContenido(int caso) {
        var arma = objetos.obtenerArmas().getFirst();
        var producto = new ProductoTienda(arma, 12);
        List<ProductoTienda> ofertas = List.of(producto);
        List<DefinicionArma> armas = objetos.obtenerArmas();
        List<DefinicionPocion> pociones = objetos.obtenerPociones();
        switch (caso) {
            case 0 -> ofertas = null;
            case 1 -> ofertas = Arrays.asList(producto, null);
            case 2 -> ofertas = List.of(producto, producto);
            case 3 -> ofertas = List.of(new ProductoTienda(new DefinicionArma("otra", "Otra", Map.of()), 2));
            case 4 -> ofertas = List.of(new ProductoTienda(new DefinicionArma(arma.id(), arma.nombre(), Map.of()), 2));
            case 5 -> armas = List.of(arma, arma);
            case 6 -> pociones = List.of(new DefinicionPocion(arma.id(), TipoPocion.PEQUENA, "Conflicto", 8));
            case 7 -> pociones = List.of(objetos.obtenerPociones().getFirst(), new DefinicionPocion("otra", TipoPocion.PEQUENA, "Otra", 8));
            case 8 -> ofertas = List.of(new ProductoTienda(new DefinicionPocion(
                    objetos.obtenerPociones().getFirst().id(), TipoPocion.PEQUENA, "Otro nombre", 999), 4));
        }
        var ofertasFinales = ofertas;
        var armasFinales = armas;
        var pocionesFinales = pociones;
        assertThrows(RuntimeException.class, () -> RepositorioTienda.validarProductos(ofertasFinales, armasFinales, pocionesFinales));
    }
}
