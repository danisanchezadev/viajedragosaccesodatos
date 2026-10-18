package org.viajedragos.modelo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.arma.Arma;
import org.viajedragos.arma.Atributo;
import org.viajedragos.arma.Equipo;
import org.viajedragos.arma.TipoEquipo;
import org.viajedragos.catalogo.RepositorioTienda;
import org.viajedragos.catalogo.RepositorioTiendaMemoria;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class OroCompraTest {
    private final RepositorioTienda tienda = new RepositorioTiendaMemoria();

    @Test
    void saldoInicialSeConservaEnElPersonajeYUnJugadorNuevoEmpiezaConQuince() {
        Jugador jugador = new Jugador();
        assertEquals(15, jugador.getOro());
        jugador.agregarOro(5);
        assertEquals(20, jugador.getOro());
        assertTrue(jugador.gastarOro(12));
        assertEquals(8, jugador.getOro());
        assertFalse(jugador.gastarOro(9));
        assertEquals(8, jugador.getOro());
        assertTrue(jugador.gastarOro(8));
        assertEquals(0, jugador.getOro());
        assertFalse(jugador.gastarOro(1));
        assertEquals(0, jugador.getOro());
        assertEquals(15, new Jugador().getOro());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void cantidadesInvalidasNoAlteranElSaldo(int cantidad) {
        Jugador jugador = new Jugador();
        assertThrows(IllegalArgumentException.class, () -> jugador.agregarOro(cantidad));
        assertThrows(IllegalArgumentException.class, () -> jugador.gastarOro(cantidad));
        assertEquals(15, jugador.getOro());
    }

    @Test
    void desbordarElSaldoSeRechazaSinConvertirloEnNegativo() {
        Jugador jugador = new Jugador();
        jugador.agregarOro(Integer.MAX_VALUE - 15);
        assertThrows(ArithmeticException.class, () -> jugador.agregarOro(1));
        assertEquals(Integer.MAX_VALUE, jugador.getOro());
    }

    @ParameterizedTest
    @CsvSource({"pocion_pequena,5", "pocion_grande,10", "espada_desgastada,12", "espada_hierro,20"})
    void compraCadaProductoConSaldoExactoSinEquiparNiModificarAtributos(String id, int precio) {
        Jugador jugador = new Jugador("Prueba", "", 6, 2, 3, 2, 2, 1, 100);
        jugador.gastarOro(15);
        jugador.agregarOro(precio);
        ProductoTienda producto = tienda.buscarPorId(id).orElseThrow();
        assertEquals(ResultadoCompra.COMPRADA, jugador.comprar(id, tienda));
        assertEquals(0, jugador.getOro());
        assertEquals(1, jugador.getVida());
        assertEquals(6, jugador.getFuerza());
        assertTrue(jugador.getEquipoEquipado().isEmpty());
        assertTrue(jugador.getHabilidades().isEmpty());
        if (producto.pocion() != null) {
            assertEquals(Map.of(producto.pocion().tipo(), 1), jugador.getInventarioPociones());
            assertTrue(jugador.getInventarioEquipo().isEmpty());
        } else {
            assertTrue(jugador.getInventarioPociones().isEmpty());
            assertEquals(1, jugador.getInventarioEquipo().size());
            Equipo arma = jugador.getInventarioEquipo().getFirst();
            assertEquals(producto.arma().nombre(), arma.getNombre());
            assertEquals(producto.arma().bonificaciones(), arma.getBonificaciones());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"pocion_pequena", "pocion_grande", "espada_desgastada", "espada_hierro"})
    void comprasSucesivasAcumulanYConservanLasInstanciasAnteriores(String id) {
        Jugador jugador = new Jugador();
        jugador.agregarOro(100);
        Equipo anterior = new Arma("Anterior", TipoEquipo.ARMA, Map.of(), Map.of());
        jugador.agregarEquipo(anterior);
        jugador.agregarPocion(TipoPocion.PEQUENA);
        jugador.agregarPocion(TipoPocion.GRANDE);
        ProductoTienda producto = tienda.buscarPorId(id).orElseThrow();
        assertEquals(ResultadoCompra.COMPRADA, jugador.comprar(id, tienda));
        assertEquals(ResultadoCompra.COMPRADA, jugador.comprar(id, tienda));
        assertEquals(115 - 2 * producto.precio(), jugador.getOro());
        assertSame(anterior, jugador.getInventarioEquipo().getFirst());
        if (producto.arma() != null) {
            assertEquals(3, jugador.getInventarioEquipo().size());
            assertNotSame(jugador.getInventarioEquipo().get(1), jugador.getInventarioEquipo().get(2));
            assertEquals(1, jugador.getCantidadPociones(TipoPocion.PEQUENA));
            assertEquals(1, jugador.getCantidadPociones(TipoPocion.GRANDE));
        } else {
            assertEquals(3, jugador.getCantidadPociones(producto.pocion().tipo()));
            TipoPocion otra = producto.pocion().tipo() == TipoPocion.PEQUENA ? TipoPocion.GRANDE : TipoPocion.PEQUENA;
            assertEquals(1, jugador.getCantidadPociones(otra));
            assertEquals(List.of(anterior), jugador.getInventarioEquipo());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"pocion_pequena", "pocion_grande", "espada_desgastada", "espada_hierro"})
    void saldoInsuficienteNoCambiaNada(String id) {
        Jugador jugador = new Jugador();
        jugador.gastarOro(15);
        jugador.agregarOro(tienda.buscarPorId(id).orElseThrow().precio() - 1);
        int oro = jugador.getOro();
        assertEquals(ResultadoCompra.SALDO_INSUFICIENTE, jugador.comprar(id, tienda));
        assertEquals(oro, jugador.getOro());
        assertTrue(jugador.getInventarioEquipo().isEmpty());
        assertTrue(jugador.getInventarioPociones().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"inexistente", "", "  "})
    void identificadorInexistenteNoDescuenta(String id) {
        Jugador jugador = new Jugador();
        assertEquals(ResultadoCompra.PRODUCTO_INEXISTENTE, jugador.comprar(id, tienda));
        assertEquals(15, jugador.getOro());
        assertTrue(jugador.getInventarioEquipo().isEmpty());
        assertTrue(jugador.getInventarioPociones().isEmpty());
    }

    @Test
    void definicionDePocionIncompatibleNoSeVendeConOtrosEfectos() {
        Jugador jugador = new Jugador();
        ProductoTienda otra = new ProductoTienda(new DefinicionPocion(
                "pocion_pequena", TipoPocion.PEQUENA, "Otra", 99), 1);
        assertEquals(ResultadoCompra.PRODUCTO_INVALIDO, jugador.comprar(otra.id(), () -> List.of(otra)));
        assertEquals(15, jugador.getOro());
        assertTrue(jugador.getInventarioPociones().isEmpty());
    }

    @Test
    void pocionDeRepositorioAlternativoSeCompraYConsumeConSuDefinicionReal() {
        var pociones = List.of(new DefinicionPocion("p", TipoPocion.PEQUENA, "Pequeña alternativa", 11),
                new DefinicionPocion("g", TipoPocion.GRANDE, "Grande alternativa", 21));
        Jugador jugador = new Jugador("Prueba", "", 6, 2, 3, 2, 2, 1, 100, pociones);
        ProductoTienda producto = new ProductoTienda(pociones.getFirst(), 3);
        assertEquals(ResultadoCompra.COMPRADA, jugador.comprar("p", () -> List.of(producto)));
        assertEquals(12, jugador.getOro());
        assertEquals(11, jugador.usarPocion(TipoPocion.PEQUENA));
        assertEquals(12, jugador.getVida());
        assertEquals(0, jugador.getCantidadPociones(TipoPocion.PEQUENA));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void repositorioInvalidoOIncoherenteSeRechazaSinEfectos(int caso) {
        Jugador jugador = new Jugador();
        RepositorioTienda repositorio = switch (caso) {
            case 0 -> null;
            case 1 -> () -> null;
            case 2 -> () -> { throw new IllegalStateException("Lectura fallida"); };
            default -> new RepositorioTienda() {
                public List<ProductoTienda> obtenerTodos() { return tienda.obtenerTodos(); }
                public Optional<ProductoTienda> buscarPorId(String id) {
                    return tienda.buscarPorId("pocion_grande");
                }
            };
        };
        assertEquals(ResultadoCompra.PRODUCTO_INVALIDO, jugador.comprar("pocion_pequena", repositorio));
        assertEquals(15, jugador.getOro());
        assertTrue(jugador.getInventarioEquipo().isEmpty());
        assertTrue(jugador.getInventarioPociones().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"pocion_pequena,true", "pocion_pequena,false", "espada_desgastada,true", "espada_desgastada,false"})
    void falloDeIncorporacionAntesODespuesDeAgregarRestauraSaldoEInventarios(String id, boolean parcial) {
        Jugador jugador = new Jugador() {
            private boolean fallar;
            @Override public void agregarEquipo(Equipo equipo) {
                if (!fallar) { super.agregarEquipo(equipo); fallar = true; return; }
                if (parcial) super.agregarEquipo(equipo);
                throw new IllegalStateException("Incorporación fallida");
            }
            @Override public void agregarPocion(TipoPocion tipo) {
                if (parcial) super.agregarPocion(tipo);
                throw new IllegalStateException("Incorporación fallida");
            }
        };
        Equipo anterior = new Arma("Anterior", TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 1), Map.of());
        jugador.agregarEquipo(anterior);
        assertEquals(ResultadoCompra.INCORPORACION_FALLIDA, jugador.comprar(id, tienda));
        assertEquals(15, jugador.getOro());
        assertEquals(List.of(anterior), jugador.getInventarioEquipo());
        assertSame(anterior, jugador.getInventarioEquipo().getFirst());
        assertTrue(jugador.getInventarioPociones().isEmpty());
        assertTrue(jugador.getEquipoEquipado().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"pocion_pequena", "espada_desgastada"})
    void incorporacionQueNoHaceNadaNoPuedeCobrar(String id) {
        Jugador jugador = new Jugador() {
            @Override public void agregarPocion(TipoPocion tipo) { }
            @Override public void agregarEquipo(Equipo equipo) { }
        };
        assertEquals(ResultadoCompra.INCORPORACION_FALLIDA, jugador.comprar(id, tienda));
        assertEquals(15, jugador.getOro());
        assertTrue(jugador.getInventarioEquipo().isEmpty());
        assertTrue(jugador.getInventarioPociones().isEmpty());
    }

    @Test
    void falloAlCobrarTrasIncorporarRestauraTambienElObjeto() {
        Jugador jugador = new Jugador() {
            @Override public boolean gastarOro(int cantidad) {
                super.gastarOro(cantidad);
                return false;
            }
        };
        assertEquals(ResultadoCompra.INCORPORACION_FALLIDA, jugador.comprar("pocion_pequena", tienda));
        assertEquals(15, jugador.getOro());
        assertTrue(jugador.getInventarioPociones().isEmpty());
    }
}
