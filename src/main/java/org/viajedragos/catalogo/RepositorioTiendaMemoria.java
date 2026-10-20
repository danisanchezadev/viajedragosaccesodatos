package org.viajedragos.catalogo;

import org.viajedragos.modelo.DefinicionArma;
import org.viajedragos.modelo.DefinicionPocion;
import org.viajedragos.modelo.ProductoTienda;
import org.viajedragos.modelo.DefinicionEquipo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RepositorioTiendaMemoria implements RepositorioTienda {
    private final List<ProductoTienda> productos;
    private final Map<String, ProductoTienda> porId;
    private final Map<Integer, List<ProductoTienda>> porPlanta;

    public RepositorioTiendaMemoria() {
        this(new RepositorioObjetosMemoria());
    }

    public RepositorioTiendaMemoria(RepositorioObjetos objetos) {
        this(objetos.obtenerArmas(), objetos.obtenerPociones(),
                objetos.obtenerEquipamiento().toArray(DefinicionEquipo[]::new));
    }

    // Permite reutilizar las definiciones ya cargadas por la aventura, sin releer el catálogo.
    public RepositorioTiendaMemoria(List<DefinicionArma> armas, List<DefinicionPocion> pociones) {
        this(CatalogoTiendaMemoria.productos(armas, pociones), armas, pociones, List.of(), true);
    }

    public RepositorioTiendaMemoria(List<ProductoTienda> ofertas,
            List<DefinicionArma> armas, List<DefinicionPocion> pociones) {
        this(ofertas, armas, pociones, List.of());
    }

    // El array permite conservar el constructor antiguo de tres listas.
    public RepositorioTiendaMemoria(List<DefinicionArma> armas, List<DefinicionPocion> pociones,
                                   DefinicionEquipo... equipos) {
        this(CatalogoTiendaMemoria.productos(armas, pociones, List.of(equipos)), armas, pociones, List.of(equipos), true);
    }

    public RepositorioTiendaMemoria(List<ProductoTienda> ofertas,
            List<DefinicionArma> armas, List<DefinicionPocion> pociones, List<DefinicionEquipo> equipos) {
        this(ofertas, armas, pociones, equipos, false);
    }

    private RepositorioTiendaMemoria(List<ProductoTienda> ofertas,
            List<DefinicionArma> armas, List<DefinicionPocion> pociones, List<DefinicionEquipo> equipos, boolean distribuir) {
        this(ofertas, armas, pociones, equipos, distribuir ? CatalogoTiendaMemoria.asociaciones(ofertas) : Map.of());
    }

    /** Un mapa vacío conserva las ofertas en todas las plantas; un mapa explícito admite tiendas vacías. */
    public RepositorioTiendaMemoria(List<ProductoTienda> ofertas,
            List<DefinicionArma> armas, List<DefinicionPocion> pociones, List<DefinicionEquipo> equipos,
            Map<Integer, List<String>> asociaciones) {
        productos = RepositorioTienda.validarProductos(ofertas, armas, pociones, equipos);
        var indice = new LinkedHashMap<String, ProductoTienda>();
        productos.forEach(producto -> indice.put(producto.id(), producto));
        porId = java.util.Collections.unmodifiableMap(indice);
        var plantas = new LinkedHashMap<Integer, List<ProductoTienda>>();
        for (var entrada : asociaciones.entrySet()) {
            org.viajedragos.modelo.Planta.porNumero(entrada.getKey());
            var ids = List.copyOf(entrada.getValue());
            if (ids.stream().distinct().count() != ids.size() || !porId.keySet().containsAll(ids))
                throw new IllegalArgumentException("Asociación comercial duplicada o desconocida");
        }
        for (int planta = 1; planta <= 5; planta++) {
            plantas.put(planta, asociaciones.isEmpty() ? productos
                    : asociaciones.getOrDefault(planta, List.of()).stream().map(porId::get).toList());
        }
        porPlanta = Map.copyOf(plantas);
    }

    @Override
    public List<ProductoTienda> obtenerTodos() { return productos; }

    @Override
    public Optional<ProductoTienda> buscarPorId(String id) { return Optional.ofNullable(porId.get(id)); }

    @Override
    public List<ProductoTienda> obtenerPorPlanta(int planta) {
        org.viajedragos.modelo.Planta.porNumero(planta);
        return porPlanta.get(planta);
    }
}
