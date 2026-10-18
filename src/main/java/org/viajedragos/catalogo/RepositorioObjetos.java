package org.viajedragos.catalogo;

import org.viajedragos.modelo.DefinicionArma;
import org.viajedragos.modelo.DefinicionPocion;
import java.util.List;

public interface RepositorioObjetos {
    List<DefinicionArma> obtenerArmas();
    List<DefinicionPocion> obtenerPociones();
}
