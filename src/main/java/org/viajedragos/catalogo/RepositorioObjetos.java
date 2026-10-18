package org.viajedragos.catalogo;

import org.viajedragos.modelo.DefinicionArma;
import org.viajedragos.modelo.DefinicionPocion;
import org.viajedragos.modelo.DefinicionEquipo;
import java.util.List;

public interface RepositorioObjetos {
    List<DefinicionArma> obtenerArmas();
    List<DefinicionPocion> obtenerPociones();
    // Definiciones adicionales a obtenerArmas(), sin repetir sus identificadores.
    default List<DefinicionEquipo> obtenerEquipamiento() { return List.of(); }
}
