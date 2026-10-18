package org.viajedragos.modelo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.arma.*;
import org.viajedragos.catalogo.*;
import org.viajedragos.habilidad.implementacion.InstintoSupervivencia;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class VentaObjetosTest {
    private final RepositorioTienda tienda = new RepositorioTiendaMemoria();
    private Jugador jugador() { return new Jugador("Prueba", "", 6, 2, 3, 2, 2, 1, 100); }
    private DefinicionArma espada() { return tienda.buscarPorId("espada_desgastada").orElseThrow().arma(); }

    @ParameterizedTest
    @CsvSource({"1,0", "5,2", "10,5", "12,6", "20,10", "2147483647,1073741823"})
    void precioCentralizadoConDivisionEntera(int compra, int venta) {
        var producto = new ProductoTienda(espada(), compra);
        assertEquals(compra, producto.precio());
        assertEquals(venta, producto.precioVenta());
    }

    @ParameterizedTest
    @CsvSource({"PEQUENA,2", "GRANDE,5"})
    void vendeUnaPocionCadaVezSinCurarse(TipoPocion tipo, int precio) {
        Jugador j = jugador();
        j.agregarPocion(tipo);
        j.agregarPocion(tipo);
        assertTrue(j.venderPocion(tipo, tienda));
        assertEquals(1, j.getCantidadPociones(tipo));
        assertEquals(15 + precio, j.getOro());
        assertEquals(1, j.getVida());
        assertTrue(j.venderPocion(tipo, tienda));
        assertFalse(j.venderPocion(tipo, tienda));
        assertEquals(15 + 2 * precio, j.getOro());
        assertTrue(j.getInventarioPociones().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void vendeArmaGuardadaOEquipadaUnaSolaVez(boolean equipada) {
        Jugador j = jugador();
        var arma = espada().crearArma();
        var otra = espada().crearArma();
        j.agregarEquipo(otra);
        if (equipada) assertTrue(j.equipar(arma));
        else j.agregarEquipo(arma);
        assertTrue(j.venderEquipo(arma, equipada, tienda));
        assertEquals(21, j.getOro());
        assertEquals(6, j.getFuerza());
        assertEquals(List.of(otra), j.getInventarioEquipo());
        assertTrue(j.getEquipoEquipado().isEmpty());
        assertFalse(j.venderEquipo(arma, equipada, tienda));
        assertFalse(j.venderEquipo(arma, !equipada, tienda));
        assertEquals(21, j.getOro());
    }

    @Test
    void ubicacionIncorrectaONoPoseidaNoVende() {
        Jugador j = jugador();
        var arma = espada().crearArma();
        assertFalse(j.venderEquipo(arma, false, tienda));
        assertFalse(j.venderEquipo(arma, true, tienda));
        j.agregarEquipo(arma);
        assertFalse(j.venderEquipo(arma, true, tienda));
        assertEquals(List.of(arma), j.getInventarioEquipo());
        assertTrue(j.equiparDesdeInventario(TipoEquipo.ARMA, 0));
        assertFalse(j.venderEquipo(arma, false, tienda));
        assertSame(arma, j.getEquipoEquipado().get(TipoEquipo.ARMA));
        assertEquals(7, j.getFuerza());
        assertEquals(15, j.getOro());
        assertFalse(j.venderPocion(null, tienda));
        assertFalse(j.venderEquipo(null, false, tienda));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void noValoraArmasDesconocidasOConEfectosDiferentes(int caso) {
        Jugador j = jugador();
        Equipo arma = switch (caso) {
            case 0 -> new Arma("Desconocida", TipoEquipo.ARMA, espada().bonificaciones(), Map.of());
            case 1 -> new Arma(espada().nombre(), TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 99), Map.of());
            case 2 -> new Arma(espada().nombre(), TipoEquipo.ARMA, espada().bonificaciones(), Map.of(new InstintoSupervivencia(), 1));
            default -> new Arma(espada().nombre(), TipoEquipo.values()[1], espada().bonificaciones(), Map.of());
        };
        j.agregarEquipo(arma);
        assertFalse(j.venderEquipo(arma, false, tienda));
        assertEquals(List.of(arma), j.getInventarioEquipo());
        assertEquals(15, j.getOro());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4})
    void catalogoInvalidoOAmbiguoNoRetiraObjetos(int caso) {
        Jugador j = jugador();
        j.agregarPocion(TipoPocion.PEQUENA);
        var p = tienda.obtenerTodos().getFirst();
        RepositorioTienda repo = switch (caso) {
            case 0 -> () -> null;
            case 1 -> () -> Arrays.asList(p, null);
            case 2 -> () -> List.of(p, p);
            case 3 -> () -> { throw new IllegalStateException(); };
            default -> new RepositorioTienda() {
                public List<ProductoTienda> obtenerTodos() { return List.of(p); }
                public Optional<ProductoTienda> buscarPorId(String id) { return Optional.empty(); }
            };
        };
        assertFalse(j.venderPocion(TipoPocion.PEQUENA, repo));
        assertEquals(1, j.getCantidadPociones(TipoPocion.PEQUENA));
        assertEquals(15, j.getOro());
    }

    @Test
    void definicionesAlternativasUsanSusPreciosYNoLosPredeterminados() {
        Jugador j = jugador();
        var p = new DefinicionPocion("alternativa", TipoPocion.PEQUENA, "Tónico", 11);
        var grande = j.getDefinicionPocion(TipoPocion.GRANDE);
        j.configurarPociones(List.of(p, grande));
        j.agregarPocion(TipoPocion.PEQUENA);
        assertFalse(j.venderPocion(TipoPocion.PEQUENA, tienda));
        assertTrue(j.venderPocion(TipoPocion.PEQUENA, () -> List.of(new ProductoTienda(p, 17))));
        assertEquals(23, j.getOro());
        var arma = espada().crearArma();
        j.agregarEquipo(arma);
        assertTrue(j.venderEquipo(arma, false, () -> List.of(new ProductoTienda(espada(), 9))));
        assertEquals(27, j.getOro());
    }

    @Test
    void dosDefinicionesDeArmaIndistinguiblesSeRechazan() {
        Jugador j = jugador();
        var arma = espada().crearArma();
        j.agregarEquipo(arma);
        var alternativa = new DefinicionArma("otra", espada().nombre(), espada().bonificaciones());
        assertFalse(j.venderEquipo(arma, false, () -> List.of(
                new ProductoTienda(espada(), 12), new ProductoTienda(alternativa, 30))));
        assertEquals(15, j.getOro());
        assertEquals(List.of(arma), j.getInventarioEquipo());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void desbordamientoNoRetiraPocionesNiArmas(boolean equipada) {
        Jugador j = jugador();
        j.agregarOro(Integer.MAX_VALUE - 15);
        j.agregarPocion(TipoPocion.PEQUENA);
        var arma = espada().crearArma();
        if (equipada) j.equipar(arma); else j.agregarEquipo(arma);
        assertFalse(j.venderPocion(TipoPocion.PEQUENA, tienda));
        assertFalse(j.venderEquipo(arma, equipada, tienda));
        assertEquals(1, j.getCantidadPociones(TipoPocion.PEQUENA));
        assertEquals(Integer.MAX_VALUE, j.getOro());
        assertEquals(equipada ? 7 : 6, j.getFuerza());
        if (equipada) assertSame(arma, j.getEquipoEquipado().get(TipoEquipo.ARMA));
        else assertEquals(List.of(arma), j.getInventarioEquipo());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void falloDeRetiradaRestauraAtributosVidaHabilidadesYEstado(boolean excepcion) {
        Jugador j = jugador();
        var habilidad = new InstintoSupervivencia();
        j.aprenderHabilidad(habilidad, 1);
        var estado = j.getHabilidades().get(habilidad);
        j.curarse(8);
        int fuerza = j.getFuerza();
        var arma = new Arma(espada().nombre(), TipoEquipo.ARMA, espada().bonificaciones(), Map.of()) {
            @Override public void retirarEfectos(Combatiente c) {
                super.retirarEfectos(c);
                c.setVida(0);
                c.setResistencia(20);
                estado.setEfectoTemporalActivo(false);
                c.getHabilidades().clear();
                if (excepcion) throw new IllegalStateException("Fallo parcial");
            }
        };
        j.equipar(arma);
        // El segundo caso simula una operación que devuelve false tras retirar efectos.
        Jugador objetivo = j;
        if (!excepcion) {
            objetivo = new Jugador("Prueba", "", 6, 2, 3, 2, 2, 9, 100) {
                @Override public boolean desequipar(TipoEquipo tipo) {
                    super.desequipar(tipo);
                    return false;
                }
            };
            objetivo.aprenderHabilidad(habilidad, 1);
            objetivo.equipar(espada().crearArma());
        }
        var equipado = objetivo.getEquipoEquipado().get(TipoEquipo.ARMA);
        int fuerzaAntes = objetivo.getFuerza();
        var estados = new HashMap<>(objetivo.getHabilidades());
        assertFalse(objetivo.venderEquipo(equipado, true, tienda));
        assertSame(equipado, objetivo.getEquipoEquipado().get(TipoEquipo.ARMA));
        assertEquals(fuerzaAntes, objetivo.getFuerza());
        assertEquals(2, objetivo.getResistencia());
        assertEquals(9, objetivo.getVida());
        assertEquals(15, objetivo.getOro());
        assertEquals(estados, objetivo.getHabilidades());
        assertTrue(objetivo.getInventarioEquipo().isEmpty());
        if (excepcion) {
            assertSame(estado, j.getHabilidades().get(habilidad));
            assertTrue(estado.isEfectoTemporalActivo());
            assertEquals(fuerza + 1, j.getFuerza());
        }
    }

    @Test
    void venderNoLimpiaHabilidadAprendidaNiLaBonificacionDeOtraPieza() {
        Jugador j = jugador();
        var habilidad = new InstintoSupervivencia();
        j.aprenderHabilidad(habilidad, 1);
        j.curarse(8);
        var estado = j.getHabilidades().get(habilidad);
        int fuerza = j.getFuerza();
        var arma = espada().crearArma();
        var armadura = new Arma("Otra pieza", TipoEquipo.ARMADURA, Map.of(Atributo.FUERZA, 2), Map.of());
        j.equipar(armadura);
        j.equipar(arma);
        assertTrue(j.venderEquipo(arma, true, tienda));
        assertEquals(fuerza + 2, j.getFuerza());
        assertSame(armadura, j.getEquipoEquipado().get(TipoEquipo.ARMADURA));
        assertSame(estado, j.getHabilidades().get(habilidad));
        assertTrue(estado.isEfectoTemporalActivo());
    }

    @Test
    void referenciaDuplicadaNoPermiteMonetizarElMismoObjetoDosVeces() {
        Jugador j = jugador();
        var arma = espada().crearArma();
        j.agregarEquipo(arma);
        j.agregarEquipo(arma);
        assertFalse(j.venderEquipo(arma, false, tienda));
        j.equipar(arma);
        assertFalse(j.venderEquipo(arma, true, tienda));
        assertFalse(j.venderEquipo(arma, false, tienda));
        assertEquals(15, j.getOro());
        assertEquals(List.of(arma, arma), j.getInventarioEquipo());
        assertSame(arma, j.getEquipoEquipado().get(TipoEquipo.ARMA));
    }
}
