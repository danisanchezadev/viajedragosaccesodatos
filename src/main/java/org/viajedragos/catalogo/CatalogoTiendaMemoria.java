package org.viajedragos.catalogo;

import org.viajedragos.modelo.*;
import java.util.List;

/** Ofertas predeterminadas, separadas de la indexación y validación del repositorio. */
final class CatalogoTiendaMemoria {
    private CatalogoTiendaMemoria() { }

    static List<ProductoTienda> productos(List<DefinicionArma> armas, List<DefinicionPocion> pociones) {
        return List.of(
                new ProductoTienda(pocion(pociones, TipoPocion.PEQUENA), 5),
                new ProductoTienda(pocion(pociones, TipoPocion.GRANDE), 10),
                new ProductoTienda(arma(armas, DefinicionArma.ID_ESPADA_DESGASTADA), 12),
                new ProductoTienda(arma(armas, DefinicionArma.ID_ESPADA_HIERRO), 20));
    }

    static List<ProductoTienda> productos(List<DefinicionArma> armas, List<DefinicionPocion> pociones,
                                         List<DefinicionEquipo> equipos) {
        var ofertas = new java.util.ArrayList<>(productos(armas, pociones));
        var precios = java.util.Map.of("armadura_cuero", 8, "casco_cuero", 8, "guantes_cuero", 10,
                "botas_ligeras", 8, "anillo_sencillo", 10, "amuleto_sencillo", 10);
        for (var equipo : equipos) {
            Integer precio = precios.get(equipo.id());
            if (precio != null) ofertas.add(new ProductoTienda(equipo, precio));
        }
        return List.copyOf(ofertas);
    }

    private static DefinicionArma arma(List<DefinicionArma> armas, String id) {
        return armas.stream().filter(a -> a.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Falta el arma " + id));
    }

    private static DefinicionPocion pocion(List<DefinicionPocion> pociones, TipoPocion tipo) {
        return pociones.stream().filter(p -> p.tipo() == tipo).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Falta la poción " + tipo));
    }
}
