package org.viajedragos.catalogo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.arma.*;
import org.viajedragos.modelo.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CatalogoEquipoGeneralTest {
    @Test
    void seisPiezasNuevasYCuatroProductosAnterioresMantienenDatosYPrecios() {
        var objetos = new RepositorioObjetosMemoria();
        var equipos = objetos.obtenerEquipamiento();
        assertEquals(List.of(TipoEquipo.ARMADURA, TipoEquipo.CASCO, TipoEquipo.GUANTES,
                TipoEquipo.BOTAS, TipoEquipo.ANILLO, TipoEquipo.AMULETO), equipos.stream().map(DefinicionEquipo::tipo).toList());
        assertEquals(List.of(Map.of(Atributo.RESISTENCIA, 1), Map.of(Atributo.RESISTENCIA, 1),
                Map.of(Atributo.FUERZA, 1), Map.of(Atributo.AGILIDAD, 1), Map.of(Atributo.INICIATIVA, 1),
                Map.of(Atributo.VIDA, 2)), equipos.stream().map(DefinicionEquipo::bonificaciones).toList());
        var tienda = new RepositorioTiendaMemoria(objetos);
        assertEquals(10, tienda.obtenerTodos().size());
        assertEquals(List.of(5, 10, 12, 20, 8, 8, 10, 8, 10, 10), tienda.obtenerTodos().stream().map(ProductoTienda::precio).toList());
        for (int i = 0; i < 6; i++) assertSame(equipos.get(i), tienda.obtenerTodos().get(i + 4).equipo());
        assertThrows(UnsupportedOperationException.class, equipos::clear);
        assertThrows(UnsupportedOperationException.class, () -> equipos.getFirst().bonificaciones().clear());
        assertEquals(7, TipoEquipo.values().length);
    }

    @Test
    void repositorioAnteriorNoNecesitaImplementarNuevosMetodos() {
        RepositorioObjetos repo = new RepositorioObjetos() {
            public List<DefinicionArma> obtenerArmas() { return new RepositorioObjetosMemoria().obtenerArmas(); }
            public List<DefinicionPocion> obtenerPociones() { return new RepositorioObjetosMemoria().obtenerPociones(); }
        };
        assertTrue(repo.obtenerEquipamiento().isEmpty());
        assertEquals(4, new RepositorioTiendaMemoria(repo).obtenerTodos().size());
        assertEquals(10, ConfiguracionRepositorios.tienda(repo.obtenerArmas(), repo.obtenerPociones(),
                new RepositorioObjetosMemoria().obtenerEquipamiento()).obtenerTodos().size());
    }

    @Test
    void definicionCopiaLosDatosYNoComparteEstadoDePartida() {
        var bonos = new EnumMap<Atributo, Integer>(Atributo.class);
        bonos.put(Atributo.VIDA, 5); bonos.put(Atributo.AGILIDAD, 2);
        var d = new DefinicionEquipo("botas", "Botas", TipoEquipo.BOTAS, bonos);
        bonos.clear();
        var a = d.crearEquipo(); var b = d.crearEquipo();
        a.setNombre("Nombre de un ejemplar");
        assertEquals("Botas", d.nombre()); assertEquals("Botas", b.getNombre());
        assertNotSame(a, b);
        assertEquals(2, d.bonificaciones().size());
        assertEquals("botas", a.getIdDefinicion()); assertEquals("botas", b.getIdDefinicion());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7})
    void rechazaDatosInvalidosSinCrearEjemplares(int caso) {
        assertThrows(RuntimeException.class, () -> {
            switch (caso) {
                case 0 -> new DefinicionEquipo(null, "Nombre", TipoEquipo.ARMA, Map.of());
                case 1 -> new DefinicionEquipo(" ", "Nombre", TipoEquipo.ARMA, Map.of());
                case 2 -> new DefinicionEquipo("id", " ", TipoEquipo.ARMA, Map.of());
                case 3 -> new DefinicionEquipo("id", "Nombre", null, Map.of());
                case 4 -> new DefinicionEquipo("id", "Nombre", TipoEquipo.ARMA, null);
                case 5 -> new DefinicionEquipo("id", "Nombre", TipoEquipo.ARMA, Collections.singletonMap(null, 1));
                case 6 -> new DefinicionEquipo("id", "Nombre", TipoEquipo.ARMA, Collections.singletonMap(Atributo.VIDA, null));
                default -> new DefinicionEquipo("id", "Nombre", TipoEquipo.ARMA, Map.of(Atributo.VIDA, Integer.MIN_VALUE));
            }
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4})
    void validacionEntreCategoriasRechazaDuplicadosYDiferencias(int caso) {
        var objetos = new RepositorioObjetosMemoria();
        var d = objetos.obtenerEquipamiento().getFirst();
        List<DefinicionEquipo> equipos = List.of(d);
        List<ProductoTienda> productos = List.of(new ProductoTienda(d, 8));
        switch (caso) {
            case 0 -> equipos = List.of(d, d);
            case 1 -> equipos = List.of(new DefinicionEquipo(objetos.obtenerArmas().getFirst().id(), "Otra", d.tipo(), Map.of()));
            case 2 -> equipos = List.of(new DefinicionEquipo(objetos.obtenerPociones().getFirst().id(), "Otra", d.tipo(), Map.of()));
            case 3 -> productos = List.of(new ProductoTienda(new DefinicionEquipo(d.id(), d.nombre(), d.tipo(), Map.of(Atributo.RESISTENCIA, 99)), 8));
            default -> productos = List.of(new ProductoTienda(new DefinicionEquipo("ausente", "Otra", d.tipo(), Map.of()), 8));
        }
        var equiposFinales = equipos; var productosFinales = productos;
        assertThrows(IllegalArgumentException.class, () -> RepositorioTienda.validarProductos(productosFinales,
                objetos.obtenerArmas(), objetos.obtenerPociones(), equiposFinales));
    }

    @Test
    void equipoArmaGenericoConservaCompatibilidadConLaClaseArma() {
        var d = new DefinicionEquipo("nueva_arma", "Nueva", TipoEquipo.ARMA, Map.of(Atributo.VIDA, 3));
        assertInstanceOf(Arma.class, d.crearEquipo());
        var producto = new ProductoTienda(d, 11);
        Jugador j = new Jugador();
        assertEquals(ResultadoCompra.COMPRADA, j.comprar(d.id(), () -> List.of(producto)));
        Equipo pieza = j.getInventarioEquipo().getFirst();
        assertEquals(d.id(), pieza.getIdDefinicion());
        assertTrue(j.venderEquipo(pieza, false, () -> List.of(producto)));
        assertEquals(9, j.getOro());
        assertNull(producto.arma(), "El acceso legado solo representa DefinicionArma");
        assertEquals(d, producto.equipo());
    }
}
