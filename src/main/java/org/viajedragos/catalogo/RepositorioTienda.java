package org.viajedragos.catalogo;

import org.viajedragos.modelo.ProductoTienda;
import java.util.List;
import java.util.Optional;

public interface RepositorioTienda {
    List<ProductoTienda> obtenerTodos();

    default Optional<ProductoTienda> buscarPorId(String id) {
        return obtenerTodos().stream().filter(producto -> producto.id().equals(id)).findFirst();
    }
}
