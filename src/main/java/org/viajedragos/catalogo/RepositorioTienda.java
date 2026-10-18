package org.viajedragos.catalogo;

import org.viajedragos.modelo.ProductoTienda;
import org.viajedragos.modelo.DefinicionArma;
import org.viajedragos.modelo.DefinicionPocion;
import org.viajedragos.modelo.DefinicionEquipo;
import java.util.List;
import java.util.Optional;

public interface RepositorioTienda {
    List<ProductoTienda> obtenerTodos();

    default Optional<ProductoTienda> buscarPorId(String id) {
        return obtenerTodos().stream().filter(producto -> producto.id().equals(id)).findFirst();
    }

    // Común a cualquier origen. Un catálogo comercial vacío es válido.
    static List<ProductoTienda> validarProductos(List<ProductoTienda> productos,
            List<DefinicionArma> armas, List<DefinicionPocion> pociones) {
        return validarProductos(productos, armas, pociones, List.of());
    }

    static List<ProductoTienda> validarProductos(List<ProductoTienda> productos,
            List<DefinicionArma> armas, List<DefinicionPocion> pociones, List<DefinicionEquipo> equipos) {
        var definiciones = new java.util.HashMap<String, Object>();
        for (var arma : List.copyOf(armas)) {
            if (definiciones.putIfAbsent(arma.id(), arma.comoEquipo()) != null)
                throw new IllegalArgumentException("Identificador de objeto repetido");
        }
        for (var equipo : List.copyOf(equipos)) {
            if (definiciones.putIfAbsent(equipo.id(), equipo) != null)
                throw new IllegalArgumentException("Identificador de objeto repetido");
        }
        var tipos = java.util.EnumSet.noneOf(org.viajedragos.modelo.TipoPocion.class);
        for (var pocion : List.copyOf(pociones)) {
            if (!tipos.add(pocion.tipo()) || definiciones.putIfAbsent(pocion.id(), pocion) != null)
                throw new IllegalArgumentException("Identificador o tipo de poción repetido");
        }
        var copia = List.copyOf(productos);
        var ids = new java.util.HashSet<String>();
        for (var producto : copia) {
            Object definicion = producto.equipo() != null ? producto.equipo() : producto.pocion();
            if (!ids.add(producto.id()) || !definicion.equals(definiciones.get(producto.id())))
                throw new IllegalArgumentException("Producto duplicado o sin definición de objeto coincidente");
        }
        return copia;
    }
}
