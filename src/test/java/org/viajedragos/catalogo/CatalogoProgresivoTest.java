package org.viajedragos.catalogo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.viajedragos.arma.*;
import org.viajedragos.modelo.*;
import java.util.*;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

class CatalogoProgresivoTest {
    private final RepositorioObjetosMemoria objetos = new RepositorioObjetosMemoria();
    private final RepositorioTiendaMemoria tienda = new RepositorioTiendaMemoria(objetos);

    @ParameterizedTest @EnumSource(TipoEquipo.class)
    void cadaRanuraTieneTresCalidadesYLasPiezasSePuedenEquiparYVender(TipoEquipo tipo) {
        var entradas = objetos.obtenerBotinEquipamiento().stream().filter(d -> d.equipo().tipo() == tipo).toList();
        assertEquals(Set.of(CalidadEquipo.values()), entradas.stream().map(DefinicionBotinEquipo::calidad).collect(Collectors.toSet()));
        for (var d : entradas) {
            var j = new Jugador("Prueba", "", 6, 2, 3, 2, 2, 48, 48);
            var pieza = d.equipo().crearEquipo(0);
            j.agregarEquipo(pieza);
            assertTrue(j.equiparDesdeInventario(tipo, 0));
            assertSame(pieza, j.getEquipoEquipado().get(tipo));
            for (var a : d.equipo().bonificaciones().entrySet()) {
                int base = a.getKey() == Atributo.VIDA ? 48 : j.getAtributoBase(a.getKey());
                int efectivo = a.getKey() == Atributo.VIDA ? j.getVidaMaxima() : j.getAtributo(a.getKey());
                assertEquals(base + a.getValue(), efectivo);
            }
            assertEquals(48, j.getVida(), "Aumentar el máximo no cura");
            int oro = j.getOro();
            assertTrue(j.venderEquipo(pieza, true, tienda));
            assertEquals(oro + tienda.buscarPorId(d.equipo().id()).orElseThrow().precioVenta(), j.getOro());
            assertTrue(j.getEquipoEquipado().isEmpty()); assertTrue(j.getInventarioEquipo().isEmpty());
            assertEquals(6, j.getFuerza()); assertEquals(48, j.getVidaMaxima());
            assertFalse(j.venderEquipo(pieza, true, tienda));
        }
    }

    @ParameterizedTest @ValueSource(ints = {1, 2, 3, 4, 5})
    void calidadesYOfertasRespetanLaPlantaSinDuplicarLaValoracion(int planta) {
        var permitidas = switch (planta) {
            case 1 -> Set.of(CalidadEquipo.BASICO);
            case 2 -> Set.of(CalidadEquipo.BASICO, CalidadEquipo.MEJORADO);
            case 3 -> Set.of(CalidadEquipo.MEJORADO);
            case 4 -> Set.of(CalidadEquipo.MEJORADO, CalidadEquipo.SUPERIOR);
            default -> Set.of(CalidadEquipo.SUPERIOR);
        };
        var botin = objetos.obtenerBotinEquipamiento();
        var disponibles = botin.stream().filter(d -> d.disponibleEn(planta)).toList();
        assertEquals(permitidas, disponibles.stream().map(DefinicionBotinEquipo::calidad).collect(Collectors.toSet()));
        assertEquals(Set.of(TipoEquipo.values()), disponibles.stream().map(d -> d.equipo().tipo()).collect(Collectors.toSet()));
        assertEquals(new int[]{7, 13, 8, 15, 7}[planta - 1], disponibles.size());
        Set<String> ofertasEsperadas = switch (planta) {
            case 1 -> Set.of("espada_desgastada", "armadura_cuero", "casco_cuero");
            case 2 -> Set.of("espada_hierro", "guantes_cuero", "botas_ligeras", "casco_hierro", "botas_marcha");
            case 3 -> Set.of("espada_acero", "armadura_reforzada", "casco_hierro", "botas_marcha", "anillo_vigilancia", "amuleto_vigor");
            case 4 -> Set.of("espada_acero", "armadura_reforzada", "botas_marcha", "anillo_vigilancia", "amuleto_vigor", "armadura_acero", "casco_acero");
            default -> Set.of("espada_forja", "armadura_acero", "casco_acero", "guantes_acero", "botas_veloces", "anillo_reflejos", "amuleto_vital");
        };
        assertEquals(ofertasEsperadas, tienda.obtenerPorPlanta(planta).stream().filter(p -> p.equipo() != null)
                .map(ProductoTienda::id).collect(Collectors.toSet()));
        for (var p : tienda.obtenerPorPlanta(planta)) {
            assertSame(p, tienda.buscarPorId(p.id()).orElseThrow());
            if (p.equipo() != null) assertTrue(botin.stream().anyMatch(d -> d.equipo().equals(p.equipo()) && permitidas.contains(d.calidad())));
        }
        for (var d : disponibles) assertEquals(d.equipo(), tienda.buscarPorId(d.equipo().id()).orElseThrow().equipo());
    }

    @ParameterizedTest @CsvSource({
            "espada_desgastada,FUERZA,1,12", "espada_hierro,FUERZA,2,20", "espada_acero,FUERZA,3,35", "espada_forja,FUERZA,4,50",
            "armadura_cuero,RESISTENCIA,1,8", "casco_cuero,RESISTENCIA,1,8", "guantes_cuero,FUERZA,1,10",
            "botas_ligeras,AGILIDAD,1,8", "anillo_sencillo,INICIATIVA,1,10", "amuleto_sencillo,VIDA,2,10", "armadura_reforzada,RESISTENCIA,3,35"})
    void onceDefinicionesPreviasConservanTodosSusBonosYPrecios(String id, Atributo atributo, int bono, int precio) {
        var p = tienda.buscarPorId(id).orElseThrow();
        assertEquals(Map.of(atributo, bono), p.equipo().bonificaciones());
        assertEquals(precio, p.precio()); assertEquals(precio / 2, p.precioVenta());
    }

    @ParameterizedTest @CsvSource({
            "casco_hierro,RESISTENCIA,2,,0,20", "guantes_reforzados,FUERZA,2,,0,24",
            "botas_marcha,AGILIDAD,1,VELOCIDAD,1,22", "anillo_vigilancia,INICIATIVA,2,,0,24", "amuleto_vigor,VIDA,6,,0,25",
            "armadura_acero,RESISTENCIA,4,,0,50", "casco_acero,RESISTENCIA,3,,0,40", "guantes_acero,FUERZA,3,,0,45",
            "botas_veloces,AGILIDAD,2,VELOCIDAD,2,45", "anillo_reflejos,INICIATIVA,3,,0,45", "amuleto_vital,VIDA,10,,0,50"})
    void nuevasDefinicionesTienenBonosProvisionalesYPreciosCentralizados(String id, Atributo atributo, int bono,
                                                                    Atributo segundo, int adicional, int precio) {
        var esperado = new EnumMap<Atributo, Integer>(Atributo.class); esperado.put(atributo, bono);
        if (segundo != null) esperado.put(segundo, adicional);
        var p = tienda.buscarPorId(id).orElseThrow();
        assertEquals(esperado, p.equipo().bonificaciones()); assertEquals(precio, p.precio()); assertEquals(precio / 2, p.precioVenta());
        var a = p.equipo().crearEquipo(1); var b = p.equipo().crearEquipo(3);
        assertNotSame(a, b); assertEquals(esperado, a.getBonificaciones()); assertEquals(esperado, b.getBonificaciones());
        assertEquals(1, a.getMejorasDisponibles()); assertEquals(3, b.getMejorasDisponibles());
        assertTrue(a.getHabilidadesOtorgadas().isEmpty());
    }

    @Test void catalogoUnicoConReferenciasValidasIncluyeValoracionDeBotinExclusivo() {
        var canonicas = new ArrayList<>(objetos.obtenerEquipamiento());
        objetos.obtenerArmas().forEach(a -> canonicas.add(a.comoEquipo()));
        assertEquals(22, canonicas.size()); assertEquals(22, canonicas.stream().map(DefinicionEquipo::id).distinct().count());
        var botin = RepositorioObjetos.validarBotin(objetos.obtenerBotinEquipamiento(), objetos.obtenerArmas(), objetos.obtenerEquipamiento());
        assertEquals(22, botin.size()); assertEquals(24, tienda.obtenerTodos().size());
        for (var d : botin) assertTrue(canonicas.contains(d.equipo()));
        for (int planta = 1; planta <= 5; planta++) {
            assertTrue(tienda.obtenerPorPlanta(planta).stream().noneMatch(p -> p.id().equals("guantes_reforzados")));
            var j = new Jugador(); var pieza = tienda.buscarPorId("guantes_reforzados").orElseThrow().equipo().crearEquipo(1);
            j.agregarEquipo(pieza); int oro = j.getOro();
            assertEquals(ResultadoCompra.PRODUCTO_INEXISTENTE, j.comprar(pieza.getIdDefinicion(), tienda.paraPlanta(planta)));
            assertTrue(j.venderEquipo(pieza, false, tienda));
            assertEquals(oro + 12, j.getOro()); assertTrue(j.getInventarioEquipo().isEmpty());
        }
    }
}
