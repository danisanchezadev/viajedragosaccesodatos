package org.viajedragos.catalogo;

import org.viajedragos.modelo.DefinicionArma;
import org.viajedragos.modelo.DefinicionPocion;
import org.viajedragos.modelo.ProductoTienda;

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
        this(objetos.obtenerArmas(), objetos.obtenerPociones());
    }

    // Permite reutilizar las definiciones ya cargadas por la aventura, sin releer el catálogo.
    public RepositorioTiendaMemoria(List<DefinicionArma> armas, List<DefinicionPocion> pociones) {
        this(CatalogoTiendaMemoria.productos(armas, pociones), armas, pociones);
    }

    public RepositorioTiendaMemoria(List<ProductoTienda> ofertas,
            List<DefinicionArma> armas, List<DefinicionPocion> pociones) {
        productos = RepositorioTienda.validarProductos(ofertas, armas, pociones);
        var indice = new LinkedHashMap<String, ProductoTienda>();
        productos.forEach(producto -> indice.put(producto.id(), producto));
        porId = java.util.Collections.unmodifiableMap(indice);
    }

    @Override
    public List<ProductoTienda> obtenerTodos() { return productos; }

    @Override
    public Optional<ProductoTienda> buscarPorId(String id) { return Optional.ofNullable(porId.get(id)); }
}
