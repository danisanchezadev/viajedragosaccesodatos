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

    public RepositorioTiendaMemoria() {
        this(new RepositorioObjetosMemoria());
    }

    public RepositorioTiendaMemoria(RepositorioObjetos objetos) {
        this(objetos.obtenerArmas(), objetos.obtenerPociones(),
                objetos.obtenerEquipamiento().toArray(DefinicionEquipo[]::new));
    }

    // Permite reutilizar las definiciones ya cargadas por la aventura, sin releer el catálogo.
    public RepositorioTiendaMemoria(List<DefinicionArma> armas, List<DefinicionPocion> pociones) {
        this(CatalogoTiendaMemoria.productos(armas, pociones), armas, pociones);
    }

    public RepositorioTiendaMemoria(List<ProductoTienda> ofertas,
            List<DefinicionArma> armas, List<DefinicionPocion> pociones) {
        this(ofertas, armas, pociones, List.of());
    }

    // El array permite conservar el constructor antiguo de tres listas.
    public RepositorioTiendaMemoria(List<DefinicionArma> armas, List<DefinicionPocion> pociones,
                                   DefinicionEquipo... equipos) {
        this(CatalogoTiendaMemoria.productos(armas, pociones, List.of(equipos)), armas, pociones, List.of(equipos));
    }

    public RepositorioTiendaMemoria(List<ProductoTienda> ofertas,
            List<DefinicionArma> armas, List<DefinicionPocion> pociones, List<DefinicionEquipo> equipos) {
        productos = RepositorioTienda.validarProductos(ofertas, armas, pociones, equipos);
        var indice = new LinkedHashMap<String, ProductoTienda>();
        productos.forEach(producto -> indice.put(producto.id(), producto));
        porId = java.util.Collections.unmodifiableMap(indice);
    }

    @Override
    public List<ProductoTienda> obtenerTodos() { return productos; }

    @Override
    public Optional<ProductoTienda> buscarPorId(String id) { return Optional.ofNullable(porId.get(id)); }
}
