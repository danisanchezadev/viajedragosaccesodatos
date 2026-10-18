package org.viajedragos.catalogo;

import org.viajedragos.modelo.DefinicionArma;
import org.viajedragos.modelo.DefinicionPocion;
import org.viajedragos.modelo.ProductoTienda;
import org.viajedragos.modelo.TipoPocion;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        var ids = new HashSet<String>();
        var armasPorId = new LinkedHashMap<String, DefinicionArma>();
        for (DefinicionArma arma : List.copyOf(armas)) {
            if (!ids.add(arma.id())) throw new IllegalArgumentException("Identificador de objeto repetido");
            armasPorId.put(arma.id(), arma);
        }
        var porTipo = new EnumMap<TipoPocion, DefinicionPocion>(TipoPocion.class);
        for (DefinicionPocion pocion : List.copyOf(pociones)) {
            if (!ids.add(pocion.id()) || porTipo.putIfAbsent(pocion.tipo(), pocion) != null) {
                throw new IllegalArgumentException("Identificador o tipo de poción repetido");
            }
        }
        productos = List.of(
                new ProductoTienda(Objects.requireNonNull(porTipo.get(TipoPocion.PEQUENA)), 5),
                new ProductoTienda(Objects.requireNonNull(porTipo.get(TipoPocion.GRANDE)), 10),
                new ProductoTienda(Objects.requireNonNull(armasPorId.get(DefinicionArma.ID_ESPADA_DESGASTADA)), 12),
                new ProductoTienda(Objects.requireNonNull(armasPorId.get(DefinicionArma.ID_ESPADA_HIERRO)), 20));
        var indice = new LinkedHashMap<String, ProductoTienda>();
        productos.forEach(producto -> indice.put(producto.id(), producto));
        porId = java.util.Collections.unmodifiableMap(indice);
    }

    @Override
    public List<ProductoTienda> obtenerTodos() { return productos; }

    @Override
    public Optional<ProductoTienda> buscarPorId(String id) { return Optional.ofNullable(porId.get(id)); }
}
