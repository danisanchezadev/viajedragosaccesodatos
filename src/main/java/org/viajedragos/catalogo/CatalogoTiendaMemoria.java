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
                "botas_ligeras", 8, "anillo_sencillo", 10, "amuleto_sencillo", 10, "armadura_reforzada", 35);
        for (var equipo : equipos) {
            Integer precio = precios.get(equipo.id());
            if (precio != null) ofertas.add(new ProductoTienda(equipo, precio));
        }
        var preciosArmas = java.util.Map.of("espada_acero", 35, "espada_forja", 50);
        for (var arma : armas) {
            Integer precio = preciosArmas.get(arma.id());
            if (precio != null) ofertas.add(new ProductoTienda(arma, precio));
        }
        // También valora el botín no comprable, sin inventar precios para IDs externos.
        var preciosNuevos = java.util.Map.ofEntries(
                java.util.Map.entry("casco_hierro", 20), java.util.Map.entry("guantes_reforzados", 24),
                java.util.Map.entry("botas_marcha", 22), java.util.Map.entry("anillo_vigilancia", 24),
                java.util.Map.entry("amuleto_vigor", 25), java.util.Map.entry("armadura_acero", 50),
                java.util.Map.entry("casco_acero", 40), java.util.Map.entry("guantes_acero", 45),
                java.util.Map.entry("botas_veloces", 45), java.util.Map.entry("anillo_reflejos", 45),
                java.util.Map.entry("amuleto_vital", 50));
        for (var equipo : equipos) {
            Integer precio = preciosNuevos.get(equipo.id());
            if (precio != null) ofertas.add(new ProductoTienda(equipo, precio));
        }
        return List.copyOf(ofertas);
    }

    static java.util.Map<Integer, List<String>> asociaciones(List<ProductoTienda> productos) {
        var equipo = java.util.Map.of(
                1, List.of("espada_desgastada", "armadura_cuero", "casco_cuero"),
                2, List.of("espada_hierro", "guantes_cuero", "botas_ligeras", "casco_hierro", "botas_marcha"),
                3, List.of("espada_acero", "armadura_reforzada", "casco_hierro", "botas_marcha", "anillo_vigilancia", "amuleto_vigor"),
                4, List.of("espada_acero", "armadura_reforzada", "botas_marcha", "anillo_vigilancia", "amuleto_vigor", "armadura_acero", "casco_acero"),
                5, List.of("espada_forja", "armadura_acero", "casco_acero", "guantes_acero", "botas_veloces", "anillo_reflejos", "amuleto_vital"));
        var asociaciones = new java.util.HashMap<Integer, List<String>>();
        for (int planta = 1; planta <= 5; planta++) {
            var idsEquipo = equipo.get(planta);
            int numero = planta;
            asociaciones.put(planta, productos.stream().filter(p -> p.pocion() != null
                    ? numero > 1 || p.pocion().tipo() == TipoPocion.PEQUENA : idsEquipo.contains(p.id()))
                    .map(ProductoTienda::id).toList());
        }
        return java.util.Map.copyOf(asociaciones);
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
